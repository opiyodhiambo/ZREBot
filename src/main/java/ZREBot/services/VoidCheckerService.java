package ZREBot.services;

import ZREBot.models.EventNameData;
import ZREBot.models.UserData;
import ZREBot.repositories.EventNameRepository;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageReaction;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.requests.restaction.pagination.ReactionPaginationAction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

public class VoidCheckerService {
    private final EventNameRepository eventNameRepository;

    public VoidCheckerService(EventNameRepository eventNameRepository) {
        this.eventNameRepository = eventNameRepository;
    }

    public void checkUserReaction(
            MessageChannel messageChannel,
            String messageId,
            String queryName,
            User targetUser,
            Guild guild,
            Consumer<UserCheckResult> onSuccess,
            Runnable onUserNotFound,
            Runnable onNoReactions,
            Runnable onNoValidReactions,
            Consumer<String> onMessageNotFound,
            Consumer<Throwable> onError) {
        try {
            messageChannel.retrieveMessageById(messageId).queue(
                    targetMessage -> {
                        try {
                            List<MessageReaction> reactions = targetMessage.getReactions();
                            if (reactions.isEmpty()) {
                                onNoReactions.run();
                                return;
                            }

                            Map<String, UserData> userData = collectUserData(reactions, guild);

                            if (userData.isEmpty()) {
                                onNoValidReactions.run();
                                return;
                            }

                            UserData foundUser = findUser(userData, queryName, targetUser);

                            if (foundUser == null) {
                                onUserNotFound.run();
                                return;
                            }

                            onSuccess.accept(new UserCheckResult(foundUser, userData.size()));
                        } catch (Exception e) {
                            onError.accept(e);
                        }
                    },
                    error -> onMessageNotFound.accept(error.getMessage())
            );
        } catch (Exception e) {
            onError.accept(e);
        }
    }

    public String formatUserCheckResult(UserCheckResult result) {
        UserData foundUser = result.getUserData();
        int totalReactions = result.getTotalReactions();

        return "🌍 **" + foundUser.getUserName() + " (" + foundUser.getUserId() + ") is reacted (out of `" +
                totalReactions + "` reacts):** \n\n" +
                "Here is all the info I was able to find on the user you searched for...\n" +
                "```\n" +
                "USERNAME: " + foundUser.getUserName() + "\n" +
                "DISPLAY NAME: " + foundUser.getDisplayName() + "\n" +
                "NICKNAME: " + (foundUser.getNickname() != null ? foundUser.getNickname() : "None") + "\n" +
                "EVENTNAME: " + (foundUser.getEventName() != null ? foundUser.getEventName() : "None") + "\n" +
                "USERID: " + foundUser.getUserId() + "\n" +
                "```\n\n" +
                "As long as the IGN of this user is any of the names above, this user's wins **should not be voided.**";
    }

    private Map<String, UserData> collectUserData(List<MessageReaction> reactions, Guild guild)
            throws ExecutionException, InterruptedException {
        Map<String, UserData> userData = new HashMap<>();

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
                        member = guild.retrieveMemberById(user.getId()).complete();
                    } catch (Exception e) {
                        System.err.println("Error retrieving member " + user.getId() + ": " + e.getMessage());
                        continue;
                    }

                    if (member == null) {
                        System.out.println("Could not retrieve member for " + user.getName() + " (" + user.getId() + ")");
                        continue;
                    }

                    String eventName = null;
                    EventNameData data = eventNameRepository.getEventNameByUser(user.getId());
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

    public CompletableFuture<Boolean> doesMessageExist(MessageChannel messageChannel, String messageId) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        messageChannel.retrieveMessageById(messageId).queue(
                message -> future.complete(true),
                error -> future.complete(false)
        );

        return future;
    }

    public static class UserCheckResult {
        private final UserData userData;
        private final int totalReactions;

        public UserCheckResult(UserData userData, int totalReactions) {
            this.userData = userData;
            this.totalReactions = totalReactions;
        }

        public UserData getUserData() {
            return userData;
        }

        public int getTotalReactions() {
            return totalReactions;
        }
    }
}