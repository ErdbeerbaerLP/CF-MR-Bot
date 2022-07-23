package de.erdbeerbaerlp.curseforgeBot.commands;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public interface CFCommand {
    void execute(SlashCommandInteractionEvent ev);
}
