package com.feiyu.discord.sg.tavern.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Getter
@Configuration
public class ValuesConfig {
    
    @Value("${guildId}")
    private String guildId;
    
    @Value("${introChannelId}")
    private String introChannelId;
    
    @Value("${admin.bot.channel}")
    private String adminBotChannelId;
    
    @Value("${adminUserId}")
    private String adminUserId;
    
}
