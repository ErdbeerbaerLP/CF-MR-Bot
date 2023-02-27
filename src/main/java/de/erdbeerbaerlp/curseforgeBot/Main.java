package de.erdbeerbaerlp.curseforgeBot;


import de.erdbeerbaerlp.cfcore.CFCoreAPI;
import de.erdbeerbaerlp.cfcore.json.CFMod;
import de.erdbeerbaerlp.curseforgeBot.commands.*;
import de.erdbeerbaerlp.curseforgeBot.storage.Config;
import de.erdbeerbaerlp.curseforgeBot.storage.DatabaseInterface;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import net.dv8tion.jda.internal.interactions.CommandDataImpl;

import javax.security.auth.login.LoginException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Main {
    static JDA jda;

    public static final HashMap<Long, CurseforgeProject> projects = new HashMap<>();
    private static final CFCommand[] commandList = new CFCommand[]{new AddProjectCommand(), new DelProjectCommand(), new FetchLatestFileCommand(), new SupportCommand()};
    private static final HashMap<Long, CFCommand> registeredCMDs = new HashMap<>();
    public static DatabaseInterface ifa;


    public static void main(String[] args) throws SQLException, ClassNotFoundException, InterruptedException, LoginException {
        Config.instance().loadConfig();
        CFCoreAPI.setApiKey(Config.instance().general.apiKey);
        ifa = new DatabaseInterface();
        jda = JDABuilder.createLight(Config.instance().general.botToken).setActivity(Activity.playing("in Beta")).addEventListeners((EventListener) event -> {
            if (event instanceof final SlashCommandInteractionEvent ev) {
                if (registeredCMDs.containsKey(ev.getCommandIdLong())) {
                    final CFCommand cfCommand = registeredCMDs.get(ev.getCommandIdLong());
                    System.out.println("Running command " + ((CommandData) cfCommand).getName());
                    cfCommand.execute(ev);
                }
            }
        }).build().awaitReady();

        //Load everything from database
        final ArrayList<CurseforgeProject.CFChannel> allChannels = ifa.getAllChannels();
        for (final CurseforgeProject.CFChannel c : allChannels) {
            System.out.println(c.channelID + " Cid");
            for (final Long projID : c.data.projects) {
                System.out.println(projID + " Pid");
                if (projects.containsKey(projID)) {
                    projects.get(projID).addChannel(c);
                } else {
                    final CFMod project = CFCoreAPI.getModFromID(projID);
                    projects.put(projID, new CurseforgeProject(project, c));
                }
                Thread.sleep(100); //Not totally spam the api, causing slowdown of the startup process
            }
        }

        //Add commands
        registerCommands();
        while (true) {
            for (final CurseforgeProject proj : projects.values()) {
                System.out.println(proj.proj.name);
                proj.run();
                Thread.sleep(100);
            }
            TimeUnit.MINUTES.sleep(10);

        }
    }

    private static void registerCommands() {
        final List<Command> cmds = jda.retrieveCommands().complete();
        boolean regenCommands = false;
        if (commandList.length == cmds.size())
            for (final CFCommand cmd : commandList) {
                for (final Command c : cmds) {
                    final CommandDataImpl command = (CommandDataImpl) cmd;
                    if (command.getName().equals(c.getName())) {
                        if (!optionsEqual(command.getOptions(), c.getOptions())) {
                            regenCommands = true;
                        }
                    }
                }
            }
        else regenCommands = true;
        if (regenCommands) {
            System.out.println("Regenerating commands...");
            CommandListUpdateAction commandListUpdateAction = jda.updateCommands();
            for (final CFCommand cmd : commandList) {
                commandListUpdateAction = commandListUpdateAction.addCommands((CommandData) cmd);
            }
            commandListUpdateAction.submit().thenAccept(Main::addCmds);
        } else {
            System.out.println("NO need to regen commands");
            addCmds(cmds);
        }

    }

    private static void addCmds(List<Command> cmds) {
        for (final Command cmd : cmds) {
            for (final CFCommand cfcmd : commandList) {
                if (cmd.getName().equals(((CommandData) cfcmd).getName())) {
                    registeredCMDs.put(cmd.getIdLong(), cfcmd);
                    System.out.println("Added command " + cmd.getName() + " with ID " + cmd.getIdLong());
                }
            }
        }
    }

    private static boolean optionsEqual(List<OptionData> data, List<Command.Option> options) {
        if (data.size() != options.size()) return false;
        for (int i = 0; i < data.size(); i++) {
            final OptionData optionData = data.get(i);
            final Command.Option option = options.get(i);
            return option.getName().equals(optionData.getName()) && option.getChoices().equals(optionData.getChoices()) && option.getDescription().equals(optionData.getDescription()) && option.isRequired() == optionData.isRequired() && option.getType().equals(optionData.getType());
        }
        return true;
    }
}
