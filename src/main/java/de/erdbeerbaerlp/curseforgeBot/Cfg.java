package de.erdbeerbaerlp.curseforgeBot;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import java.io.*;
import java.nio.file.Files;
import java.util.List;

public class Cfg {
    public static final File configFile = new File("bot.conf");
    public static final File cacheFile = new File("Caches_DONT-DELETE");
    private final Config conf;
    public String BOT_TOKEN;
    public List<String> URLS;
    public String DefaultChannel;

    Cfg() {
        try {
            if (!configFile.exists()) {
                InputStream link = (getClass().getResourceAsStream("/" + configFile.getName()));
                Files.copy(link, configFile.getAbsoluteFile().toPath());
            }
        } catch (IOException e) {
            System.err.println("Could not extract default config file");
            e.printStackTrace();
        }
        conf = ConfigFactory.parseFile(configFile);
        loadConfig();
    }

    public void loadConfig() {
        BOT_TOKEN = conf.getString("BotToken");
        URLS = conf.getStringList("URLs");
        DefaultChannel = conf.getString("DefaultChannelID");
    }

    void saveCache() {
        try {
            if (!cacheFile.exists()) cacheFile.createNewFile();
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
