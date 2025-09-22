package com.feiyu.discord.sg.tavern.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

@Getter
@Setter
@Configuration
@ConfigurationProperties
public class ListConfig {
    
    private Set<String> privateChannels;
    private Set<String> privateThreads;
    
}
