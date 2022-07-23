package de.erdbeerbaerlp.curseforgeBot.storage;

import com.moandjiezana.toml.Toml;
import com.moandjiezana.toml.TomlComment;
import com.moandjiezana.toml.TomlIgnore;
import com.moandjiezana.toml.TomlWriter;

import java.io.File;
import java.io.IOException;

public class Config {

    @TomlIgnore
    public static File configFile = new File("./CurseforgeBot.toml");
    @TomlIgnore
    private static Config INSTANCE;

    static {
        INSTANCE = new Config();
        INSTANCE.loadConfig();
    }

    public General general = new General();
    @TomlComment("MySQL Database settings")
    public Database database = new Database();

    public static Config instance() {
        return INSTANCE;
    }

    public void loadConfig() {
        if (!configFile.exists()) {
            INSTANCE = new Config();
            INSTANCE.saveConfig();
            return;
        }
        INSTANCE = new Toml().read(configFile).to(Config.class);
        INSTANCE.saveConfig(); //Re-write the config so new values get added after updates
    }

    public void saveConfig() {
        try {
            if (!configFile.exists()) {
                if (!configFile.getParentFile().exists()) configFile.getParentFile().mkdirs();
                configFile.createNewFile();
            }
            final TomlWriter w = new TomlWriter.Builder()
                    .indentValuesBy(2)
                    .indentTablesBy(4)
                    .padArrayDelimitersBy(2)
                    .build();
            w.write(this, configFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static class Database {
        @TomlComment("Database IP")
        public String ip = "0.0.0.0";
        @TomlComment("Database Port")
        public int port = 3306;
        @TomlComment("Username")
        public String username = "curseforge";
        @TomlComment("Password")
        public String password = "password";
        @TomlComment("Database Name")
        public String dbName = "CurseforgeBot";
    }

    public static class General {
        @TomlComment({"Insert your discord bot token here", "or use \"args\" here and the argument \"- token TOKEN\" after the jar"})
        public String botToken = "InsertHere";
        @TomlComment("Insert your CFCore API Key here")
        public String apiKey = "InsertHere";
    }

}
