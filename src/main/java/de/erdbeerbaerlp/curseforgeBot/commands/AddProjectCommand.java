package de.erdbeerbaerlp.curseforgeBot.commands;

import com.therandomlabs.curseapi.CurseAPI;
import com.therandomlabs.curseapi.CurseException;
import com.therandomlabs.curseapi.file.CurseFile;
import com.therandomlabs.curseapi.file.CurseFiles;
import com.therandomlabs.curseapi.project.CurseProject;
import de.erdbeerbaerlp.curseforgeBot.CurseforgeProject;
import de.erdbeerbaerlp.curseforgeBot.Main;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

import java.util.Optional;

public class AddProjectCommand extends CommandData implements CFCommand {
    public AddProjectCommand() {
        super("add-project", "Adds an curseforge project to this channel");
        addOption(OptionType.INTEGER, "project-id", "ID of the Curseforge Project", true);
    }

    @Override
    public void execute(SlashCommandEvent ev) {
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
                try {
                    ev.reply("Attached Project \"" + Main.projects.get(projectID).proj.name() + "\" to this channel!" + (Main.projects.get(projectID).proj.refreshFiles().isEmpty() ? "\n*This project does not have any files yet. If there are files already, this project's game or category is not (yet) supported*" : "")).queue();
                } catch (CurseException e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    final Optional<CurseProject> project = CurseAPI.project(projectID);
                    if (project.isPresent()) {
                        final CurseProject curseProject = project.get();
                        if (curseProject.categorySection() == null) {
                            ev.reply("This project's category is not (yet) supported, so files cannot be retrieved. This is an issue on curseforge's side, see <https://github.com/TheRandomLabs/CurseAPI/issues/25> if you want more info about this").queue();
                        } else {
                            CurseforgeProject prj = new CurseforgeProject(curseProject, Main.ifa.getOrCreateCFChannel(ev.getChannel().getIdLong()));
                            Main.ifa.addChannelToProject(projectID, ev.getChannel().getIdLong());
                            final CurseFiles<CurseFile> curseFiles = curseProject.refreshFiles();
                            if (!curseFiles.isEmpty()) Main.ifa.updateCache(projectID, curseFiles.first().id());
                            Main.projects.put(projectID, prj);
                            ev.reply("Attached Project \"" + prj.proj.name() + "\" to this channel!" + (curseFiles.isEmpty() ? "\n*This project does not have any files yet. If there are files already, this project's game or category is not (yet) supported*" : "")).queue();
                        }
                    } else {
                        ev.reply("Project does not exist!").queue();
                    }
                } catch (CurseException e) {
                    e.printStackTrace();
                    ev.reply(e.getLocalizedMessage()).queue();
                }
            }
        }
    }
}
