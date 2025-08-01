package ZREBot.services;

import ZREBot.models.EventNameData;
import ZREBot.repositories.EventNameRepository;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import java.awt.Color;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class EventNameService {
    private final EventNameRepository repository;

    public EventNameService(EventNameRepository repository) {
        this.repository = repository;
    }

    public void saveEventName(String userId, String eventName, String username, String nameLogChannelId, Guild guild) {

        repository.saveEventName(userId, eventName);

        if (nameLogChannelId != null && !nameLogChannelId.isEmpty()) {
            TextChannel nameChannel = guild.getTextChannelById(nameLogChannelId);
            if (nameChannel != null) {
                EmbedBuilder embed = new EmbedBuilder()
                        .setTitle(eventName)
                        .setColor(Color.BLUE)
                        .setFooter("Nickname of " + username + " (" + userId + ")")
                        .setTimestamp(Instant.now());

                nameChannel.sendMessageEmbeds(embed.build()).queue();
            }
        }
    }

    public EventNameData getEventNameByUser(String userId) {
        return repository.getEventNameByUser(userId);
    }

    public EventNameData getEventNameByUserAndName(String userId, String name) {
        return repository.getEventNameByUserAndName(userId, name);
    }

    public List<EventNameData> searchEventNameByName(String name) {
        return repository.searchEventNameByName(name);
    }

    public String formatEventNameData(EventNameData userData, User user) {
        if (userData == null) {
            return null;
        }

        String date = "<t:" + (long)(userData.getTimestamp() / 1000) + ":R>";

        return "🌍 **" + user.getName() + "'s Event Name Information:** \n\n" +
                "> **Name:** `" + userData.getName() + "` \n" +
                "> **Date Submitted:** " + date + " \n\n" +
                "Please note: this was their most recent name submission, and is what their name should be ingame (or their discord name)";
    }

    public String formatEventNameSearchResults(String queryName, List<EventNameData> queryData, JDA jda) {
        if (queryData.isEmpty()) {
            return null;
        }

        StringBuilder outputString = new StringBuilder("🌍 **Event Name Data Matching `" + queryName + "`**\n\n");

        for (EventNameData data : queryData) {
            try {
                User qUser = jda.retrieveUserById(data.getUserId()).complete();
                if (qUser != null) {
                    outputString.append("> **").append(qUser.getName()).append("** has submitted `")
                            .append(data.getName()).append("` as their event name.\n\n");
                }
            } catch (Exception e) {
                outputString.append("> Unknown user (ID: ").append(data.getUserId())
                        .append(") has submitted `").append(data.getName()).append("` as their event name.\n\n");
            }
        }

        return outputString.toString();
    }
}