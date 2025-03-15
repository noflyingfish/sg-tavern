package com.feiyu.discord.sg.tavern;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableAsync
@EnableScheduling
@SpringBootApplication
public class SgTavernApplication {
    
    
    public static void main(String[] args) {
        SpringApplication.run(SgTavernApplication.class, args);
    }
    
}
