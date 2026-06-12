package com.feiyu.discord.sg.tavern.commands;

import com.feiyu.discord.sg.tavern.config.ValuesConfig;
import com.feiyu.discord.sg.tavern.services.EventService;
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
    private final EventService eventService;
    
    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        
        //check for channel to be public thread in the correct channel
        if (ChannelType.GUILD_PUBLIC_THREAD.equals(event.getChannelType()) &&
                valuesConfig.getUpcomingEventChannelId().equals(
                        event.getChannel().asThreadChannel().getParentChannel().getId())) {
            
            if (event.getName().equals("everyone")) {
                
                log.info("Command - pingthread - {} - user - {}", event.getChannel().asThreadChannel().getId(), event.getUser().getId());
                
                event.reply("Pinging everyone inside this post...")
                        .setEphemeral(true)
                        .queue();
                
                String userName = event.getMember().getEffectiveName();
                
                event.getChannel()
                        .sendMessage(userName + " pinged @everyone (inside this event)")
                        .queue();
            }
        }
        
        //check for channel to be public thread in the correct channel
        if (ChannelType.GUILD_PUBLIC_THREAD.equals(event.getChannelType()) &&
                valuesConfig.getUpcomingEventChannelId().equals(
                        event.getChannel().asThreadChannel().getParentChannel().getId())) {
            
            if (event.getName().equals("pingreacts")) {

                log.info("Command - pingreacts - {} - user - {}", event.getChannel().asThreadChannel().getId(), event.getUser().getId());

                String messageId = event.getOption("messageid").getAsString();

                event.deferReply(true).queue();

                String pingList = eventService.pingReacts(event.getGuild(), messageId);

                if (pingList == null) {
                    event.getHook().sendMessage("Message not found :/").setEphemeral(true).queue();
                    return;
                }

                if (pingList.isEmpty()) {
                    event.getHook().sendMessage("No one reacted to that message.").setEphemeral(true).queue();
                    return;
                }

                String commandUserMention = event.getMember().getAsMention();
                String pingMessage = commandUserMention + " pinged " + pingList;

                event.getChannel().sendMessage(pingMessage).queue();
                event.getHook().sendMessage("Ping done :)").setEphemeral(true).queue();
            }
        }

        //check for channel to be public thread in the correct channel
        if (ChannelType.GUILD_PUBLIC_THREAD.equals(event.getChannelType()) &&
                valuesConfig.getUpcomingEventChannelId().equals(
                        event.getChannel().asThreadChannel().getParentChannel().getId())) {

            if (event.getName().equals("pingattending")) {

                log.info("Command - pingattending - {} - user - {}",
                        event.getChannel().asThreadChannel().getId(),
                        event.getUser().getId());

                String postId = event.getChannel().asThreadChannel().getId();
                event.deferReply(true).queue();

                String pingList = eventService.pingAttending(postId);
                String signUpMsgId = eventService.getSignUpMsgId(postId);

                if (signUpMsgId == null) {
                    event.getHook().sendMessage("not support yet")
                            .setEphemeral(true).queue();
                    return;
                }

                if (pingList.isEmpty()) {
                    event.getHook().sendMessage("No one is attending this event.")
                            .setEphemeral(true).queue();
                    return;
                }

                String commandUserMention = event.getMember().getAsMention();
                String pingMessage = commandUserMention
                        + " has pinged attending members\n" + pingList;

                event.getChannel().asThreadChannel()
                        .retrieveMessageById(signUpMsgId)
                        .queue(signUpMsg -> signUpMsg.reply(pingMessage).queue());

                event.getHook().sendMessage("Ping done :)")
                        .setEphemeral(true).queue();
            }
        }
    }
}
