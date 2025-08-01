package ZREBot.events;

import ZREBot.ZREBot;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class ReactionEventListener extends ListenerAdapter {
    private final ZREBot bot;

    private static final String STAFF_STRIKES_CHANNEL_ID = "1197710900426190910";

    private final String reactionRoleId;

    public ReactionEventListener(ZREBot bot) {
        this.bot = bot;
        this.reactionRoleId = bot.getConfig().getStaffStrikesRoleId();
    }

    @Override
    public void onMessageReactionAdd(MessageReactionAddEvent event) {

        if (!event.getChannel().getId().equals(STAFF_STRIKES_CHANNEL_ID)) {
            return;
        }

        if (event.getUser().isBot()) {
            return;
        }

        if (isValidReactionEmoji(event.getEmoji())) {
            assignRoleToUser(event.getMember());
        }
    }

    @Override
    public void onMessageReactionRemove(MessageReactionRemoveEvent event) {

        if (!event.getChannel().getId().equals(STAFF_STRIKES_CHANNEL_ID)) {
            return;
        }

        if (event.getUser().isBot()) {
            return;
        }

        if (isValidReactionEmoji(event.getEmoji())) {

            event.getGuild().retrieveMemberById(event.getUserId()).queue(
                    member -> removeRoleFromUser(member),
                    error -> System.err.println("Failed to retrieve member for reaction removal: " + error.getMessage())
            );
        }
    }

    private boolean isValidReactionEmoji(Emoji emoji) {
        String emojiId = bot.getConfig().getZreEmojiId();
        String emojiName = bot.getConfig().getZreEmojiName();

        String emojiFormatted = emoji.getFormatted();
        String expectedZreEmoji = "<:" + emojiName + ":" + emojiId + ">";

        if (emojiFormatted.equals(expectedZreEmoji)) {
            return true;
        }

        return emojiFormatted.equals("👍");
    }

    private void assignRoleToUser(Member member) {
        if (member == null) {
            return;
        }

        Role role = member.getGuild().getRoleById(reactionRoleId);
        if (role == null) {
            System.err.println("Role with ID " + reactionRoleId + " not found!");
            return;
        }

        if (member.getRoles().contains(role)) {
            System.out.println("User " + member.getUser().getName() + " already has the role " + role.getName());
            return;
        }

        member.getGuild().addRoleToMember(member, role).queue(
                success -> System.out.println("Successfully assigned role " + role.getName() + " to " + member.getUser().getName()),
                error -> System.err.println("Failed to assign role to " + member.getUser().getName() + ": " + error.getMessage())
        );
    }

    private void removeRoleFromUser(Member member) {
        if (member == null) {
            return;
        }

        Role role = member.getGuild().getRoleById(reactionRoleId);
        if (role == null) {
            System.err.println("Role with ID " + reactionRoleId + " not found!");
            return;
        }

        if (!member.getRoles().contains(role)) {
            System.out.println("User " + member.getUser().getName() + " doesn't have the role " + role.getName());
            return;
        }

        member.getGuild().removeRoleFromMember(member, role).queue(
                success -> System.out.println("Successfully removed role " + role.getName() + " from " + member.getUser().getName()),
                error -> System.err.println("Failed to remove role from " + member.getUser().getName() + ": " + error.getMessage())
        );
    }
}