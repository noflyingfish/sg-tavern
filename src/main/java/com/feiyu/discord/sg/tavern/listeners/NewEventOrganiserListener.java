package com.feiyu.discord.sg.tavern.listeners;

import com.feiyu.discord.sg.tavern.config.ValuesConfig;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.channel.ChannelCreateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@AllArgsConstructor
public class NewEventOrganiserListener extends ListenerAdapter {
    
    private final ValuesConfig valuesConfig;
    
    @Override
    public void onChannelCreate(ChannelCreateEvent event) {
        
        //check for channel to be public thread in the correct channel
        if (ChannelType.GUILD_PUBLIC_THREAD.equals(event.getChannelType()) &&
                valuesConfig.getUpcomingEventChannelId().equals(
                        event.getChannel().asThreadChannel().getParentChannel().getId())) {
            
            Guild guild = event.getGuild();
            User user = guild.retrieveMemberById(event.getChannel().asThreadChannel().getOwnerId())
                    .complete()
                    .getUser();
            Role eventOrganiserRole = guild.getRoleById(valuesConfig.getEventOrganiserRoleId());
            
            guild.addRoleToMember(user, eventOrganiserRole).queue();
            
            String adminMessage =  user.getEffectiveName() + " - " + user.getName() + " has posted an event : \n"
                    + event.getChannel().asThreadChannel().getName();
            TextChannel adminChannel = guild.getTextChannelById(valuesConfig.getAdminBotChannelId());
            adminChannel.sendMessage(adminMessage).queue();
            
            log.info("NewEventOrganiserListener : " + event.getRawData().toString());
        }
    }
    
}
