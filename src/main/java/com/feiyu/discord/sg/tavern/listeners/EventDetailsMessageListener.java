package com.feiyu.discord.sg.tavern.listeners;

import com.feiyu.discord.sg.tavern.config.ValuesConfig;
import com.feiyu.discord.sg.tavern.utils.RegexUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@AllArgsConstructor
public class EventDetailsMessageListener extends ListenerAdapter {
    
    private final ValuesConfig valuesConfig;
    
    @Override
    public void onMessageReceived(MessageReceivedEvent event){
        
        //check for channel to be public thread in the correct channel
        if (ChannelType.GUILD_PUBLIC_THREAD.equals(event.getChannelType()) &&
                valuesConfig.getUpcomingEventChannelId().equals(
                        event.getChannel().asThreadChannel().getParentChannel().getId())) {
            
            String message = event.getMessage().getContentDisplay();
            if(RegexUtil.containTime(message) && RegexUtil.containDate(message)){
                log.info("TEST - datetime detection - message ====");
                log.info(event.getMessage().getJumpUrl());
            }
        }
    }
}
