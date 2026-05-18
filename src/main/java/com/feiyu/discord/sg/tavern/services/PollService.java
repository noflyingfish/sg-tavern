package com.feiyu.discord.sg.tavern.services;

import com.feiyu.discord.sg.tavern.entities.PollEntity;
import com.feiyu.discord.sg.tavern.entities.PollVoteEntity;
import com.feiyu.discord.sg.tavern.exceptions.PollOperationException;
import com.feiyu.discord.sg.tavern.models.poll.ActivePoll;
import com.feiyu.discord.sg.tavern.models.poll.ClosedPoll;
import com.feiyu.discord.sg.tavern.models.poll.DraftPoll;
import com.feiyu.discord.sg.tavern.models.poll.PartialDraftPoll;
import com.feiyu.discord.sg.tavern.models.poll.PollOptionResult;
import com.feiyu.discord.sg.tavern.repositories.PollRepository;
import com.feiyu.discord.sg.tavern.repositories.PollVoteRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Slf4j
@Service
@AllArgsConstructor
public class PollService {

    private static final String POLL_STATUS_ACTIVE = "ACTIVE";
    private static final String POLL_STATUS_CLOSED = "CLOSED";

    private final PollRepository pollRepository;
    private final PollVoteRepository pollVoteRepository;

    private final ConcurrentMap<String, PartialDraftPoll> partialDrafts = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, DraftPoll> drafts = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Set<Integer>> voteSelections = new ConcurrentHashMap<>();

    public PartialDraftPoll createPartialDraft(String draftId,
                                               String creatorId,
                                               String creatorMention,
                                               String question,
                                               String maxSelectionValue,
                                               String durationOption) {
        int maxSelection;
        try {
            maxSelection = Integer.parseInt(maxSelectionValue);
        } catch (NumberFormatException ex) {
            throw new PollOperationException("Max selections must be a number.");
        }

        PartialDraftPoll draft = PartialDraftPoll.builder()
                .draftId(draftId)
                .creatorId(creatorId)
                .creatorMention(creatorMention)
                .question(question)
                .maxSelection(maxSelection)
                .durationOption(durationOption)
                .build();

        partialDrafts.put(draft.getDraftId(), draft);
        return draft;
    }

    public PartialDraftPoll getPartialDraftOrThrow(String draftId) {
        PartialDraftPoll draft = partialDrafts.get(draftId);
        if (draft == null) {
            throw new PollOperationException("This draft is no longer available. Start again with /pollanonymous.");
        }
        return draft;
    }

    public DraftPoll createDraft(String draftId, List<String> options) {
        PartialDraftPoll partialDraft = getPartialDraftOrThrow(draftId);

        List<String> sanitizedOptions = options.stream()
                .filter(Objects::nonNull)
                .toList();

        DraftPoll draft = DraftPoll.builder()
                .draftId(draftId)
                .creatorId(partialDraft.getCreatorId())
                .creatorTag(partialDraft.getCreatorMention())
                .question(partialDraft.getQuestion())
                .options(List.copyOf(sanitizedOptions))
                .maxSelection(partialDraft.getMaxSelection())
                .durationOption(partialDraft.getDurationOption())
                .build();

        partialDrafts.remove(draftId);
        drafts.put(draft.getDraftId(), draft);
        return draft;
    }

    public void cancelDraft(String draftId) {
        partialDrafts.remove(draftId);
        drafts.remove(draftId);
    }

    @Transactional
    public ActivePoll publishDraft(String draftId, String channelId) {
        DraftPoll draft = drafts.remove(draftId);
        if (draft == null) {
            throw new PollOperationException("This draft is no longer available. Start again with /pollanonymous.");
        }
        
        PollEntity pollEntity = PollEntity.builder()
                .channelId(channelId)
                .creatorUserId(draft.getCreatorId())
                .question(draft.getQuestion())
                .maxSelection(draft.getMaxSelection())
                .durationOption(draft.getDurationOption())
                .option1(getOption(draft.getOptions(), 0))
                .option2(getOption(draft.getOptions(), 1))
                .option3(getOption(draft.getOptions(), 2))
                .option4(getOption(draft.getOptions(), 3))
                .option5(getOption(draft.getOptions(), 4))
                .status(POLL_STATUS_ACTIVE)
                .createdOn(LocalDateTime.now())
                .publishedOn(LocalDateTime.now())
                .closesOn(resolveClosesOn(LocalDateTime.now(), draft.getDurationOption()))
                .build();

        PollEntity savedPoll = pollRepository.save(pollEntity);
        log.info("Poll Saved to DB : {}", pollEntity);
        
        return mapToActivePoll(savedPoll, draft.getCreatorTag());
    }

