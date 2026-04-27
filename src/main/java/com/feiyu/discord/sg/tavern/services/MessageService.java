package com.feiyu.discord.sg.tavern.services;

import com.feiyu.discord.sg.tavern.config.ValuesConfig;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class MessageService {
    
    private final ValuesConfig valuesConfig;
    
    public void sendAdminChannelMessage(Guild guild, String message){
        TextChannel adminChannel = guild.getTextChannelById(valuesConfig.getAdminBotChannelId());
        adminChannel.sendMessage(message).queue();
    }
    
    public void sendMemberMessage(User user, String message){
        PrivateChannel pc = user.openPrivateChannel().complete();
        pc.sendMessage(message).queue();
    }
    
}
