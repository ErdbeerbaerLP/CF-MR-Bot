package de.erdbeerbaerlp.curseforgeBot;


import com.therandomlabs.curseapi.CurseAPI;
import com.therandomlabs.curseapi.CurseException;
import com.therandomlabs.curseapi.project.CurseProject;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import org.apache.commons.cli.*;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.PagedSearchIterable;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class Main {
    public static final Cfg cfg = new Cfg();
    static final Map<String, Integer> cache = new HashMap<>();
    static final int CFG_VERSION = 2;
    static GitHub github;
    static boolean cacheGenerated = Cfg.cacheFile.exists();
    static boolean debug = false;
    static boolean useGithub = false;
    static boolean cacheChanged;
    static GHRepository repo = null;
    static JDA jda;

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
            if (cfg.BOT_TOKEN.equals("InsertHere") || cfg.DefaultChannel.equals("000000000")) {
                System.err.println("You didnt modify the config! This bot wont work without Channel ID or Token!");
                System.exit(1);
            }
            System.out.println("Bot-Token is " + cfg.BOT_TOKEN);
            try {
                jda = new JDABuilder()
                        .setToken(cfg.BOT_TOKEN)
                        .build().awaitReady();
            } catch (Exception e) {
                System.err.println("<JDA> " + e.getMessage());
                System.exit(1);
            }
            if (!cacheGenerated) {
                System.out.println("Generating cache...");
                for (String p : cfg.IDs) {
                    try {
                        final Optional<CurseProject> project = CurseAPI.project(Integer.parseInt(p.split(";;")[0]));
                        if (!project.isPresent()) throw new CurseException("Project not found");
                        final CurseProject pr = project.get();
                        cache.put(pr.name(), pr.files().last().id());
                    } catch (CurseException e) {
                        e.printStackTrace();
                    }
                }
            /*for (String p : cfg.USERs) {
                try {
                    final CurseProject pr = CurseProject.fromID(p.split(";;")[0]);
                    cache.put(pr.title(), pr.latestFile().id());
                    cfg.saveCache();
                } catch (CurseException e) {
                    e.printStackTrace();
                }
            }*/
                cfg.saveCache();
                System.out.println("Done!");
            } else cfg.loadCache();
            for (String p : cfg.IDs) {
                try {
                    new CurseforgeUpdateThread(p).start();
                } catch (CurseException e) {
                    e.printStackTrace();
                }
            }
        /*for (String p : cfg.USERs) {
            try {
                // No way to do this *now*
                new CurseforgeUpdateThread(p).start();
            } catch (CurseException e) {
                e.printStackTrace();
            }
        }*/
            while (true) {
                try {
                    Thread.sleep(TimeUnit.SECONDS.toMillis(30));
                    System.out.println("MAIN Tick");
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
}
