package com.feiyu.discord.sg.tavern.listeners;

import com.feiyu.discord.sg.tavern.entities.EventAttendanceEntity;
import com.feiyu.discord.sg.tavern.entities.EventEntity;
import com.feiyu.discord.sg.tavern.services.EventManageService;
import com.feiyu.discord.sg.tavern.services.EventSignUpService;
import com.feiyu.discord.sg.tavern.utils.StringUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.modals.Modal;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Component
@AllArgsConstructor
public class EventSignUpListener extends ListenerAdapter {
    
    private static final String ATTEND_PREFIX = "event:signup:attend:";
    private static final String KIV_PREFIX = "event:signup:kiv:";
    private static final String RESERVE_PREFIX = "event:signup:reserve:";
    private static final String MY_SLOTS_PREFIX = "event:signup:myslots:";
    private static final String MANAGE_EVENT_PREFIX = "event:signup:manageevent:";
    private static final String EDIT_SLOT_PREFIX = "event:slots:edit:";
    private static final String REMOVE_SLOT_PREFIX = "event:slots:remove:";
    private static final String MODAL_MANAGE_EVENT_PREFIX = "event:signup:modal:manageevent:";
    private static final String MODAL_EDIT_SLOT_PREFIX = "event:modal:editslot:";
    private static final String SLOT_SELECT_PREFIX = "event:slots:select:";
    private static final String BACK_TO_SLOTS_PREFIX = "event:slots:back:";
    private static final String PROMOTE_SLOT_PREFIX = "event:slots:promote:";
    private static final String MANAGE_EDIT_PREFIX = "event:manage:edit:";
    private static final String MANAGE_MEMBERS_PREFIX = "event:manage:members:";
    private static final String MEMBER_SELECT_PREFIX = "event:members:select:";
    private static final String REMOVE_MEMBER_PREFIX = "event:members:remove:";
    private static final String MEMBER_BACK_PREFIX = "event:members:back:";

    private final EventSignUpService eventSignUpService;
    private final EventManageService eventManageService;
    
    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        String componentId = event.getComponentId();
        
