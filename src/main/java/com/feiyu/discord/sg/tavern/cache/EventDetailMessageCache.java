package com.feiyu.discord.sg.tavern.cache;

import com.feiyu.discord.sg.tavern.repositories.EventRepository;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@AllArgsConstructor
public class EventDetailMessageCache {
    
    private final EventRepository eventRepository;
    private final List<String> TRACKED_STATUS = List.of("NEW", "EDITED", "MANAGED");
    
    // key = eventDetailMsgId, value = postId
    private final Cache<String, String> cache = Caffeine.newBuilder()
            .maximumSize(10000L)
            .expireAfterAccess(7, TimeUnit.DAYS)
            .build();
    
    @EventListener(ApplicationReadyEvent.class)
    public void warmUp() {
        
        eventRepository.findAllByPostStatusInAndEventDetailMsgIdIsNotNull(TRACKED_STATUS)
                .forEach(eventEntity -> cache.put(eventEntity.getEventDetailMsgId(), true));
        
        log.info("EventDetailMessageCache loaded - {} ids", cache.estimatedSize());
    }
    
    public boolean contains(String messageId) {
        return cache.getIfPresent(messageId) != null;
    }
    
    public void put(String messageId, String postId) {
        if (messageId != null) cache.put(messageId, postId);
    }
    
    public void evict(String messageId) {
        if (messageId != null) cache.invalidate(messageId);
    }
}
