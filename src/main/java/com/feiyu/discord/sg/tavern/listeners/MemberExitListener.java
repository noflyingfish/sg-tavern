package com.feiyu.discord.sg.tavern.listeners;

import com.feiyu.discord.sg.tavern.config.ValuesConfig;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@AllArgsConstructor
public class MemberExitListener extends ListenerAdapter {
    
    private final ValuesConfig valuesConfig;
    
    @Override
    public void onGuildMemberRemove(GuildMemberRemoveEvent event) {
        Guild guild = event.getGuild();
        User user = event.getUser();
        
        String adminMessage = "Someone has left the server : "
                + user.getEffectiveName() + " - " + user.getName() + " - " + user.getId();
        log.info(adminMessage);
        TextChannel adminChannel = guild.getTextChannelById(valuesConfig.getAdminBotChannelId());
        adminChannel.sendMessage(adminMessage).queue();
    }
    
}
