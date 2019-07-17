package de.erdbeerbaerlp.curseforgeBot;


import com.therandomlabs.curseapi.CurseAPI;
import com.therandomlabs.curseapi.CurseException;
import com.therandomlabs.curseapi.project.CurseProject;
import com.therandomlabs.utils.logging.Logger;
import com.therandomlabs.utils.logging.Logging;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import org.apache.commons.cli.*;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.PagedSearchIterable;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class Main {
    public static final Logger logger = Logging.getLogger();
    public static final Cfg cfg = new Cfg();
    static final Map<String, Integer> cache = new HashMap<>();
    static final int CFG_VERSION = 2;
    static final GitHub github;
    static boolean cacheGenerated = Cfg.cacheFile.exists();
    static boolean debug = false;
    static boolean useGithub = false;
    static boolean cacheChanged;
    static GHRepository repo = null;
    static JDA jda;

    static {
        GitHub out;
        try {
            out = GitHub.connectUsingOAuth(cfg.githubToken);
        } catch (IOException e) {
            out = null;
            e.printStackTrace();
        }
        github = out;
    }

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

            //Disable log spam if not debugging
            if (!debug) Logging.getLogger().disableDebug();

            logger.debug("Cought command line args!");
            useGithub = line.hasOption("github");
            logger.debug("Using github: " + useGithub);
            if (line.hasOption("token") && cfg.BOT_TOKEN.equals("args")) cfg.BOT_TOKEN = line.getOptionValue("token");
            if (useGithub && line.getOptionValue("github") != null) {
                cfg.githubToken = line.getOptionValue("github");
                logger.debug("Took github token from command line");
            }
            if (useGithub) {
                logger.debug("Attempting to use repository \"" + cfg.githubRepo + "\"");
                try {
                    final PagedSearchIterable<GHRepository> tmp = github.searchRepositories().user(github.getMyself().getLogin()).list();
                    logger.debug(tmp);
                    GHRepository rep = null;
                    logger.debug("Searching existing repos...");
                    for (final GHRepository r : tmp) {
                        logger.debug("Found repo " + r.getName());
                        if (r.getName().equals(cfg.githubRepo)) {
                            rep = r;
                            break;
                        }
                    }
                    if (rep == null) {
                        logger.info("Generating new private repository...");
                        repo = github.createRepository(cfg.githubRepo).private_(true).description("Repository used by the Curseforge Bot to store cache externally").create();
                    } else repo = rep;
                    cacheGenerated = cfg.doesGHCacheExist();
                } catch (IOException e) {
                    logger.fatalError("Failed to connect to github!\n" + e.getMessage());
                }

            }
            if (cfg.BOT_TOKEN.equals("InsertHere") || cfg.DefaultChannel.equals("000000000")) {
                logger.fatalError("You didnt modify the config! This bot wont work without Channel ID or Token!");
                System.exit(1);
            }
            logger.debug("Bot-Token is " + cfg.BOT_TOKEN);
            CurseAPI.setMaximumThreads(6);
            try {
                jda = new JDABuilder()
                        .setToken(cfg.BOT_TOKEN)
                        .build().awaitReady();
            } catch (Exception e) {
                logger.fatalError("<JDA> " + e.getMessage());
                System.exit(1);
            }
            if (!cacheGenerated) {
                logger.info("Generating cache...");
                for (String p : cfg.IDs) {
                    try {
                        final CurseProject pr = CurseProject.fromID(p.split(";;")[0]);
                        cache.put(pr.title(), pr.latestFile().id());

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
                logger.info("Done!");
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
                    logger.debug("MAIN Tick");
                    if (cacheChanged) {
                        logger.info("Saving changed caches...");
                        cacheChanged = false;
                        cfg.saveCache();
                    }
                } catch (InterruptedException e) {
                    logger.info("Main Thread interrupted!");
                }
            }
        } catch (ParseException exp) {
            logger.error(exp.getMessage());
        }
    }
}
