package com.feiyu.discord.sg.tavern.repositories;

import com.feiyu.discord.sg.tavern.entities.GeMinigameEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GeMinigameRepository extends JpaRepository<GeMinigameEntity, String> {
}
