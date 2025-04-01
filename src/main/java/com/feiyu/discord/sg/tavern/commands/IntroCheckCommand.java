package com.feiyu.discord.sg.tavern.commands;

import com.feiyu.discord.sg.tavern.schedulers.IntroMsgScheduler;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class IntroCheckCommand extends ListenerAdapter {
    
    private final IntroMsgScheduler introMsgScheduler;
    
    // Have to resort to lazy loading....
    // Spring boot doesn't seems to be the best framework for a discord bot...
    public IntroCheckCommand(@Lazy IntroMsgScheduler introMsgScheduler) {
        this.introMsgScheduler = introMsgScheduler;
    }
    
    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if(event.getName().equals("hammycheckintro")) {
            log.info("IntroCheckCommand.introcheck by : {}", event.getMember().getEffectiveName());
            //Send a message in response to the command being run
            event.reply("The result is sent in a PM to Hammy...").setEphemeral(true).queue();
            introMsgScheduler.introReminder();
            log.info("IntroCheckCommand.introcheck : Done");
        }
    }
}
