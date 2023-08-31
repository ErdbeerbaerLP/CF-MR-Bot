package de.erdbeerbaerlp.curseforgeBot;


import de.erdbeerbaerlp.cfcore.CFCoreAPI;
import de.erdbeerbaerlp.cfcore.json.CFMod;
import de.erdbeerbaerlp.curseforgeBot.commands.*;
import de.erdbeerbaerlp.curseforgeBot.projects.CurseforgeProject;
import de.erdbeerbaerlp.curseforgeBot.projects.ModrinthProject;
import de.erdbeerbaerlp.curseforgeBot.storage.Config;
import de.erdbeerbaerlp.curseforgeBot.storage.DatabaseInterface;
import de.erdbeerbaerlp.curseforgeBot.storage.json.DCChannel;
import masecla.modrinth4j.client.agent.UserAgent;
import masecla.modrinth4j.main.ModrinthAPI;
import masecla.modrinth4j.model.project.Project;
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
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class Main {
    public static final HashMap<Long, CurseforgeProject> cfProjects = new HashMap<>();
    public static final HashMap<String, ModrinthProject> mrProjects = new HashMap<>();
    private static final DCCommand[] commandList = new DCCommand[]{new AddProjectCommand(), new DelProjectCommand(), new FetchLatestFileCommand(), new SupportCommand()};
    private static final HashMap<Long, DCCommand> registeredCMDs = new HashMap<>();
    public static JDA jda;
    public static DatabaseInterface ifa;
    public static ModrinthAPI mrAPI;


    public static void main(String[] args) throws SQLException, ClassNotFoundException, InterruptedException, LoginException {
        Config.instance().loadConfig();
        CFCoreAPI.setApiKey(Config.instance().general.apiKey);
        mrAPI = ModrinthAPI.rateLimited(UserAgent.builder().authorUsername("ErdbeerbaerLP").contact("https://discord.gg/PGPWdRBQms").build(), Config.instance().general.modrinthKey);
        ifa = new DatabaseInterface();
        jda = JDABuilder.createLight(Config.instance().general.botToken).setActivity(Activity.playing("in Beta")).addEventListeners((EventListener) event -> {
            if (event instanceof final SlashCommandInteractionEvent ev) {
                if (registeredCMDs.containsKey(ev.getCommandIdLong())) {
                    final DCCommand dcCommand = registeredCMDs.get(ev.getCommandIdLong());
                    System.out.println("Running command " + ((CommandData) dcCommand).getName());
                    dcCommand.execute(ev);
                }
            }
        }).build().awaitReady();

        //Load everything from database
        final ArrayList<DCChannel> allChannels = ifa.getAllChannels();


        for (final DCChannel c : allChannels) {
            System.out.println(c.channelID + " Cid");
            for (final Long projID : c.data.projects) {
                System.out.println(projID + " Pid");
                if (cfProjects.containsKey(projID)) {
                    cfProjects.get(projID).addChannel(c);
                } else {
                    final CFMod project = CFCoreAPI.getModFromID(projID);
                    cfProjects.put(projID, new CurseforgeProject(project, c));
                }
                Thread.sleep(100); //Not totally spam the api, causing slowdown of the startup process
            }
            for (final String projID : c.data.mrProjects) {
                System.out.println(projID + " Pid");
                if (mrProjects.containsKey(projID)) {
                    mrProjects.get(projID).addChannel(c);
                } else {
                    final Project project;
                    try {
                        project = mrAPI.projects().get(projID).get();
                        mrProjects.put(projID, new ModrinthProject(project, c));
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    }
                }
                Thread.sleep(100); //Not totally spam the api, causing slowdown of the startup process
            }
        }

        //Add commands
        registerCommands();

        Timer ti = new Timer();
        TimerTask curseforge = new TimerTask() {
            @Override
            public void run() {
                for (final CurseforgeProject proj : cfProjects.values()) {
                    System.out.println(proj.proj.name);
                    try {
                        proj.run();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ignored) {
                    }
                }
            }
        };
        TimerTask modrinth = new TimerTask() {
            @Override
            public void run() {
                for (final ModrinthProject proj : mrProjects.values()) {
                    System.out.println(proj.proj.getTitle());
                    try {
                        proj.run();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ignored) {
                    }
                }
            }
        };

        ti.scheduleAtFixedRate(curseforge, 500, TimeUnit.MINUTES.toMillis(10));
        ti.scheduleAtFixedRate(modrinth, 500, TimeUnit.MINUTES.toMillis(5));
    }

    private static void registerCommands() {
        final List<Command> cmds = jda.retrieveCommands().complete();
        boolean regenCommands = false;
        if (commandList.length == cmds.size())
            for (final DCCommand cmd : commandList) {
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
            for (final DCCommand cmd : commandList) {
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
            for (final DCCommand cfcmd : commandList) {
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
