package com.feiyu.discord.sg.tavern.models.poll;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PollOptionResult {

    int optionNumber;
    String optionText;
    int voteCount;
    int totalVoters;
    int percentage;
}
