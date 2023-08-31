package de.erdbeerbaerlp.curseforgeBot.commands;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public interface DCCommand {
    void execute(SlashCommandInteractionEvent ev);
}
