package com.feiyu.discord.sg.tavern.listeners;

import com.feiyu.discord.sg.tavern.config.ListConfig;
import com.feiyu.discord.sg.tavern.services.EventService;
import com.feiyu.discord.sg.tavern.services.MessageService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@AllArgsConstructor
public class EventConfirmationListener extends ListenerAdapter {

    private static final String CONFIRM_PREFIX = "event:confirm:confirm:";
    private static final String CANCEL_PREFIX = "event:confirm:cancel:";

    private final EventService eventService;
    private final MessageService messageService;
    private final ListConfig listConfig;

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        String componentId = event.getComponentId();

        if (componentId.startsWith(CONFIRM_PREFIX)) {
            handleConfirm(event, componentId.substring(CONFIRM_PREFIX.length()));
            return;
        }
        if (componentId.startsWith(CANCEL_PREFIX)) {
            handleCancel(event, componentId.substring(CANCEL_PREFIX.length()));
        }
    }

    private void handleConfirm(ButtonInteractionEvent event, String postId) {
        event.deferReply(true).queue();

        boolean success = eventService.confirmEvent(postId, event.getGuild());

        if (success) {
            event.getHook().sendMessage("Event confirmed and details extracted!")
                    .setEphemeral(true).queue();

            String adminMessage = "New event triggered : " + event.getChannel().asThreadChannel().getJumpUrl();;
            messageService.sendAdminChannelMessage(event.getGuild(), adminMessage);

            if (listConfig.getEventPilotPostIds().contains(postId)) {
                eventService.triggerSignUp(postId, event.getGuild());
            }
        } else {
            event.getHook().sendMessage("Extraction failed. The event will be retried in the nightly batch.")
                    .setEphemeral(true).queue();
        }

        event.getChannel().deleteMessageById(event.getMessageId()).queue();
    }

    private void handleCancel(ButtonInteractionEvent event, String postId) {
        eventService.cancelEventDetailTracking(postId);

        event.reply("Draft discarded. Repost your event details when ready.")
                .setEphemeral(true).queue();
        event.getChannel().deleteMessageById(event.getMessageId()).queue();
    }
}
