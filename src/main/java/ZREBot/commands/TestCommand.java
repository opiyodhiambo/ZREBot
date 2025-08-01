package ZREBot.commands;

import ZREBot.utils.EmbedUtils;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

import java.awt.Color;

public class TestCommand implements Command {

    @Override
    public CommandData getCommandData() {
        return Commands.slash("test", "Check if the bot is working");
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        event.replyEmbeds(EmbedUtils.createEmbed(
                Color.BLUE,
                "<:ZRE:1075937292675461270> ZombsRoyale Events bot is currently working!"
        )).setEphemeral(true).queue();
    }
}