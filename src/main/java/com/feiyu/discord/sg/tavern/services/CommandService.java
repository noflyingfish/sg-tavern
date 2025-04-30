package com.feiyu.discord.sg.tavern.services;

import com.feiyu.discord.sg.tavern.config.ValuesConfig;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@AllArgsConstructor
public class CommandService {
    
    private final JDA jda;
    private final ValuesConfig valuesConfig;
    
    @EventListener(ApplicationReadyEvent.class)
    public void commands(){
        
        log.info("Inserting commands...");
        Guild guild = jda.getGuildById(valuesConfig.getGuildId());
        
        // admin commands
        guild.upsertCommand("hammycheckintro", "Hammy runs a check to return a list of members without intro")
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))
                .queue();
        
        // general commands
        guild.upsertCommand("invite", "Get an invite link to this server (valid 48hrs)")
                .setDefaultPermissions(DefaultMemberPermissions.ENABLED)
                .queue();
        
        
        // ge2025 mini game
        guild.upsertCommand("makemyprediction", "Make a prediction for GE 2025 result")
                .addOptions(new OptionData(OptionType.NUMBER, "wpsengkang","Predict Workers' Party Vote % - Sengkang GRC",true),
                        new OptionData(OptionType.NUMBER, "wppunggol","Predict Workers' Party Vote % - Punggol GRC",true),
                        new OptionData(OptionType.NUMBER, "wptampines","Predict Workers' Party Vote % - Tampines GRC",true))
                .setDefaultPermissions(DefaultMemberPermissions.ENABLED)
                .queue();
        guild.upsertCommand("seemyprediction", "See my prediction for GE 2025 result")
                .setDefaultPermissions(DefaultMemberPermissions.ENABLED)
                .queue();
        
        log.info("Finish insert commands");
    }
    
}
