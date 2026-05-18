package com.feiyu.discord.sg.tavern.schedulers;

import com.feiyu.discord.sg.tavern.config.ValuesConfig;
import com.feiyu.discord.sg.tavern.entities.NewJoinerEntity;
import com.feiyu.discord.sg.tavern.repositories.NewJoinerRepository;
import com.feiyu.discord.sg.tavern.services.MessageService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Component
@AllArgsConstructor
public class NewJoinerScheduler {
    
    private final JDA jda;
    private final ValuesConfig valuesConfig;
    private final NewJoinerRepository newJoinerRepository;
    private final MessageService messageService;
    
    @Async
    @Scheduled(cron = "0 30 0 * * ?", zone = "Asia/Singapore")
    public void updateNewbieScheduler() {
        log.info("NewJoinerScheduler.updateNewbieScheduler Start");

        List<NewJoinerEntity> newJoinerEntityList = newJoinerRepository.findAll();
        newJoinerEntityList.sort(Comparator.comparing(NewJoinerEntity::getJoinDateTime));
        log.info("Newbie count : {}", newJoinerEntityList.size());
        List<String> newbieNames = new ArrayList<>(newJoinerEntityList.size());
        for (NewJoinerEntity newbie : newJoinerEntityList) {
            newbieNames.add(newbie.getUsername());
        }
        log.info("Newbies: {}", newbieNames);
        
        Guild guild = jda.getGuildById(valuesConfig.getGuildId());
        for (NewJoinerEntity newbie : newJoinerEntityList) {
            try {
                User user = guild.retrieveMemberById(newbie.getUserId()).complete().getUser();
                if (newbie.getJoinDateTime().isBefore(LocalDateTime.now().minusMonths(3L))) {
                    Role newJoinerRole = guild.getRoleById(valuesConfig.getNewJoinerRoleId());
                    
                    newJoinerRepository.delete(newbie);
                    log.info("Member not longer a newbie : {}", newbie.getUsername());
                    
                    guild.removeRoleFromMember(user, newJoinerRole).queue();
                    String adminRoleMessage = user.getEffectiveName() + " - " +  user.getName() + " removed role : " + newJoinerRole.getName();
                    messageService.sendAdminChannelMessage(guild, adminRoleMessage);
                }
            } catch (ErrorResponseException ex) {
                System.out.println(ex.getErrorCode());;
                if (10007 == ex.getErrorCode()) { // 10007 : Unknown Member
                    log.info("Member already left server: {}", newbie.getUsername());
                    newJoinerRepository.delete(newbie);
                }
            } catch (Exception e){
                log.error("Error at retrieving member with {}", newbie.getUsername(), e);
            }
        }
        log.info("NewJoinerScheduler.updateNewbieScheduler End");
    }
}
