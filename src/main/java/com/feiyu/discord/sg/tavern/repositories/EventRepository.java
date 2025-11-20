package com.feiyu.discord.sg.tavern.repositories;

import com.feiyu.discord.sg.tavern.entities.EventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EventRepository extends JpaRepository<EventEntity, String> {
    
    Optional<EventEntity> findTopByPostId(String postId);
    
    List<EventEntity> findAllByPostStatus(String postStatus);
    
    List<EventEntity> findAllByPostStatusAndEventDetailMsgIdIsNotNull(String postStatus);
    
}
