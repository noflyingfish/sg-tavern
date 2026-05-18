package com.feiyu.discord.sg.tavern.commands;

import com.feiyu.discord.sg.tavern.exceptions.PollOperationException;
import com.feiyu.discord.sg.tavern.models.poll.ActivePoll;
import com.feiyu.discord.sg.tavern.models.poll.ClosedPoll;
import com.feiyu.discord.sg.tavern.models.poll.DraftPoll;
import com.feiyu.discord.sg.tavern.models.poll.PartialDraftPoll;
import com.feiyu.discord.sg.tavern.models.poll.PollOptionResult;
import com.feiyu.discord.sg.tavern.services.PollService;
import lombok.AllArgsConstructor;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import net.dv8tion.jda.api.modals.Modal;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;

@Component
@AllArgsConstructor
public class AnonymousPollCommand extends ListenerAdapter {
    
    private static final String COMMAND_NAME = "pollanonymous";
    private static final String CREATE_STEP_ONE_MODAL_ID = "anonpoll:create:step1";
    private static final String CREATE_STEP_TWO_MODAL_PREFIX = "anonpoll:create:step2:";
    private static final String CONTINUE_BUTTON_PREFIX = "anonpoll:continue:";
    private static final String PUBLISH_BUTTON_PREFIX = "anonpoll:publish:";
    private static final String CANCEL_BUTTON_PREFIX = "anonpoll:cancel:";
    private static final String VOTE_BUTTON_PREFIX = "anonpoll:vote:";
    private static final String TOGGLE_BUTTON_PREFIX = "anonpoll:toggle:";
    private static final String SUBMIT_BUTTON_PREFIX = "anonpoll:submit:";
    private static final String CLOSED_BUTTON_ID = "anonpoll:closed";
    
    private static final String QUESTION_FIELD = "question";
    private static final String MAX_SELECTION_FIELD = "maxSelections";
    private static final String DURATION_FIELD = "duration";
    private static final String OPTION_FIELD_PREFIX = "option";
    
