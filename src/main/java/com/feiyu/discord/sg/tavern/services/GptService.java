package com.feiyu.discord.sg.tavern.services;

import com.feiyu.discord.sg.tavern.config.ValuesConfig;
import com.feiyu.discord.sg.tavern.entities.EventEntity;
import com.feiyu.discord.sg.tavern.models.gpt.GptEventResponse;
import com.feiyu.discord.sg.tavern.models.gpt.GptEventResponseList;
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
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@AllArgsConstructor
public class GptService {
    
    private final ValuesConfig valuesConfig;
    private final EventRepository eventRepository;
    private final MessageService messageService;
    
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
                .model(ChatModel.GPT_4_1)
                .responseFormat(GptEventResponseList.class)
                .addSystemMessage("""
                            Extract event data from each line. Multiple lines in one message.
                            For each line, return:
                            - Event Name
                            - Event Location
                            - Event Datetime
                            Rules when parsing:
                            - Do not repeat Event Name in Event Location
                            - Each line only refers to ONE event, if multiple events detected return the earlier one
                            - Event Datetime MUST be formated to "yyyy-MM-ddTHH:mm:ss" ISO-8601 format for parsing
                            - Reference Year 2026. infer using this reference if message doesn't contain year.
                        """)
                .addUserMessage(userMessage)
                .build();
        
        StructuredChatCompletion<GptEventResponseList> result = client.chat().completions().create(params);
        List<GptEventResponse> gptEventList = result.choices().getFirst().message().content().stream().toList().getFirst().getGptEventList();
        log.info("GPT response full: {} ", result);
        log.info("GPT response size: {} ", gptEventList.size());
        
        for (int i = 0; i < gptEventList.size(); i++) {
            EventEntity e = eventEntityList.get(i);
            try {
                GptEventResponse gptEventResponse = gptEventList.get(i);
                log.info("GPT response event : {}", gptEventResponse);
                log.info("Database event pre update: {}", e);
                e.setProcessedEventName(gptEventResponse.getEventName());
                e.setProcessedEventLocation(gptEventResponse.getEventLocation());
                e.setProcessedEventDateTime(LocalDateTime.parse(gptEventResponse.getEventDatetime()));
                e.setUpdatedOn(LocalDateTime.now());
                e.setPostStatus("MANAGED");
                eventRepository.save(e);
                log.info("Database event post update: {}", e);
            } catch (Exception ex){
                User dev = guild.retrieveMemberById(valuesConfig.getDevUserId()).complete().getUser();
                String errorMessage = "Error parsing : " + e.getPostUrl();
                messageService.sendMemberMessage(dev, errorMessage);
            }
        }
    }
    
}
