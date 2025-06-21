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
@Table(name = "message_reference")
public class MessageEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    String id;
    String messageId;
    String messagePurpose;
    LocalDateTime createdOn;
    LocalDateTime updatedOn;
    
}
