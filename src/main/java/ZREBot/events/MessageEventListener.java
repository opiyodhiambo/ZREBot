package ZREBot.events;

import ZREBot.ZREBot;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.List;

public class MessageEventListener extends ListenerAdapter {
    private final ZREBot bot;

    public MessageEventListener(ZREBot bot) {
        this.bot = bot;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        String channelId = event.getChannel().getId();
        List<String> autoReactionChannels = bot.getConfig().getAutoReactionChannels();

        if (autoReactionChannels.contains(channelId)) {
            addReactionsToMessage(event.getMessage());
            if (event.getAuthor().isBot() && !event.isWebhookMessage()) {
                return;
            }
        }
        else if (event.getAuthor().isBot()) {
            return;
        }

        String content = event.getMessage().getContentRaw().toLowerCase();
        String eventNameChannel = bot.getConfig().getEventNameChannelId();

        if (channelId.equals(eventNameChannel) &&
                (content.contains("eventname") || content.contains("-eventname"))) {
            event.getMessage().reply("⚠️ **THAT IS NOT HOW YOU SUBMIT EVENT NAMES!** " +
                            "TO SUBMIT NAMES: use the `/eventname submit` SLASH COMMAND within this channel. " +
                            "If you don't correct this, your wins will be voided.")
                    .queue();
        }
    }

    private void addReactionsToMessage(Message message) {
        try {
            String emojiName = bot.getConfig().getZreEmojiName();
            String emojiId = bot.getConfig().getZreEmojiId();

            message.addReaction(Emoji.fromCustom(emojiName, Long.parseLong(emojiId), false)).queue(
                    success -> System.out.println("Added ZRE reaction to message in channel " + message.getChannel().getId()),
                    error -> {
                        System.err.println("Failed to add ZRE reaction to message: " + error.getMessage());
                        message.addReaction(Emoji.fromUnicode("👍")).queue();
                    }
            );
        } catch (Exception e) {
            System.err.println("Error adding reactions to message: " + e.getMessage());
            e.printStackTrace();

            try {
                message.addReaction(Emoji.fromUnicode("👍")).queue();
            } catch (Exception ex) {
                System.err.println("Fallback reaction also failed: " + ex.getMessage());
            }
        }
    }
}