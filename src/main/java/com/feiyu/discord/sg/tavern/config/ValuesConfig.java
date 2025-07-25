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
    @Value("${channelId.upcoming-event}")
    private String upcomingEventChannelId;
    @Value("${channelId.events-promo}")
    private String eventsPromoChannelId;
    
    @Value("${userId.owner}")
    private String adminUserId;
    @Value("${userId.dev}")
    private String devUserId;
    @Value("${userId.bot}")
    private String botId;
    
    @Value("${roleId.event-organiser}")
    private String eventOrganiserRoleId;
    @Value("${roleId.new-joiner}")
    private String newJoinerRoleId;
    @Value("${roleId.name-colour-anchor}")
    private String nameColourAnchorRoleId;
    
}
