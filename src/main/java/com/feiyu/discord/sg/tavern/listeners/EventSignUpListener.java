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

@Slf4j
@Component
@AllArgsConstructor
public class EventSignUpListener extends ListenerAdapter {

    private static final String ATTEND_PREFIX = "event:signup:attend:";
    private static final String KIV_PREFIX = "event:signup:kiv:";
    private static final String NICKNAME_PREFIX = "event:signup:nickname:";
    private static final String MODAL_PREFIX = "event:signup:modal:nickname:";

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
        }
    }

    @Override
    public void onModalInteraction(@NotNull ModalInteractionEvent event) {
        String modalId = event.getModalId();

        if (modalId.startsWith(MODAL_PREFIX)) {
            String postId = modalId.substring(MODAL_PREFIX.length());
            String nickname = event.getValue("nickname").getAsString();

            event.deferReply(true).queue();
            eventSignUpService.signUp(postId, event.getUser().getId(), nickname);
            eventSignUpService.refreshSignUpMessage(postId, event.getGuild());
            event.getHook().sendMessage("Signed up as **" + nickname + "**!").setEphemeral(true).queue();
        }
    }

    private void handleAttend(ButtonInteractionEvent event, String postId) {
        event.deferReply(true).queue();
        String displayName = event.getMember().getEffectiveName();
        String result = eventSignUpService.signUp(postId, event.getUser().getId(), displayName);
        eventSignUpService.refreshSignUpMessage(postId, event.getGuild());
        String msg = "WITHDRAWN".equals(result) ? "Withdrawn from event." : "Signed up!";
        event.getHook().sendMessage(msg).setEphemeral(true).queue();
    }

    private void handleKiv(ButtonInteractionEvent event, String postId) {
        event.deferReply(true).queue();
        String displayName = event.getMember().getEffectiveName();
        String result = eventSignUpService.kiv(postId, event.getUser().getId(), displayName);
        eventSignUpService.refreshSignUpMessage(postId, event.getGuild());
        String msg = "KIV_REMOVED".equals(result) ? "Removed from KIV list." : "Added to KIV list.";
        event.getHook().sendMessage(msg).setEphemeral(true).queue();
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
