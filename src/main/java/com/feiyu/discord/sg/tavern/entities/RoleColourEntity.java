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
@Table(name = "role_colour")
public class RoleColourEntity {
    
    @Id
    String userId;
    String username;
    String colourCode;
    String roleId;
    String randomFlag;
    LocalDateTime updatedOn;
    
}
