package com.feiyu.discord.sg.tavern.services;

import com.feiyu.discord.sg.tavern.entities.MemberEntity;
import com.feiyu.discord.sg.tavern.repositories.MemberRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.User;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@AllArgsConstructor
public class MemberService {
    
    private final MemberRepository memberRepository;
    
    public boolean allowedToCreateInvite(User user) {
        
        Optional<MemberEntity> memberEntityOptional = memberRepository.findTopByMemberId(user.getId());
        
        if (memberEntityOptional.isEmpty()) {
            MemberEntity memberEntity = MemberEntity.builder()
                    .memberId(user.getId())
                    .updatedOn(LocalDateTime.now())
                    .totalMembersInvited(0)
                    .inviteLinkCounter(1)
                    .build();
            memberRepository.save(memberEntity);
        } else {
            MemberEntity memberEntity = memberEntityOptional.get();
            int inviteLinkCounter = memberEntity.getInviteLinkCounter();
            if (inviteLinkCounter >= 3) {
                return false;
            }
            memberEntity.setInviteLinkCounter(inviteLinkCounter + 1);
            memberEntity.setUpdatedOn(LocalDateTime.now());
            memberRepository.save(memberEntity);
        }
        return true;
    }
    
}
