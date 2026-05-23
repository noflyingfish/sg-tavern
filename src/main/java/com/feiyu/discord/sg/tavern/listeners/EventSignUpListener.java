package com.feiyu.discord.sg.tavern.listeners;

import com.feiyu.discord.sg.tavern.services.EventSignUpService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.modals.Modal;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@AllArgsConstructor
public class EventSignUpListener extends ListenerAdapter {

    private static final String ATTEND_PREFIX = "event:signup:attend:";
    private static final String KIV_PREFIX = "event:signup:kiv:";
    private static final String NICKNAME_PREFIX = "event:signup:nickname:";
    private static final String SET_CAP_PREFIX = "event:signup:setcap:";
    private static final String MODAL_PREFIX = "event:signup:modal:nickname:";
    private static final String MODAL_SET_CAP_PREFIX = "event:signup:modal:setcap:";

    private final EventSignUpService eventSignUpService;

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        String componentId = event.getComponentId();

        if (componentId.startsWith(ATTEND_PREFIX)) {
            handleAttend(event, componentId.substring(ATTEND_PREFIX.length()));
            return;
        }
        if (componentId.startsWith(KIV_PREFIX)) {
            handleKiv(event, componentId.substring(KIV_PREFIX.length()));
            return;
        }
        if (componentId.startsWith(NICKNAME_PREFIX)) {
            handleNickname(event, componentId.substring(NICKNAME_PREFIX.length()));
            return;
        }
        if (componentId.startsWith(SET_CAP_PREFIX)) {
            handleSetCap(event, componentId.substring(SET_CAP_PREFIX.length()));
        }
    }

    @Override
    public void onModalInteraction(@NotNull ModalInteractionEvent event) {
        String modalId = event.getModalId();
        event.deferReply(true).queue();
        
        if (modalId.startsWith(MODAL_PREFIX)) {
            String postId = modalId.substring(MODAL_PREFIX.length());
            String nickname = event.getValue("nickname").getAsString();

            eventSignUpService.signUp(postId, event.getUser().getId(), nickname);
            eventSignUpService.refreshSignUpMessage(postId, event.getGuild());
            event.getHook().sendMessage("Signed up as **" + nickname + "**!").setEphemeral(true).queue();
        }

        if (modalId.startsWith(MODAL_SET_CAP_PREFIX)) {
            String postId = modalId.substring(MODAL_SET_CAP_PREFIX.length());
            String capStr = event.getValue("cap_number").getAsString().trim();
            int cap;
            try {
                cap = Math.abs(Integer.parseInt(capStr));
            } catch (NumberFormatException e) {
                event.getHook().sendMessage("Please enter a valid number.").setEphemeral(true).queue();
                return;
            }
            eventSignUpService.setCap(postId, cap);
            String type = eventSignUpService.detectRebalanceType(postId);
            List<String> affected = eventSignUpService.applyRebalance(postId);
            eventSignUpService.refreshSignUpMessage(postId, event.getGuild());
            
            String capMsg = cap == 0 ? "unlimited" : String.valueOf(cap);
            // Reply to cap change
            event.getHook().setEphemeral(true).sendMessage("Cap set to " + capMsg + ".").queue();
            // Message to affected users (sent to channel, not ephemeral)
            if ("DEMOTE".equals(type)) {
                String tags = affected.stream().map(id -> "<@" + id + ">").collect(Collectors.joining(" "));
                event.getChannel().sendMessage(tags + " moved to waitlist.").queue();
            } else if ("PROMOTE".equals(type)) {
                String tags = affected.stream().map(id -> "<@" + id + ">").collect(Collectors.joining(" "));
                event.getChannel().sendMessage(tags + " moved from waitlist.").queue();
            }
        }
    }

    private void handleAttend(ButtonInteractionEvent event, String postId) {
        event.deferReply(true).queue();
        String displayName = event.getMember().getEffectiveName();
        String result = eventSignUpService.signUp(postId, event.getUser().getId(), displayName);

        String msg = switch (result) {
            case "WITHDRAWN" -> "Withdrawn from event.";
            case "WAITLIST_REMOVED" -> "Removed from waitlist.";
            case "WAITLIST" -> "Added to waitlist (event is full).";
            default -> "Signed up!";
        };
        event.getHook().sendMessage(msg).setEphemeral(true).queue();

        if ("WITHDRAWN".equals(result)) {
            String promoted = eventSignUpService.promoteOldestWaitlist(postId);
            if (promoted != null) {
                event.getHook().sendMessage("<@" + promoted + "> has been auto-promoted from the waitlist!").queue();
            }
        }

        eventSignUpService.refreshSignUpMessage(postId, event.getGuild());
    }

    private void handleKiv(ButtonInteractionEvent event, String postId) {
        event.deferReply(true).queue();
        String displayName = event.getMember().getEffectiveName();
        String result = eventSignUpService.kiv(postId, event.getUser().getId(), displayName);
        eventSignUpService.refreshSignUpMessage(postId, event.getGuild());
        String msg = "KIV_REMOVED".equals(result) ? "Removed from KIV list." : "Added to KIV list.";
        event.getHook().sendMessage(msg).setEphemeral(true).queue();
    }

    private void handleSetCap(ButtonInteractionEvent event, String postId) {
        TextInput capInput = TextInput.create("cap_number", TextInputStyle.SHORT)
                .setRequired(true)
                .setMaxLength(4)
                .setPlaceholder("Enter max attendees (0 = no cap)")
                .build();

        Label label = Label.of("Max Cap", capInput);

        Modal modal = Modal.create(MODAL_SET_CAP_PREFIX + postId, "Set Event Capacity")
                .addComponents(label)
                .build();

        event.replyModal(modal).queue();
    }

    private void handleNickname(ButtonInteractionEvent event, String postId) {
        TextInput nicknameInput = TextInput.create("nickname", TextInputStyle.SHORT)
                .setRequired(true)
                .setMaxLength(32)
                .setPlaceholder("Enter your nickname")
                .build();

        Label label = Label.of("Nickname", nicknameInput);

        Modal modal = Modal.create(MODAL_PREFIX + postId, "Sign Up with Nickname")
                .addComponents(label)
                .build();

        event.replyModal(modal).queue();
    }
}
