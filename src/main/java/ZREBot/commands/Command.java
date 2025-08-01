package ZREBot.commands;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

public interface Command {

    CommandData getCommandData();

    void execute(SlashCommandInteractionEvent event);

    default String getName() {
        return getCommandData().getName();
    }
}