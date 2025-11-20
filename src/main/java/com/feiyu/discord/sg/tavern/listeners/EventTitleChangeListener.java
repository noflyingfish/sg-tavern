package com.feiyu.discord.sg.tavern.listeners;

import com.feiyu.discord.sg.tavern.config.ValuesConfig;
import com.feiyu.discord.sg.tavern.entities.EventEntity;
import com.feiyu.discord.sg.tavern.repositories.EventRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.events.channel.update.ChannelUpdateNameEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@AllArgsConstructor
public class EventTitleChangeListener extends ListenerAdapter {
    
    private final ValuesConfig valuesConfig;
    private final EventRepository eventRepository;
    
    @Override
    public void onChannelUpdateName(ChannelUpdateNameEvent event) {
        
        //check for channel to be public thread in the correct channel
        if (ChannelType.GUILD_PUBLIC_THREAD.equals(event.getChannelType()) &&
                valuesConfig.getUpcomingEventChannelId().equals(
                        event.getChannel().asThreadChannel().getParentChannel().getId())) {
            
            ThreadChannel eventPost = event.getChannel().asThreadChannel();
            Optional<EventEntity> optionalEventEntity = eventRepository.findTopByPostId(eventPost.getId());
            
            if(optionalEventEntity.isPresent()){
                EventEntity editedEvent = optionalEventEntity.get();
                editedEvent.setPostName(eventPost.getName());
                editedEvent.setPostStatus("EDITED");
                editedEvent.setEventDetailMsgId(null);
                editedEvent.setUpdatedOn(LocalDateTime.now());
                
                eventRepository.save(editedEvent);
                log.info("Event post updated : {}", eventPost.getName());
            }
            
            EmbedBuilder eb = new EmbedBuilder();
            eb.setDescription("Post your event details after the title change for it to be captured by the bot :)");
            MessageEmbed me = eb.build();
            Message m = event.getChannel().asThreadChannel().sendMessageEmbeds(me).complete();
            
            log.info("Event title changed : [{}] to [{}]", event.getOldValue() , event.getNewValue());
            
            // Schedule the deletion for 60 seconds later
            CompletableFuture.delayedExecutor(60, TimeUnit.SECONDS)
                    .execute(() -> m.delete().queue());
        }
    }
    
}

