package com.feiyu.discord.sg.tavern.listeners;

import com.feiyu.discord.sg.tavern.services.EventSignUpService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.feiyu.discord.sg.tavern.entities.EventAttendanceEntity;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.modals.Modal;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@AllArgsConstructor
public class EventSignUpListener extends ListenerAdapter {

    private static final String ATTEND_PREFIX = "event:signup:attend:";
    private static final String KIV_PREFIX = "event:signup:kiv:";
    private static final String RESERVE_PREFIX = "event:signup:reserve:";
    private static final String MY_SLOTS_PREFIX = "event:signup:myslots:";
    private static final String SET_CAP_PREFIX = "event:signup:setcap:";
    private static final String EDIT_SLOT_PREFIX = "event:slots:edit:";
    private static final String REMOVE_SLOT_PREFIX = "event:slots:remove:";
    private static final String MODAL_SET_CAP_PREFIX = "event:signup:modal:setcap:";
    private static final String MODAL_EDIT_SLOT_PREFIX = "event:modal:editslot:";
    private static final String SLOT_SELECT_PREFIX = "event:slots:select:";
    private static final String BACK_TO_SLOTS_PREFIX = "event:slots:back:";
    private static final String PROMOTE_SLOT_PREFIX = "event:slots:promote:";

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
        if (componentId.startsWith(RESERVE_PREFIX)) {
            handleReserve(event, componentId.substring(RESERVE_PREFIX.length()));
            return;
        }
        if (componentId.startsWith(MY_SLOTS_PREFIX)) {
            handleMySlots(event, componentId.substring(MY_SLOTS_PREFIX.length()));
            return;
        }
        if (componentId.startsWith(EDIT_SLOT_PREFIX)) {
            handleEditSlot(event, componentId.substring(EDIT_SLOT_PREFIX.length()));
            return;
        }
        if (componentId.startsWith(REMOVE_SLOT_PREFIX)) {
            handleRemoveSlot(event, componentId.substring(REMOVE_SLOT_PREFIX.length()));
            return;
        }
        if (componentId.startsWith(BACK_TO_SLOTS_PREFIX)) {
            handleBackToSlots(event, componentId.substring(BACK_TO_SLOTS_PREFIX.length()));
            return;
        }
        if (componentId.startsWith(PROMOTE_SLOT_PREFIX)) {
            handlePromoteSlot(event, componentId.substring(PROMOTE_SLOT_PREFIX.length()));
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
        
        if (modalId.startsWith(MODAL_EDIT_SLOT_PREFIX)) {
            Long attendanceId = Long.parseLong(modalId.substring(MODAL_EDIT_SLOT_PREFIX.length()));
            String remark = event.getValue("displayName").getAsString();

            boolean updated = eventSignUpService.editSlotDisplayName(attendanceId, remark);
            if (updated) {
                var slot = eventSignUpService.getSlotById(attendanceId);
                if (slot != null) {
                    eventSignUpService.refreshSignUpMessage(slot.getPostId(), event.getGuild());
                }
                event.getHook().sendMessage("Display name updated to **" + remark + "**. Reopen My Slots to see changes.").setEphemeral(true).queue();
            } else {
                event.getHook().sendMessage("Slot not found.").setEphemeral(true).queue();
            }
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
            if ("DEMOTE".equals(type) && !affected.isEmpty()) {
                String tags = affected.stream().map(id -> "<@" + id + ">").collect(Collectors.joining(" "));
                event.getChannel().sendMessage(tags + " moved to waitlist.").queue();
            }
        }
    }

