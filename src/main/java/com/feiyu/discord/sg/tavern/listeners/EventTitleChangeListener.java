package com.feiyu.discord.sg.tavern.listeners;

import com.feiyu.discord.sg.tavern.config.ValuesConfig;
import com.feiyu.discord.sg.tavern.utils.RegexUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.events.channel.update.ChannelUpdateNameEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@AllArgsConstructor
public class EventTitleChangeListener extends ListenerAdapter {
    
    private final ValuesConfig valuesConfig;
    
    @Override
    public void onChannelUpdateName(ChannelUpdateNameEvent event) {
        
        //check for channel to be public thread in the correct channel
        if (ChannelType.GUILD_PUBLIC_THREAD.equals(event.getChannelType()) &&
                valuesConfig.getUpcomingEventChannelId().equals(
                        event.getChannel().asThreadChannel().getParentChannel().getId())) {
            
            EmbedBuilder eb = new EmbedBuilder();
            eb.setDescription("Post your event after the title change for it to be captured by the bot :)");
            eb.setFooter("Work-in-progress");
            MessageEmbed me = eb.build();
            Message m = event.getChannel().asThreadChannel().sendMessageEmbeds(me).complete();
            
            log.info("Event title changed : [{}] to [{}]", event.getOldValue() , event.getNewValue());
            
            // Schedule the deletion for 60 seconds later
            CompletableFuture.delayedExecutor(60, TimeUnit.SECONDS)
                    .execute(() -> m.delete().queue());
        }
    }
    
}

