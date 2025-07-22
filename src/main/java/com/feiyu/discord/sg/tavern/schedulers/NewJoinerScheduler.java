package com.feiyu.discord.sg.tavern.schedulers;

import com.feiyu.discord.sg.tavern.config.ValuesConfig;
import com.feiyu.discord.sg.tavern.entities.NewJoinerEntity;
import com.feiyu.discord.sg.tavern.repositories.NewJoinerRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@AllArgsConstructor
public class NewJoinerScheduler {
    
    private final JDA jda;
    private final ValuesConfig valuesConfig;
    private final NewJoinerRepository newJoinerRepository;
    
    @Async
    @Scheduled(cron = "0 0 0 * * ?", zone = "Asia/Singapore")
    public void updateNewbieScheduler() {
        log.info("NewJoinerScheduler.updateNewbieScheduler Start");
        
        List<NewJoinerEntity> newJoinerEntityList = newJoinerRepository.findAll();
        log.info("Newbie count : {}", newJoinerEntityList.size());
        
        Guild guild = jda.getGuildById(valuesConfig.getGuildId());
        for (NewJoinerEntity newbie : newJoinerEntityList) {
            try {
                Member member = guild.retrieveMemberById(newbie.getUserId()).complete();
                if (newbie.getJoinDateTime().isBefore(LocalDateTime.now().minusMonths(3L))) {
                    guild.removeRoleFromMember(member.getUser(), guild.getRoleById(valuesConfig.getNewJoinerRoleId())).queue();
                    log.info("Member not longer a newbie : {}", newbie);
                    newJoinerRepository.delete(newbie);
                } else {
                    log.info("Member still newbie : {}", newbie);
                }
            } catch (ErrorResponseException ex) {
                System.out.println(ex.getErrorCode());;
                if (10007 == ex.getErrorCode()) { // 10007 : Unknown Member
                    log.info("Member already left server: {}", newbie);
                    newJoinerRepository.delete(newbie);
                }
            } catch (Exception e){
                log.error("Error at retrieving member with {}", newbie);
            }
        }
        log.info("NewJoinerScheduler.updateNewbieScheduler End");
    }
}
