package com.feiyu.discord.sg.tavern.services;

import com.feiyu.discord.sg.tavern.config.ValuesConfig;
import com.feiyu.discord.sg.tavern.entities.EventEntity;
import com.feiyu.discord.sg.tavern.repositories.EventRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageReaction;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@AllArgsConstructor
public class EventService {

    private final ValuesConfig valuesConfig;
    private final EventRepository eventRepository;
    private final GptService gptService;
    private final EventSignUpService eventSignUpService;

    public boolean confirmEvent(String postId, Guild guild) {
        EventEntity entity = eventRepository.findTopByPostId(postId).orElse(null);
        if (entity == null) {
            log.warn("confirmEvent called for non-existent postId: {}", postId);
            return false;
        }

        gptService.sendGpt(List.of(entity), guild);

        EventEntity updated = eventRepository.findTopByPostId(postId).orElse(null);
        if (updated != null && "MANAGED".equals(updated.getPostStatus())) {
            updated.setConfirmedOn(LocalDateTime.now());
            eventRepository.save(updated);
            log.info("Event confirmed: {}", postId);
            return true;
        }

        log.warn("GPT extraction failed for event: {}", postId);
        return false;
    }

    public void triggerSignUp(String postId, Guild guild) {
        eventSignUpService.createSignUpMessage(postId, guild);
    }

    public void cancelEventDetailTracking(String postId) {
        eventRepository.findTopByPostId(postId).ifPresent(entity -> {
            entity.setEventDetailMsgId(null);
            entity.setUpdatedOn(LocalDateTime.now());
            eventRepository.save(entity);
            log.info("Event details tracking cancelled: {}", postId);
        });
    }
    
    public String pingReacts(Guild guild, String messageId) {
        String[] channelIds = {
                valuesConfig.getEventsPromoChannelId(),
                valuesConfig.getPromoYourselfChannelId()
        };

        Message message = null;
        for (String channelId : channelIds) {
            TextChannel channel = guild.getTextChannelById(channelId);
            if (channel != null) {
                try {
                    message = channel.retrieveMessageById(messageId).complete();
                    break;
                } catch (Exception ignored) {
                    // not in this channel, try next
                }
            }
        }

        if (message == null) {
            return null;
        }

        Set<User> reactors = new LinkedHashSet<>();

        for (MessageReaction reaction : message.getReactions()) {
            List<User> users = reaction.retrieveUsers().complete();
            reactors.addAll(users);
        }

        reactors.removeIf(User::isBot);

        return reactors.stream()
                .map(User::getAsMention)
                .collect(Collectors.joining(" "));
    }
}
