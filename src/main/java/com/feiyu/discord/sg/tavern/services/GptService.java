package com.feiyu.discord.sg.tavern.services;

import com.feiyu.discord.sg.tavern.config.ValuesConfig;
import com.feiyu.discord.sg.tavern.entities.EventEntity;
import com.feiyu.discord.sg.tavern.models.GptEventResponse;
import com.feiyu.discord.sg.tavern.models.GptEventResponseList;
import com.feiyu.discord.sg.tavern.repositories.EventRepository;
import com.feiyu.discord.sg.tavern.utils.RegexUtil;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.ChatModel;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.chat.completions.StructuredChatCompletion;
import com.openai.models.chat.completions.StructuredChatCompletionCreateParams;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@AllArgsConstructor
public class GptService {
    
    private final ValuesConfig valuesConfig;
    private final EventRepository eventRepository;
    
    public void sendGpt(List<EventEntity> eventEntityList, Guild guild) {
        
        StringBuilder userMessageBuilder = new StringBuilder();
        
        for (EventEntity event : eventEntityList) {
            try {
                String postTitle = event.getPostName();
                Message eventDetailMessage = guild.getThreadChannelById(event.getPostId())
                        .retrieveMessageById(event.getEventDetailMsgId())
                        .complete();
                String eventDetailMessageContent = eventDetailMessage.getContentStripped()
                        .replaceAll("\n", " ");
                userMessageBuilder.append(RegexUtil.keepAscii(postTitle));
                userMessageBuilder.append("\t");
                userMessageBuilder.append(RegexUtil.keepAscii(eventDetailMessageContent));
                userMessageBuilder.append("\n");
            } catch (Exception e) {
                log.error("{} at {}", e.getMessage(), event);
            }
        }
        String userMessage = userMessageBuilder.toString();
        log.info("User message : {}", userMessage);
        log.info("Send GPT count : {}", eventEntityList.size());
        
        OpenAIClient client = OpenAIOkHttpClient.builder()
                .apiKey(valuesConfig.getGptApiKey())
                .build();
        
        StructuredChatCompletionCreateParams<GptEventResponseList> params = ChatCompletionCreateParams.builder()
                .model(ChatModel.GPT_4O_MINI)
                .responseFormat(GptEventResponseList.class)
                .addSystemMessage("""
                            Extract event data from each line. Multiple lines in one message.
                            For each line, return:
                            - Event Name (Don't repeat location)
                            - Event Location
                            - Event Datetime (Info with event created date)
                            Rules:
                            - Do NOT guess or infer dates that are not explicitly stated.
                            - If the line does NOT contain a clear date/time expression, return event_datetime = null.
                            - It is year 2025 now. The event date/time should be in the near future.
                        """)
                .addUserMessage(userMessage)
                .build();
        
        StructuredChatCompletion<GptEventResponseList> result = client.chat().completions().create(params);
        log.info("GPT response full: {} ", result);
        List<GptEventResponse> gptEventList = result.choices().getFirst().message().content().stream().toList().getFirst().getGptEventList();
        log.info("GPT response size: {} ", result);
        
        for (int i = 0; i < gptEventList.size(); i++) {
            GptEventResponse gptEventResponse = gptEventList.get(i);
            EventEntity e = eventEntityList.get(i);
            log.info("GPT response event : {}", gptEventResponse);
            log.info("Database event pre update: {}", e);
            e.setProcessedEventName(gptEventResponse.getEventName());
            e.setProcessedEventLocation(gptEventResponse.getEventLocation());
            e.setProcessedEventDateTime(gptEventResponse.getEventDatetime());
            e.setUpdatedOn(LocalDateTime.now());
            e.setPostStatus("MANAGED");
            eventRepository.save(e);
            log.info("Database event post update: {}", e);
        }
    }
    
}
