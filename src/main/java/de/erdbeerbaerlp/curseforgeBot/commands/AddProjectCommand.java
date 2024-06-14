package de.erdbeerbaerlp.curseforgeBot.commands;

import de.erdbeerbaerlp.cfcore.CFCoreAPI;
import de.erdbeerbaerlp.cfcore.json.CFFileIndex;
import de.erdbeerbaerlp.cfcore.json.CFMod;
import de.erdbeerbaerlp.curseforgeBot.Main;
import de.erdbeerbaerlp.curseforgeBot.projects.CurseforgeProject;
import de.erdbeerbaerlp.curseforgeBot.projects.ModrinthProject;
import masecla.modrinth4j.model.project.Project;
import masecla.modrinth4j.model.version.ProjectVersion;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.internal.interactions.CommandDataImpl;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class AddProjectCommand extends CommandDataImpl implements DCCommand {
    public AddProjectCommand() {
        super("add-project", "Adds an curseforge project to this channel");
        final SubcommandData curseforge = new SubcommandData("curseforge", "Adds an curseforge project to this channel");
        final SubcommandData modrinth = new SubcommandData("modrinth", "Adds an Modrinth Project to this channel");
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
                        Main.cfProjects.get(projectID).addChannel(Main.ifa.addChannelToCFProject(projectID, ev.getChannel().getIdLong()));
                        ev.reply("Attached Project \"" + Main.cfProjects.get(projectID).proj.name + "\" to this channel!" + (Main.cfProjects.get(projectID).proj.latestFilesIndexes.length == 0 ? "\n*This project does not have any files yet. If there are files already, this project's game or category is not (yet) supported*" : "")).queue();

                    } else {
                        final CFMod mod = CFCoreAPI.getModFromID(projectID);
                        if (mod != null) {
                            CurseforgeProject prj = new CurseforgeProject(mod, Main.ifa.getOrCreateDCChannel(ev.getChannel().getIdLong()));
                            Main.ifa.addChannelToCFProject(projectID, ev.getChannel().getIdLong());
                            final CFFileIndex[] curseFiles = mod.latestFilesIndexes;
                            if (curseFiles.length != 0) Main.ifa.updateCFCache(projectID, curseFiles[0].fileId);
                            Main.cfProjects.put(projectID, prj);
                            ev.reply("Attached Project \"" + prj.proj.name + "\" to this channel!" + (curseFiles.length == 0 ? "\n*This project does not have any files yet. If there are files already, this project's game or category is not (yet) supported*" : "")).queue();
                        } else {
                            ev.reply("Project does not exist!").queue();
                        }
                    }
                }
                break;
            case "modrinth":
                try {
                    final OptionMapping projID = ev.getOption("project-id");
                    if (projID == null) ev.reply("Project ID == null ?!?").queue();
                    else {
                        final String projectID = projID.getAsString();
                        if (Main.mrProjects.containsKey(projectID)) {
                            final ModrinthProject modrinthProject = Main.mrProjects.get(projectID);
                            modrinthProject.addChannel(Main.ifa.addChannelToMRProject(projectID, ev.getChannel().getIdLong()));
                            ev.reply("Attached Project \"" + modrinthProject.proj.getTitle() + "\" to this channel!" + (Main.mrProjects.get(projectID).proj.getVersions().isEmpty() ? "\n*This project does not have any files yet. If there are files already, this project's game or category is not (yet) supported*" : "")).queue();

                        } else {
                            final Project mod = Main.mrAPI.projects().get(projectID).get();
                            if (mod != null) {
                                final ModrinthProject prj = new ModrinthProject(mod, Main.ifa.getOrCreateDCChannel(ev.getChannel().getIdLong()), projectID);
                                Main.ifa.addChannelToMRProject(projectID, ev.getChannel().getIdLong());
                                final List<ProjectVersion> versions = Main.mrAPI.versions().getVersion(mod.getVersions()).get();
                                Collections.reverse(versions);
                                if (!versions.isEmpty())
                                    Main.ifa.updateMRCache(projectID, versions.get(0).getDatePublished().toEpochMilli());
                                Main.mrProjects.put(projectID, prj);
                                ev.reply("Attached Project \"" + prj.proj.getTitle() + "\" to this channel!" + (versions.isEmpty() ? "\n*This project does not have any files yet. If there are files already, this might be a bug*" : "")).queue();
                            } else {
                                ev.reply("Project does not exist!").queue();
                            }
                        }
                    }
                } catch (ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                }
                break;
        }
    }

}
