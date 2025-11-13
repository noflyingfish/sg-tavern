package com.feiyu.discord.sg.tavern.listeners;

import com.feiyu.discord.sg.tavern.config.ValuesConfig;
import com.feiyu.discord.sg.tavern.entities.EventEntity;
import com.feiyu.discord.sg.tavern.repositories.EventRepository;
import com.feiyu.discord.sg.tavern.utils.RegexUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Component
@AllArgsConstructor
public class EventDetailsMessageListener extends ListenerAdapter {
    
    private final ValuesConfig valuesConfig;
    private final EventRepository eventRepository;
    
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        
        //check for channel to be public thread in the correct channel
        if (ChannelType.GUILD_PUBLIC_THREAD.equals(event.getChannelType()) &&
                valuesConfig.getUpcomingEventChannelId().equals(
                        event.getChannel().asThreadChannel().getParentChannel().getId())) {
            
            String message = event.getMessage().getContentDisplay();
            if (RegexUtil.containTime(message) && RegexUtil.containDate(message)) {
                log.info("Event post detected : name - {}, url - {}",
                        event.getChannel().getName(),
                        event.getMessage().getJumpUrl());
                
                Optional<EventEntity> eventEntityOptional = eventRepository.findTopByPostId(event.getChannel().getId());
                
                if (eventEntityOptional.isEmpty()) {
                    log.error("Event is not tracked");
                } else if (eventEntityOptional.get().getEventDetailMsgId() == null) {
                    EventEntity eventEntity = eventEntityOptional.get();
                    eventEntity.setEventDetailMsgId(event.getMessage().getId());
                    eventEntity.setUpdatedOn(LocalDateTime.now());
                    eventRepository.save(eventEntity);
                    log.info("Event Details tracked");
                }
            }
        }
    }
}