    private final PollService pollService;
    
    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (!COMMAND_NAME.equals(event.getName())) {
            return;
        }
        event.replyModal(buildStepOneModal()).queue();
    }
    
    @Override
    public void onModalInteraction(@NotNull ModalInteractionEvent event) {
        String modalId = event.getModalId();
        
        if (CREATE_STEP_ONE_MODAL_ID.equals(modalId)) {
            // step 1 create modal
            handleStepOneModal(event);
            return;
        }
        
        if (modalId.startsWith(CREATE_STEP_TWO_MODAL_PREFIX)) {
            // step 2 create modal
            handleStepTwoModal(event, modalId);
        }
    }
    
    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        String componentId = event.getComponentId();
        
        if (componentId.startsWith(CONTINUE_BUTTON_PREFIX)) {
            // continue to step 2 modal create modal
            handleContinue(event, componentId.substring(CONTINUE_BUTTON_PREFIX.length()));
            return;
        }
        
        if (componentId.startsWith(CANCEL_BUTTON_PREFIX)) {
            // cancel after step 1 create is done
            handleCancel(event, componentId.substring(CANCEL_BUTTON_PREFIX.length()));
            return;
        }
        
        if (componentId.startsWith(PUBLISH_BUTTON_PREFIX)) {
            // publish poll button
            handlePublish(event, componentId.substring(PUBLISH_BUTTON_PREFIX.length()));
            return;
        }
        
        if (componentId.startsWith(VOTE_BUTTON_PREFIX)) {
            // users start vote
            handleVoteOpen(event, componentId.substring(VOTE_BUTTON_PREFIX.length()));
            return;
        }
        
        if (componentId.startsWith(TOGGLE_BUTTON_PREFIX)) {
            // users clicks on options
            handleToggle(event, componentId.substring(TOGGLE_BUTTON_PREFIX.length()));
            return;
        }
        
        if (componentId.startsWith(SUBMIT_BUTTON_PREFIX)) {
            // users submit vote
            handleSubmit(event, componentId.substring(SUBMIT_BUTTON_PREFIX.length()));
        }
    }
    
    private void handleContinue(ButtonInteractionEvent event, String draftId) {
        try {
            pollService.getPartialDraftOrThrow(draftId);
        } catch (PollOperationException ex) {
            event.reply(ex.getMessage())
                    .setEphemeral(true)
                    .queue();
            return;
        }
        
        event.replyModal(buildStepTwoModal(draftId)).queue();
    }
    
    private void handleCancel(ButtonInteractionEvent event, String draftId) {
        pollService.cancelDraft(draftId);
        event.editMessage("Poll creation cancelled.")
                .setComponents(List.of())
                .setEmbeds(List.of())
                .queue();
    }
    
    private void handlePublish(ButtonInteractionEvent event, String draftId) {
        ActivePoll poll;
        event.deferReply(true).queue();
        
        try {
            poll = pollService.publishDraft(
                    draftId,
                    event.getChannelId()
            );
        } catch (PollOperationException ex) {
            event.reply(ex.getMessage())
                    .setEphemeral(true)
                    .queue();
            return;
        }
        
        Message pollMessage = event.getChannel().sendMessageEmbeds(buildPublicPoll(poll))
                .addComponents(ActionRow.of(Button.primary(VOTE_BUTTON_PREFIX + poll.getPollId(), "Vote")))
                .complete();
        
        pollService.updatePublishedMessageId(poll.getPollId(), pollMessage.getId());
        
        event.getHook().sendMessage("Poll posted.")
                .queue();
    }
    
    private void handleVoteOpen(ButtonInteractionEvent event, String pollId) {
        ActivePoll poll;
        try {
            poll = pollService.openVote(pollId, event.getUser().getId());
        } catch (PollOperationException ex) {
            event.reply(ex.getMessage())
                    .setEphemeral(true)
                    .queue();
            return;
        }
        
        event.reply(buildVoteText(poll))
                .addComponents(buildVoteActionRows(poll, pollService.getVoteSelections(pollId, event.getUser().getId())))
                .setEphemeral(true)
                .queue();
    }
    
    private void handleToggle(ButtonInteractionEvent event, String togglePayload) {
        String[] parts = togglePayload.split(":");
        if (parts.length != 2) {
            event.reply("Invalid vote action.").setEphemeral(true).queue();
            return;
        }
        
        String pollId = parts[0];
        int optionIndex;
        try {
            optionIndex = Integer.parseInt(parts[1]);
        } catch (NumberFormatException ex) {
            event.reply("Invalid vote option.").setEphemeral(true).queue();
            return;
        }
        
        ActivePoll poll;
        Set<Integer> selected;
        try {
            poll = pollService.getActivePollOrThrow(pollId);
            selected = pollService.toggleVoteSelection(pollId, event.getUser().getId(), optionIndex);
        } catch (PollOperationException ex) {
            event.reply(ex.getMessage()).setEphemeral(true).queue();
            return;
        }
        
        event.editMessage(buildVoteText(poll))
                .setComponents(buildVoteActionRows(poll, selected))
                .queue();
    }
    
    private void handleSubmit(ButtonInteractionEvent event, String pollId) {
        event.deferReply(true).queue();
        ActivePoll poll;
        List<String> selectedOptions;
        try {
            // retrieve the poll
            poll = pollService.getActivePollOrThrow(pollId);
            
            selectedOptions = pollService.submitVote(pollId, event.getUser().getId());
        } catch (PollOperationException ex) {
            event.reply(ex.getMessage())
                    .setEphemeral(true)
                    .queue();
            return;
        }

        if (poll.getMessageId() != null) {
            Message pollMessage = event.getChannel()
                    .retrieveMessageById(poll.getMessageId())
                    .complete();
            pollMessage.editMessageEmbeds(buildPublicPoll(poll))
                    .queue();
        }
        
        String message = selectedOptions.isEmpty()
                ? "Vote submitted with no selections. This would be treated as a withdraw."
                : "Vote captured for you with selections " + joinSelections(pollService.getVoteSelections(pollId, event.getUser().getId()));
        
        event.getHook()
                .sendMessage(message)
                .queue();
    }
    
    private void handleStepOneModal(ModalInteractionEvent event) {
        PartialDraftPoll draft;
        try {
            draft = pollService.createPartialDraft(
                    event.getId(),
                    event.getUser().getId(),
                    event.getUser().getAsMention(),
                    getRequiredValue(event, QUESTION_FIELD),
                    getRequiredSelectValue(event, MAX_SELECTION_FIELD),
                    getRequiredSelectValue(event, DURATION_FIELD)
            );
        } catch (PollOperationException ex) {
            event.reply(ex.getMessage())
                    .setEphemeral(true)
                    .queue();
            return;
        }
        
        event.reply("Question and max selections captured.")
                .addComponents(ActionRow.of(
                        Button.primary(CONTINUE_BUTTON_PREFIX + draft.getDraftId(), "Continue input options"),
                        Button.danger(CANCEL_BUTTON_PREFIX + draft.getDraftId(), "Cancel")
                ))
                .setEphemeral(true)
                .queue();
    }
    
    private void handleStepTwoModal(ModalInteractionEvent event, String modalId) {
        String draftId = modalId.substring(CREATE_STEP_TWO_MODAL_PREFIX.length());
        DraftPoll draft;
        try {
            draft = pollService.createDraft(draftId, readOptions(event));
        } catch (PollOperationException ex) {
            event.reply(ex.getMessage())
                    .setEphemeral(true)
                    .queue();
            return;
        }
        
        event.replyEmbeds(buildDraftPreview(draft))
                .addComponents(ActionRow.of(
                        Button.success(PUBLISH_BUTTON_PREFIX + draft.getDraftId(), "Start Poll"),
                        Button.danger(CANCEL_BUTTON_PREFIX + draft.getDraftId(), "Cancel")
                ))
                .setEphemeral(true)
                .queue();
    }
    
    private Modal buildStepOneModal() {
        return Modal.create(CREATE_STEP_ONE_MODAL_ID, "Create Anonymous Poll")
                .addComponents(
                        Label.of("Question", TextInput.create(QUESTION_FIELD, TextInputStyle.PARAGRAPH)
                                .setRequired(true)
                                .setMaxLength(2000)
                                .build()),
                        Label.of("How many voting option each person allows", StringSelectMenu.create(MAX_SELECTION_FIELD)
                                .addOption("1", "1")
                                .addOption("2", "2")
                                .addOption("3", "3")
                                .addOption("4", "4")
                                .addOption("5", "5")
                                .setRequiredRange(1, 1)
                                .setPlaceholder("Choose max selections")
                                .build()),
                        Label.of("Poll duration", StringSelectMenu.create(DURATION_FIELD)
                                .addOption("End of today", "TODAY_EOD")
                                .addOption("End of 1 day", "PLUS_1_DAY_EOD")
                                .addOption("End of 3 days", "PLUS_3_DAYS_EOD")
                                .addOption("End of 5 days", "PLUS_5_DAYS_EOD")
                                .setRequiredRange(1, 1)
                                .setPlaceholder("Choose poll duration")
                                .build())
                )
                .build();
    }
    
    private Modal buildStepTwoModal(String draftId) {
        return Modal.create(CREATE_STEP_TWO_MODAL_PREFIX + draftId, "Anonymous Poll Options")
                .addComponents(
                        Label.of("Option 1", TextInput.create(OPTION_FIELD_PREFIX + "1", TextInputStyle.SHORT)
                                .setPlaceholder("Option 1 (Required)")
                                .setRequired(true)
                                .setMaxLength(250)
                                .build()),
                        Label.of("Option 2", TextInput.create(OPTION_FIELD_PREFIX + "2", TextInputStyle.SHORT)
                                .setPlaceholder("Option 2")
                                .setRequired(false)
                                .setMaxLength(250)
                                .build()),
                        Label.of("Option 3", TextInput.create(OPTION_FIELD_PREFIX + "3", TextInputStyle.SHORT)
                                .setPlaceholder("Option 3")
                                .setRequired(false)
                                .setMaxLength(250)
                                .build()),
                        Label.of("Option 4", TextInput.create(OPTION_FIELD_PREFIX + "4", TextInputStyle.SHORT)
                                .setPlaceholder("Option 4")
                                .setRequired(false)
                                .setMaxLength(250)
                                .build()),
                        Label.of("Option 5", TextInput.create(OPTION_FIELD_PREFIX + "5", TextInputStyle.SHORT)
                                .setPlaceholder("Option 5")
                                .setRequired(false)
                                .setMaxLength(250)
                                .build())
                )
                .build();
    }
    
    private List<String> readOptions(ModalInteractionEvent event) {
        List<String> options = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            String value = getOptionalValue(event, OPTION_FIELD_PREFIX + i);
            if (value != null) {
                options.add(value);
            }
        }
        return options;
    }
    
    private MessageEmbed buildDraftPreview(DraftPoll draft) {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Anonymous Poll Preview");
        eb.setColor(Color.ORANGE);
        eb.setDescription(formatPollBody(
                draft.getQuestion(),
                draft.getOptions(),
                draft.getMaxSelection(),
                draft.getCreatorTag(),
                draft.getDurationOption(),
                null,
                "0"
        ));
        return eb.build();
    }
    
    public MessageEmbed buildPublicPoll(ActivePoll poll) {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Anonymous Poll");
        eb.setColor(Color.CYAN);
        eb.setDescription(formatPollBody(
                poll.getQuestion(),
                poll.getOptions(),
                poll.getMaxSelection(),
                poll.getCreatorTag(),
                poll.getDurationOption(),
                poll.getClosesOn(),
                String.valueOf(pollService.currentVoterCount(poll.getPollId()))
        ));
        return eb.build();
    }

    public MessageEmbed buildClosedPoll(ClosedPoll poll) {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Anonymous Poll");
        eb.setColor(Color.GRAY);
        eb.setDescription(formatClosedPollBody(poll));
        return eb.build();
    }

    public List<ActionRow> buildClosedActionRows() {
        return List.of(ActionRow.of(Button.secondary(CLOSED_BUTTON_ID, "Closed").asDisabled()));
    }
    
    private String formatPollBody(String question,
                                  List<String> options,
                                  int maxSelections,
                                  String creatorTag,
                                  String durationOption,
                                  LocalDateTime closesOn,
                                  String totalVoters) {
        StringJoiner joiner = new StringJoiner("\n");
        joiner.add(question);
        joiner.add("");
        joiner.add("Started by: " + creatorTag);
        joiner.add("Choose up to " + maxSelections + " options");
        joiner.add("Duration: " + formatDurationLabel(durationOption, closesOn));
        joiner.add("Total voters: " + totalVoters);
        joiner.add("");
        for (int i = 0; i < options.size(); i++) {
            joiner.add((i + 1) + ". " + options.get(i));
        }
        return joiner.toString();
    }

    private String formatClosedPollBody(ClosedPoll poll) {
        StringJoiner joiner = new StringJoiner("\n");
        joiner.add(poll.getQuestion());
        joiner.add("");
        joiner.add("Started by: " + poll.getCreatorTag());
        joiner.add("Choose up to " + poll.getMaxSelection() + " options");
        joiner.add("Duration: " + formatDurationLabel(poll.getDurationOption(), poll.getClosesOn()));
        joiner.add("Total voters: " + poll.getTotalVoters());
        joiner.add("");

        for (PollOptionResult optionResult : poll.getOptionResults()) {
            joiner.add(optionResult.getOptionNumber() + ". " + optionResult.getOptionText());
            joiner.add("Result: " + optionResult.getVoteCount() + "/" + optionResult.getTotalVoters()
                    + " (" + optionResult.getPercentage() + "%)");
            joiner.add("");
        }

        return joiner.toString().trim();
    }
    
    private String buildVoteText(ActivePoll poll) {
        return "Choose up to " + poll.getMaxSelection()
                + ". Submit with no selections to withdraw your vote.\n";
    }
    
    private List<ActionRow> buildVoteActionRows(ActivePoll poll, Set<Integer> selected) {
        List<Button> optionButtons = new ArrayList<>();
        for (int i = 0; i < poll.getOptions().size(); i++) {
            int optionIndex = i + 1;
            boolean isSelected = selected.contains(optionIndex);
            Button button = isSelected
                    ? Button.success(TOGGLE_BUTTON_PREFIX + poll.getPollId() + ":" + optionIndex, String.valueOf(optionIndex))
                    : Button.secondary(TOGGLE_BUTTON_PREFIX + poll.getPollId() + ":" + optionIndex, String.valueOf(optionIndex));
            optionButtons.add(button);
        }
        
        List<ActionRow> rows = new ArrayList<>();
        rows.add(ActionRow.of(optionButtons));
        rows.add(ActionRow.of(Button.primary(SUBMIT_BUTTON_PREFIX + poll.getPollId(), "Submit")));
        return rows;
    }
    
    private String getRequiredValue(ModalInteractionEvent event, String id) {
        return event.getValue(id).getAsString().trim();
    }

    private String getRequiredSelectValue(ModalInteractionEvent event, String id) {
        ModalMapping mapping = event.getValue(id);
        if (mapping == null || mapping.getAsStringList().isEmpty()) {
            throw new PollOperationException("Missing required selection for " + id + ".");
        }
        return mapping.getAsStringList().getFirst().trim();
    }
    
    private String getOptionalValue(ModalInteractionEvent event, String id) {
        ModalMapping mapping = event.getValue(id);
        if (mapping == null) {
            return null;
        }
        
        String value = mapping.getAsString().trim();
        return value.isBlank() ? null : value;
    }
    
    private String joinSelections(Set<Integer> selected) {
        return selected.stream()
                .sorted()
                .map(String::valueOf)
                .reduce((left, right) -> left + ", " + right)
                .orElse("none");
    }

    private String formatDurationLabel(String durationOption, LocalDateTime closesOn) {
        String label = switch (durationOption) {
            case "TODAY_EOD" -> "End of today";
            case "PLUS_1_DAY_EOD" -> "End of 1 day";
            case "PLUS_3_DAYS_EOD" -> "End of 3 days";
            case "PLUS_5_DAYS_EOD" -> "End of 5 days";
            default -> durationOption;
        };

        if (closesOn == null) {
            return label;
        }

        long closesOnEpochSeconds = closesOn.atZone(ZoneId.systemDefault()).toEpochSecond();
        return label + " (<t:" + closesOnEpochSeconds + ":F>)";
    }
}
