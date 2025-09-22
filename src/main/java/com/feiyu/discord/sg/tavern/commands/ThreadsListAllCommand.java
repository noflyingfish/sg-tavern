package com.feiyu.discord.sg.tavern.commands;

import com.feiyu.discord.sg.tavern.config.ListConfig;
import com.feiyu.discord.sg.tavern.config.ValuesConfig;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@AllArgsConstructor
public class ThreadsListAllCommand extends ListenerAdapter {
    
    private final ValuesConfig valuesConfig;
    private final ListConfig listConfig;
    
    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        
        if ("allthreads".equals(event.getName())) {
            
            Guild guild = event.getGuild();
            List<TextChannel> channelList = guild.getTextChannels();
            
            StringBuilder sb = new StringBuilder();
            sb.append("**Channels and Threads**\n");
            sb.append("This is all the publicly available Channels and Threads!\n\n");
            
            
            for (TextChannel channel : channelList) {
                // remove all mod/admin related channels
                if (!listConfig.getPrivateChannels().contains(channel.getId())) {
                    sb.append(channel.getName() + "\n");
                    
                    if (!channel.getThreadChannels().isEmpty()) {
                        sb.append("-# ");
                        for (ThreadChannel thread : channel.getThreadChannels()) {
                            if (!listConfig.getPrivateThreads().contains(thread.getId())) {
                                sb.append(thread.getName() + " // ");
                            }
                        }
                        sb.delete(sb.length() - 3, sb.length());
                        sb.append("\n");
                    }
                }
            }
            event.reply(sb.toString())
                    .setEphemeral(true)
                    .queue();
        }
    }
}
