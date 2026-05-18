package com.feiyu.discord.sg.tavern.repositories;

import com.feiyu.discord.sg.tavern.entities.PollVoteEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PollVoteRepository extends JpaRepository<PollVoteEntity, Long> {

    List<PollVoteEntity> findAllByPollIdOrderByOptionNumberAsc(Long pollId);

    List<PollVoteEntity> findAllByPollIdAndVoterUserIdOrderByOptionNumberAsc(Long pollId, String voterUserId);

    void deleteAllByPollIdAndVoterUserId(Long pollId, String voterUserId);

    int countDistinctVoterUserIdByPollId(Long pollId);
}
