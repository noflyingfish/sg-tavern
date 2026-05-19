package com.feiyu.discord.sg.tavern.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
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
@Table(name = "poll")
public class PollEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    String channelId;
    String messageId;
    String creatorUserId;
    String question;
    Integer maxSelection;
    String durationOption;
    String option1;
    String option2;
    String option3;
    String option4;
    String option5;
    String status; // ACTIVE / CLOSED / CANCELLED
    LocalDateTime createdOn;
    LocalDateTime publishedOn;
    LocalDateTime closesOn;
    LocalDateTime closedOn;

}
