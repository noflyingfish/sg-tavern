package com.feiyu.discord.sg.tavern.commands;

import com.feiyu.discord.sg.tavern.config.ValuesConfig;
import com.feiyu.discord.sg.tavern.entities.EventEntity;
import com.feiyu.discord.sg.tavern.repositories.EventRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.ChannelType;
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
    private final ValuesConfig valuesConfig;
    
    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        
        //check for channel to be public thread in the correct channel
        if (ChannelType.GUILD_PUBLIC_THREAD.equals(event.getChannelType()) &&
                valuesConfig.getUpcomingEventChannelId().equals(
                        event.getChannel().asThreadChannel().getParentChannel().getId())) {
            // command - /eventstatus
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
                    eb.addField("Event Detail Post",
                            eventEntity.getEventDetailMsgId() != null ? event.getChannel().asThreadChannel()
                                    .retrieveMessageById(eventEntity.getEventDetailMsgId())
                                    .complete()
                                    .getJumpUrl() : "",
                            false);
                    MessageEmbed me = eb.build();
                    
                    event.replyEmbeds(me)
                            .setEphemeral(true)
                            .queue();
                }
            }
            
            // command - /manageevent
            if ("manageevent".equals(event.getName())) {
                
                Optional<EventEntity> optionalEventEntity = eventRepository.findTopByPostId(event.getChannelId());
                
                if (optionalEventEntity.isEmpty()) {
                    event.reply("Event waiting to be captured. Please try again next day")
                            .setEphemeral(true)
                            .queue();
                } else {
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
                            event.reply("Please follow this time format exactly \"yyyy-MM-dd HH:mm\" (there is a space between dd HH)")
                                    .setEphemeral(true)
                                    .queue();
                            return;
                        }
                    }
                    
                    if (eventEntity.getProcessedEventName() != null &&
                            eventEntity.getProcessedEventLocation() != null &&
                            eventEntity.getProcessedEventDateTime() != null) {
                        eventEntity.setPostStatus("MANAGED");
                    }
                    
                    eventRepository.save(eventEntity);
                    event.reply("Updated!")
                            .setEphemeral(true)
                            .queue();
                }
            }
            
            // command - /resetevent
            if ("resetevent".equals(event.getName())) {
                Optional<EventEntity> optionalEventEntity = eventRepository.findTopByPostId(event.getChannelId());
                if (optionalEventEntity.isEmpty()) {
                    log.error("Reset event not captured");
                    event.reply("Event not captured. PM Rain to find out why ._.")
                            .setEphemeral(true)
                            .queue();
                } else {
                    EventEntity eventEntity = optionalEventEntity.get();
                    eventEntity.setEventDetailMsgId(null);
                    eventEntity.setUpdatedOn(LocalDateTime.now());
                    eventRepository.save(eventEntity);
                    
                    event.reply("Event details message id reset")
                            .setEphemeral(true)
                            .queue();
                }
            }
            
            // command - /pastevent
            if ("pastevent".equals(event.getName())) {
                Optional<EventEntity> optionalEventEntity = eventRepository.findTopByPostId(event.getChannelId());
                if (optionalEventEntity.isEmpty()) {
                    log.error("Reset event not captured");
                    event.reply("Event not captured. PM Rain to find out why ._.")
                            .setEphemeral(true)
                            .queue();
                } else {
                    EventEntity eventEntity = optionalEventEntity.get();
                    eventEntity.setPostStatus("PAST");
                    eventEntity.setUpdatedOn(LocalDateTime.now());
                    eventRepository.save(eventEntity);
                    
                    event.reply("Event status set to PAST")
                            .setEphemeral(true)
                            .queue();
                }
            }
        }
    }
}
