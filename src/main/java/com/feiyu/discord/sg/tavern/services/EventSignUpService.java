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

import java.awt.Color;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@AllArgsConstructor
public class EventSignUpService {

    private static final String STATUS_ATTENDING = "ATTENDING";
    private static final String STATUS_KIV = "KIV";
    private static final String STATUS_WAITLIST = "WAITLIST";
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
                            Button.success("event:signup:attend:" + postId, "(un)Sign Up"),
                            Button.primary("event:signup:reserve:" + postId, "Chope another slot"),
                            Button.secondary("event:signup:kiv:" + postId, "KIV"),
                            Button.secondary("event:signup:myslots:" + postId, "My Slots"),
                            Button.danger("event:signup:setcap:" + postId, "Set Cap")
                    ))
                    .queue(signUpMsg -> {
                        signUpMsg.pin().queue();
                        event.setSignUpMsgId(signUpMsg.getId());
                        eventRepository.save(event);
                        log.info("Sign-up form created for {}: msgId={}", postId, signUpMsg.getId());
                    });
        });
    }

    public List<EventAttendanceEntity> getUserSlots(String postId, String userId) {
        return attendanceRepository.findAllByPostIdAndUserId(postId, userId);
    }

    public EventAttendanceEntity getSlotById(Long attendanceId) {
        return attendanceRepository.findById(attendanceId).orElse(null);
    }

    @Transactional
    public boolean editSlotDisplayName(Long attendanceId, String displayName) {
        var slot = attendanceRepository.findById(attendanceId);
        if (slot.isPresent()) {
            slot.get().setDisplayName(displayName);
            attendanceRepository.save(slot.get());
            log.info("Updated display name : {}", slot);
            return true;
        }
        return false;
    }

    @Transactional
    public String removeSlot(Long attendanceId) {
        EventAttendanceEntity slot = attendanceRepository.findById(attendanceId).orElse(null);
        if (slot == null) return null;
        String postId = slot.getPostId();
        attendanceRepository.deleteById(attendanceId);
        attendanceRepository.flush();
        log.info("Removed from event : {}", slot);
        return postId;
    }

    @Transactional
    public String promoteSlot(Long attendanceId) {
        EventAttendanceEntity slot = attendanceRepository.findById(attendanceId).orElse(null);
        if (slot == null) return "NOT_FOUND";
        if (STATUS_ATTENDING.equals(slot.getStatus())) return "ALREADY_ATTENDING";
        if (isAttendingFull(slot.getPostId())) return "FULL";
        slot.setStatus(STATUS_ATTENDING);
        attendanceRepository.save(slot);
        log.info("Waitlist to Attending : {}", slot);
        return "PROMOTED";
    }

    // Only Add to the list
    @Transactional
    public String reserveSlot(String postId, String userId, String remark) {
        boolean full = isAttendingFull(postId);
        String status = full ? STATUS_WAITLIST : STATUS_ATTENDING;
        EventAttendanceEntity entity = EventAttendanceEntity.builder()
                .postId(postId)
                .userId(userId)
                .displayName(remark)
                .status(status)
                .isMain(false)
                .createdOn(LocalDateTime.now())
                .build();
        attendanceRepository.save(entity);
        log.info("User : {} - reserved slot '{}' [{}] for event : {}", userId, remark, status, postId);
        return status;
    }

    // Bidirectional Add/Remove depending on current state
    @Transactional
    public String signUp(String postId, String userId, String displayName) {
        var existing = attendanceRepository.findByPostIdAndUserIdAndIsMain(postId, userId, true);
        boolean full = isAttendingFull(postId);
        log.info("event is full : {}", full);

        // WITHDRAW from attending (main slot only)
        if (existing.isPresent() && STATUS_ATTENDING.equals(existing.get().getStatus())) {
            removeSlot(existing.get().getId());
            return "WITHDRAWN";
        }

        // WITHDRAW from waitlist (main slot only)
        if (existing.isPresent() && STATUS_WAITLIST.equals(existing.get().getStatus())) {
            removeSlot(existing.get().getId());
            return "WAITLIST_REMOVED";
        }

        // KIV to ATTENDING / WAITLIST (main slot only)
        if (existing.isPresent() && STATUS_KIV.equals(existing.get().getStatus())) {
            EventAttendanceEntity ea = existing.get();
            ea.setStatus(full ? STATUS_WAITLIST : STATUS_ATTENDING);
            ea.setDisplayName(displayName);
            attendanceRepository.save(ea);
            log.info("User : {} - {} attending event : {}", userId, ea.getStatus(), postId);
            return ea.getStatus();
        }

        // New sign-up
        String status = full ? STATUS_WAITLIST : STATUS_ATTENDING;
        EventAttendanceEntity entity = EventAttendanceEntity.builder()
                .postId(postId)
                .userId(userId)
                .displayName(displayName)
                .status(status)
                .isMain(true)
                .createdOn(LocalDateTime.now())
                .build();
        attendanceRepository.save(entity);
        log.info("User : {} - {} event : {}", userId, status, postId);
        return status;
    }

    @Transactional
    public String kiv(String postId, String userId, String displayName) {
        var existing = attendanceRepository.findByPostIdAndUserId(postId, userId);

        // REMOVE KIV
        if (existing.isPresent() && STATUS_KIV.equals(existing.get().getStatus())) {
            removeSlot(existing.get().getId());
            return "KIV_REMOVED";
        }

        // ATTENDING to KIV
        if (existing.isPresent() && STATUS_ATTENDING.equals(existing.get().getStatus())) {
            EventAttendanceEntity ea = existing.get();
            ea.setStatus(STATUS_KIV);
            ea.setDisplayName(displayName);
            attendanceRepository.save(ea);
            log.info("User : {} - kiv event : {}", userId, postId);
            return "KIV";
        }

        // WAITLIST to KIV
        if (existing.isPresent() && STATUS_WAITLIST.equals(existing.get().getStatus())) {
            EventAttendanceEntity ea = existing.get();
            ea.setStatus(STATUS_KIV);
            ea.setDisplayName(displayName);
            attendanceRepository.save(ea);
            log.info("User : {} - kiv event (from waitlist) : {}", userId, postId);
            return "KIV";
        }

        // New KIV
        EventAttendanceEntity entity = EventAttendanceEntity.builder()
                .postId(postId)
                .userId(userId)
                .displayName(displayName)
                .status(STATUS_KIV)
                .createdOn(LocalDateTime.now())
                .build();
        attendanceRepository.save(entity);
        log.info("User : {} - kiv event : {}", userId, postId);
        return "KIV";
    }

    @Transactional
    public void setCap(String postId, int cap) {
        EventEntity event = eventRepository.findTopByPostId(postId).orElse(null);
        if(event != null){
            event.setMaxCap(cap <= 0 ? null : cap);
            eventRepository.save(event);
            log.info("postId : {}, set maxcap : {}", postId, cap);
        } else {
            log.error("PostId : {} not detected at setCap", postId);
        }
       
    }

    public String detectRebalanceType(String postId) {
        EventEntity event = eventRepository.findTopByPostId(postId).orElse(null);
        if (event == null) return "NONE";

        List<EventAttendanceEntity> all = attendanceRepository.findAllByPostId(postId);
        long attendingCount = all.stream().filter(a -> STATUS_ATTENDING.equals(a.getStatus())).count();
        Integer maxCap = event.getMaxCap();

        if (maxCap != null && maxCap > 0 && attendingCount > maxCap) {
            return "DEMOTE";
        }
        return "NONE";
    }

    @Transactional
    public List<String> applyRebalance(String postId) {
        EventEntity event = eventRepository.findTopByPostId(postId).orElse(null);
        if (event == null) return List.of();

        List<EventAttendanceEntity> all = attendanceRepository.findAllByPostId(postId);
        long attendingCount = all.stream().filter(a -> STATUS_ATTENDING.equals(a.getStatus())).count();
        Integer maxCap = event.getMaxCap();

        if (maxCap != null && maxCap > 0 && attendingCount > maxCap) {
            long excess = attendingCount - maxCap;
            List<EventAttendanceEntity> attending = attendanceRepository
                    .findByPostIdAndStatusOrderByCreatedOnDesc(postId, STATUS_ATTENDING);
            List<EventAttendanceEntity> toDemote = attending.subList(0, (int) excess);
            toDemote.forEach(r -> r.setStatus(STATUS_WAITLIST));
            attendanceRepository.saveAll(toDemote);
            List<String> demotedIds = toDemote.stream().map(EventAttendanceEntity::getUserId).toList();
            log.info("Demoted {} users to waitlist for postId: {}", toDemote.size(), postId);
            return demotedIds;
        }

        return List.of();
    }

    private boolean isAttendingFull(String postId) {
        EventEntity event = eventRepository.findTopByPostId(postId).orElse(null);
        if (event == null || event.getMaxCap() == null || event.getMaxCap() <= 0) {
            return false;
        }
        List<EventAttendanceEntity> all = attendanceRepository.findAllByPostId(postId);
        long attendingCount = all.stream()
                .filter(a -> STATUS_ATTENDING.equals(a.getStatus()))
                .count();
        return attendingCount >= event.getMaxCap();
    }

    public MessageEmbed renderEmbed(String postId) {
        EventEntity event = eventRepository.findTopByPostId(postId).orElse(null);
        if (event == null) return new EmbedBuilder().setDescription("Event not found.").build();

        List<EventAttendanceEntity> attendees = attendanceRepository.findAllByPostId(postId);
        List<EventAttendanceEntity> attending = attendees.stream()
                .filter(a -> STATUS_ATTENDING.equals(a.getStatus())).collect(Collectors.toList());
        List<EventAttendanceEntity> kivList = attendees.stream()
                .filter(a -> STATUS_KIV.equals(a.getStatus())).collect(Collectors.toList());
        List<EventAttendanceEntity> waitList = attendees.stream()
                .filter(a -> STATUS_WAITLIST.equals(a.getStatus()))
                .sorted(Comparator.comparing(EventAttendanceEntity::getCreatedOn))
                .collect(Collectors.toList());

        StringBuilder sb = new StringBuilder();
        sb.append(event.getProcessedEventLocation() != null ? event.getProcessedEventLocation() : "—");
        sb.append("\n");
        sb.append(event.getProcessedEventDateTime() != null ? event.getProcessedEventDateTime().format(DATE_FMT) : "—");

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle(event.getProcessedEventName() != null ? event.getProcessedEventName() : "Event Sign-Up")
                .setDescription(sb.toString());

        String attendingTitle = "Attending (" + attending.size() + ")";
        if (event.getMaxCap() != null && event.getMaxCap() > 0) {
            attendingTitle = "Attending (" + attending.size() + "/" + event.getMaxCap() + ")";
        }
        embed.addField(attendingTitle,
                attending.isEmpty() ? "—" : attending.stream().map(a -> "• " + a.getDisplayName()).collect(Collectors.joining("\n")),
                false);

        if(!kivList.isEmpty()) {
            embed.addField("KIV (" + kivList.size() + ")",
                    kivList.stream().map(a -> "• " + a.getDisplayName()).collect(Collectors.joining("\n")),
                    false);
        }
        if(!waitList.isEmpty()) {
            embed.addField("Waitlist (" + waitList.size() + ")",
                    waitList.stream().map(a -> "• " + a.getDisplayName()).collect(Collectors.joining("\n")),
                    false);
        }

        embed.setColor(Color.GREEN);
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
