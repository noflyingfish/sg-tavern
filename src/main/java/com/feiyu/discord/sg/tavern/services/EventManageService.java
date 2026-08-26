package com.feiyu.discord.sg.tavern.services;

import com.feiyu.discord.sg.tavern.entities.EventAttendanceEntity;
import com.feiyu.discord.sg.tavern.entities.EventEntity;
import com.feiyu.discord.sg.tavern.repositories.EventAttendanceRepository;
import com.feiyu.discord.sg.tavern.repositories.EventRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@AllArgsConstructor
public class EventManageService {

    private static final String EXPECTED_FORMAT = "yyyy-MM-dd HH:mm";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern(EXPECTED_FORMAT);

    private final EventRepository eventRepository;
    private final EventAttendanceRepository attendanceRepository;
    private final GptService gptService;

    public Optional<EventEntity> getEvent(String postId) {
        return eventRepository.findTopByPostId(postId);
    }

    @Transactional
    public EventEntity updateEvent(String postId, String eventName, String eventLocation,
                                    String eventDateTime, Integer maxCap) {
        EventEntity entity = eventRepository.findTopByPostId(postId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found for postId: " + postId));

        if (eventName != null && !eventName.isBlank()) {
            entity.setProcessedEventName(eventName.trim());
        }
        if (eventLocation != null && !eventLocation.isBlank()) {
            entity.setProcessedEventLocation(eventLocation.trim());
        }
        if (eventDateTime != null && !eventDateTime.isBlank()) {
            try {
                LocalDateTime parsed = LocalDateTime.parse(eventDateTime.trim(), FORMATTER);
                entity.setProcessedEventDateTime(parsed);
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("Invalid datetime format. Expected: " + EXPECTED_FORMAT);
            }
        }
        if (maxCap != null) {
            entity.setMaxCap(maxCap <= 0 ? null : maxCap);
        }

        if (entity.getProcessedEventName() != null &&
                entity.getProcessedEventLocation() != null &&
                entity.getProcessedEventDateTime() != null) {
            entity.setPostStatus("MANAGED");
        }

        entity.setUpdatedOn(LocalDateTime.now());
        eventRepository.save(entity);
        log.info("Event updated for postId: {} | name={} location={} datetime={} maxCap={}",
                postId, entity.getProcessedEventName(), entity.getProcessedEventLocation(),
                entity.getProcessedEventDateTime(), entity.getMaxCap());
        return entity;
    }

    @Transactional
    public void resetEventDetailMsg(String postId) {
        EventEntity entity = eventRepository.findTopByPostId(postId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found for postId: " + postId));
        entity.setEventDetailMsgId(null);
        entity.setUpdatedOn(LocalDateTime.now());
        eventRepository.save(entity);
        log.info("Event detail msg reset for postId: {}", postId);
    }

    @Transactional
    public void deleteSignUpForm(String postId, Guild guild) {
        log.info("Deleting sign-up for postId : {}", postId);
        
        EventEntity entity = eventRepository.findTopByPostId(postId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found for postId: " + postId));

        if (entity.getSignUpMsgId() == null) {
            // no post recorded to gpt
            return;
        }

        ThreadChannel thread = guild.getThreadChannelById(postId);
        if (thread != null) {
            Message msg = thread.retrieveMessageById(entity.getSignUpMsgId()).complete();
            if (msg != null) {
                // strip the buttons but keep the message as a record of who attended
                msg.editMessageEmbeds(msg.getEmbeds()).setComponents(List.of()).complete();
            }
        }
        
        List<EventAttendanceEntity> list = attendanceRepository.findAllByPostId(postId);
        for (EventAttendanceEntity eae : list){
            log.info("userId : {}, displayedName : {}", eae.getUserId(), eae.getDisplayName());
        }

        attendanceRepository.deleteAllByPostId(postId);
        entity.setSignUpMsgId(null);
        entity.setEventDetailMsgId(null);
        entity.setPostStatus("EDITED");
        entity.setUpdatedOn(LocalDateTime.now());
        eventRepository.save(entity);
        log.info("Sign-up form + attendance records deleted for postId: {}", postId);
    }

    @Transactional
    public void markEventAsPast(String postId) {
        EventEntity entity = eventRepository.findTopByPostId(postId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found for postId: " + postId));
        entity.setPostStatus("PAST");
        entity.setUpdatedOn(LocalDateTime.now());
        eventRepository.save(entity);
        log.info("Event marked as PAST for postId: {}", postId);
    }

    public void extractEvent(String postId, Guild guild) {
        EventEntity entity = eventRepository.findTopByPostId(postId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found for postId: " + postId));
        gptService.sendGpt(List.of(entity), guild);
        log.info("Event sent to GPT for postId: {}", postId);
    }
}
