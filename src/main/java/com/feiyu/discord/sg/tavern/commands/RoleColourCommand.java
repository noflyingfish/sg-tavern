package com.feiyu.discord.sg.tavern.commands;

import com.feiyu.discord.sg.tavern.config.ValuesConfig;
import com.feiyu.discord.sg.tavern.entities.RoleColourEntity;
import com.feiyu.discord.sg.tavern.repositories.RoleColourRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Slf4j
@Component
@AllArgsConstructor
public class RoleColourCommand extends ListenerAdapter {
    
    private final ValuesConfig valuesConfig;
    private final RoleColourRepository roleColourRepository;
    
    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        String cssInput = event.getOption("colour").getAsString().toUpperCase();
        Color color;
        
        // Create colour string
        if ("random".equalsIgnoreCase(cssInput)) {
            Random random = new Random();
            int red = random.nextInt(256);
            int green = random.nextInt(256);
            int blue = random.nextInt(256);
            // Hexadecimal Formatting with zero padding
            color = new Color(red, green, blue);
            log.info("Random colour role : {}", event.getUser().getName());
            
            event.reply("This is still work in progress OTL").setEphemeral(true).queue();
            // createRandomColourRole(event, color, cssInput);
        } else if (cssInput.matches("^#([A-Fa-f0-9]{6})$")) {
            // deferReply used cuz operation >3s
            event.deferReply(true).queue();
            
            log.info("Input colour : {}, User : {} ", cssInput, event.getUser().getName());
            int red = Integer.parseInt(cssInput.substring(1, 3), 16);
            int green = Integer.parseInt(cssInput.substring(3, 5), 16);
            int blue = Integer.parseInt(cssInput.substring(5, 7), 16);
            color = new Color(red, green, blue);
            
            Role newRole = createAddChosenColour(event, color, cssInput);
            roleCleanUp(event);
            updateDatabase(event, newRole);
            
            event.getHook().sendMessage("Done :)").queue();
        } else {
            event.reply("Please enter a valid CSS code").setEphemeral(true).queue();
        }
    }
    
    private Role createAddChosenColour(SlashCommandInteractionEvent event, Color color, String cssInput) {
        // Role management stuffs
        Guild guild = event.getGuild();
        List<RoleColourEntity> existingRoleColourList = roleColourRepository.findAllByColourCode(cssInput);
        if (existingRoleColourList.isEmpty()) {
            // new colour role
            Role newColourRole = event.getGuild().createRole()
                    .setPermissions(Permission.EMPTY_PERMISSIONS)
                    .setColor(color)
                    .setName(cssInput)
                    .complete();
            Role nameColourAnchor = guild.getRoleById(valuesConfig.getNameColourAnchorRoleId());
            // using an anchor with moveBelow
            // moveTo is weird as its input value index is flipped from role.getPositionRaw()
            guild.modifyRolePositions()
                    .selectPosition(newColourRole)
                    .moveBelow(nameColourAnchor)
                    .queue();
            guild.addRoleToMember(event.getUser(), newColourRole)
                    .queue();
            log.info("New colour role created. Added to user");
            return newColourRole;
        } else {
            // existing colour role
            RoleColourEntity roleColourEntity = existingRoleColourList.get(0);
            Role existing = guild.getRoleById(roleColourEntity.getRoleId());
            guild.addRoleToMember(event.getUser(), existing).queue();
            log.info("Existing colour role found. Added to user.");
            return existing;
        }
    }
    
    private void roleCleanUp(SlashCommandInteractionEvent event) {
        Guild guild = event.getGuild();
        Optional<RoleColourEntity> existingUserOptional = roleColourRepository.findById(event.getUser().getId());
        // user has existing role
        if (existingUserOptional.isPresent()) {
            RoleColourEntity entity = existingUserOptional.get();
            Role previousColourRole = guild.getRoleById(entity.getRoleId());
            guild.removeRoleFromMember(event.getUser(),previousColourRole).complete();
            log.info("Removed old colour role from user : {}", previousColourRole.getName());
            guild.findMembersWithRoles(previousColourRole).onSuccess(members -> {
                if (members.isEmpty()) {
                    previousColourRole.delete().complete();
                    log.info("No user. Colour role removed : {}", previousColourRole.getName());
                }
            });
        }
    }
    
    private void updateDatabase(SlashCommandInteractionEvent event, Role newRole) {
        Optional<RoleColourEntity> existingUserOptional = roleColourRepository.findById(event.getUser().getId());
        if (existingUserOptional.isPresent()) {
            RoleColourEntity entity = existingUserOptional.get();
            //update entity
            entity.setRoleId(newRole.getId());
            entity.setColourCode(newRole.getName());
            entity.setUpdatedOn(LocalDateTime.now());
            roleColourRepository.save(entity);
        } else {
            //create new entity
            RoleColourEntity entity = RoleColourEntity.builder()
                    .userId(event.getUser().getId())
                    .username(event.getUser().getName())
                    .colourCode(newRole.getName())
                    .roleId(newRole.getId())
                    .updatedOn(LocalDateTime.now())
                    .build();
            roleColourRepository.save(entity);
        }
    }
    
    private void createRandomColourRole(SlashCommandInteractionEvent event, Color color, String cssInput) {
    
    }
    
}

