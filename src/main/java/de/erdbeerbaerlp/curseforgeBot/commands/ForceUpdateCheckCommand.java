package de.erdbeerbaerlp.curseforgeBot.commands;

import de.erdbeerbaerlp.curseforgeBot.Main;
import de.erdbeerbaerlp.curseforgeBot.projects.CurseforgeProject;
import de.erdbeerbaerlp.curseforgeBot.projects.ModrinthProject;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.internal.interactions.CommandDataImpl;

public class ForceUpdateCheckCommand extends CommandDataImpl implements DCCommand {
    public ForceUpdateCheckCommand() {
        super("force-recheck", "Forces an recheck of an Project ID");
        final SubcommandData curseforge = new SubcommandData("curseforge", "Forces an recheck of an Project ID");
        final SubcommandData modrinth = new SubcommandData("modrinth", "Forces an recheck of an Project ID");
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
                    ev.deferReply().queue();
                    final long projectID = Long.parseLong(id.getAsString());
                    final CurseforgeProject curseforgeProject = Main.cfProjects.get(projectID);
                    Main.ifa.updateCFCache(projectID, 0);
                    curseforgeProject.run();
                }
                break;
            case "modrinth":
                final OptionMapping projID = ev.getOption("project-id");
                if (projID == null) ev.reply("Project ID == null ?!?").queue();
                else {
                    ev.deferReply().queue();
                    final String projectID = projID.getAsString();
                    final ModrinthProject modrinthProject = Main.mrProjects.get(projectID);
                    Main.ifa.updateMRCache(projectID, 0);
                    modrinthProject.run();
                }
                break;
        }
    }
}
