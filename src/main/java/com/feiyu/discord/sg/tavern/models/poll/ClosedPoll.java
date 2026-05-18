package com.feiyu.discord.sg.tavern.models.poll;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ClosedPoll {

    String pollId;
    String channelId;
    String messageId;
    String creatorId;
    String creatorTag;
    String question;
    List<String> options;
    int maxSelection;
    String durationOption;
    LocalDateTime closesOn;
    int totalVoters;
    List<PollOptionResult> optionResults;
}
