package com.feiyu.discord.sg.tavern.listeners;

import com.feiyu.discord.sg.tavern.config.ValuesConfig;
import com.feiyu.discord.sg.tavern.entities.NewJoinerEntity;
import com.feiyu.discord.sg.tavern.repositories.NewJoinerRepository;
import com.feiyu.discord.sg.tavern.services.InviteCacheService;
import com.feiyu.discord.sg.tavern.services.MemberService;
import com.feiyu.discord.sg.tavern.services.MessageService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Component
@AllArgsConstructor
public class MemberJoinListener extends ListenerAdapter {
    
    private final ValuesConfig valuesConfig;
    private final NewJoinerRepository newJoinerRepository;
    private final MessageService messageService;
    private final InviteCacheService inviteCacheService;
    private final MemberService memberService;
    
    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {

        Guild guild = event.getGuild();
        User user = event.getUser();
        Role newJoinerRole = guild.getRoleById(valuesConfig.getNewJoinerRoleId());
        
        NewJoinerEntity newJoinerEntity = NewJoinerEntity.builder()
                .userId(user.getId())
                .username(user.getName())
                .joinDateTime(LocalDateTime.now())
                .build();
        
        newJoinerRepository.save(newJoinerEntity);
        
        String adminMessage = "Someone has joined the server : " + user.getEffectiveName() + " - " + user.getName();
        messageService.sendAdminChannelMessage(guild, adminMessage);
        
        String adminRoleMessage = user.getEffectiveName() + " - " +  user.getName() + " assigned role : " + newJoinerRole.getName();
        guild.addRoleToMember(user, newJoinerRole).queue();
        
        // retrieve invite code used to join
        Optional<String> inviteCodeOptional = inviteCacheService.updateCache();
        memberService.registerNewMember(user, inviteCodeOptional);
        
        messageService.sendAdminChannelMessage(guild, adminRoleMessage);
    }
    
}
