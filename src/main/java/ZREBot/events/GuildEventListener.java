package ZREBot.events;

import ZREBot.ZREBot;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdatePendingEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class GuildEventListener extends ListenerAdapter {
    private final ZREBot bot;

    public GuildEventListener(ZREBot bot) {
        this.bot = bot;
    }

    @Override
    public void onReady(ReadyEvent event) {
        System.out.println("Bot is ready! Connected to " + event.getGuildTotalCount() + " guilds");
    }

    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        System.out.println("New member joined: " + event.getUser().getName());

        String memberRoleId = bot.getConfig().getMemberRoleId();
        Role memberRole = event.getGuild().getRoleById(memberRoleId);
        if (memberRole != null) {
            event.getGuild().addRoleToMember(event.getMember(), memberRole).queue();
        }

        if (event.getMember().isPending()) {
            System.out.println("Member is pending verification, waiting to send welcome message");
            return;
        }

        sendWelcomeMessage(event.getGuild(), event.getMember());
    }

    @Override
    public void onGuildMemberUpdatePending(GuildMemberUpdatePendingEvent event) {
        if (!event.getMember().isPending()) {
            String memberRoleId = bot.getConfig().getMemberRoleId();
            Role memberRole = event.getGuild().getRoleById(memberRoleId);
            if (memberRole != null) {
                event.getGuild().addRoleToMember(event.getMember(), memberRole).queue();
            }

            sendWelcomeMessage(event.getGuild(), event.getMember());
        }
    }

    private void sendWelcomeMessage(Guild guild, Member member) {
        String welcomeChannelId = bot.getConfig().getWelcomeChannelId();
        TextChannel welcomeChannel = guild.getTextChannelById(welcomeChannelId);

        if (welcomeChannel != null) {
            String welcomeMessage = "Welcome to the server, " + member.getAsMention() + "! 👋";
            welcomeChannel.sendMessage(welcomeMessage)
                    .queue(message -> {
                        message.addReaction(Emoji.fromUnicode("👋")).queue();
                    });
        } else {
            System.out.println("Welcome channel not found with ID: " + welcomeChannelId);
        }
    }
}