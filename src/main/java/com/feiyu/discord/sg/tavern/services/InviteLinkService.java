package com.feiyu.discord.sg.tavern.services;

import com.feiyu.discord.sg.tavern.entities.InviteLinkEntity;
import com.feiyu.discord.sg.tavern.repositories.InviteLinkRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@AllArgsConstructor
public class InviteLinkService {
    
    private final InviteLinkRepository inviteLinkRepository;
    
    public void saveInviteLink(String inviteCode, String creatorMemberId) {
        InviteLinkEntity inviteLinkEntity = InviteLinkEntity.builder()
                .inviteCode(inviteCode)
                .creatorMemberId(creatorMemberId)
                .createdOn(LocalDateTime.now())
                .build();
        inviteLinkRepository.save(inviteLinkEntity);
        log.info("InviteLinkService.saveInviteLink stored invite {} for member {}", inviteCode, creatorMemberId);
    }
    
    public String findCreatorIdByInviteCode(String inviteCode) {
        Optional<InviteLinkEntity> optionalInviteLinkEntity = inviteLinkRepository.findById(inviteCode);
        if (optionalInviteLinkEntity.isPresent()) {
            return optionalInviteLinkEntity.get().getCreatorMemberId();
        }
        return "UNKNOWN";
    }
}
