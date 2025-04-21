package com.feiyu.discord.sg.tavern.listeners;

import com.feiyu.discord.sg.tavern.config.ValuesConfig;
import com.feiyu.discord.sg.tavern.entities.NewJoinerEntity;
import com.feiyu.discord.sg.tavern.repositories.NewJoinerRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@AllArgsConstructor
public class NewJoinerListener extends ListenerAdapter {
    
    private final ValuesConfig valuesConfig;
    private final NewJoinerRepository newJoinerRepository;
    
    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        Guild guild = event.getGuild();
        User user = event.getUser();
        Role newJoinerRole = guild.getRoleById(valuesConfig.getNewJoinerRoleId());
        
        NewJoinerEntity newJoinerEntity = NewJoinerEntity.builder()
                .userId(user.getId())
                .username(user.getEffectiveName())
                .joinDateTime(LocalDateTime.now())
                .build();
        
        newJoinerRepository.save(newJoinerEntity);
        
        log.info("New user joined and assigned new comer role : {} ", user.getEffectiveName());
        guild.addRoleToMember(user, newJoinerRole).queue();
    }
    
}
