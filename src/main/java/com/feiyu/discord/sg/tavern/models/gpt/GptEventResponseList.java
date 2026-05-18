package com.feiyu.discord.sg.tavern.models.gpt;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GptEventResponseList {
    
    List<GptEventResponse> gptEventList;
    
}
