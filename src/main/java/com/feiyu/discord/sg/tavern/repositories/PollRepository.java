package com.feiyu.discord.sg.tavern.repositories;

import com.feiyu.discord.sg.tavern.entities.PollEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PollRepository extends JpaRepository<PollEntity, Long> {

    List<PollEntity> findAllByStatus(String status);

    List<PollEntity> findAllByStatusAndClosesOnLessThanEqual(String status, LocalDateTime closesOn);

    Optional<PollEntity> findByIdAndStatus(Long id, String status);
}
