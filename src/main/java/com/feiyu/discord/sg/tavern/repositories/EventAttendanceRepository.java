package com.feiyu.discord.sg.tavern.repositories;

import com.feiyu.discord.sg.tavern.entities.EventAttendanceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EventAttendanceRepository extends JpaRepository<EventAttendanceEntity, Long> {

    List<EventAttendanceEntity> findAllByPostId(String postId);

    Optional<EventAttendanceEntity> findByPostIdAndUserId(String postId, String userId);

    List<EventAttendanceEntity> findAllByPostIdAndUserId(String postId, String userId);

    Optional<EventAttendanceEntity> findByPostIdAndUserIdAndIsMain(String postId, String userId, Boolean isMain);

    void deleteByPostIdAndUserId(String postId, String userId);

    void deleteAllByPostId(String postId);

    List<EventAttendanceEntity> findByPostIdAndStatusOrderByCreatedOnDesc(String postId, String status);

}
