package de.erdbeerbaerlp.curseforgeBot.commands;

import de.erdbeerbaerlp.curseforgeBot.Main;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.internal.interactions.CommandDataImpl;

public class DelProjectCommand extends CommandDataImpl implements CFCommand {
    public DelProjectCommand() {
        super("del-project", "Removes an curseforge project from this channel");
        addOption(OptionType.INTEGER, "project-id", "ID of the Curseforge Project", true);
    }

    @Override
    public void execute(SlashCommandInteractionEvent ev) {
        if (!ev.getMember().hasPermission(Permission.MANAGE_SERVER)) {
            ev.deferReply(true).setContent("You need manage server permissions for this command!").queue();
            return;
        }
        final OptionMapping id = ev.getOption("project-id");
        if (id == null) ev.reply("Project ID == null ?!?").queue();
        else {
            final int projectID = Integer.parseInt(id.getAsString());
            if (Main.projects.containsKey(projectID)) {
                final String name = Main.projects.get(projectID).proj.name;
                Main.projects.get(projectID).removeChannel(Main.ifa.deleteChannelFromProject(projectID, ev.getChannel().getIdLong()));
                ev.reply("Removed Project \"" + name + "\" from this channel!").queue();
            } else {
                ev.reply("This project does not seem to be attached to this channel :thinking:").queue();
            }
        }
    }
}
