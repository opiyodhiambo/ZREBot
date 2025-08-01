package ZREBot.events;

import ZREBot.ZREBot;
import ZREBot.repositories.PostgresEventNameRepository;
import ZREBot.utils.EmbedUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.awt.Color;
import java.time.Instant;

public class ModalEventListener extends ListenerAdapter {
    private final ZREBot bot;

    public ModalEventListener(ZREBot bot) {
        this.bot = bot;
    }

    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        if (!event.getModalId().equals("eventNameModal")) {
            return;
        }

        String eventName = event.getValue("name").getAsString();
        String userId = event.getUser().getId();
        String username = event.getUser().getName();

        // Defer reply to handle potential database delays
        event.deferReply(true).queue();

        try {
            // Use PostgreSQL repository
            PostgresEventNameRepository repository = bot.getEventNameRepository();
            repository.saveEventName(userId, eventName);

            // Log to channel
            String nameLogChannelId = bot.getConfig().getNameLogChannelId();
            TextChannel nameChannel = event.getGuild().getTextChannelById(nameLogChannelId);

            if (nameChannel != null) {
                EmbedBuilder embed = new EmbedBuilder()
                        .setTitle(eventName)
                        .setColor(Color.BLUE)
                        .setFooter("Nickname of " + username + " (" + userId + ")")
                        .setTimestamp(Instant.now());

                nameChannel.sendMessageEmbeds(embed.build()).queue();
            }

            // Send confirmation to user
            event.getHook().sendMessageEmbeds(EmbedUtils.createEmbed(
                    Color.BLUE,
                    "<:ZRE:1075937292675461270> Your eventname has been recorded. " +
                            "You can play under the name \"" + eventName + "\" for all future " +
                            event.getGuild().getName() + " events. To change your name, do /eventname again"
            )).queue();

        } catch (Exception e) {
            System.err.println("Error saving event name for user " + userId + ": " + e.getMessage());
            e.printStackTrace();

            event.getHook().sendMessageEmbeds(EmbedUtils.createErrorEmbed(
                    "An error occurred while saving your event name. Please try again later."
            )).queue();
        }
    }
}