    @Transactional
    public void updatePublishedMessageId(String pollId, String messageId) {
        PollEntity pollEntity = getPollEntityOrThrow(parsePollId(pollId));
        pollEntity.setMessageId(messageId);
        pollRepository.save(pollEntity);
    }

    public ActivePoll openVote(String pollId, String userId) {
        ActivePoll poll = getActivePollOrThrow(pollId);
        voteSelections.computeIfAbsent(buildVoteKey(pollId, userId), key -> loadPersistedSelections(parsePollId(pollId), userId));
        return poll;
    }

    public ActivePoll getActivePollOrThrow(String pollId) {
        Long parsedPollId = parsePollId(pollId);
        PollEntity pollEntity = pollRepository.findByIdAndStatus(parsedPollId, POLL_STATUS_ACTIVE)
                .orElseThrow(() -> new PollOperationException("This poll is no longer available."));
        return mapToActivePoll(pollEntity, null);
    }

    public Set<Integer> getVoteSelections(String pollId, String userId) {
        getActivePollOrThrow(pollId);
        return new LinkedHashSet<>(voteSelections.computeIfAbsent(buildVoteKey(pollId, userId), key -> loadPersistedSelections(parsePollId(pollId), userId)));
    }

    public Set<Integer> toggleVoteSelection(String pollId, String userId, int optionIndex) {
        ActivePoll poll = getActivePollOrThrow(pollId);
        if (optionIndex < 1 || optionIndex > poll.getOptions().size()) {
            throw new PollOperationException("Invalid vote option.");
        }

        Set<Integer> selected = voteSelections.computeIfAbsent(buildVoteKey(pollId, userId), key -> loadPersistedSelections(parsePollId(pollId), userId));
        if (selected.contains(optionIndex)) {
            selected.remove(optionIndex);
        } else {
            if (selected.size() >= poll.getMaxSelection()) {
                throw new PollOperationException("You can only choose up to " + poll.getMaxSelection() + " option(s).");
            }
            selected.add(optionIndex);
        }

        return new LinkedHashSet<>(selected);
    }

    @Transactional
    public List<String> submitVote(String pollId, String userId) {
        ActivePoll poll = getActivePollOrThrow(pollId);
        Long parsedPollId = parsePollId(pollId);
        Set<Integer> selected = voteSelections.computeIfAbsent(buildVoteKey(pollId, userId), key -> loadPersistedSelections(parsedPollId, userId));

        pollVoteRepository.deleteAllByPollIdAndVoterUserId(parsedPollId, userId);
        pollVoteRepository.flush();
        
        for (Integer optionNumber : selected.stream().sorted().toList()) {
            PollVoteEntity voteEntity = PollVoteEntity.builder()
                    .pollId(parsedPollId)
                    .voterUserId(userId)
                    .optionNumber(optionNumber)
                    .createdOn(LocalDateTime.now())
                    .build();
            pollVoteRepository.save(voteEntity);
        }

        List<String> selectedOptions = selected.stream()
                .sorted()
                .map(index -> poll.getOptions().get(index - 1))
                .toList();

        log.info("Poll vote captured - pollId={}, userId={}, selectedOptionNumbers={}, selectedOptions={}",
                pollId,
                userId,
                selected.stream().sorted().toList(),
                selectedOptions);

        return selectedOptions;
    }

    public int currentVoterCount(String pollId) {
        return pollVoteRepository.countDistinctVoterUserIdByPollId(parsePollId(pollId));
    }

    public List<ClosedPoll> processActivePollStatuses() {
        LocalDateTime now = LocalDateTime.now();
        List<PollEntity> overdueActivePolls = pollRepository.findAllByStatusAndClosesOnLessThanEqual(POLL_STATUS_ACTIVE, now);

        log.info("Overdue active polls found: {}", overdueActivePolls.size());
        return overdueActivePolls.stream()
                .map(this::mapToClosedPoll)
                .toList();
    }

    @Transactional
    public void closePoll(String pollId) {
        PollEntity pollEntity = getPollEntityOrThrow(parsePollId(pollId));
        pollEntity.setStatus(POLL_STATUS_CLOSED);
        pollEntity.setClosedOn(LocalDateTime.now());
        pollRepository.save(pollEntity);
    }

