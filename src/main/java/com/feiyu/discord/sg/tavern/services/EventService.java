package com.feiyu.discord.sg.tavern.services;

import com.feiyu.discord.sg.tavern.entities.EventEntity;
import com.feiyu.discord.sg.tavern.repositories.EventRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Guild;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@AllArgsConstructor
public class EventService {

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
}
