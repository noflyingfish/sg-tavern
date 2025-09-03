package com.feiyu.discord.sg.tavern.commands;

import com.feiyu.discord.sg.tavern.entities.EventEntity;
import com.feiyu.discord.sg.tavern.repositories.EventRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;

@Slf4j
@Component
@AllArgsConstructor
public class EventManagementCommand extends ListenerAdapter {
    
    private static final String EXPECTED_FORMAT = "yyyy-MM-dd HH:mm";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern(EXPECTED_FORMAT);
    private final EventRepository eventRepository;
    
    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        
        if ("eventstatus".equals(event.getName())) {
            
            Optional<EventEntity> optionalEventEntity = eventRepository.findTopByPostId(event.getChannelId());
            
            if (optionalEventEntity.isEmpty()) {
                event.reply("Event waiting to be captured. Please try again next day")
                        .setEphemeral(true)
                        .queue();
            } else {
                EventEntity eventEntity = optionalEventEntity.get();
                EmbedBuilder eb = new EmbedBuilder();
                eb.setTitle("Event Details");
                eb.setDescription("Status : " + eventEntity.getPostStatus());
                eb.addField("Event Name",
                        eventEntity.getProcessedEventName() != null
                                ? eventEntity.getProcessedEventName() : "",
                        false);
                eb.addField("Event Location",
                        eventEntity.getProcessedEventLocation() != null
                                ? eventEntity.getProcessedEventLocation() : "",
                        false);
                eb.addField("Event DateTime",
                        eventEntity.getProcessedEventDateTime() != null
                                ? eventEntity.getProcessedEventDateTime().toString() : "",
                        false);
                MessageEmbed me = eb.build();
                
                event.replyEmbeds(me)
                        .setEphemeral(true)
                        .queue();
            }
        }
        
        if ("manageevent".equals(event.getName())) {
            
            Optional<EventEntity> optionalEventEntity = eventRepository.findTopByPostId(event.getChannelId());
            
            if (optionalEventEntity.isEmpty()) {
                event.reply("Event waiting to be captured. Please try again next day")
                        .setEphemeral(true)
                        .queue();
            } else {
                log.info("Manage Event : {}", event);
                EventEntity eventEntity = optionalEventEntity.get();
                
                OptionMapping processedEventName = event.getOption("eventname");
                if (processedEventName != null) {
                    eventEntity.setProcessedEventName(processedEventName.getAsString());
                }
                OptionMapping processedEventLocation = event.getOption("eventlocation");
                if (processedEventLocation != null) {
                    eventEntity.setProcessedEventLocation(processedEventLocation.getAsString());
                }
                OptionMapping processedEventDateTime = event.getOption("eventdatetime");
                if (processedEventDateTime != null) {
                    String inputDatetime = processedEventDateTime.getAsString().trim();
                    try {
                        LocalDateTime newEventDateTime = LocalDateTime.parse(inputDatetime, FORMATTER);
                        eventEntity.setProcessedEventDateTime(newEventDateTime);
                    } catch (DateTimeParseException ex) {
                        event.reply("Please follow this time format exactly \"yyyy-MM-dd HH:mm\"")
                                .setEphemeral(true)
                                .queue();
                        return;
                    }
                }
                
                if(eventEntity.getProcessedEventName() != null &&
                        eventEntity.getProcessedEventLocation() != null &&
                        eventEntity.getProcessedEventDateTime() != null){
                    eventEntity.setPostStatus("MANAGED");
                }
                
                eventRepository.save(eventEntity);
                event.reply("Updated!")
                        .setEphemeral(true)
                        .queue();
            }
        }
    }
}
