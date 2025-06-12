package com.feiyu.discord.sg.tavern.schedulers;

import com.feiyu.discord.sg.tavern.config.ValuesConfig;
import com.feiyu.discord.sg.tavern.entities.EventEntity;
import com.feiyu.discord.sg.tavern.repositories.EventRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.ForumChannel;
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@AllArgsConstructor
public class EventMonitorScheduler {
    
    private final JDA jda;
    private final ValuesConfig valuesConfig;
    private final EventRepository eventRepository;
    
    // 4am daily
    @Async
    @Scheduled(cron = "0 0 4 * * ?", zone = "Asia/Singapore")
    public void newEditedEventMonitorScheduler() {
        log.info("EventMonitorScheduler.newEditedEventMonitorScheduler Start");
        
        // Get all the event posts
        Guild guild = jda.getGuildById(valuesConfig.getGuildId());
        ForumChannel upcomingEventForum = guild.getForumChannelById(valuesConfig.getUpcomingEventChannelId());
        List<ThreadChannel> eventPostList = upcomingEventForum.getThreadChannels();
        
        List<EventEntity> newEventList = new ArrayList<>();
        List<EventEntity> updateEventList = new ArrayList<>();
        
        for(ThreadChannel post : eventPostList){
            
            String postId = post.getId();
            String postName = post.getName();
            Optional<EventEntity> optionalEventEntity = eventRepository.findTopByPostId(postId);
            
            // new post
            if(optionalEventEntity.isEmpty()){
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
            if(optionalEventEntity.isPresent() &&
                    !post.getName().equals(optionalEventEntity.get().getPostName())){
                
                EventEntity editedEvent = optionalEventEntity.get();
                editedEvent.setPostName(post.getName());
                editedEvent.setPostStatus("EDITED");
                editedEvent.setUpdatedOn(LocalDateTime.now());
                
                eventRepository.save(editedEvent);
                updateEventList.add(editedEvent);
            }
        }
        if(!newEventList.isEmpty() || !updateEventList.isEmpty()) {
            String message = "New posts : " + newEventList.size() + "\n"
                    + "Updated posts : " + updateEventList.size();
            
            Member devMember = guild.retrieveMemberById(valuesConfig.getDevUserId()).complete();
            log.info("Message sent to : {}", devMember);
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
        
        for(EventEntity eventEntity: managedEventList){
            if (eventEntity.getProcessedEventDateTime().isBefore(LocalDateTime.now())){
                eventEntity.setPostStatus("PAST");
                
                eventRepository.save(eventEntity);
                log.info("EventEntity has passed : {} ", eventEntity);
            }
        }
        log.info("EventMonitorScheduler.pastEventMonitorScheduler End");
    }
    
    public void sendEventScheduler(){
    
    }
    
}
