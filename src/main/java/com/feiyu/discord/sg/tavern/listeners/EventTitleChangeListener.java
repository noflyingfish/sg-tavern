package com.feiyu.discord.sg.tavern.listeners;

import com.feiyu.discord.sg.tavern.config.ValuesConfig;
import com.feiyu.discord.sg.tavern.entities.EventEntity;
import com.feiyu.discord.sg.tavern.repositories.EventRepository;
import com.feiyu.discord.sg.tavern.services.EventManageService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.events.channel.update.ChannelUpdateNameEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@AllArgsConstructor
public class EventTitleChangeListener extends ListenerAdapter {
    
    private static final String CONFIRM_DELETE_FORM_PROMPT =
            "Sign-up form detected. Delete it to prevent further sign-ups?";
    
    private final ValuesConfig valuesConfig;
    private final EventRepository eventRepository;
    private final EventManageService eventManageService;
    
    @Override
    public void onChannelUpdateName(ChannelUpdateNameEvent event) {
        
        //check for channel to be public thread in the correct channel
        if (ChannelType.GUILD_PUBLIC_THREAD.equals(event.getChannelType()) &&
                valuesConfig.getUpcomingEventChannelId().equals(
                        event.getChannel().asThreadChannel().getParentChannel().getId())) {
            log.info("Event title changed : [{}] to [{}]", event.getOldValue(), event.getNewValue());
            
            ThreadChannel eventPost = event.getChannel().asThreadChannel();
            Optional<EventEntity> optionalEventEntity = eventRepository.findTopByPostId(eventPost.getId());
            
            String existingSignUpMsgId = null;
            if (optionalEventEntity.isPresent()) {
                EventEntity editedEvent = optionalEventEntity.get();
                editedEvent.setPostName(eventPost.getName());
                editedEvent.setPostStatus("EDITED");
                editedEvent.setEventDetailMsgId(null);
                editedEvent.setUpdatedOn(LocalDateTime.now());
                
                eventRepository.save(editedEvent);
                log.info("Event post updated : {}", eventPost.getName());
                existingSignUpMsgId = editedEvent.getSignUpMsgId();
            }
            
            if (existingSignUpMsgId != null) {
                eventPost.sendMessage("Make sure you have the event organiser's permission.\n" +
                                "Deleting signup and attendance is not reversible.")
                        .addComponents(ActionRow.of(
                                Button.danger("event:titlechange:deleteform:" + eventPost.getId(), "Delete Sign-Up Form AND Attendance"),
                                Button.secondary("event:titlechange:canceldelete:" + eventPost.getId(), "Do Nothing")
                        ))
                        .queue();
            }
        }
    }
    
    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        String componentId = event.getComponentId();
        
        if (componentId.startsWith("event:titlechange:deleteform:")) {
            handleDeleteForm(event, componentId.substring("event:titlechange:deleteform:".length()));
            return;
        }
        if (componentId.startsWith("event:titlechange:canceldelete:")) {
            handleCancelDelete(event);
        }
    }
    
    private void handleDeleteForm(ButtonInteractionEvent event, String postId) {
        event.deferReply(true).queue();
        try {
            event.getMessageChannel().sendMessage("Event reset by : " + event.getUser().getName()).queue();
            eventManageService.deleteSignUpForm(postId, event.getGuild());
            event.getHook().sendMessage("Sign-up form deleted.").setEphemeral(true).queue();
            
            EmbedBuilder eb = new EmbedBuilder();
            eb.setDescription("Post your event details after the title change for it to be captured by the bot :)");
            MessageEmbed me = eb.build();
            Message m = event.getChannel().asThreadChannel().sendMessageEmbeds(me).complete();
            // Schedule the deletion for 60 seconds later
            CompletableFuture.delayedExecutor(60, TimeUnit.SECONDS)
                    .execute(() -> m.delete().queue());
            
        } catch (IllegalArgumentException ex) {
            event.getHook().sendMessage(ex.getMessage()).setEphemeral(true).queue();
        }
        event.getChannel().deleteMessageById(event.getMessageId()).queue();
    }
    
    private void handleCancelDelete(ButtonInteractionEvent event) {
        event.reply("Nothing happen. Event goes on :)").setEphemeral(true).queue();
        event.getChannel().deleteMessageById(event.getMessageId()).queue();
    }
    
}

