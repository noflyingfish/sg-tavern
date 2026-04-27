package com.feiyu.discord.sg.tavern.listeners;

import com.feiyu.discord.sg.tavern.config.ValuesConfig;
import com.feiyu.discord.sg.tavern.services.MessageService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.events.channel.ChannelCreateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@AllArgsConstructor
public class NewEventOrganiserListener extends ListenerAdapter {
    
    private final ValuesConfig valuesConfig;
    private final MessageService messageService;
    
    @Override
    public void onChannelCreate(ChannelCreateEvent event) {
        
        //check for channel to be public thread in the correct channel
        if (ChannelType.GUILD_PUBLIC_THREAD.equals(event.getChannelType()) &&
                valuesConfig.getUpcomingEventChannelId().equals(
                        event.getChannel().asThreadChannel().getParentChannel().getId())) {
            
            Guild guild = event.getGuild();
            Member member = guild.retrieveMemberById(event.getChannel().asThreadChannel().getOwnerId())
                    .complete();
            User user = member.getUser();
            
            Role eventOrganiserRole = guild.getRoleById(valuesConfig.getEventOrganiserRoleId());
            
            if (member.getRoles().stream()
                    .noneMatch(role -> role.getId().equals(valuesConfig.getEventOrganiserRoleId()))) {
                guild.addRoleToMember(user, eventOrganiserRole).queue();
                String adminRoleMessage = user.getEffectiveName() + " - " + user.getName() + " assigned role : " + eventOrganiserRole.getName();
                messageService.sendAdminChannelMessage(guild, adminRoleMessage);
            }
            
            String adminMessage = user.getEffectiveName() + " - " + user.getName() + " has posted an event : \n"
                    + event.getChannel().asThreadChannel().getName();
            messageService.sendAdminChannelMessage(guild, adminMessage);
            

            
            log.info("NewEventOrganiserListener : " + event.getRawData().toString());
        }
    }
    
}