    @Override
    public void onStringSelectInteraction(@NotNull StringSelectInteractionEvent event) {
        String componentId = event.getComponentId();
        if (componentId.startsWith(SLOT_SELECT_PREFIX)) {
            handleSlotSelect(event, componentId.substring(SLOT_SELECT_PREFIX.length()));
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

    private void handleReserve(ButtonInteractionEvent event, String postId) {
        event.deferReply(true).queue();
        String remark = event.getMember().getEffectiveName() + " +1";
        String status = eventSignUpService.reserveSlot(postId, event.getUser().getId(), remark);
        eventSignUpService.refreshSignUpMessage(postId, event.getGuild());
        String msg = "WAITLIST".equals(status)
                ? "Reserved slot (waitlist — event is full)."
                : "Reserved slot!";
        event.getHook().sendMessage(msg).setEphemeral(true).queue();
    }

    private MessageEmbed buildSlotsEmbed(List<EventAttendanceEntity> slots) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("Your Slots")
                .setColor(Color.CYAN);
        StringBuilder desc = new StringBuilder();
        int i = 1;
        for (var slot : slots) {
            desc.append("**#").append(i).append("** ").append(slot.getDisplayName())
                    .append(" [").append(slot.getStatus()).append("]\n");
            i++;
        }
        embed.setDescription(desc.toString());
        return embed.build();
    }

    private StringSelectMenu buildSlotSelectMenu(String postId, List<EventAttendanceEntity> slots) {
        StringSelectMenu.Builder menu = StringSelectMenu.create(SLOT_SELECT_PREFIX + postId)
                .setPlaceholder("Select a slot to manage...")
                .setRequiredRange(1, 1);
        int i = 1;
        int added = 0;
        for (var slot : slots) {
            if (added >= 25) break;
            String label = "#" + i + " — " + slot.getDisplayName() + " [" + slot.getStatus() + "]";
            if (label.length() > 100) {
                label = label.substring(0, 97) + "...";
            }
            menu.addOption(label, String.valueOf(slot.getId()));
            i++;
            added++;
        }
        return menu.build();
    }

    private void handleMySlots(ButtonInteractionEvent event, String postId) {
        event.deferReply(true).queue();

        String userId = event.getUser().getId();
        List<EventAttendanceEntity> slots = eventSignUpService.getUserSlots(postId, userId);

        if (slots.isEmpty()) {
            event.getHook().sendMessage("You have no sign up for this event.")
                    .setEphemeral(true).queue();
            return;
        }

        event.getHook().sendMessageEmbeds(buildSlotsEmbed(slots))
                .addComponents(ActionRow.of(buildSlotSelectMenu(postId, slots)))
                .setEphemeral(true)
                .queue();
    }

    private void handleSlotSelect(StringSelectInteractionEvent event, String postId) {
        String selectedValue = event.getValues().get(0);
        Long attendanceId = Long.parseLong(selectedValue);
        var slot = eventSignUpService.getSlotById(attendanceId);

        if (slot == null) {
            event.deferEdit().queue();
            event.getHook().editOriginal("This slot no longer exists.")
                    .setComponents().queue();
            return;
        }

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("Slot Details")
                .setColor(Color.CYAN);
        embed.setDescription("**Display Name:** " + slot.getDisplayName() + "\n"
                + "**Status:** " + slot.getStatus() + "\n"
                + "**Type:** " + (Boolean.TRUE.equals(slot.getIsMain()) ? "Main" : "Reserve"));

        event.deferEdit().queue();

        List<Button> buttons = new ArrayList<>();
        if ("WAITLIST".equals(slot.getStatus())) {
            buttons.add(Button.success(PROMOTE_SLOT_PREFIX + slot.getId(), "Move to Attending"));
        }
        buttons.add(Button.secondary(EDIT_SLOT_PREFIX + slot.getId(), "Edit Display Name"));
        buttons.add(Button.danger(REMOVE_SLOT_PREFIX + slot.getId(), "Remove"));
        buttons.add(Button.secondary(BACK_TO_SLOTS_PREFIX + postId, "⬅ Back"));

        event.getHook().editOriginalEmbeds(embed.build())
                .setComponents(ActionRow.of(buttons))
                .queue();
    }

    private void handleBackToSlots(ButtonInteractionEvent event, String postId) {
        event.deferEdit().queue();
        String userId = event.getUser().getId();
        List<EventAttendanceEntity> slots = eventSignUpService.getUserSlots(postId, userId);

        if (slots.isEmpty()) {
            event.getHook().editOriginal("You have no sign up for this event.")
                    .setComponents().queue();
            return;
        }

        event.getHook().editOriginalEmbeds(buildSlotsEmbed(slots))
                .setComponents(ActionRow.of(buildSlotSelectMenu(postId, slots)))
                .queue();
    }

    private void handlePromoteSlot(ButtonInteractionEvent event, String attendanceIdStr) {
        Long attendanceId = Long.parseLong(attendanceIdStr);
        event.deferEdit().queue();

        String result = eventSignUpService.promoteSlot(attendanceId);

        if ("PROMOTED".equals(result)) {
            var slot = eventSignUpService.getSlotById(attendanceId);
            if (slot != null) {
                eventSignUpService.refreshSignUpMessage(slot.getPostId(), event.getGuild());
                EmbedBuilder embed = new EmbedBuilder()
                        .setTitle("Slot Details")
                        .setColor(Color.CYAN);
                embed.setDescription("**Display Name:** " + slot.getDisplayName() + "\n"
                        + "**Status:** " + slot.getStatus() + "\n"
                        + "**Type:** " + (Boolean.TRUE.equals(slot.getIsMain()) ? "Main" : "Reserve"));

                List<Button> buttons = new ArrayList<>();
                buttons.add(Button.secondary(EDIT_SLOT_PREFIX + slot.getId(), "Edit Display Name"));
                buttons.add(Button.danger(REMOVE_SLOT_PREFIX + slot.getId(), "Remove"));
                buttons.add(Button.secondary(BACK_TO_SLOTS_PREFIX + slot.getPostId(), "⬅ Back"));

                event.getHook().editOriginalEmbeds(embed.build())
                        .setComponents(ActionRow.of(buttons))
                        .queue();
            }
            return;
        }

        String msg = switch (result) {
            case "FULL" -> "Event is still full, cannot promote.";
            case "ALREADY_ATTENDING" -> "Already attending.";
            default -> "Slot not found.";
        };
        event.getHook().sendMessage(msg).setEphemeral(true).queue();
    }

    private void handleEditSlot(ButtonInteractionEvent event, String attendanceIdStr) {
        Long attendanceId = Long.parseLong(attendanceIdStr);
        var slot = eventSignUpService.getSlotById(attendanceId);
        if (slot == null) {
            event.deferReply(true).queue(hook ->
                    hook.sendMessage("No sign up found.").setEphemeral(true).queue());
            return;
        }

        TextInput displayedNameInput = TextInput.create("displayName", TextInputStyle.SHORT)
                .setRequired(true)
                .setMaxLength(32)
                .setValue(slot.getDisplayName())
                .setPlaceholder("Nickname")
                .build();

        Modal modal = Modal.create(MODAL_EDIT_SLOT_PREFIX + attendanceId, "Edit Displayed Name")
                .addComponents(Label.of("Displayed Name", displayedNameInput))
                .build();

        event.replyModal(modal).queue();
    }

    private void handleRemoveSlot(ButtonInteractionEvent event, String attendanceIdStr) {
        Long attendanceId = Long.parseLong(attendanceIdStr);
        event.deferEdit().queue();

        String postId = eventSignUpService.removeSlot(attendanceId);
        if (postId == null) {
            event.getHook().sendMessage("This slot was already removed.")
                    .setEphemeral(true).queue();
            return;
        }

        eventSignUpService.refreshSignUpMessage(postId, event.getGuild());

        List<EventAttendanceEntity> remainingSlots =
                eventSignUpService.getUserSlots(postId, event.getUser().getId());

        if (remainingSlots.isEmpty()) {
            event.getHook().editOriginal("You have no sign up for this event.")
                    .setComponents().queue();
            return;
        }

        event.getHook().editOriginalEmbeds(buildSlotsEmbed(remainingSlots))
                .setComponents(ActionRow.of(buildSlotSelectMenu(postId, remainingSlots)))
                .queue();
    }
}
