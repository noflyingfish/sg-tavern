package com.feiyu.discord.sg.tavern.scheduler;

import com.feiyu.discord.sg.tavern.config.ValuesConfig;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@AllArgsConstructor
public class IntroMsgScheduler {
    
    private final ValuesConfig valuesConfig;
    private final JDA jda;
    
    // Run every 2 days at 9 PM SGT
    // @Scheduled(cron = "0 0 21 */2 * ?", zone = "Asia/Singapore")
    @Scheduled(fixedRate = 10000L)
    public void introReminder() {
        log.info("IntroMsgScheduler.introReminder Start");
        
        // Get the Guild and Channel
        Guild guild = jda.getGuildById(valuesConfig.getGuildId());
        List<Member> allGuildMemberList = guild.loadMembers().get();
        TextChannel introChannel = jda.getTextChannelById(valuesConfig.getIntroChannelId());
        TextChannel adminChannel = jda.getTextChannelById(valuesConfig.getAdminBotChannelId());
        
        // The DS to compare
        Set<String> allMemberIdSet = allGuildMemberList.stream()
                .map(member -> member.getUser().getId())
                .collect(Collectors.toSet());
        Set<String> introedExitedMemberSet = new HashSet<>();
        List<Message> allMessage = new ArrayList<>();
        log.info("Guild total member : {}", allMemberIdSet.size());
        
        // Get the first 100 msg
        MessageHistory first100MessageHistory = introChannel.getHistoryFromBeginning(100).complete();
        List<Message> first100Msg = first100MessageHistory.getRetrievedHistory();
        allMessage.addAll(first100Msg);
        log.info("first100message List size : {}", first100Msg.size());
        
        // Get the recurring message after
        if (first100Msg.size() == 100) {
            String lastMsgId = first100Msg.get(0).getId();
            List<Message> next100Msg;
            do {
                MessageHistory next100messageHistory = introChannel.getHistoryAfter(lastMsgId, 100).complete();
                next100Msg = next100messageHistory.getRetrievedHistory();
                lastMsgId = next100Msg.get(0).getId();
                allMessage.addAll(next100Msg);
            } while (next100Msg.size() == 100);
        }
        
        // filter through the messages
        for (Message message : allMessage) {
            User messageUser = message.getAuthor();
            // have msg in intro, user not in server
            if (!allMemberIdSet.contains(messageUser.getId())) {
                introedExitedMemberSet.add(messageUser.getName());
            }
            // have msg in intro, user in server
            // thus remaining is no intro
            allMemberIdSet.remove(messageUser.getId());
        }
        
        // Build message to be send
        List<String> noIntroMemberIdList = allMemberIdSet.stream().toList();
        List<Member> noIntroMemberList = new ArrayList<>();
        for (String memberId : noIntroMemberIdList) {
            noIntroMemberList.add(guild.retrieveMemberById(memberId).complete());
        }
        log.info("Didn't intro member size : {}", noIntroMemberList.size());
        String message = "Hi Admin. Please remind these " + noIntroMemberList.size()
                + " members to do their introduction. Thank you.\n\n";
        for (Member member : noIntroMemberList) {
            message = message + member.getEffectiveName() + "\n";
        }
        log.info("Message to be sent : {}", message.replaceAll("\\n", ", "));
        
        //adminChannel.sendMessage(message).complete();
        Member adminMember = guild.retrieveMemberById(valuesConfig.getAdminUserId()).complete();
        PrivateChannel pc = adminMember.getUser().openPrivateChannel().complete();
        pc.sendMessage(message).complete();
        log.info("IntroMsgScheduler.introReminder End");
    }
}
