package com.feiyu.discord.sg.tavern.commands;

import com.feiyu.discord.sg.tavern.config.ValuesConfig;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@AllArgsConstructor
public class EventPingCommand extends ListenerAdapter {
    
    private final ValuesConfig valuesConfig;
    
    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        
        //check for channel to be public thread in the correct channel
        if (ChannelType.GUILD_PUBLIC_THREAD.equals(event.getChannelType()) &&
                valuesConfig.getUpcomingEventChannelId().equals(
                        event.getChannel().asThreadChannel().getParentChannel().getId())) {
            
            if (event.getName().equals("everyone")) {
                
                log.info("Command - thread - {} - user - {}", event.getChannel().asThreadChannel().getId(), event.getUser().getId());
                
                event.reply("Pinging everyone inside this post...")
                        .setEphemeral(true)
                        .queue();
                
                String userName = event.getMember().getEffectiveName();
                
                event.getChannel()
                        .sendMessage(userName + " pinged @everyone (inside this event)")
                        .queue();
            }
        }
    }
}
