package com.feiyu.discord.sg.tavern.services;

import com.feiyu.discord.sg.tavern.entities.EventAttendanceEntity;
import com.feiyu.discord.sg.tavern.entities.EventEntity;
import com.feiyu.discord.sg.tavern.repositories.EventAttendanceRepository;
import com.feiyu.discord.sg.tavern.repositories.EventRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@AllArgsConstructor
public class EventSignUpService {

    private static final String STATUS_ATTENDING = "ATTENDING";
    private static final String STATUS_KIV = "KIV";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("EEE, MMM d, yyyy 'at' h:mm a");

    private final EventRepository eventRepository;
    private final EventAttendanceRepository attendanceRepository;

    public void createSignUpMessage(String postId, Guild guild) {
        EventEntity event = eventRepository.findTopByPostId(postId).orElse(null);
        if (event == null || event.getEventDetailMsgId() == null) {
            log.warn("createSignUpMessage: missing event or detail msg for {}", postId);
            return;
        }

        ThreadChannel thread = guild.getThreadChannelById(postId);
        if (thread == null) {
            log.warn("createSignUpMessage: thread not found for {}", postId);
            return;
        }

        MessageEmbed embed = renderEmbed(postId);

        thread.retrieveMessageById(event.getEventDetailMsgId()).queue(detailMsg -> {
            detailMsg.replyEmbeds(embed)
                    .addComponents(ActionRow.of(
                            Button.success("event:signup:attend:" + postId, "Sign Up"),
                            Button.secondary("event:signup:kiv:" + postId, "KIV"),
                            Button.primary("event:signup:nickname:" + postId, "+ Nickname")
                    ))
                    .queue(signUpMsg -> {
                        event.setSignUpMsgId(signUpMsg.getId());
                        eventRepository.save(event);
                        log.info("Sign-up form created for {}: msgId={}", postId, signUpMsg.getId());
                    });
        });
    }

    @Transactional
    public String signUp(String postId, String userId, String displayName) {
        var existing = attendanceRepository.findByPostIdAndUserId(postId, userId);

        // WITHDRAW event
        if (existing.isPresent() && STATUS_ATTENDING.equals(existing.get().getStatus())) {
            attendanceRepository.delete(existing.get());
            attendanceRepository.flush();
            log.info("User : {} - Withdraw event : {}", userId, postId);
            return "WITHDRAWN";
        //KIV to ATTENDING
        } else if(existing.isPresent() && STATUS_KIV.equals(existing.get().getStatus())){
            EventAttendanceEntity ea = existing.get();
            ea.setStatus(STATUS_ATTENDING);
            ea.setDisplayName(displayName);
            attendanceRepository.save(ea);
        // ATTENDING
        } else {
            EventAttendanceEntity entity = EventAttendanceEntity.builder()
                    .postId(postId)
                    .userId(userId)
                    .displayName(displayName)
                    .status(STATUS_ATTENDING)
                    .createdOn(LocalDateTime.now())
                    .build();
            attendanceRepository.save(entity);
        }
        log.info("User : {} - attending event : {}", userId, postId);
        return "ATTENDING";
    }

    @Transactional
    public String kiv(String postId, String userId, String displayName) {
        var existing = attendanceRepository.findByPostIdAndUserId(postId, userId);

        // REMOVE KIV
        if (existing.isPresent() && STATUS_KIV.equals(existing.get().getStatus())) {
            attendanceRepository.delete(existing.get());
            attendanceRepository.flush();
            log.info("User : {} - Withdraw kiv : {}", userId, postId);
            return "KIV_REMOVED";
        // ATTENDING to KIV
        } else if(existing.isPresent() && STATUS_ATTENDING.equals(existing.get().getStatus())){
            EventAttendanceEntity ea = existing.get();
            ea.setStatus(STATUS_KIV);
            ea.setDisplayName(displayName);
            attendanceRepository.save(ea);
        // KIV
        } else {
            EventAttendanceEntity entity = EventAttendanceEntity.builder()
                    .postId(postId)
                    .userId(userId)
                    .displayName(displayName)
                    .status(STATUS_KIV)
                    .createdOn(LocalDateTime.now())
                    .build();
            attendanceRepository.save(entity);
        }
        log.info("User : {} - kiv event : {}", userId, postId);
        return "KIV";
    }

    public MessageEmbed renderEmbed(String postId) {
        EventEntity event = eventRepository.findTopByPostId(postId).orElse(null);
        if (event == null) return new EmbedBuilder().setDescription("Event not found.").build();

        List<EventAttendanceEntity> attendees = attendanceRepository.findAllByPostId(postId);
        List<EventAttendanceEntity> attending = attendees.stream()
                .filter(a -> STATUS_ATTENDING.equals(a.getStatus())).collect(Collectors.toList());
        List<EventAttendanceEntity> kivList = attendees.stream()
                .filter(a -> STATUS_KIV.equals(a.getStatus())).collect(Collectors.toList());
        List<EventAttendanceEntity> waitList = new ArrayList<>();

        StringBuilder sb = new StringBuilder();
        sb.append(event.getProcessedEventLocation() != null ? event.getProcessedEventLocation() : "—");
        sb.append("\n");
        sb.append(event.getProcessedEventDateTime() != null ? event.getProcessedEventDateTime().format(DATE_FMT) : "—");
        
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle(event.getProcessedEventName() != null ? event.getProcessedEventName() : "Event Sign-Up")
                .setDescription(sb.toString());
        embed.addField("Attending (" + attending.size() + ")",
                attending.isEmpty() ? "—" : attending.stream().map(a -> "• " + a.getDisplayName()).collect(Collectors.joining("\n")),
                false);

        if(!kivList.isEmpty()) {
            embed.addField("KIV (" + kivList.size() + ")",
                    kivList.isEmpty() ? "—" : kivList.stream().map(a -> "• " + a.getDisplayName()).collect(Collectors.joining("\n")),
                    false);
        }
        if(!waitList.isEmpty()) {
            embed.addField("KIV (" + waitList.size() + ")",
                    waitList.isEmpty() ? "—" : waitList.stream().map(a -> "• " + a.getDisplayName()).collect(Collectors.joining("\n")),
                    false);
        }

        embed.setColor(0x00FF00);
        return embed.build();
    }

    public void refreshSignUpMessage(String postId, Guild guild) {
        EventEntity event = eventRepository.findTopByPostId(postId).orElse(null);
        if (event == null || event.getSignUpMsgId() == null) return;

        ThreadChannel thread = guild.getThreadChannelById(postId);
        if (thread == null) return;

        MessageEmbed embed = renderEmbed(postId);

        thread.retrieveMessageById(event.getSignUpMsgId()).queue(msg -> {
            msg.editMessageEmbeds(embed).queue();
        }, failure -> {
            log.warn("Failed to refresh sign-up message for {}: {}", postId, failure.getMessage());
        });
    }
}
