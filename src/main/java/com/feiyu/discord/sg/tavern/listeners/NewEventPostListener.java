package com.feiyu.discord.sg.tavern.listeners;

import com.feiyu.discord.sg.tavern.config.ValuesConfig;
import com.feiyu.discord.sg.tavern.entities.EventEntity;
import com.feiyu.discord.sg.tavern.repositories.EventRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.events.channel.ChannelCreateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@AllArgsConstructor
public class NewEventPostListener extends ListenerAdapter {
    
    private final ValuesConfig valuesConfig;
    private final EventRepository eventRepository;
    
    @Override
    public void onChannelCreate(ChannelCreateEvent event) {
        
        //check for channel to be public thread in the correct channel
        if (ChannelType.GUILD_PUBLIC_THREAD.equals(event.getChannelType()) &&
                valuesConfig.getUpcomingEventChannelId().equals(
                        event.getChannel().asThreadChannel().getParentChannel().getId())) {

            ThreadChannel eventPost = event.getChannel().asThreadChannel();
            EventEntity newEvent = EventEntity.builder()
                    .postId(eventPost.getId())
                    .postName(eventPost.getName())
                    .postUrl(eventPost.getJumpUrl())
                    .postStatus("NEW")
                    .createdOn(LocalDateTime.now())
                    .updatedOn(LocalDateTime.now())
                    .build();
            
            log.info("New event post : {}", newEvent.toString());
            
            eventRepository.save(newEvent);
        }
    }
    
}


