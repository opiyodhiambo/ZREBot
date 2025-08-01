package ZREBot.commands;

import ZREBot.ZREBot;
import ZREBot.utils.EmbedUtils;
import ZREBot.utils.PermissionUtils;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command.Choice;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

import java.awt.Color;
import java.util.List;
import java.util.stream.Collectors;

public class RoleCommand implements Command {
    private final ZREBot bot;

    private static final SubcommandData ROLE_ADD = new SubcommandData(
            "add", "Add a role to a user"
    )
            .addOption(OptionType.USER, "user", "The user to add the role to", true)
            .addOption(OptionType.ROLE, "role", "The role to add", true);

    private static final SubcommandData ROLE_REMOVE = new SubcommandData(
            "remove", "Remove a role from a user"
    )
            .addOption(OptionType.USER, "user", "The user to remove the role from", true)
            .addOption(OptionType.ROLE, "role", "The role to remove", true);

    public RoleCommand(ZREBot bot) {
        this.bot = bot;
    }

    @Override
    public CommandData getCommandData() {
        return Commands.slash("role", "Role management")
                .addSubcommands(ROLE_ADD, ROLE_REMOVE);
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        boolean hasPermission = hasRoleManagerPermission(event.getMember());

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

        switch (subcommandName) {
            case "add":
                handleRoleAdd(event, guild);
                break;
            case "remove":
                handleRoleRemove(event, guild);
                break;
            default:
                event.reply("Unknown subcommand: " + subcommandName).setEphemeral(true).queue();
        }
    }

    private void handleRoleAdd(SlashCommandInteractionEvent event, Guild guild) {
        User targetUser = event.getOption("user").getAsUser();
        Role targetRole = event.getOption("role").getAsRole();
        Member moderator = event.getMember();

        event.deferReply(true).queue();

        if (!canManageRole(moderator, targetRole, guild)) {
            event.getHook().sendMessageEmbeds(EmbedUtils.createErrorEmbed(
                    "You cannot add this role because it's equal to or higher than your highest role."
            )).queue();
            return;
        }

        guild.retrieveMemberById(targetUser.getId()).queue(
                targetMember -> {

                    if (targetMember.getRoles().contains(targetRole)) {
                        event.getHook().sendMessageEmbeds(EmbedUtils.createWarningEmbed(
                                targetUser.getName() + " already has the role " + targetRole.getName() + "."
                        )).queue();
                        return;
                    }

                    guild.addRoleToMember(targetMember, targetRole).queue(
                            success -> {

                                logRoleAction(guild, "added", moderator.getUser(), targetUser, targetRole);

                                event.getHook().sendMessageEmbeds(EmbedUtils.createEmbed(
                                        "Role Added",
                                        Color.GREEN,
                                        "✅ Successfully added role " + targetRole.getName() + " to " + targetUser.getName()
                                )).queue();
                            },
                            error -> {
                                event.getHook().sendMessageEmbeds(EmbedUtils.createErrorEmbed(
                                        "Failed to add role: " + error.getMessage()
                                )).queue();
                            }
                    );
                },
                error -> {
                    event.getHook().sendMessageEmbeds(EmbedUtils.createErrorEmbed(
                            "Could not find that user in this server."
                    )).queue();
                }
        );
    }

    private void handleRoleRemove(SlashCommandInteractionEvent event, Guild guild) {
        User targetUser = event.getOption("user").getAsUser();
        Role targetRole = event.getOption("role").getAsRole();
        Member moderator = event.getMember();

        event.deferReply(true).queue();

        if (!canManageRole(moderator, targetRole, guild)) {
            event.getHook().sendMessageEmbeds(EmbedUtils.createErrorEmbed(
                    "You cannot remove this role because it's equal to or higher than your highest role."
            )).queue();
            return;
        }

        guild.retrieveMemberById(targetUser.getId()).queue(
                targetMember -> {

                    if (!targetMember.getRoles().contains(targetRole)) {
                        event.getHook().sendMessageEmbeds(EmbedUtils.createWarningEmbed(
                                targetUser.getName() + " doesn't have the role " + targetRole.getName() + "."
                        )).queue();
                        return;
                    }

                    guild.removeRoleFromMember(targetMember, targetRole).queue(
                            success -> {

                                logRoleAction(guild, "removed", moderator.getUser(), targetUser, targetRole);

                                event.getHook().sendMessageEmbeds(EmbedUtils.createEmbed(
                                        "Role Removed",
                                        Color.GREEN,
                                        "✅ Successfully removed role " + targetRole.getName() + " from " + targetUser.getName()
                                )).queue();
                            },
                            error -> {
                                event.getHook().sendMessageEmbeds(EmbedUtils.createErrorEmbed(
                                        "Failed to remove role: " + error.getMessage()
                                )).queue();
                            }
                    );
                },
                error -> {
                    event.getHook().sendMessageEmbeds(EmbedUtils.createErrorEmbed(
                            "Could not find that user in this server."
                    )).queue();
                }
        );
    }

    private boolean hasRoleManagerPermission(Member member) {

        if (PermissionUtils.isAdmin(member)) {
            return true;
        }

        List<String> roleManagerRoles = List.of(
                "898223053832601611", // Server Owners
                "709747039562366977", // Management
                "1352351516882894968", //Helpers
                "1329555577646354483" //Overseer
        );

        return member.getRoles().stream()
                .anyMatch(role -> roleManagerRoles.contains(role.getId()));
    }
    private boolean canManageRole(Member moderator, Role targetRole, Guild guild) {

        if (PermissionUtils.isAdmin(moderator)) {
            return true;
        }

        Role highestRole = moderator.getRoles().isEmpty() ? null : moderator.getRoles().get(0);

        if (highestRole == null) {
            return targetRole.equals(guild.getPublicRole());
        }

        return highestRole.getPosition() > targetRole.getPosition();
    }

    private void logRoleAction(Guild guild, String action, User moderator, User target, Role role) {
        String modLogChannelId = bot.getConfig().getModLogChannelId();
        TextChannel modChannel = guild.getTextChannelById(modLogChannelId);

        if (modChannel != null) {
            String title = action.equals("added") ? "🔹 Role Added" : "🔸 Role Removed";
            String actionText = action.equals("added") ? "added role to" : "removed role from";

            net.dv8tion.jda.api.EmbedBuilder embed = new net.dv8tion.jda.api.EmbedBuilder()
                    .setTitle(title)
                    .setColor(new Color(135, 206, 235))
                    .addField("Target User", target.getName() + " (`" + target.getId() + "`)", true)
                    .addField("Role", role.getName() + " (`" + role.getId() + "`)", true)
                    .addField("Moderator", moderator.getName() + " (`" + moderator.getId() + "`)", false)
                    .setThumbnail(target.getAvatarUrl())
                    .setTimestamp(java.time.Instant.now())
                    .setFooter("Role " + action.substring(0, 1).toUpperCase() + action.substring(1));

            modChannel.sendMessageEmbeds(embed.build()).queue();
        }
    }
}