package com.feiyu.discord.sg.tavern.services;

import com.feiyu.discord.sg.tavern.commands.IntroCheckCommand;
import com.feiyu.discord.sg.tavern.commands.InviteLinkCommand;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class JdaService {
    
    private final IntroCheckCommand introCheckCommand;
    private final InviteLinkCommand inviteLinkCommand;

    
    @Bean
    public net.dv8tion.jda.api.JDA jda(@Value("${discord.bot.token}") String token) throws InterruptedException {
        // Create and configure the JDA instance
        net.dv8tion.jda.api.JDA jda = JDABuilder.createDefault(token)
                .enableIntents(GatewayIntent.GUILD_MEMBERS)
                .addEventListeners(introCheckCommand)
                .addEventListeners(inviteLinkCommand)
                .build();
        
        // Optionally wait until the JDA instance is fully ready
        jda.awaitReady();
        return jda;
    }
    
}
