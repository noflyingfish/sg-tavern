package com.feiyu.discord.sg.tavern.services;

import com.feiyu.discord.sg.tavern.config.ValuesConfig;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.List;

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
        
        // event command
        guild.upsertCommand("poststatus", "Update event postStatus (MANAGED/PAST/CANCELLED/TBC)")
                .addOptions(List.of(new OptionData(OptionType.STRING, "poststatus", "Update Post Status", false)))
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_GUILD_EXPRESSIONS))
                .queue();
        guild.upsertCommand("manageevent", "Update event processedEventName")
                .addOptions(List.of(
                        new OptionData(OptionType.STRING, "eventname", "Event Name", false),
                        new OptionData(OptionType.STRING, "eventlocation", "Event Location", false),
                        new OptionData(OptionType.STRING, "eventdatetime", "Event DateTime", false)
                ))
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_GUILD_EXPRESSIONS))
                .queue();
        
        // general commands
        guild.upsertCommand("invite", "Get an invite link to this server (valid 48hrs)")
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.CREATE_INSTANT_INVITE))
                .queue();
        
        log.info("Finish insert commands");
    }
    
}
