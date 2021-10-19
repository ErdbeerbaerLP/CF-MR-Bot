package de.erdbeerbaerlp.curseforgeBot.commands;

import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

public class SupportCommand extends CommandData implements CFCommand {
    public SupportCommand() {
        super("support", "Need help? Join my discord server");
    }

    @Override
    public void execute(SlashCommandEvent ev) {
        ev.deferReply(true).setContent("https://discord.gg/PGPWdRBQms").queue();
    }
}
