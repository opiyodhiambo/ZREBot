package ZREBot.commands;

import ZREBot.ZREBot;
import ZREBot.models.EventNameData;
import ZREBot.repositories.PostgresEventNameRepository;
import ZREBot.utils.EmbedUtils;
import ZREBot.utils.PermissionUtils;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;

import java.awt.Color;
import java.util.List;

public class EventNameCommand implements Command {
    private final ZREBot bot;
    private final PostgresEventNameRepository repository; // Changed to PostgreSQL

    private static final SubcommandData EVENTNAME_SUBMIT = new SubcommandData(
            "submit", "Submit an eventname for all future events"
    );

    private static final SubcommandData EVENTNAME_CHECK = new SubcommandData(
            "check", "Check a user's event name (moderator only)"
    )
            .addOption(OptionType.USER, "user", "The user to check", false)
            .addOption(OptionType.STRING, "name", "Query an eventname", false);

    public EventNameCommand(ZREBot bot) {
        this.bot = bot;
        this.repository = bot.getEventNameRepository(); // Now returns PostgresEventNameRepository
    }

    @Override
    public CommandData getCommandData() {
        return Commands.slash("eventname", "Submit your name for a current event")
                .addSubcommands(EVENTNAME_SUBMIT, EVENTNAME_CHECK);
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        String subcommandName = event.getSubcommandName();

        if (subcommandName == null) {
            event.reply("Invalid subcommand!").setEphemeral(true).queue();
            return;
        }

        switch (subcommandName) {
            case "submit":
                handleEventNameSubmit(event);
                break;
            case "check":
                handleEventNameCheck(event);
                break;
            default:
                event.reply("Unknown subcommand: " + subcommandName).setEphemeral(true).queue();
        }
    }

    private void handleEventNameSubmit(SlashCommandInteractionEvent event) {
        TextInput nameInput = TextInput.create("name", "Submit your eventname below", TextInputStyle.SHORT)
                .setPlaceholder("This is the name you are going to play under")
                .setRequired(true)
                .setMaxLength(50)
                .build();

        Modal modal = Modal.create("eventNameModal", "ZombsRoyale Eventname Form")
                .addActionRow(nameInput)
                .build();

        event.replyModal(modal).queue();
    }

    private void handleEventNameCheck(SlashCommandInteractionEvent event) {
        boolean hasPermission = PermissionUtils.isModerator(event.getMember(), bot.getConfig());

        if (!hasPermission) {
            event.replyEmbeds(EmbedUtils.createErrorEmbed(
                    "You don't have permission to check event names. This command is only available to moderators."
            )).setEphemeral(true).queue();
            return;
        }

        OptionMapping userOption = event.getOption("user");
        OptionMapping nameOption = event.getOption("name");

        User user = userOption != null ? userOption.getAsUser() : null;
        String queryName = nameOption != null ? nameOption.getAsString() : null;

        if (user == null && queryName == null) {
            event.replyEmbeds(EmbedUtils.createWarningEmbed(
                    "You must provide either a user or a name to check"
            )).setEphemeral(true).queue();
            return;
        }

        // Defer reply for database operations
        event.deferReply(true).queue();

        try {
            if (user != null && queryName != null) {
                checkUserAndName(event, user, queryName);
            } else if (user != null) {
                checkUser(event, user);
            } else if (queryName != null) {
                checkName(event, queryName);
            }
        } catch (Exception e) {
            e.printStackTrace();
            event.getHook().sendMessageEmbeds(EmbedUtils.createErrorEmbed(
                    "An error occurred while processing your request: " + e.getMessage()
            )).queue();
        }
    }

    private void checkUserAndName(SlashCommandInteractionEvent event, User user, String queryName) {
        EventNameData userData = repository.getEventNameByUserAndName(user.getId(), queryName);

        if (userData == null) {
            event.getHook().sendMessageEmbeds(EmbedUtils.createErrorEmbed(
                    "There was no event name data matching both `" + user.getName() + "` and `" + queryName + "`"
            )).queue();
            return;
        }

        String date = "<t:" + (long)(userData.getTimestamp() / 1000) + ":R>";

        event.getHook().sendMessageEmbeds(EmbedUtils.createEmbed(
                Color.BLUE,
                "🌍 **" + user.getName() + "'s Event Name Information:** \n\n" +
                        "> **Name:** `" + userData.getName() + "` \n" +
                        "> **Date Submitted:** " + date + " \n\n" +
                        "Please note: this was their most recent name submission, and is what their name should be ingame (or their discord name)"
        )).queue();
    }

    private void checkUser(SlashCommandInteractionEvent event, User user) {
        EventNameData userData = repository.getEventNameByUser(user.getId());

        if (userData == null) {
            event.getHook().sendMessageEmbeds(EmbedUtils.createErrorEmbed(
                    "There is no event name data for `" + user.getName() + "`"
            )).queue();
            return;
        }

        String date = "<t:" + (long)(userData.getTimestamp() / 1000) + ":R>";

        event.getHook().sendMessageEmbeds(EmbedUtils.createEmbed(
                Color.BLUE,
                "🌍 **" + user.getName() + "'s Event Name Information:** \n\n" +
                        "> **Name:** `" + userData.getName() + "` \n" +
                        "> **Date Submitted:** " + date + " \n\n" +
                        "Please note: this was their most recent name submission, and is what their name should be ingame (or their discord name)"
        )).queue();
    }

    private void checkName(SlashCommandInteractionEvent event, String queryName) {
        List<EventNameData> queryData = repository.searchEventNameByName(queryName);

        if (queryData.isEmpty()) {
            event.getHook().sendMessageEmbeds(EmbedUtils.createErrorEmbed(
                    "No data found matching `" + queryName + "`"
            )).queue();
            return;
        }

        StringBuilder outputString = new StringBuilder("🌍 **Event Name Data Matching `" + queryName + "`**\n\n");

        for (EventNameData data : queryData) {
            try {
                User qUser = event.getJDA().retrieveUserById(data.getUserId()).complete();
                if (qUser != null) {
                    outputString.append("> **").append(qUser.getName()).append("** has submitted `")
                            .append(data.getName()).append("` as their event name.\n\n");
                }
            } catch (Exception e) {
                outputString.append("> Unknown user (ID: ").append(data.getUserId())
                        .append(") has submitted `").append(data.getName()).append("` as their event name.\n\n");
            }
        }

        event.getHook().sendMessageEmbeds(EmbedUtils.createEmbed(
                Color.BLUE,
                outputString.toString()
        )).queue();
    }
}