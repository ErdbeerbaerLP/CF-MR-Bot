package de.erdbeerbaerlp.curseforgeBot.commands;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.internal.interactions.CommandDataImpl;

public class SupportCommand extends CommandDataImpl implements CFCommand {
    public SupportCommand() {
        super("support", "Need help? Join my discord server");
    }

    @Override
    public void execute(SlashCommandInteractionEvent ev) {
        ev.deferReply(true).setContent("https://discord.gg/PGPWdRBQms").queue();
    }
}
