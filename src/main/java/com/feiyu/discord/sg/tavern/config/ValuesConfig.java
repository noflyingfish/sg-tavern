package com.feiyu.discord.sg.tavern.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Getter
@Configuration
public class ValuesConfig {
    
    @Value("${guildId}")
    private String guildId;
    
    @Value("${channelId.intro}")
    private String introChannelId;
    @Value("${channelId.rules}")
    private String rulesChannelId;
    @Value("${channelId.admin-bot}")
    private String adminBotChannelId;
    
    @Value("${userId.owner}")
    private String adminUserId;
    
    @Value("${roleId.event-organiser}")
    private String eventOrganiserRoleId;
    @Value("${roleId.new-joiner}")
    private String newJoinerRoleId;
    
}
