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
@Table(name = "ge_minigame")
public class GeMinigameEntity {

    @Id
    String userId;
    String username;
    Double sengkang;
    Double tampines;
    Double punggol;
    LocalDateTime createdOn;
    
}
