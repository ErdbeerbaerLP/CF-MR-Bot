package de.erdbeerbaerlp.curseforgeBot.commands;

import de.erdbeerbaerlp.curseforgeBot.Main;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.internal.interactions.CommandDataImpl;

public class DelProjectCommand extends CommandDataImpl implements DCCommand {
    public DelProjectCommand() {
        super("del-project", "Removes an curseforge project from this channel");
        final SubcommandData curseforge = new SubcommandData("curseforge", "Removes an curseforge project from this channel");
        final SubcommandData modrinth = new SubcommandData("modrinth", "Removes an modrinth project from this channel");
        curseforge.addOption(OptionType.INTEGER, "project-id", "ID of the Curseforge Project", true);
        modrinth.addOption(OptionType.STRING, "project-id", "ID of the Modrinth Project", true);

        addSubcommands(curseforge, modrinth);
    }

    @Override
    public void execute(SlashCommandInteractionEvent ev) {
        if (!ev.getMember().hasPermission(Permission.MANAGE_SERVER)) {
            ev.deferReply(true).setContent("You need manage server permissions for this command!").queue();
            return;
        }
        switch (ev.getSubcommandName()) {
            case "curseforge":
                final OptionMapping id = ev.getOption("project-id");
                if (id == null) ev.reply("Project ID == null ?!?").queue();
                else {
                    final long projectID = Long.parseLong(id.getAsString());
                    if (Main.cfProjects.containsKey(projectID)) {
                        final String name = Main.cfProjects.get(projectID).proj.name;
                        Main.ifa.deleteChannelFromCFProject(projectID, ev.getChannel().getIdLong());
                        final boolean fullyRemoved = Main.cfProjects.get(projectID).removeChannel(ev.getChannel().getIdLong());
                        ev.reply("Removed Project \"" + name + "\" from this channel!" + (fullyRemoved ? "" : "\nFailed to remove channel from memory")).queue();
                    } else {
                        ev.reply("This project does not seem to be attached to this channel :thinking:").queue();
                    }
                }
                break;
            case "modrinth":
                final OptionMapping projID = ev.getOption("project-id");
                if (projID == null) ev.reply("Project ID == null ?!?").queue();
                else {
                    final String projectID = projID.getAsString();
                    if (Main.mrProjects.containsKey(projectID)) {
                        final String name = Main.mrProjects.get(projectID).proj.getTitle();
                        Main.ifa.deleteChannelFromMRProject(projectID, ev.getChannel().getIdLong());
                        final boolean fullyRemoved = Main.mrProjects.get(projectID).removeChannel(ev.getChannel().getIdLong());
                        ev.reply("Removed Project \"" + name + "\" from this channel!" + (fullyRemoved ? "" : "\nFailed to remove channel from memory")).queue();
                    } else {
                        ev.reply("This project does not seem to be attached to this channel :thinking:").queue();
                    }
                }
                break;
        }
    }
}
