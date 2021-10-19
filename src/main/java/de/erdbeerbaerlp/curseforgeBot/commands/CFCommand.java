package de.erdbeerbaerlp.curseforgeBot.commands;

import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

public interface CFCommand {
    void execute(SlashCommandEvent ev);
}
