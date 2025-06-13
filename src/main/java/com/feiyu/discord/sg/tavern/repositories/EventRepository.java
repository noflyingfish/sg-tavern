package com.feiyu.discord.sg.tavern.repositories;

import com.feiyu.discord.sg.tavern.entities.EventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EventRepository extends JpaRepository<EventEntity, String> {
    
    Optional<EventEntity> findTopByPostId(String postId);
    
}
