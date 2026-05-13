package com.feiyu.discord.sg.tavern.repositories;

import com.feiyu.discord.sg.tavern.entities.MemberEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<MemberEntity, Long> {

    Optional<MemberEntity> findFirstByUserIdAndLeaveDatetimeIsNullOrderByJoinDatetimeDesc(String userId);
    
}
