package com.feiyu.discord.sg.tavern.schedulers;

import com.feiyu.discord.sg.tavern.config.ValuesConfig;
import com.feiyu.discord.sg.tavern.entities.EventEntity;
import com.feiyu.discord.sg.tavern.entities.MessageEntity;
import com.feiyu.discord.sg.tavern.repositories.EventRepository;
import com.feiyu.discord.sg.tavern.repositories.MessageRepository;
import com.feiyu.discord.sg.tavern.services.GptService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Component
@AllArgsConstructor
public class EventMonitorScheduler {
    
    private final JDA jda;
    private final ValuesConfig valuesConfig;
    private final EventRepository eventRepository;
    private final MessageRepository messageRepository;
    private final GptService gptService;
    
    // 11pm daily
    @Async
    @Scheduled(cron = "0 0 23 * * ?", zone = "Asia/Singapore")
    public void newEditedEventMonitorScheduler() {
        log.info("EventMonitorScheduler.newEditedEventMonitorScheduler Start");
        Guild guild = jda.getGuildById(valuesConfig.getGuildId());
        
        List<EventEntity> newEventList = eventRepository.findAllByPostStatus("NEW");
        List<EventEntity> updateEventList = eventRepository.findAllByPostStatus("EDITED");
        StringBuilder sb = new StringBuilder();
        
        List<EventEntity> combinedEventList = new ArrayList<>();
        combinedEventList.addAll(newEventList);
        combinedEventList.addAll(updateEventList);
        
        // deleted event post
        newEventList.removeIf(eventEntity ->
                checkEventDeleted(guild, eventEntity)
        );
        updateEventList.removeIf(eventEntity ->
                checkEventDeleted(guild, eventEntity)
        );
        
        // message to track new/updated event
        if (!newEventList.isEmpty() || !updateEventList.isEmpty()) {
            sb.append("New posts : ").append(newEventList.size()).append("\n");
            newEventList.forEach(newEvent -> sb.append(newEvent.getPostUrl()).append("\n"));
            sb.append("Updated posts : ").append(updateEventList.size()).append("\n");
            updateEventList.forEach(updatedEvent -> sb.append(updatedEvent.getPostUrl()).append("\n"));
        }
        
        // sent event to gpt
        List<EventEntity> postWithDetailsList = combinedEventList.stream()
                .filter(eventEntity -> eventEntity.getEventDetailMsgId() != null)
                .toList();
        gptService.sendGpt(postWithDetailsList, guild);
        
        // message to track gpt evnt
        sb.append("GPT posts : ").append(postWithDetailsList.size()).append("\n");
        postWithDetailsList.forEach(updatedEvent -> sb.append(updatedEvent.getPostUrl()).append("\n"));
        String message = sb.toString();
        
        Member devMember = guild.retrieveMemberById(valuesConfig.getDevUserId()).complete();
        log.info("Message sent to dev : {}", message);
        PrivateChannel pc = devMember.getUser().openPrivateChannel().complete();
        pc.sendMessage(message).queue();
        
        log.info("EventMonitorScheduler.newEditedEventMonitorScheduler End");
    }
    
    private boolean checkEventDeleted(Guild guild, EventEntity eventEntity) {
        if (guild.getThreadChannelById(eventEntity.getPostId()) == null) {
            eventEntity.setPostStatus("DELETED");
            log.info("EventEntity DELETED : {} ", eventEntity);
            eventRepository.save(eventEntity);
            return true; // Remove this EventEntity
        }
        return false; // Keep this EventEntity
    }
    
