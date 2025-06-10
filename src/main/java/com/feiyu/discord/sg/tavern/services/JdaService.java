package com.feiyu.discord.sg.tavern.services;

import com.feiyu.discord.sg.tavern.commands.IntroCheckCommand;
import com.feiyu.discord.sg.tavern.commands.InviteLinkCommand;

import com.feiyu.discord.sg.tavern.listeners.MemberExitListener;
import com.feiyu.discord.sg.tavern.listeners.NewEventOrganiserListener;
import com.feiyu.discord.sg.tavern.listeners.NewJoinerListener;
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
    
    private final NewJoinerListener newJoinerListener;
    private final MemberExitListener memberExitListener;
    private final NewEventOrganiserListener newEventOrganiserListener;
    
    @Bean
    public net.dv8tion.jda.api.JDA jda(@Value("${discord.bot.token}") String token) throws InterruptedException {
        // Create and configure the JDA instance
        net.dv8tion.jda.api.JDA jda = JDABuilder.createDefault(token)
                .enableIntents(GatewayIntent.GUILD_MEMBERS)
                .setEventPassthrough(true)
                .addEventListeners(introCheckCommand)
                .addEventListeners(inviteLinkCommand)
                .addEventListeners(newJoinerListener)
                .addEventListeners(memberExitListener)
                .addEventListeners(newEventOrganiserListener)
                .build();

        // Optionally wait until the JDA instance is fully ready
        jda.awaitReady();
        return jda;
    }
    
}
