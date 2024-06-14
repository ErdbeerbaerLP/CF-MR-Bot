package de.erdbeerbaerlp.curseforgeBot.commands;

import de.erdbeerbaerlp.cfcore.CFCoreAPI;
import de.erdbeerbaerlp.cfcore.json.CFFileIndex;
import de.erdbeerbaerlp.cfcore.json.CFMod;
import de.erdbeerbaerlp.curseforgeBot.Main;
import masecla.modrinth4j.endpoints.version.GetProjectVersions;
import masecla.modrinth4j.model.project.Project;
import masecla.modrinth4j.model.version.ProjectVersion;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.internal.interactions.CommandDataImpl;

import java.util.List;

public class FetchLatestFileCommand extends CommandDataImpl implements DCCommand {
    public FetchLatestFileCommand() {
        super("dev-fetchproject", "Development command, fetches an curseforge project");
        final SubcommandData curseforge = new SubcommandData("curseforge", "Development command, fetches an curseforge project");
        final SubcommandData modrinth = new SubcommandData("modrinth", "Development command, fetches an modrinth project");
        curseforge.addOption(OptionType.INTEGER, "project-id", "ID of the Curseforge Project", true);
        modrinth.addOption(OptionType.STRING, "project-id", "ID of the Modrinth Project", true);
        addSubcommands(curseforge, modrinth);
    }

    @Override
    public void execute(SlashCommandInteractionEvent ev) {
        if (ev.getUser().getIdLong() != 135802962013454336L) {
            ev.deferReply(true).setContent("You don't look like the developer, do you?\n(This command can only be used by the developer)").queue();
            return;
        }
        switch (ev.getSubcommandName()) {
            case "curseforge":
                final OptionMapping id = ev.getOption("project-id");
                if (id == null) ev.reply("Project ID == null ?!?").queue();
                else {
                    final int projectID = Integer.parseInt(id.getAsString());
                    try {
                        final CFMod curseProject = CFCoreAPI.getModFromID(projectID);
                        final CFFileIndex[] files = curseProject.latestFilesIndexes;
                        final EmbedBuilder b = new EmbedBuilder();
                        b.setTitle(curseProject.name, curseProject.links.websiteUrl);
                        b.addField("Primary Category ID", curseProject.primaryCategoryId + "", false);
                        for (int i = 0; i < Math.min(5, files.length); i++) {
                            final CFFileIndex curseFile = files[i];
                            b.addField(curseFile.filename, curseFile.fileId + "", true);
                        }
                        ev.replyEmbeds(b.build()).queue();
                    } catch (Exception e) {
                        ev.reply(e.getMessage()).queue();
                    }
                }
                break;
            case "modrinth":

                final OptionMapping projID = ev.getOption("project-id");
                if (projID == null) ev.reply("Project ID == null ?!?").queue();
                else {
                    final String projectID = projID.getAsString();
                    try {
                        final Project project = Main.mrAPI.projects().get(projectID).get();
                        final List<ProjectVersion> versions = Main.mrAPI.versions().getProjectVersions(project.getSlug(), GetProjectVersions.GetProjectVersionsRequest.builder().featured(false).build()).get();
                        final EmbedBuilder b = new EmbedBuilder();
                        b.setTitle(project.getTitle(), "https://modrinth.com/project/" + project.getId());
                        b.addField("Primary Category", project.getCategories().get(0), false);
                        for (int i = 0; i < Math.min(5, versions.size()); i++) {
                            final ProjectVersion file = versions.get(i);
                            b.addField(file.getFiles().get(0).getFilename(), file.getId(), true);
                        }
                        ev.replyEmbeds(b.build()).queue();
                    } catch (Exception e) {
                        ev.reply(e.getMessage()).queue();
                    }
                }
                break;
        }
    }
}
