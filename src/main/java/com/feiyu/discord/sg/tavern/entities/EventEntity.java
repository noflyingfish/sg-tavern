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
@Table(name = "upcoming_event")
public class EventEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    String id;
    // raw data
    String postName;
    String postId;
    String postUrl;
    
    // for admin usage, entity meta data
    String postStatus;  // NEW / EDITED / MANAGED / PAST / CANCELLED / TBC
    LocalDateTime createdOn;
    LocalDateTime updatedOn;
    
    // processed data
    String processedEventName;
    String processedEventLocation;
    LocalDateTime processedEventDateTime;
    
}
