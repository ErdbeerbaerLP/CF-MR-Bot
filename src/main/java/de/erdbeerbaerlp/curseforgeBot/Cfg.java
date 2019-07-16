package de.erdbeerbaerlp.curseforgeBot;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Cfg {
    public static final File configFile = new File("bot.conf");
    public static final File cacheFile = new File("Caches_DONT-DELETE");
    private final Config conf;
    public String BOT_TOKEN;
    public List<String> IDs;
    public String DefaultChannel;
    //public List<String> USERs;

    Cfg() {
            if (!configFile.exists()) {
                //noinspection finally
                try {
                    InputStream link = (getClass().getResourceAsStream("/" + configFile.getName()));
                    Files.copy(link, configFile.getAbsoluteFile().toPath());
                    link.close();
                    System.err.println("Please set the token and the Channel ID in the new config file");
                } catch (IOException e) {
                    System.err.println("Could not extract default config file");
                    e.printStackTrace();
                } finally {
                    System.exit(0);
                }
            }

        conf = ConfigFactory.parseFile(configFile);
        if (!conf.hasPath("ver") || conf.getInt("ver") != Main.CFG_VERSION) {
            //noinspection finally
            try {
                System.err.println("Resetting config, creaing backup...");
                Files.move(configFile.toPath(), Paths.get(configFile.getAbsolutePath() + ".backup.txt"));
                InputStream link = (getClass().getResourceAsStream("/" + configFile.getName()));
                Files.copy(link, configFile.getAbsoluteFile().toPath());
                link.close();
                System.err.println("Reset completed! Please reconfigurate.");
            } catch (IOException e) {
                System.err.println("Could not reset config file!");
                e.printStackTrace();
            } finally {
                System.exit(0);
            }
        }
        loadConfig();
    }

    public void loadConfig() {
        BOT_TOKEN = conf.getString("BotToken");
        IDs = conf.getStringList("ids");
        DefaultChannel = conf.getString("DefaultChannelID");
        //USERs = conf.getStringList("users");
    }

    void saveCache() {
        try {
            if (!cacheFile.exists()) //noinspection ResultOfMethodCallIgnored
                cacheFile.createNewFile();
            PrintWriter out = new PrintWriter(cacheFile);
            Main.cache.forEach((a, b) -> out.println(a + ";;" + b));
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void loadCache() {
        try {
            BufferedReader r = new BufferedReader(new FileReader(cacheFile));
            r.lines().forEach((s -> {
                final String[] ca = s.split(";;");
                if (ca.length != 2) {
                    System.err.println("Could not load cache line " + s);
                    return;
                }
                Main.cache.put(ca[0], Integer.parseInt(ca[1]));
            }));
            r.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    boolean isNewFile(String name, int id) {
        if (!Main.cache.containsKey(name)) return true;
        return Main.cache.get(name) < id;
    }
}
