package com.feiyu.discord.sg.tavern;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class SgTavernApplication {
    
    
    public static void main(String[] args) {
        SpringApplication.run(SgTavernApplication.class, args);
    }
    
    @Bean
    public JDA jda(@Value("${discord.bot.token}") String token) throws InterruptedException {
        // Create and configure the JDA instance
        JDA jda = JDABuilder.createDefault(token)
                .enableIntents(GatewayIntent.GUILD_MEMBERS)
                .build();
        // Optionally wait until the JDA instance is fully ready
        jda.awaitReady();
        return jda;
    }
    
}
