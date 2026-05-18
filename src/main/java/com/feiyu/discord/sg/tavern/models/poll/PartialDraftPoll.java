package com.feiyu.discord.sg.tavern.models.poll;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PartialDraftPoll {

    String draftId;
    String creatorId;
    String creatorMention;
    String question;
    int maxSelection;
    String durationOption;
}
