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
    private final InviteLinkService inviteLinkService;
    
    public boolean allowedToCreateInvite(User user) {
        MemberEntity memberEntity = getOrCreateMember(user);
        
        int inviteLinkCounter = Optional.ofNullable(memberEntity.getInviteLinkCounter()).orElse(0);
        if (inviteLinkCounter >= 3) {
            return false;
        }
        
        memberEntity.setInviteLinkCounter(inviteLinkCounter + 1);
        memberEntity.setUpdatedOn(LocalDateTime.now());
        memberRepository.save(memberEntity);
        return true;
    }
    
    public MemberEntity getOrCreateMember(User user) {
        Optional<MemberEntity> memberEntityOptional =
                memberRepository.findFirstByUserIdAndLeaveDatetimeIsNullOrderByJoinDatetimeDesc(user.getId());
        
        if (memberEntityOptional.isEmpty()) {
            MemberEntity memberEntity = MemberEntity.builder()
                    .userId(user.getId())
                    .inviteLinkCounter(0)
                    .totalMembersInvited(0)
                    .updatedOn(LocalDateTime.now())
                    .joinDatetime(LocalDateTime.now())
                    .build();
            return memberRepository.save(memberEntity);
        } else {
            return memberEntityOptional.get();
        }
    }
    
    public void registerNewMember(User user, Optional<String> inviteCodeOptional) {
        String inviterId = "UNKNOWN";
        if (inviteCodeOptional.isPresent()) {
            inviterId = inviteLinkService.findCreatorIdByInviteCode(inviteCodeOptional.get());
            Optional<MemberEntity> inviterOptional = memberRepository.findFirstByUserIdAndLeaveDatetimeIsNullOrderByJoinDatetimeDesc(inviterId);
            if(inviterOptional.isPresent()){
                MemberEntity inviterMemberEntity = inviterOptional.get();
                int lastTotalInvitedCount = inviterMemberEntity.getTotalMembersInvited();
                lastTotalInvitedCount++;
                inviterMemberEntity.setTotalMembersInvited(lastTotalInvitedCount);
                inviterMemberEntity.setUpdatedOn(LocalDateTime.now());
                memberRepository.save(inviterMemberEntity);
                log.info("Total Member Counter +1 : {}", inviterId);
            }
        }
        
        MemberEntity me = MemberEntity.builder()
                .userId(user.getId())
                .inviterId(inviterId)
                .updatedOn(LocalDateTime.now())
                .joinDatetime(LocalDateTime.now())
                .build();
        
        memberRepository.save(me);
    }
    
}