        if (componentId.startsWith(ATTEND_PREFIX)) {
            handleAttend(event, componentId.substring(ATTEND_PREFIX.length()));
            return;
        }
        if (componentId.startsWith(RESERVE_PREFIX)) {
            handleReserve(event, componentId.substring(RESERVE_PREFIX.length()));
            return;
        }
        if (componentId.startsWith(KIV_PREFIX)) {
            handleKiv(event, componentId.substring(KIV_PREFIX.length()));
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
        if (componentId.startsWith(MANAGE_EDIT_PREFIX)) {
            handleManageEdit(event, componentId.substring(MANAGE_EDIT_PREFIX.length()));
            return;
        }
        if (componentId.startsWith(MANAGE_MEMBERS_PREFIX)) {
            handleManageMembers(event, componentId.substring(MANAGE_MEMBERS_PREFIX.length()));
            return;
        }
        if (componentId.startsWith(REMOVE_MEMBER_PREFIX)) {
            handleRemoveMember(event, componentId.substring(REMOVE_MEMBER_PREFIX.length()));
            return;
        }
        if (componentId.startsWith(MEMBER_BACK_PREFIX)) {
            handleMemberBack(event, componentId.substring(MEMBER_BACK_PREFIX.length()));
            return;
        }
        if (componentId.startsWith(MANAGE_EVENT_PREFIX)) {
            handleManageEvent(event, componentId.substring(MANAGE_EVENT_PREFIX.length()));
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
        
        if (modalId.startsWith(MODAL_MANAGE_EVENT_PREFIX)) {
            String postId = modalId.substring(MODAL_MANAGE_EVENT_PREFIX.length());
            
            String eventName = getModalValue(event, "event_name");
            String eventLocation = getModalValue(event, "event_location");
            String eventDateTime = getModalValue(event, "event_datetime");
            String capStr = getModalValue(event, "max_cap");
            
            log.info("Event update attempt for postId: {} | name={} location={} datetime={} maxCap={}",
                    postId, eventName, eventLocation,
                    eventDateTime, capStr);
            
            Integer maxCap = null;
            if (capStr != null && !capStr.isBlank()) {
                try {
                    maxCap = Math.abs(Integer.parseInt(capStr.trim()));
                } catch (NumberFormatException e) {
                    event.getHook().sendMessage("Invalid max cap number.").setEphemeral(true).queue();
                    return;
                }
            }
            
            try {
                eventManageService.updateEvent(postId, eventName, eventLocation, eventDateTime, maxCap);
            } catch (IllegalArgumentException ex) {
                event.getHook().sendMessage(ex.getMessage()).setEphemeral(true).queue();
                return;
            }
            
            String type = eventSignUpService.detectRebalanceType(postId);
            List<String> affected = eventSignUpService.applyRebalance(postId);
            eventSignUpService.refreshSignUpMessage(postId, event.getGuild());
            
            event.getHook().setEphemeral(true).sendMessage("Event updated.").queue();
            if ("DEMOTE".equals(type) && !affected.isEmpty()) {
                String tags = affected.stream().map(id -> "<@" + id + ">").collect(Collectors.joining(" "));
                event.getChannel().sendMessage(tags + " moved to waitlist.").queue();
            }
        }
    }
    
    @Override
    public void onStringSelectInteraction(@NotNull StringSelectInteractionEvent event) {
        String componentId = event.getComponentId();
        if (componentId.startsWith(MEMBER_SELECT_PREFIX)) {
            handleMemberSelect(event, componentId.substring(MEMBER_SELECT_PREFIX.length()));
            return;
        }
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
    
    private void handleReserve(ButtonInteractionEvent event, String postId) {
        event.deferReply(true).queue();
        String displayName = event.getMember().getEffectiveName();
        String status = eventSignUpService.reserveSlot(postId, event.getUser().getId(), displayName);
        eventSignUpService.refreshSignUpMessage(postId, event.getGuild());
        String msg = "WAITLIST".equals(status)
                ? "Reserved slot (waitlist — event is full)."
                : "Reserved slot!";
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
    
    private void handleManageEvent(ButtonInteractionEvent event, String postId) {
        event.deferReply(true).queue();
        event.getHook().sendMessage("Manage Event")
                .addComponents(ActionRow.of(
                        Button.danger(MANAGE_EDIT_PREFIX + postId, "Edit Event Details"),
                        Button.danger(MANAGE_MEMBERS_PREFIX + postId, "Edit Event Members")))
                .setEphemeral(true).queue();
    }

    private void handleManageEdit(ButtonInteractionEvent event, String postId) {
        Modal modal = buildManageEventModal(postId);
        if (modal == null) {
            event.reply("Event not found.").setEphemeral(true).queue();
            return;
        }
        event.replyModal(modal).queue();
    }

    private Modal buildManageEventModal(String postId) {
        Optional<EventEntity> optEntity = eventManageService.getEvent(postId);
        if (optEntity.isEmpty()) {
            return null;
        }
        EventEntity entity = optEntity.get();
        log.info("EventEntity extracted : {}", entity);

        TextInput nameInput = TextInput.create("event_name", TextInputStyle.SHORT)
                .setRequired(false)
                .setMaxLength(100)
                .setValue(StringUtils.truncateTo(entity.getProcessedEventName(), 100, "No event name detected"))
                .setPlaceholder("Event Name")
                .build();

        TextInput locationInput = TextInput.create("event_location", TextInputStyle.SHORT)
                .setRequired(false)
                .setMaxLength(100)
                .setValue(StringUtils.truncateTo(entity.getProcessedEventLocation(), 100, "No event location detected"))
                .setPlaceholder("Event Location")
                .build();

        TextInput dateTimeInput = TextInput.create("event_datetime", TextInputStyle.SHORT)
                .setRequired(false)
                .setMaxLength(16)
                .setValue(StringUtils.datetimeToString(entity.getProcessedEventDateTime(), "No datetime"))
                .setPlaceholder("yyyy-MM-dd HH:mm")
                .build();

        TextInput capInput = TextInput.create("max_cap", TextInputStyle.SHORT)
                .setRequired(false)
                .setMaxLength(4)
                .setValue(StringUtils.intToString(entity.getMaxCap(), 4, "0"))
                .setPlaceholder("0 = unlimited")
                .build();

        return Modal.create(MODAL_MANAGE_EVENT_PREFIX + postId, "Manage Event")
                .addComponents(
                        Label.of("Event Name", nameInput),
                        Label.of("Event Location", locationInput),
                        Label.of("Event DateTime (yyyy-MM-dd HH:mm) format only", dateTimeInput),
                        Label.of("Max Cap (0 = unlimited)", capInput)
                )
                .build();
    }
    
    private String getModalValue(ModalInteractionEvent event, String key) {
        var mapping = event.getValue(key);
        if (mapping == null) return null;
        String value = mapping.getAsString();
        return (value != null && !value.isBlank()) ? value.trim() : null;
    }
    
    private MessageEmbed buildListEmbed(String title, List<EventAttendanceEntity> slots, Color color) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle(title)
                .setColor(color);
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

    private MessageEmbed buildMemberDetailsEmbed(String title, EventAttendanceEntity slot) {
        return new EmbedBuilder()
                .setTitle(title)
                .setColor(Color.CYAN)
                .setDescription("**Display Name:** " + slot.getDisplayName() + "\n"
                        + "**Status:** " + slot.getStatus())
                .build();
    }

    private StringSelectMenu buildSelectMenu(String componentId, String placeholder, List<EventAttendanceEntity> slots) {
        StringSelectMenu.Builder menu = StringSelectMenu.create(componentId)
                .setPlaceholder(placeholder)
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
        
        event.getHook().sendMessageEmbeds(buildListEmbed("Your Slots", slots, Color.CYAN))
                .addComponents(ActionRow.of(buildSelectMenu(SLOT_SELECT_PREFIX + postId, "Select a slot to manage...", slots)))
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
        
        MessageEmbed embed = buildMemberDetailsEmbed("Slot Details", slot);
        
        event.deferEdit().queue();
        
        List<Button> buttons = new ArrayList<>();
        if ("WAITLIST".equals(slot.getStatus())) {
            buttons.add(Button.success(PROMOTE_SLOT_PREFIX + slot.getId(), "Move to Attending"));
        }
        buttons.add(Button.secondary(EDIT_SLOT_PREFIX + slot.getId(), "Edit Display Name"));
        buttons.add(Button.danger(REMOVE_SLOT_PREFIX + slot.getId(), "Remove"));
        buttons.add(Button.secondary(BACK_TO_SLOTS_PREFIX + postId, "⬅ Back"));
        
        event.getHook().editOriginalEmbeds(embed)
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
        
        event.getHook().editOriginalEmbeds(buildListEmbed("Your Slots", slots, Color.CYAN))
                .setComponents(ActionRow.of(buildSelectMenu(SLOT_SELECT_PREFIX + postId, "Select a slot to manage...", slots)))
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
                MessageEmbed embed = buildMemberDetailsEmbed("Slot Details", slot);
                
                List<Button> buttons = new ArrayList<>();
                buttons.add(Button.secondary(EDIT_SLOT_PREFIX + slot.getId(), "Edit Display Name"));
                buttons.add(Button.danger(REMOVE_SLOT_PREFIX + slot.getId(), "Remove"));
                buttons.add(Button.secondary(BACK_TO_SLOTS_PREFIX + slot.getPostId(), "⬅ Back"));
                
                event.getHook().editOriginalEmbeds(embed)
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
        
        EventAttendanceEntity removed = eventSignUpService.removeSlot(attendanceId);
        if (removed == null) {
            event.getHook().sendMessage("This slot was already removed.")
                    .setEphemeral(true).queue();
            return;
        }
        String postId = removed.getPostId();
        
        eventSignUpService.refreshSignUpMessage(postId, event.getGuild());
        
        List<EventAttendanceEntity> remainingSlots =
                eventSignUpService.getUserSlots(postId, event.getUser().getId());
        
        if (remainingSlots.isEmpty()) {
            event.getHook().editOriginal("You have no sign up for this event.")
                    .setComponents().queue();
            return;
        }
        
        event.getHook().editOriginalEmbeds(buildListEmbed("Your Slots", remainingSlots, Color.CYAN))
                .setComponents(ActionRow.of(buildSelectMenu(SLOT_SELECT_PREFIX + postId, "Select a slot to manage...", remainingSlots)))
                .queue();
    }

    private void handleManageMembers(ButtonInteractionEvent event, String postId) {
        event.deferReply(true).queue();
        List<EventAttendanceEntity> members = eventSignUpService.getAllAttendees(postId);

        if (members.isEmpty()) {
            event.getHook().sendMessage("No members yet.").setEphemeral(true).queue();
            return;
        }

        event.getHook().sendMessageEmbeds(buildListEmbed("Event Members", members, Color.CYAN))
                .addComponents(ActionRow.of(buildSelectMenu(MEMBER_SELECT_PREFIX + postId, "Select a member to manage...", members)))
                .setEphemeral(true)
                .queue();
    }

    private void handleMemberSelect(StringSelectInteractionEvent event, String postId) {
        String selectedValue = event.getValues().get(0);
        Long attendanceId = Long.parseLong(selectedValue);
        var member = eventSignUpService.getSlotById(attendanceId);

        if (member == null) {
            event.deferEdit().queue();
            event.getHook().editOriginal("This member no longer exists.")
                    .setComponents().queue();
            return;
        }

        MessageEmbed embed = buildMemberDetailsEmbed("Member Details", member);

        event.deferEdit().queue();

        List<Button> buttons = new ArrayList<>();
        buttons.add(Button.secondary(EDIT_SLOT_PREFIX + member.getId(), "Edit Display Name"));
        buttons.add(Button.danger(REMOVE_MEMBER_PREFIX + member.getId(), "Remove"));
        buttons.add(Button.secondary(MEMBER_BACK_PREFIX + postId, "⬅ Back"));

        event.getHook().editOriginalEmbeds(embed)
                .setComponents(ActionRow.of(buttons))
                .queue();
    }

    private void handleMemberBack(ButtonInteractionEvent event, String postId) {
        event.deferEdit().queue();
        List<EventAttendanceEntity> members = eventSignUpService.getAllAttendees(postId);

        if (members.isEmpty()) {
            event.getHook().editOriginal("No members yet.")
                    .setComponents().queue();
            return;
        }

        event.getHook().editOriginalEmbeds(buildListEmbed("Event Members", members, Color.CYAN))
                .setComponents(ActionRow.of(buildSelectMenu(MEMBER_SELECT_PREFIX + postId, "Select a member to manage...", members)))
                .queue();
    }

    private void handleRemoveMember(ButtonInteractionEvent event, String attendanceIdStr) {
        Long attendanceId = Long.parseLong(attendanceIdStr);
        event.deferEdit().queue();

        EventAttendanceEntity removed = eventSignUpService.removeSlot(attendanceId);
        if (removed == null) {
            event.getHook().editOriginal("This member was already removed.")
                    .setComponents().queue();
            return;
        }

        eventSignUpService.refreshSignUpMessage(removed.getPostId(), event.getGuild());

        String removerName = event.getUser().getName();
        event.getChannel().sendMessage(
                removerName + " removed " + removed.getDisplayName() + " from this event.").queue();

        List<EventAttendanceEntity> remaining = eventSignUpService.getAllAttendees(removed.getPostId());
        if (remaining.isEmpty()) {
            event.getHook().editOriginal("No members yet.")
                    .setComponents().queue();
            return;
        }

        event.getHook().editOriginalEmbeds(buildListEmbed("Event Members", remaining, Color.CYAN))
                .setComponents(ActionRow.of(buildSelectMenu(MEMBER_SELECT_PREFIX + removed.getPostId(), "Select a member to manage...", remaining)))
                .queue();
    }
}
