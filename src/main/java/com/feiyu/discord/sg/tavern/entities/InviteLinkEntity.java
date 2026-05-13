package com.feiyu.discord.sg.tavern.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "invite_link")
public class InviteLinkEntity {
    
    @Id
    String inviteCode;
    String creatorMemberId;
    LocalDateTime createdOn;
    
}
