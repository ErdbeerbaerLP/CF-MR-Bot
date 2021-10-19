package de.erdbeerbaerlp.curseforgeBot.commands;

import com.therandomlabs.curseapi.CurseAPI;
import com.therandomlabs.curseapi.file.CurseFile;
import com.therandomlabs.curseapi.file.CurseFiles;
import com.therandomlabs.curseapi.project.CurseProject;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

public class FetchLatestFileCommand extends CommandData implements CFCommand {
    public FetchLatestFileCommand() {
        super("dev-fetchproject", "Development command, fetches an curseforge project");
        addOption(OptionType.INTEGER, "project-id", "ID of the Curseforge Project", true);
    }

    @Override
    public void execute(SlashCommandEvent ev) {
        if (ev.getUser().getIdLong() != 817445521589010473L && ev.getUser().getIdLong() != 817445521589010473L) {
            ev.deferReply(true).setContent("You don't look like the developer, do you?\n(This command can only be used by the developer)").queue();
            return;
        }
        final OptionMapping id = ev.getOption("project-id");
        if (id == null) ev.reply("Project ID == null ?!?").queue();
        else {
            final int projectID = Integer.parseInt(id.getAsString());
            try {
                final CurseProject curseProject = CurseAPI.project(projectID).get();
                final CurseFiles<CurseFile> files = curseProject.refreshFiles();
                final EmbedBuilder b = new EmbedBuilder();
                b.setTitle(curseProject.name(), curseProject.url().url().toString());
                b.addField("Category Section", (curseProject.categorySection() == null) ? "No category" : curseProject.categorySection().name(), false);
                b.addField("Primary Category", (curseProject.primaryCategory() == null) ? "No category" : curseProject.primaryCategory().name(), false);
                for (int i = 0; i < Math.min(5, files.size()); i++) {
                    final CurseFile curseFile = files.pollFirst();
                    b.addField(curseFile.displayName(), curseFile.id() + "", true);
                }
                ev.replyEmbeds(b.build()).queue();
            } catch (Exception e) {
                ev.reply(e.getMessage()).queue();
            }
        }
    }
}
