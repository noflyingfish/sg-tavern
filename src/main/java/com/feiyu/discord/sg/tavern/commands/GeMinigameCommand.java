package com.feiyu.discord.sg.tavern.commands;

import com.feiyu.discord.sg.tavern.entities.GeMinigameEntity;
import com.feiyu.discord.sg.tavern.repositories.GeMinigameRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Component
@AllArgsConstructor
public class GeMinigameCommand extends ListenerAdapter {
    
    GeMinigameRepository geMinigameRepository;
    
    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        
        if (event.getName().equals("seemyprediction")) {
            User user = event.getMember().getUser();
            Optional<GeMinigameEntity> geMinigameEntityOptional = geMinigameRepository.findById(user.getId());
            
            if (geMinigameEntityOptional.isEmpty()) {
                String noRecordMessage = "You didn't make your prediction yet. Do it with /makemyprediction command";
                event.reply(noRecordMessage).setEphemeral(true).queue();
            } else {
                GeMinigameEntity entity = geMinigameEntityOptional.get();
                
                String memberMessage = "You prediction for GE2025 WP's vote % is : \n" +
                        "Sengkang - " + entity.getSengkang() + "%, " +
                        "Tampines- " + entity.getTampines() + "%, " +
                        "Punggol - " + entity.getPunggol() + "%\n" +
                        "Thanks for taking part in GE2025 minigame :D";
                
                event.reply(memberMessage).setEphemeral(true).queue();
            }
        }
        
        if (event.getName().equals("makemyprediction")) {
            User user = event.getMember().getUser();
            double sengkang = event.getOption("wpsengkang").getAsDouble();
            double tampines = event.getOption("wppunggol").getAsDouble();
            double punggol = event.getOption("wptampines").getAsDouble();
            
            GeMinigameEntity geMinigameEntity = GeMinigameEntity.builder()
                    .userId(user.getId())
                    .username(user.getName())
                    .sengkang(sengkang)
                    .tampines(tampines)
                    .punggol(punggol)
                    .createdOn(LocalDateTime.now())
                    .build();
            
            geMinigameRepository.save(geMinigameEntity);
            
            log.info("A prediction for GE2025 : {}", geMinigameEntity);
            
            String memberMessage = "You prediction for GE2025 WP's vote % is : \n" +
                    "Sengkang - " + sengkang + "%, " +
                    "Tampines- " + tampines + "%, " +
                    "Punggol - " + punggol + "%\n" +
                    "Thanks for taking part in Ge2025 minigame :D";
            
            event.reply(memberMessage).setEphemeral(true).queue();
        }
    }
}
