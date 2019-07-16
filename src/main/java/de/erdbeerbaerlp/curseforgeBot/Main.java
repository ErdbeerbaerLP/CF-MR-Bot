package de.erdbeerbaerlp.curseforgeBot;


import com.therandomlabs.curseapi.CurseException;
import com.therandomlabs.curseapi.project.CurseProject;
import com.therandomlabs.utils.collection.ArrayUtils;
import com.therandomlabs.utils.logging.Logger;
import com.therandomlabs.utils.logging.Logging;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;

import java.util.HashMap;
import java.util.Map;

public class Main {
    static final boolean cacheGenerated = Cfg.cacheFile.exists();
    static final Map<String, Integer> cache = new HashMap<>();
    public static Cfg cfg = new Cfg();
    static JDA jda;
    public static final Logger logger = Logging.getLogger();
    static final int CFG_VERSION = 1;
    public static boolean debug = false;

    public static void main(String[] args) {
        debug = ArrayUtils.contains(args, "debug");

        //Disable log spam if not debugging
        if (!debug) Logging.getLogger().disableDebug();
        if (cfg.BOT_TOKEN.equals("InsertHere") || cfg.DefaultChannel.equals("000000000")) {
            System.err.println("You didnt modify the config! This bot wont work without Channel ID or Token!");
            System.exit(1);
        }

        try {
            jda = new JDABuilder()
                    .setToken(cfg.BOT_TOKEN)
                    .build().awaitReady();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (!cacheGenerated) {
            System.out.println("Generating cache");
            for (String p : cfg.IDs) {
                try {
                    final CurseProject pr = CurseProject.fromID(p.split(";;")[0]);
                    cache.put(pr.title(), pr.latestFile().id());
                    cfg.saveCache();
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
    }
}
