package com.feiyu.discord.sg.tavern.models.gpt;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GptEventResponse {
    
    String eventName;
    String eventLocation;
    String eventDatetime;
    
}
