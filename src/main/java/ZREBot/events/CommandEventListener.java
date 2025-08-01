package ZREBot.events;

import ZREBot.ZREBot;
import ZREBot.commands.*;
import ZREBot.config.BotConfig;
import ZREBot.utils.EmbedUtils;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.commands.OptionType;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

public class CommandEventListener extends ListenerAdapter {
    private final ZREBot bot;
    private final Map<String, Command> commands = new HashMap<>();

    private static final OptionData[] VOID_CHECKER_OPTIONS = {
            new OptionData(OptionType.STRING, "reaction-message", "The ID of the message to check", true),
            new OptionData(OptionType.STRING, "user-name", "Check an EVENTNAME, username, nickname, or user ID", false),
            new OptionData(OptionType.USER, "user", "Check a user", false)
    };

    private static final SubcommandData EVENTNAME_SUBMIT = new SubcommandData(
            "submit", "Submit an eventname for all future events"
    );

    private static final SubcommandData EVENTNAME_CHECK = new SubcommandData(
            "check", "Check a user's event name (moderator only)"
    )
            .addOption(OptionType.USER, "user", "The user to check", false)
            .addOption(OptionType.STRING, "name", "Query an eventname", false);

    private static final SubcommandData BAN_CREATE = new SubcommandData(
            "create", "Ban a user from the server"
    )
            .addOption(OptionType.USER, "user", "The user to ban", true)
            .addOption(OptionType.STRING, "reason", "The reason for the ban", true);

    private static final SubcommandData BAN_REMOVE = new SubcommandData(
            "remove", "Unban a user from the server"
    )
            .addOption(OptionType.STRING, "user", "The user ID to unban", true);

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

    public CommandEventListener(ZREBot bot) {
        this.bot = bot;
        registerCommandHandlers();
    }

    private void registerCommandHandlers() {
        registerCommand(new TestCommand());
        registerCommand(new EventNameCommand(bot));
        registerCommand(new BanCommand(bot));
        registerCommand(new VoidCheckerCommand(bot));
        registerCommand(new RoleCommand(bot));
    }

    private void registerCommand(Command command) {
        commands.put(command.getName(), command);
    }

    public static void registerCommands(JDA jda, BotConfig config) {
        Guild guild = jda.getGuildById(config.getChannelId());
        System.out.println("Guild Id " + guild);
        try {
            guild.updateCommands()
                    .addCommands(
                            Commands.slash("test", "Check if the bot is working"),

                            Commands.slash("eventname", "Submit your name for a current event")
                                    .addSubcommands(EVENTNAME_SUBMIT, EVENTNAME_CHECK),

                            Commands.slash("ban", "Ban management")
                                    .addSubcommands(BAN_CREATE, BAN_REMOVE),

                            Commands.slash("void-checker", "Check a message for user reactions")
                                    .addOptions(VOID_CHECKER_OPTIONS),

                            Commands.slash("role", "Role management")
                                    .addSubcommands(ROLE_ADD, ROLE_REMOVE)
                    )
                    .queue(commands -> System.out.println("Successfully registered " + commands.size() + " slash commands"));
        } catch (Exception e) {
            System.err.println("Error registering commands: " + e.getMessage());
        }
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        String commandName = event.getName();
        Command command = commands.get(commandName);

        if (command != null) {
            try {
                command.execute(event);
            } catch (Exception e) {
                e.printStackTrace();
                if (!event.isAcknowledged()) {
                    event.replyEmbeds(EmbedUtils.createErrorEmbed(
                            "An error occurred while executing this command: " + e.getMessage()
                    )).setEphemeral(true).queue();
                }
            }
        } else {
            event.reply("Unknown command: " + commandName).setEphemeral(true).queue();
        }
    }
}