package com.feiyu.discord.sg.tavern.schedulers;

import com.feiyu.discord.sg.tavern.entities.MemberEntity;
import com.feiyu.discord.sg.tavern.repositories.MemberRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@AllArgsConstructor
public class MemberScheduler {
    
    private final MemberRepository memberRepository;
    
    @Async
    @Scheduled(cron = "0 5 0 1 * ?", zone = "Asia/Singapore")
    public void inviteLinkCounterScheduler() {
        log.info("MemberScheduler.inviteLinkCounterScheduler Start");
        
        List<MemberEntity> memberEntityList = memberRepository.findAll();
        memberEntityList.forEach(memberEntity -> {
            memberEntity.setInviteLinkCounter(0);
            memberEntity.setUpdatedOn(LocalDateTime.now());
        });
        memberRepository.saveAll(memberEntityList);
        
        log.info("MemberScheduler.inviteLinkCounterScheduler End");
    }
}
