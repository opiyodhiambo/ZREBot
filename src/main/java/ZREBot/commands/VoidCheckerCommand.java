package ZREBot.commands;

import ZREBot.ZREBot;
import ZREBot.models.EventNameData;
import ZREBot.models.UserData;
import ZREBot.repositories.PostgresEventNameRepository;
import ZREBot.utils.EmbedUtils;
import ZREBot.utils.PermissionUtils;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageReaction;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.requests.restaction.pagination.ReactionPaginationAction;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VoidCheckerCommand implements Command {
    private final ZREBot bot;
    private final PostgresEventNameRepository repository;

    private static final OptionData[] VOID_CHECKER_OPTIONS = {
            new OptionData(OptionType.STRING, "reaction-message", "The ID of the message to check", true),
            new OptionData(OptionType.STRING, "user-name", "Check an EVENTNAME, username, nickname, or user ID", false),
            new OptionData(OptionType.USER, "user", "Check a user", false)
    };

    public VoidCheckerCommand(ZREBot bot) {
        this.bot = bot;
        this.repository = bot.getEventNameRepository();
    }

    @Override
    public CommandData getCommandData() {
        return Commands.slash("void-checker", "Check a message for user reactions")
                .addOptions(VOID_CHECKER_OPTIONS);
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

        event.deferReply(true).queue();

        String messageId = event.getOption("reaction-message").getAsString();
        OptionMapping userNameOption = event.getOption("user-name");
        OptionMapping userOption = event.getOption("user");

        String queryName = userNameOption != null ? userNameOption.getAsString().toLowerCase() : null;
        User targetUser = userOption != null ? userOption.getAsUser() : null;

        if (targetUser == null && queryName == null) {
            event.getHook().sendMessageEmbeds(EmbedUtils.createErrorEmbed(
                    "You must provide an input option for a user scanner"
            )).queue();
            return;
        }

        try {
            event.getChannel().retrieveMessageById(messageId).queue(
                    targetMessage -> {
                        try {
                            List<MessageReaction> reactions = targetMessage.getReactions();
                            if (reactions.isEmpty()) {
                                event.getHook().sendMessageEmbeds(EmbedUtils.createEmbed(
                                        Color.YELLOW,
                                        "⚠️ No reactions found on the message"
                                )).queue();
                                return;
                            }

                            Map<String, UserData> userData = collectUserData(event, reactions);

                            if (userData.isEmpty()) {
                                event.getHook().sendMessageEmbeds(EmbedUtils.createEmbed(
                                        Color.YELLOW,
                                        "⚠️ No valid user reactions found"
                                )).queue();
                                return;
                            }

                            UserData foundUser = findUser(userData, queryName, targetUser);

                            if (foundUser == null) {
                                event.getHook().sendMessageEmbeds(EmbedUtils.createErrorEmbed(
                                        "That user is not reacted. **Try checking the user individually, or check the user name and not the discord name shown.** " +
                                                "If you are checking an event name, the win should be **voided.**"
                                )).queue();
                                return;
                            }

                            String message = "🌍 **" + foundUser.getUserName() + " (" + foundUser.getUserId() + ") is reacted (out of `" +
                                    userData.size() + "` reacts):** \n\n" +
                                    "Here is all the info I was able to find on the user you searched for...\n" +
                                    "```\n" +
                                    "USERNAME: " + foundUser.getUserName() + "\n" +
                                    "DISPLAY NAME: " + foundUser.getDisplayName() + "\n" +
                                    "NICKNAME: " + (foundUser.getNickname() != null ? foundUser.getNickname() : "None") + "\n" +
                                    "EVENTNAME: " + (foundUser.getEventName() != null ? foundUser.getEventName() : "None") + "\n" +
                                    "USERID: " + foundUser.getUserId() + "\n" +
                                    "```\n\n" +
                                    "As long as the IGN of this user is any of the names above, this user's wins **should not be voided.**";

                            event.getHook().sendMessageEmbeds(EmbedUtils.createEmbed(
                                    Color.BLUE,
                                    message
                            )).queue();
                        } catch (Exception e) {
                            e.printStackTrace();
                            event.getHook().sendMessageEmbeds(EmbedUtils.createErrorEmbed(
                                    "An error occurred while processing the message: " + e.getMessage()
                            )).queue();
                        }
                    },
                    error -> {
                        event.getHook().sendMessageEmbeds(EmbedUtils.createErrorEmbed(
                                "Could not find that message (Error " + error.getMessage() +
                                        "). **RUN THIS COMMAND IN THE CHANNEL THE MESSAGE IS IN**"
                        )).queue();
                    }
            );
        } catch (Exception e) {
            e.printStackTrace();
            event.getHook().sendMessageEmbeds(EmbedUtils.createErrorEmbed(
                    "An error occurred: " + e.getMessage()
            )).queue();
        }
    }

    private Map<String, UserData> collectUserData(SlashCommandInteractionEvent event, List<MessageReaction> reactions) {
        Map<String, UserData> userData = new HashMap<>();

        try {
            for (MessageReaction reaction : reactions) {
                System.out.println("Processing reaction: " + reaction.getEmoji() + " with count: " + reaction.getCount());

                ReactionPaginationAction users = reaction.retrieveUsers();
                List<User> allUsers = new ArrayList<>();

                users.forEachAsync(user -> {
                    allUsers.add(user);
                    return true;
                }).get();

                System.out.println("Retrieved " + allUsers.size() + " users for reaction " + reaction.getEmoji());

                for (User user : allUsers) {
                    if (user.isBot()) continue;

                    System.out.println("Found user reaction: " + user.getName() + " (" + user.getId() + ")");

                    if (userData.containsKey(user.getId())) continue;

                    try {
                        Member member;
                        try {
                            member = event.getGuild().retrieveMemberById(user.getId()).complete();
                        } catch (Exception e) {
                            System.err.println("Error retrieving member " + user.getId() + ": " + e.getMessage());
                            continue;
                        }

                        if (member == null) {
                            System.out.println("Could not retrieve member for " + user.getName() + " (" + user.getId() + ")");
                            continue;
                        }

                        String eventName = null;
                        EventNameData data = repository.getEventNameByUser(user.getId());
                        if (data != null) {
                            eventName = data.getName();
                        }

                        userData.put(user.getId(), new UserData(
                                user.getName().toLowerCase(),
                                member.getEffectiveName().toLowerCase(),
                                member.getNickname() != null ? member.getNickname().toLowerCase() : null,
                                eventName,
                                user.getId()
                        ));

                        System.out.println("Successfully processed user: " + user.getName());
                    } catch (Exception e) {
                        System.err.println("Error processing user " + user.getId() + ": " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error collecting user data: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("Total users found: " + userData.size());
        return userData;
    }

    private UserData findUser(Map<String, UserData> userData, String queryName, User targetUser) {
        if (queryName != null && targetUser != null) {
            UserData data = userData.get(targetUser.getId());
            if (data != null && data.getUserName().equals(queryName)) {
                return data;
            }
            return null;
        }

        if (targetUser != null) {
            return userData.get(targetUser.getId());
        }

        if (queryName != null) {
            for (UserData data : userData.values()) {
                if (data.getUserName().equals(queryName) ||
                        data.getDisplayName().equals(queryName) ||
                        (data.getNickname() != null && data.getNickname().equals(queryName)) ||
                        (data.getEventName() != null && data.getEventName().equals(queryName))) {
                    return data;
                }
            }
        }

        return null;
    }
}