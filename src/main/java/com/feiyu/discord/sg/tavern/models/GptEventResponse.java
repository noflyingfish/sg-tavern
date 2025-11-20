package com.feiyu.discord.sg.tavern.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GptEventResponse {
    
    String eventName;
    String eventLocation;
    LocalDateTime eventDatetime;
    
}
