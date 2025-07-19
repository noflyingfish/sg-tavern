package com.feiyu.discord.sg.tavern.commands;

import com.feiyu.discord.sg.tavern.config.ValuesConfig;
import com.feiyu.discord.sg.tavern.repositories.EventRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@AllArgsConstructor
public class EventCommand extends ListenerAdapter {
    
    private final ValuesConfig valuesConfig;
    private final EventRepository eventRepository;
    
    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
    
            if(event.getName().equals("poststatus")){
                
                OptionMapping newPostStatus = event.getOption("poststatus");
                log.info(newPostStatus.getAsString());
                log.info(event.getChannelId());
                log.info(event.getMessageChannel().getId());
                
            }
            
            if(event.getName().equals("manageevent")){
                
                OptionMapping processedEventName = event.getOption("eventname");
                OptionMapping processedEventLocation = event.getOption("eventlocation");
                OptionMapping processedEVentDateTime = event.getOption("eventdatetime");
                
                
            }
        
    }
    
    
}
