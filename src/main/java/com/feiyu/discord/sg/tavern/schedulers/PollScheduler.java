package com.feiyu.discord.sg.tavern.schedulers;

import com.feiyu.discord.sg.tavern.commands.AnonymousPollCommand;
import com.feiyu.discord.sg.tavern.models.poll.ClosedPoll;
import com.feiyu.discord.sg.tavern.services.PollService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@AllArgsConstructor
public class PollScheduler {

    private final JDA jda;
    private final AnonymousPollCommand anonymousPollCommand;
    private final PollService pollService;

    @Async
    @Scheduled(cron = "0 0 0 * * ?", zone = "Asia/Singapore")
    public void pollStatusScheduler() {
        log.info("PollScheduler.pollStatusScheduler Start");
        List<ClosedPoll> overduePolls = pollService.processActivePollStatuses();

        for (ClosedPoll poll : overduePolls) {
            try {
                updateClosedPollMessage(poll);
                pollService.closePoll(poll.getPollId());
                log.info("Poll closed - pollId={}", poll.getPollId());
            } catch (Exception ex) {
                log.error("Failed to persist closed poll status - pollId={}", poll.getPollId(), ex);
            }
        }
        log.info("PollScheduler.pollStatusScheduler End");
    }

    private void updateClosedPollMessage(ClosedPoll poll) {
        try {
            TextChannel channel = jda.getTextChannelById(poll.getChannelId());
            if (channel == null || poll.getMessageId() == null) {
                log.warn("Unable to update poll message before closing - pollId={}, channelId={}, messageId={}",
                        poll.getPollId(), poll.getChannelId(), poll.getMessageId());
                return;
            }

            channel.retrieveMessageById(poll.getMessageId())
                    .complete()
                    .editMessageEmbeds(anonymousPollCommand.buildClosedPoll(poll))
                    .setComponents(anonymousPollCommand.buildClosedActionRows())
                    .complete();
        } catch (Exception ex) {
            log.error("Failed to update closed poll message - pollId={}", poll.getPollId(), ex);
        }
    }
}
