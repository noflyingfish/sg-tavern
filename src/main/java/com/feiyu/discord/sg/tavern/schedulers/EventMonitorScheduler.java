package com.feiyu.discord.sg.tavern.schedulers;

import com.feiyu.discord.sg.tavern.config.ValuesConfig;
import com.feiyu.discord.sg.tavern.entities.EventEntity;
import com.feiyu.discord.sg.tavern.entities.MessageEntity;
import com.feiyu.discord.sg.tavern.repositories.EventRepository;
import com.feiyu.discord.sg.tavern.repositories.MessageRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.ForumChannel;
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@AllArgsConstructor
public class EventMonitorScheduler {
    
    private final JDA jda;
    private final ValuesConfig valuesConfig;
    private final EventRepository eventRepository;
    private final MessageRepository messageRepository;
    
    // 11pm daily
    @Async
    @Scheduled(cron = "0 0 23 * * ?", zone = "Asia/Singapore")
    public void newEditedEventMonitorScheduler() {
        log.info("EventMonitorScheduler.newEditedEventMonitorScheduler Start");
        
        // Get all the event posts
        Guild guild = jda.getGuildById(valuesConfig.getGuildId());
        ForumChannel upcomingEventForum = guild.getForumChannelById(valuesConfig.getUpcomingEventChannelId());
        List<ThreadChannel> eventPostList = upcomingEventForum.getThreadChannels();
        
        List<EventEntity> newEventList = new ArrayList<>();
        List<EventEntity> updateEventList = new ArrayList<>();
        
        for (ThreadChannel post : eventPostList) {
            
            String postId = post.getId();
            String postName = post.getName();
            Optional<EventEntity> optionalEventEntity = eventRepository.findTopByPostId(postId);
            
            // new post
            if (optionalEventEntity.isEmpty()) {
                EventEntity newEvent = EventEntity.builder()
                        .postId(postId)
                        .postName(postName)
                        .postUrl(post.getJumpUrl())
                        .postStatus("NEW")
                        .createdOn(LocalDateTime.now())
                        .updatedOn(LocalDateTime.now())
                        .build();
                
                eventRepository.save(newEvent);
                newEventList.add(newEvent);
            }
            
            // edited post (name only)
            if (optionalEventEntity.isPresent() &&
                    !post.getName().equals(optionalEventEntity.get().getPostName())) {
                
                EventEntity editedEvent = optionalEventEntity.get();
                editedEvent.setPostName(post.getName());
                editedEvent.setPostStatus("EDITED");
                editedEvent.setUpdatedOn(LocalDateTime.now());
                
                eventRepository.save(editedEvent);
                updateEventList.add(editedEvent);
            }
        }
        if (!newEventList.isEmpty() || !updateEventList.isEmpty()) {
            String message = "New posts : " + newEventList.size() + "\n"
                    + "Updated posts : " + updateEventList.size();
            
            Member devMember = guild.retrieveMemberById(valuesConfig.getDevUserId()).complete();
            log.info("Message sent to dev : {}", message);
            PrivateChannel pc = devMember.getUser().openPrivateChannel().complete();
            pc.sendMessage(message).queue();
        }
        log.info("EventMonitorScheduler.newEditedEventMonitorScheduler End");
    }
    
    // 1am daily
    @Async
    @Scheduled(cron = "0 0 1 * * ?", zone = "Asia/Singapore")
    public void pastEventMonitorScheduler() {
        log.info("EventMonitorScheduler.pastEventMonitorScheduler Start");
        
        List<EventEntity> managedEventList = eventRepository.findAllByPostStatus("MANAGED");
        
        for (EventEntity eventEntity : managedEventList) {
            if (eventEntity.getProcessedEventDateTime().isBefore(LocalDateTime.now())) {
                eventEntity.setPostStatus("PAST");
                
                eventRepository.save(eventEntity);
                log.info("EventEntity has passed : {} ", eventEntity);
            }
        }
        log.info("EventMonitorScheduler.pastEventMonitorScheduler End");
    }
    
    // 6am daily
    @Async
    @Scheduled(cron = "0 0 6 * * ?", zone = "Asia/Singapore")
    public void sendEventScheduler() {
        log.info("EventMonitorScheduler.sendEventScheduler Start");
        
        Guild guild = jda.getGuildById(valuesConfig.getGuildId());
        List<EventEntity> managedEventList = eventRepository.findAllByPostStatus("MANAGED");
        managedEventList.sort(Comparator.comparing(EventEntity::getProcessedEventDateTime));
        
        DateTimeFormatter df1 = DateTimeFormatter.ofPattern("dd-MMM-yyyy hh:mma");
        DateTimeFormatter df2 = DateTimeFormatter.ofPattern("dd-MMM-yyyy");
        
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Tavern Outing Notices for " + LocalDateTime.now().format(df2));
        eb.setDescription("----");
        
        int fieldCount = 0;
        
        for (EventEntity e : managedEventList) {
            eb.addField(e.getProcessedEventName() + " @ " + e.getProcessedEventLocation(),
                    e.getProcessedEventDateTime().format(df1) + " " + e.getProcessedEventDateTime().getDayOfWeek() + "\n"
                            + e.getPostUrl(),
                    false);
            
            fieldCount++;
            if (fieldCount == 25) { // each embed msg only can have max 25 fields.
                log.info("Embed max count hit.");
                break;
            }
        }
        eb.setFooter("Still under testing...events might not be captured \n"
                + "Any suggestion to this message please let Rain knows :)");
        MessageEmbed me = eb.build();
        
        TextChannel eventPromoChannel = guild.getTextChannelById(valuesConfig.getEventsPromoChannelId());
        
        // send new message
        Message m = eventPromoChannel.sendMessageEmbeds(me).complete();
        
        // remove old message
        MessageEntity promoMessage = messageRepository.findTopByMessagePurpose("EventPromo").get();
        if (promoMessage != null) {
            eventPromoChannel.deleteMessageById(promoMessage.getMessageId()).queue();
            promoMessage.setUpdatedOn(LocalDateTime.now());
            promoMessage.setMessageId(m.getId());
            messageRepository.save(promoMessage);
        } else {
            messageRepository.save(MessageEntity.builder()
                    .messageId(m.getId())
                    .messagePurpose("EventPromo")
                    .createdOn(LocalDateTime.now())
                    .build());
        }

//        Member devMember = guild.retrieveMemberById(valuesConfig.getDevUserId()).complete();
//        log.info("Message sent to dev : {}", me);
//        PrivateChannel pc = devMember.getUser().openPrivateChannel().complete();
//        pc.sendMessageEmbeds(me).queue();
        
        log.info("EventMonitorScheduler.sendEventScheduler End");
    }
    
}
