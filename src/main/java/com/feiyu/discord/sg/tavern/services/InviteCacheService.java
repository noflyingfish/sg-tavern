package com.feiyu.discord.sg.tavern.services;

import com.feiyu.discord.sg.tavern.config.ValuesConfig;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Invite;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class InviteCacheService {

    private final JDA jda;
    private final ValuesConfig valuesConfig;

    @Getter
    private final Map<String, Invite> inviteCache = new HashMap<>();
    
    public InviteCacheService(@Lazy JDA jda, ValuesConfig valuesConfig) {
        this.jda = jda;
        this.valuesConfig = valuesConfig;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void startup() {
        Guild guild = jda.getGuildById(valuesConfig.getGuildId());

        List<Invite> invites = guild.retrieveInvites().complete();
        inviteCache.clear();
        invites.forEach(invite -> inviteCache.put(invite.getCode(), invite));

        log.info("InviteCacheService.startup loaded {} invites into cache", inviteCache.size());
    }

    public Optional<String> updateCache() {
        log.info("InviteCacheService.updateCache invoked");
        Guild guild = jda.getGuildById(valuesConfig.getGuildId());

        Map<String, Invite> cachedInviteMap = new HashMap<>(inviteCache);
        cachedInviteMap.entrySet().removeIf(entry -> isExpired(entry.getValue()));

        List<Invite> freshInviteList = guild.retrieveInvites().complete();
        Map<String, Invite> freshInviteMap = new HashMap<>();
        freshInviteList.forEach(invite -> freshInviteMap.put(invite.getCode(), invite));

        log.info("cachedInviteMap ========");
        log.info(cachedInviteMap.toString());
        log.info("freshInviteMap ========");
        log.info(freshInviteMap.toString());
        log.info("========");

        List<String> matchedInviteCodes = new ArrayList<>();

        for (Map.Entry<String, Invite> cachedInviteEntry : cachedInviteMap.entrySet()) {
            String inviteCode = cachedInviteEntry.getKey();
            Invite cachedInvite = cachedInviteEntry.getValue();
            Invite freshInvite = freshInviteMap.get(inviteCode);

            if (freshInvite != null) {
                if (freshInvite.getUses() == cachedInvite.getUses() + 1) {
                    matchedInviteCodes.add(inviteCode);
                }
            } else if (isFinalUseConsumed(cachedInvite)) {
                matchedInviteCodes.add(inviteCode);
            }
        }

        Optional<String> matchedInviteCode = Optional.empty();

        if (matchedInviteCodes.size() == 1) {
            matchedInviteCode = Optional.of(matchedInviteCodes.getFirst());
            log.info("InviteCacheService.updateCache matched used invite {}", matchedInviteCodes.getFirst());
        } else if (matchedInviteCodes.size() > 1) {
            log.warn("InviteCacheService.updateCache found multiple invite matches {}", matchedInviteCodes);
        } else {
            log.warn("InviteCacheService.updateCache found no invite match");
        }

        inviteCache.clear();
        inviteCache.putAll(freshInviteMap);
        return matchedInviteCode;
    }

    public void updateCache(Invite invite) {
        inviteCache.put(invite.getCode(), invite);
        log.info("InviteCacheService.updateCache stored invite {}", invite.getCode());
    }

    public void updateCache(String inviteCode) {
        inviteCache.remove(inviteCode);
        log.info("InviteCacheService.updateCache removed invite {}", inviteCode);
    }

    private boolean isExpired(Invite invite) {
        if (invite.getMaxAge() <= 0) {
            return false;
        }

        OffsetDateTime expiresAt = invite.getTimeCreated().plusSeconds(invite.getMaxAge());
        return !expiresAt.isAfter(OffsetDateTime.now());
    }

    private boolean isFinalUseConsumed(Invite invite) {
        return invite.getMaxUses() > 0 && invite.getUses() + 1 >= invite.getMaxUses();
    }
}
