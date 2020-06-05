package de.erdbeerbaerlp.curseforgeBot;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.therandomlabs.curseapi.CurseAPI;
import com.therandomlabs.curseapi.CurseException;
import com.therandomlabs.curseapi.project.CurseProject;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.TextChannel;
import org.apache.commons.cli.*;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.PagedSearchIterable;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class Main {
    public static final Cfg cfg = new Cfg();
    static final Map<Integer, Integer> cache = new HashMap<>();
    static final int CFG_VERSION = 9431;
    static GitHub github;
    static boolean cacheGenerated = Cfg.cacheFile.exists();
    static boolean debug = false;
    static boolean useGithub = false;
    static boolean cacheChanged;
    static GHRepository repo = null;
    static JDA jda;
    static final File projectsFile = new File("projects.json");
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    static ArrayList<String> ids = new ArrayList<>();
    static HashMap<String, CurseforgeUpdateThread> threads = new HashMap<>();

    public static void main(String[] args) {
        final Options o = new Options();
        o.addOption("debug", false, "Enables debug log");
        final Option token = new Option("token", true, "Provides the bot token");
        token.setRequired(cfg.BOT_TOKEN.equals("args"));
        o.addOption(token);
        final Option ghopt = new Option("github", true, "When providing this, it will store the cache on github, login info needs to be specified in the config or as argument");
        ghopt.setOptionalArg(true);
        o.addOption(ghopt);
        CommandLineParser parser = new DefaultParser();
        try {
            // parse the command line arguments
            CommandLine line = parser.parse(o, args);
            debug = line.hasOption("debug");


            System.out.println("Cought command line args!");
            useGithub = line.hasOption("github");
            System.out.println("Using github: " + useGithub);
            if (line.hasOption("token") && cfg.BOT_TOKEN.equals("args")) cfg.BOT_TOKEN = line.getOptionValue("token");
            if (useGithub && line.getOptionValue("github") != null) {
                cfg.githubToken = line.getOptionValue("github");
                System.out.println("Took github token from command line");
            }
            if (useGithub) {
                System.out.println("Logging in to github...");
                try {
                    github = GitHub.connectUsingOAuth(cfg.githubToken);
                } catch (IOException e) {
                    System.err.println("Failed to login to guthub: " + e.getMessage());
                }
                System.out.println("Attempting to use repository \"" + cfg.githubRepo + "\"");
                try {
                    final PagedSearchIterable<GHRepository> tmp = github.searchRepositories().user(github.getMyself().getLogin()).list();
                    System.out.println(tmp);
                    GHRepository rep = null;
                    System.out.println("Searching existing repos...");
                    for (final GHRepository r : tmp) {
                        System.out.println("Found repo " + r.getName());
                        if (r.getName().equals(cfg.githubRepo)) {
                            rep = r;
                            break;
                        }
                    }
                    if (rep == null) {
                        System.out.println("Generating new private repository...");
                        repo = github.createRepository(cfg.githubRepo).private_(true).description("Repository used by the Curseforge Bot to store cache externally").create();
                    } else repo = rep;
                    cacheGenerated = cfg.doesGHCacheExist();
                } catch (IOException e) {
                    System.err.println("Failed to connect to github!\n" + e.getMessage());
                }

            }
            if (cfg.BOT_TOKEN.equals("InsertHere")) {
                System.err.println("You didnt modify the config! This bot wont work without Channel ID or Token!");
                System.exit(1);
            }
            try {
                jda = new JDABuilder()
                        .setToken(cfg.BOT_TOKEN)
                        .build().awaitReady();
            } catch (Exception e) {
                System.err.println("<JDA> " + e.getMessage());
                System.exit(1);
            }
            try {
                loadJSON();
            } catch (Exception e) {
                System.err.println("Failed to load JSON!");
                e.printStackTrace();
                System.exit(1);
            }
            jda.addEventListener(new JdaEventListener());
            if (!cacheGenerated) {
                System.out.println("Generating cache...");
                for (final String p : ids) {
                    try {
                        final Optional<CurseProject> project = CurseAPI.project(Integer.parseInt(p.split(";;")[0]));
                        if (!project.isPresent()) throw new CurseException("Project not found");
                        final CurseProject pr = project.get();
                        cache.put(pr.id(), pr.files().first().id());
                    } catch (CurseException e) {
                        e.printStackTrace();
                    }
                }
                cfg.saveCache();
                System.out.println("Done!");
            } else cfg.loadCache();
            for (final String p : ids) {
                try {
                    new CurseforgeUpdateThread(p).start();
                } catch (CurseException e) {
                    System.out.println("Failed to get project with ID " + p + ". If this happens more often, remove it");
                    e.printStackTrace();
                }
            }
            while (true) {
                try {
                    Thread.sleep(TimeUnit.SECONDS.toMillis(30));
                    if (cacheChanged) {
                        System.out.println("Saving changed caches...");
                        cacheChanged = false;
                        cfg.saveCache();
                    }
                    cfg.loadCache();
                } catch (InterruptedException e) {
                    System.out.println("Main Thread interrupted!");
                }
            }
        } catch (ParseException exp) {
            System.err.println(exp.getMessage());
        }
    }

    private static void loadJSON() throws IOException {
        if (!projectsFile.exists())
            return;
        final FileReader r = new FileReader(projectsFile);
        ids = gson.fromJson(r, ArrayList.class);
        r.close();
    }

    private static void saveJSON() throws IOException {
        if (!projectsFile.exists())
            projectsFile.createNewFile();
        final FileWriter w = new FileWriter(projectsFile);
        gson.toJson(ids, w);
        w.close();
    }

    public static CurseProject addProjectFromCommand(final String projectID, final String channelID) throws CurseException, NumberFormatException, IOException {
        final Optional<CurseProject> project = CurseAPI.project(Integer.parseInt(projectID));
        if (!project.isPresent()) throw new CurseException("Project not found");
        final CurseProject pr = project.get();
        final String combinedIDString = projectID + ";;" + channelID;
        if (ids.contains(combinedIDString)) throw new CurseException("Project already bound to channel");
        final TextChannel channel = jda.getTextChannelById(channelID);
        ids.add(combinedIDString);
        try {
            saveJSON();
        } catch (IOException e) {
            ids.remove(combinedIDString);
            throw e;
        }
        cache.put(pr.id(), pr.files().first().id());
        cacheChanged = true;
        return pr;
    }

    public static boolean removeProjectFromCommand(final String projectID, final String channelID) {
        final String combinedIDString = projectID + ";;" + channelID;
        if (ids.contains(combinedIDString)) {
            ids.remove(combinedIDString);
            cache.remove(Integer.parseInt(projectID));
            try {
                saveJSON();
                threads.get(projectID).interrupt();
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
            cacheChanged = true;
            return true;
        } else
            return false;
    }
}
