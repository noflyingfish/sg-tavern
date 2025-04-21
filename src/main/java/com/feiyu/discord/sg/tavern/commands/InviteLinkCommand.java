package com.feiyu.discord.sg.tavern.commands;

import com.feiyu.discord.sg.tavern.config.ValuesConfig;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Invite;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@AllArgsConstructor
public class InviteLinkCommand extends ListenerAdapter {
    
    private final ValuesConfig valuesConfig;
    
    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (event.getName().equals("invite")) {
            Member member = event.getMember();
            Guild guild = event.getGuild();
            TextChannel landingChannel = guild.getTextChannelById(valuesConfig.getRulesChannelId());
            log.info("InviteLinkCommand.invitelink by : {}", member.getEffectiveName());
            
            Invite invite = landingChannel.createInvite()
                    .setMaxAge(2L, TimeUnit.DAYS)
                    .setUnique(true)
                    .complete();
            
            // send member the server invite link
            String memberMessage = "Share this invite link with your friends. It is valid for 48hrs from now.\n"
                    + invite.getUrl();
            PrivateChannel pc = member.getUser().openPrivateChannel().complete();
            pc.sendMessage(memberMessage).queue();
            event.reply("Server invite link is sent to your pm!").setEphemeral(true).queue();
            
            // log admin, /invite command has been used
            String adminMessage = member.getUser().getName() + " created an invite link : " + invite.getCode();
            TextChannel adminChannel = guild.getTextChannelById(valuesConfig.getAdminBotChannelId());
            adminChannel.sendMessage(adminMessage).queue();
        }
    }
    
}
