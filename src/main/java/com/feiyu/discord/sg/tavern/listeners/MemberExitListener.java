package com.feiyu.discord.sg.tavern.listeners;

import com.feiyu.discord.sg.tavern.config.ValuesConfig;
import com.feiyu.discord.sg.tavern.services.MessageService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@AllArgsConstructor
public class MemberExitListener extends ListenerAdapter {
    
    private final ValuesConfig valuesConfig;
    private final MessageService messageService;
    
    @Override
    public void onGuildMemberRemove(GuildMemberRemoveEvent event) {
        User user = event.getUser();
        
        String adminMessage = "Someone has left the server : "
                + user.getEffectiveName() + " - " + user.getName() + " - " + user.getId();
        messageService.sendAdminChannelMessage(event.getGuild(), adminMessage);
    }
    
}
