package ZREBot.commands;

import ZREBot.ZREBot;
import ZREBot.services.BanService;
import ZREBot.utils.EmbedUtils;
import ZREBot.utils.PermissionUtils;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

import java.awt.Color;

public class BanCommand implements Command {
    private final ZREBot bot;
    private final BanService banService;

    private static final SubcommandData BAN_CREATE = new SubcommandData(
            "create", "Ban a user from the server"
    )
            .addOption(OptionType.USER, "user", "The user to ban", true)
            .addOption(OptionType.STRING, "reason", "The reason for the ban", true);

    private static final SubcommandData BAN_REMOVE = new SubcommandData(
            "remove", "Unban a user from the server"
    )
            .addOption(OptionType.STRING, "user", "The user ID to unban", true);

    public BanCommand(ZREBot bot) {
        this.bot = bot;
        this.banService = new BanService();
    }

    @Override
    public CommandData getCommandData() {
        return Commands.slash("ban", "Ban management")
                .addSubcommands(BAN_CREATE, BAN_REMOVE);
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        boolean hasPermission = PermissionUtils.isModerator(event.getMember(), bot.getConfig());

        if (!hasPermission) {
            event.replyEmbeds(EmbedUtils.createErrorEmbed(
                    "You don't have permission to use this command."
            )).setEphemeral(true).queue();
            return;
        }

        String subcommandName = event.getSubcommandName();
        if (subcommandName == null) {
            event.reply("Invalid subcommand!").setEphemeral(true).queue();
            return;
        }

        Guild guild = event.getGuild();
        if (guild == null) {
            event.reply("This command can only be used in a server.").setEphemeral(true).queue();
            return;
        }

        String modLogChannelId = bot.getConfig().getModLogChannelId();
        TextChannel modChannel = guild.getTextChannelById(modLogChannelId);

        switch (subcommandName) {
            case "create":
                handleBanCreate(event, guild, modChannel);
                break;
            case "remove":
                handleBanRemove(event, guild, modChannel);
                break;
            default:
                event.reply("Unknown subcommand: " + subcommandName).setEphemeral(true).queue();
        }
    }

    private void handleBanCreate(SlashCommandInteractionEvent event, Guild guild, TextChannel modChannel) {
        User targetUser = event.getOption("user").getAsUser();
        String reason = event.getOption("reason").getAsString();

        event.deferReply(true).queue();

        banService.banUser(
                guild,
                targetUser,
                reason,
                modChannel,
                event.getUser(),
                success -> {
                    event.getHook().sendMessageEmbeds(EmbedUtils.createEmbed(
                            "Ban Successful",
                            Color.GREEN,
                            "✅ Successfully banned " + targetUser.getName() + " for: " + reason
                    )).queue();
                },
                error -> {
                    event.getHook().sendMessageEmbeds(EmbedUtils.createEmbed(
                            "Ban Failed",
                            Color.RED,
                            "❌ Failed to ban user: " + error.getMessage()
                    )).queue();
                }
        );
    }

    private void handleBanRemove(SlashCommandInteractionEvent event, Guild guild, TextChannel modChannel) {
        String userIdToUnban = event.getOption("user").getAsString();
        event.deferReply(true).queue();

        banService.unbanUser(
                guild,
                userIdToUnban,
                modChannel,
                event.getUser(),
                success -> {
                    event.getHook().sendMessageEmbeds(EmbedUtils.createEmbed(
                            "Unban Successful",
                            Color.GREEN,
                            "✅ Successfully unbanned user with ID: " + userIdToUnban
                    )).queue();
                },
                error -> {
                    event.getHook().sendMessageEmbeds(EmbedUtils.createEmbed(
                            "Unban Failed",
                            Color.RED,
                            "❌ Failed to unban user: " + error.getMessage()
                    )).queue();
                },
                () -> {
                    event.getHook().sendMessageEmbeds(EmbedUtils.createEmbed(
                            "User Not Banned",
                            Color.YELLOW,
                            "⚠️ User with ID " + userIdToUnban + " is not banned"
                    )).queue();
                }
        );
    }
}