    // 1am daily
    @Async
    @Scheduled(cron = "0 0 1 * * ?", zone = "Asia/Singapore")
    public void pastEventMonitorScheduler() {
        log.info("EventMonitorScheduler.pastEventMonitorScheduler Start");
        Guild guild = jda.getGuildById(valuesConfig.getGuildId());
        List<EventEntity> managedEventList = eventRepository.findAllByPostStatus("MANAGED");
        
        for (EventEntity eventEntity : managedEventList) {
            
            // deleted event post
            if (guild.getThreadChannelById(eventEntity.getPostId()) == null) {
                eventEntity.setPostStatus("DELETED");
                log.info("EventEntity DELETED : {} ", eventEntity);
            }
            
            // past event posts
            if (eventEntity.getProcessedEventDateTime().isBefore(LocalDateTime.now())) {
                eventEntity.setPostStatus("PAST");
                log.info("EventEntity PAST : {} ", eventEntity);
            }
            eventRepository.save(eventEntity);
        }
        log.info("EventMonitorScheduler.pastEventMonitorScheduler End");
    }
    
    // 6am daily
    @Async
    @Scheduled(cron = "0 0 6 * * ?", zone = "Asia/Singapore")
    public void sendEventScheduler() {
        log.info("EventMonitorScheduler.sendEventScheduler Start");
        
        Guild guild = jda.getGuildById(valuesConfig.getGuildId());
        List<EventEntity> managedEventList = eventRepository.findAllByPostStatus("MANAGED");
        managedEventList.sort(Comparator.comparing(EventEntity::getProcessedEventDateTime));
        
        DateTimeFormatter df1 = DateTimeFormatter.ofPattern("dd-MMM-yyyy hh:mma");
        DateTimeFormatter df2 = DateTimeFormatter.ofPattern("dd-MMM-yyyy");
        
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Tavern Outing Notices for " + LocalDateTime.now().format(df2));
        eb.setDescription("Please follow the template here for new posts : " + valuesConfig.getEventTemplate() + "\n");
        
        int fieldCount = 0;
        
        for (EventEntity e : managedEventList) {
            // "eventName @ eventLocation"
            if (e.getProcessedEventName() != null && e.getProcessedEventLocation() != null) {
                eb.addField(e.getProcessedEventName() + " @ " + e.getProcessedEventLocation(),
                        e.getProcessedEventDateTime().format(df1) + " " + e.getProcessedEventDateTime().getDayOfWeek() + "\n"
                                + e.getPostUrl(),
                        false);
                // only "eventName" XOR "eventLocation"
            } else if (e.getProcessedEventName() == null ^ e.getProcessedEventLocation() == null) {
                eb.addField(e.getProcessedEventName() != null ? e.getProcessedEventName() : e.getProcessedEventLocation(),
                        e.getProcessedEventDateTime().format(df1) + " " + e.getProcessedEventDateTime().getDayOfWeek() + "\n"
                                + e.getPostUrl(),
                        false);
            }
            
            fieldCount++;
            if (fieldCount == 25) { // each embed msg only can have max 25 fields.
                log.info("Embed max count hit.");
                break;
            }
        }
        eb.setFooter("Still under testing...events might not be captured \n"
                + "Any suggestion to this message please let Rain knows :)");
        MessageEmbed me = eb.build();
        
        TextChannel eventPromoChannel = guild.getTextChannelById(valuesConfig.getEventsPromoChannelId());
        
        // send new message
        Message m = eventPromoChannel.sendMessageEmbeds(me).complete();
        
        // remove old message
        MessageEntity promoMessage = messageRepository.findTopByMessagePurpose("EventPromo").get();
        if (promoMessage != null) {
            eventPromoChannel.deleteMessageById(promoMessage.getMessageId()).queue();
            promoMessage.setUpdatedOn(LocalDateTime.now());
            promoMessage.setMessageId(m.getId());
            messageRepository.save(promoMessage);
        } else {
            messageRepository.save(MessageEntity.builder()
                    .messageId(m.getId())
                    .messagePurpose("EventPromo")
                    .createdOn(LocalDateTime.now())
                    .build());
        }

//        Member devMember = guild.retrieveMemberById(valuesConfig.getDevUserId()).complete();
//        log.info("Message sent to dev : {}", me);
//        PrivateChannel pc = devMember.getUser().openPrivateChannel().complete();
//        pc.sendMessageEmbeds(me).queue();
        
        log.info("EventMonitorScheduler.sendEventScheduler End");
    }
    
}
