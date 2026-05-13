package com.feiyu.discord.sg.tavern.commands;

import com.feiyu.discord.sg.tavern.config.ValuesConfig;
import com.feiyu.discord.sg.tavern.services.InviteLinkService;
import com.feiyu.discord.sg.tavern.services.MemberService;
import com.feiyu.discord.sg.tavern.services.MessageService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Invite;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@AllArgsConstructor
public class InviteLinkCommand extends ListenerAdapter {
    
    private final ValuesConfig valuesConfig;
    private final MessageService messageService;
    private final MemberService memberService;
    private final InviteLinkService inviteLinkService;
    
    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (event.getName().equals("invite")) {
            log.info("Command - invite - {}", event.getUser().getId());
            
            User user = event.getUser();
            Guild guild = event.getGuild();
            TextChannel landingChannel = guild.getTextChannelById(valuesConfig.getRulesChannelId());
            
            // save member to database
            if(memberService.allowedToCreateInvite(user)){
                Invite invite = landingChannel.createInvite()
                        .setMaxAge(2L, TimeUnit.DAYS)
                        .setMaxUses(1)
                        .setUnique(true)
                        .complete();
                
                // save creator of the link
                inviteLinkService.saveInviteLink(invite.getCode(), user.getId());
                
                // send member the server invite link
                String inviteMessage = "Share this invite link with your friend. It is valid for 48hrs from now.\n"
                        + invite.getUrl();
                messageService.sendMemberMessage(user, inviteMessage);
                
                // log admin, /invite command has been used
                String adminMessage = user.getName() + " created an invite link : " + invite.getCode();
                messageService.sendAdminChannelMessage(guild, adminMessage);
                
                event.reply("Server invite link is sent to your pm!").setEphemeral(true).queue();
            } else {
                event.reply("You probably used up your invite count this month :/ \n dm a mod for invite!")
                        .setEphemeral(true).queue();
            }

        }
        
        if (event.getName().equals("invitemany")) {
            log.info("Command - invitemany - {}", event.getUser().getId());
            
            User user = event.getUser();
            Guild guild = event.getGuild();
            int count = 1;
            OptionMapping optionInput = event.getOption("count");
            if(optionInput != null){
                count = optionInput.getAsInt();
            }
            
            TextChannel landingChannel = guild.getTextChannelById(valuesConfig.getRulesChannelId());
            
            Invite invite = landingChannel.createInvite()
                    .setMaxAge(7L, TimeUnit.DAYS)
                    .setMaxUses(count)
                    .setUnique(true)
                    .complete();
            
            // save member to database
            memberService.getOrCreateMember(user);
            
            // save creator of the link
            inviteLinkService.saveInviteLink(invite.getCode(), user.getId());
            
            // send member the server invite link
            String inviteMessage = "This is a reusable invite link. Valid for " + count + " uses.\n "+
                    "It is valid for 7 days from now.\n"
                    + invite.getUrl();
            messageService.sendMemberMessage(user, inviteMessage);
            
            // log admin, /invite command has been used
            String adminMessage = user.getName() + " created an invite link : " + invite.getCode();
            messageService.sendAdminChannelMessage(guild, adminMessage);
            
            event.reply("Server invite link is sent to your pm!").setEphemeral(true).queue();
        }
    }
    
}
