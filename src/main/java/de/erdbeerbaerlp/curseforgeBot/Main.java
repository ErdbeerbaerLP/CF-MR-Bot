package de.erdbeerbaerlp.curseforgeBot;


import com.therandomlabs.curseapi.CurseException;
import com.therandomlabs.curseapi.project.CurseProject;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;

import java.util.HashMap;
import java.util.Map;

public class Main {
    static final boolean cacheGenerated = Cfg.cacheFile.exists();
    static final Map<String, Integer> cache = new HashMap<>();
    public static Cfg cfg = new Cfg();
    static JDA jda;

    public static void main(String[] args) {
        System.out.println(cfg.BOT_TOKEN);
        try {
            jda = new JDABuilder()
                    .setToken(cfg.BOT_TOKEN)
                    .build().awaitReady();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (!cacheGenerated) {
            System.out.println("Generating cache");
            for (String p : cfg.URLS) {
                try {
                    final CurseProject pr = CurseProject.fromURL(p.split(";;")[0]);
                    cache.put(pr.title(), pr.latestFile().id());
                    cfg.saveCache();
                } catch (CurseException e) {
                    e.printStackTrace();
                }
            }
        } else cfg.loadCache();
        for (String p : cfg.URLS) {
            try {
                new CurseforgeUpdateThread(p).start();
            } catch (CurseException e) {
                e.printStackTrace();
            }
        }
    }
}
