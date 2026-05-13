package com.feiyu.discord.sg.tavern.entities;

import jakarta.persistence.*;
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
@Table(name = "members")
public class MemberEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    String userId;
    String inviterId;
    Integer inviteLinkCounter;
    Integer totalMembersInvited;
    LocalDateTime updatedOn;
    LocalDateTime leaveDatetime;
    LocalDateTime joinDatetime;
}
