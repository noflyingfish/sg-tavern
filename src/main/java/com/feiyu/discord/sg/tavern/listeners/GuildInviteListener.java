package com.feiyu.discord.sg.tavern.listeners;

import com.feiyu.discord.sg.tavern.services.InviteCacheService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.guild.invite.GuildInviteCreateEvent;
import net.dv8tion.jda.api.events.guild.invite.GuildInviteDeleteEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@AllArgsConstructor
public class GuildInviteListener extends ListenerAdapter {
    
    private final InviteCacheService inviteCacheService;
    
    @Override
    public void onGuildInviteCreate(GuildInviteCreateEvent event) {
        log.info("onGuildInviteCreate :" + event.getCode());
        inviteCacheService.updateCache(event.getInvite());
    }
    
    @Override
    public void onGuildInviteDelete(GuildInviteDeleteEvent event) {
        log.info("onGuildInviteDelete :" + event.getCode());
        inviteCacheService.updateCache(event.getCode());
    }
    
}
