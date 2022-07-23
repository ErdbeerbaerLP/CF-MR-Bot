package de.erdbeerbaerlp.curseforgeBot.commands;

import de.erdbeerbaerlp.cfcore.CFCoreAPI;
import de.erdbeerbaerlp.cfcore.json.CFFileIndex;
import de.erdbeerbaerlp.cfcore.json.CFMod;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.internal.interactions.CommandDataImpl;

public class FetchLatestFileCommand extends CommandDataImpl implements CFCommand {
    public FetchLatestFileCommand() {
        super("dev-fetchproject", "Development command, fetches an curseforge project");
        addOption(OptionType.INTEGER, "project-id", "ID of the Curseforge Project", true);
    }

    @Override
    public void execute(SlashCommandInteractionEvent ev) {
        if (ev.getUser().getIdLong() != 135802962013454336L) {
            ev.deferReply(true).setContent("You don't look like the developer, do you?\n(This command can only be used by the developer)").queue();
            return;
        }
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
    }
}
