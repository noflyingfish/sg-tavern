package com.feiyu.discord.sg.tavern.repositories;

import com.feiyu.discord.sg.tavern.entities.RoleColourEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoleColourRepository extends JpaRepository<RoleColourEntity, String> {
    
    List<RoleColourEntity> findAllByColourCode(String colourCode);
    
}
