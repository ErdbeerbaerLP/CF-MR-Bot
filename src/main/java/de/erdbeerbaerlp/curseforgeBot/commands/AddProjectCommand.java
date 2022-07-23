package de.erdbeerbaerlp.curseforgeBot.commands;

import de.erdbeerbaerlp.cfcore.CFCoreAPI;
import de.erdbeerbaerlp.cfcore.json.CFFileIndex;
import de.erdbeerbaerlp.cfcore.json.CFMod;
import de.erdbeerbaerlp.curseforgeBot.CurseforgeProject;
import de.erdbeerbaerlp.curseforgeBot.Main;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.internal.interactions.CommandDataImpl;

public class AddProjectCommand extends CommandDataImpl implements CFCommand {
    public AddProjectCommand() {
        super("add-project", "Adds an curseforge project to this channel");
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
                Main.projects.get(projectID).addChannel(Main.ifa.addChannelToProject(projectID, ev.getChannel().getIdLong()));
                ev.reply("Attached Project \"" + Main.projects.get(projectID).proj.name + "\" to this channel!" + (Main.projects.get(projectID).proj.latestFilesIndexes.length == 0 ? "\n*This project does not have any files yet. If there are files already, this project's game or category is not (yet) supported*" : "")).queue();

            } else {
                final CFMod mod = CFCoreAPI.getModFromID(projectID);
                if (mod != null) {
                    CurseforgeProject prj = new CurseforgeProject(mod, Main.ifa.getOrCreateCFChannel(ev.getChannel().getIdLong()));
                    Main.ifa.addChannelToProject(projectID, ev.getChannel().getIdLong());
                    final CFFileIndex[] curseFiles = mod.latestFilesIndexes;
                    if (curseFiles.length != 0) Main.ifa.updateCache(projectID, curseFiles[0].fileId);
                    Main.projects.put(projectID, prj);
                    ev.reply("Attached Project \"" + prj.proj.name + "\" to this channel!" + (curseFiles.length == 0 ? "\n*This project does not have any files yet. If there are files already, this project's game or category is not (yet) supported*" : "")).queue();
                } else {
                    ev.reply("Project does not exist!").queue();
                }
            }
        }
    }

}