    private Set<Integer> loadPersistedSelections(Long pollId, String userId) {
        return new LinkedHashSet<>(pollVoteRepository.findAllByPollIdAndVoterUserIdOrderByOptionNumberAsc(pollId, userId)
                .stream()
                .map(PollVoteEntity::getOptionNumber)
                .toList());
    }

    private PollEntity getPollEntityOrThrow(Long pollId) {
        return pollRepository.findById(pollId)
                .orElseThrow(() -> new PollOperationException("This poll is no longer available."));
    }

    private ActivePoll mapToActivePoll(PollEntity entity, String creatorTagOverride) {
        List<String> options = Stream.of(entity.getOption1(), entity.getOption2(), entity.getOption3(), entity.getOption4(), entity.getOption5())
                .filter(Objects::nonNull)
                .toList();

        return ActivePoll.builder()
                .pollId(String.valueOf(entity.getId()))
                .channelId(entity.getChannelId())
                .messageId(entity.getMessageId())
                .creatorId(entity.getCreatorUserId())
                .creatorTag(creatorTagOverride != null ? creatorTagOverride : "<@" + entity.getCreatorUserId() + ">")
                .question(entity.getQuestion())
                .options(options)
                .maxSelection(entity.getMaxSelection())
                .durationOption(entity.getDurationOption())
                .closesOn(entity.getClosesOn())
                .build();
    }

    private ClosedPoll mapToClosedPoll(PollEntity entity) {
        ActivePoll activePoll = mapToActivePoll(entity, null);
        List<PollVoteEntity> votes = pollVoteRepository.findAllByPollIdOrderByOptionNumberAsc(entity.getId());
        int totalVoters = pollVoteRepository.countDistinctVoterUserIdByPollId(entity.getId());

        int[] voteCounts = new int[activePoll.getOptions().size()];
        for (PollVoteEntity vote : votes) {
            int optionNumber = vote.getOptionNumber();
            if (optionNumber >= 1 && optionNumber <= voteCounts.length) {
                voteCounts[optionNumber - 1]++;
            }
        }

        List<PollOptionResult> optionResults = IntStream.range(0, activePoll.getOptions().size())
                .mapToObj(optionIndex -> buildOptionResult(activePoll, totalVoters, voteCounts, optionIndex))
                .toList();

        return ClosedPoll.builder()
                .pollId(activePoll.getPollId())
                .channelId(activePoll.getChannelId())
                .messageId(activePoll.getMessageId())
                .creatorId(activePoll.getCreatorId())
                .creatorTag(activePoll.getCreatorTag())
                .question(activePoll.getQuestion())
                .options(activePoll.getOptions())
                .maxSelection(activePoll.getMaxSelection())
                .durationOption(activePoll.getDurationOption())
                .closesOn(activePoll.getClosesOn())
                .totalVoters(totalVoters)
                .optionResults(optionResults)
                .build();
    }

    private PollOptionResult buildOptionResult(ActivePoll activePoll, int totalVoters, int[] voteCounts, int optionIndex) {
        int voteCount = voteCounts[optionIndex];
        int percentage = totalVoters == 0 ? 0 : (voteCount * 100) / totalVoters;
        return PollOptionResult.builder()
                .optionNumber(optionIndex + 1)
                .optionText(activePoll.getOptions().get(optionIndex))
                .voteCount(voteCount)
                .totalVoters(totalVoters)
                .percentage(percentage)
                .build();
    }

    private Long parsePollId(String pollId) {
        try {
            return Long.parseLong(pollId);
        } catch (NumberFormatException ex) {
            throw new PollOperationException("Invalid poll identifier.");
        }
    }

    private String buildVoteKey(String pollId, String userId) {
        return pollId + ":" + userId;
    }

    private String getOption(List<String> options, int index) {
        return index < options.size() ? options.get(index) : null;
    }

    private LocalDateTime resolveClosesOn(LocalDateTime publishedOn, String durationOption) {
        int daysToAdd = switch (durationOption) {
            case "TODAY_EOD" -> 0;
            case "PLUS_1_DAY_EOD" -> 1;
            case "PLUS_3_DAYS_EOD" -> 3;
            case "PLUS_5_DAYS_EOD" -> 5;
            default -> throw new PollOperationException("Invalid poll duration selected.");
        };

        LocalDate closeDate = publishedOn.toLocalDate().plusDays(daysToAdd);
        return LocalDateTime.of(closeDate, LocalTime.of(23, 59, 59));
    }
}
