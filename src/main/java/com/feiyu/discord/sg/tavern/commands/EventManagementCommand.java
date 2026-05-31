package com.feiyu.discord.sg.tavern.commands;

import com.feiyu.discord.sg.tavern.config.ValuesConfig;
import com.feiyu.discord.sg.tavern.entities.EventEntity;
import com.feiyu.discord.sg.tavern.services.EventManageService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
@AllArgsConstructor
public class EventManagementCommand extends ListenerAdapter {
    
    private final ValuesConfig valuesConfig;
    private final EventManageService eventManageService;
    
    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        
        //check for channel to be public thread in the correct channel
        if (ChannelType.GUILD_PUBLIC_THREAD.equals(event.getChannelType()) &&
                valuesConfig.getUpcomingEventChannelId().equals(
                        event.getChannel().asThreadChannel().getParentChannel().getId())) {
            // command - /eventstatus
            if ("eventstatus".equals(event.getName())) {
                log.info("Command - eventstatus - {}", event.getChannel().asThreadChannel().getId());

                Optional<EventEntity> optionalEventEntity = eventManageService.getEvent(event.getChannelId());

                if (optionalEventEntity.isEmpty()) {
                    event.reply("Not captured. Ping Rain to troubleshoot!")
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
                    if (eventEntity.getEventDetailMsgId() != null) {
                        Message eventDetailMessage = event.getChannel().asThreadChannel()
                                .retrieveMessageById(eventEntity.getEventDetailMsgId())
                                .complete();
                        eb.addField("Event Detail Post",
                                eventDetailMessage != null ? eventDetailMessage.getJumpUrl() : "",
                                false);
                    }
                    MessageEmbed me = eb.build();

                    event.replyEmbeds(me)
                            .setEphemeral(true)
                            .queue();
                }
            }
            
            // command - /manageevent
            if ("manageevent".equals(event.getName())) {
                log.info("Command - manageevent - {}", event.getChannel().asThreadChannel().getId());

                if (eventManageService.getEvent(event.getChannelId()).isEmpty()) {
                    event.reply("Not captured. Ping Rain to troubleshoot!")
                            .setEphemeral(true)
                            .queue();
                } else {
                    OptionMapping optName = event.getOption("eventname");
                    OptionMapping optLocation = event.getOption("eventlocation");
                    OptionMapping optDateTime = event.getOption("eventdatetime");

                    try {
                        eventManageService.updateEvent(
                                event.getChannelId(),
                                optName != null ? optName.getAsString() : null,
                                optLocation != null ? optLocation.getAsString() : null,
                                optDateTime != null ? optDateTime.getAsString() : null,
                                null);
                        event.reply("Updated!")
                                .setEphemeral(true)
                                .queue();
                    } catch (IllegalArgumentException ex) {
                        event.reply(ex.getMessage())
                                .setEphemeral(true)
                                .queue();
                    }
                }
            }
            
            // command - /resetevent
            if ("resetevent".equals(event.getName())) {
                log.info("Command - resetevent - {}", event.getChannel().asThreadChannel().getId());
                try {
                    eventManageService.resetEventDetailMsg(event.getChannelId());
                    event.reply("Event details message id reset")
                            .setEphemeral(true)
                            .queue();
                } catch (IllegalArgumentException ex) {
                    event.reply("Not captured. Ping Rain to troubleshoot!")
                            .setEphemeral(true)
                            .queue();
                }
            }
            
            // command - /pastevent
            if ("pastevent".equals(event.getName())) {
                log.info("Command - pastevent - {}", event.getChannel().asThreadChannel().getId());
                try {
                    eventManageService.markEventAsPast(event.getChannelId());
                    event.reply("Event status set to PAST")
                            .setEphemeral(true)
                            .queue();
                } catch (IllegalArgumentException ex) {
                    event.reply("Not captured. Ping Rain to troubleshoot!")
                            .setEphemeral(true)
                            .queue();
                }
            }
            
            // command - extractevent
            if ("extractevent".equals(event.getName())) {
                log.info("Command - extractevent - {}", event.getChannel().asThreadChannel().getId());
                event.deferReply(true).queue();
                try {
                    eventManageService.extractEvent(event.getChannelId(), event.getGuild());
                    event.getHook().sendMessage("Event sent to gpt")
                            .setEphemeral(true)
                            .queue();
                } catch (IllegalArgumentException ex) {
                    event.getHook().sendMessage("Not captured. Ping Rain to troubleshoot!")
                            .setEphemeral(true)
                            .queue();
                }
            }
        }
    }

}
