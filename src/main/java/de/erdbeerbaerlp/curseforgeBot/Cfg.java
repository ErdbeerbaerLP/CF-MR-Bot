package de.erdbeerbaerlp.curseforgeBot;

import com.therandomlabs.curseapi.CurseAPI;
import com.therandomlabs.curseapi.CurseException;
import com.therandomlabs.curseapi.project.CurseProject;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.kohsuke.github.GHContent;

import javax.annotation.Nullable;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

public class Cfg {
    public static final File configFile = new File("bot.conf");
    public static final File cacheFile = new File("Caches_DONT-DELETE");
    private final Config conf;
    public String BOT_TOKEN;
    public List<String> IDs;
    public String DefaultChannel;
    public String githubToken;
    public String githubRepo;
    public String changlogDiscordFormat;
    public String messageDescription;
    public String updateFileLink;
    public String mentionRole;
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
                System.out.println("Resetting config, creating backup...");
                final Path backupPath = Paths.get(configFile.getAbsolutePath() + ".backup.txt");
                if (backupPath.toFile().exists()) {
                    System.out.println("REPLACING OLD BACKUP!!!!");
                    backupPath.toFile().delete();
                }
                Files.move(configFile.toPath(), backupPath);
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
        githubToken = conf.getString("githubToken");
        githubRepo = conf.getString("githubRepo");
        changlogDiscordFormat = conf.getString("changelogDiscordFormat");
        messageDescription = conf.getString("messageDescription");
        updateFileLink = conf.getString("updateFileLink");
        mentionRole = conf.getString("mentionRole");
        //USERs = conf.getStringList("users");
    }

    void saveCache() {
        System.out.println("Attempting to save cache...");
        try {
            if (!cacheFile.exists()) //noinspection ResultOfMethodCallIgnored
                cacheFile.createNewFile();
            final PrintWriter out = new PrintWriter(cacheFile);
            Main.cache.forEach((a, b) -> out.println(a + ";;" + b));
            out.close();
        } catch (IOException e) {
            System.err.println("Failed to save cache file!+\n" + e.getMessage());
        }
        try {
            if (Main.useGithub && Main.repo != null) {
                System.out.println("Pushing to github repo...");
                if (doesGHCacheExist())
                    Main.repo.createContent()
                            .branch("master")
                            .message("Update caches")
                            .path("caches.txt")
                            .sha(getGHCache().getSha())
                            .content(Files.readAllBytes(cacheFile.toPath()))
                            .commit();
                else
                    Main.repo.createContent()
                            .branch("master")
                            .message("Update caches")
                            .path("caches.txt")
                            .content(Files.readAllBytes(cacheFile.toPath()))
                            .commit();
                cacheFile.delete();
                Main.github.refreshCache();
            }
        } catch (IOException e) {
            System.err.println("Error pushing to github!\n" + e.getMessage());
        }
    }

    boolean doesGHCacheExist() throws IOException {
        return Main.github.searchContent().filename("caches").repo(Main.github.getMyself().getLogin() + "/" + Main.repo.getName()).user(Main.github.getMyself().getLogin()).list().getTotalCount() > 0;
    }

    @Nullable
    GHContent getGHCache() throws IOException {
        if (!doesGHCacheExist()) return null;
        return Main.github.searchContent().filename("caches").extension("txt").repo(Main.github.getMyself().getLogin() + "/" + Main.repo.getName()).user(Main.github.getMyself().getLogin()).list().toList().get(0);
    }

    void loadCache() {
        if (Main.useGithub && Main.repo != null)
            try {
                BufferedReader s = new BufferedReader(new InputStreamReader(Main.repo.getFileContent("caches.txt").read()));
                s.lines().forEach(this::putToCache);
                s.close();
            } catch (IOException e) {
                System.err.println("Could not load caches!\n" + e.getMessage());
            }
        else
            try {
                BufferedReader r = new BufferedReader(new FileReader(cacheFile));
                r.lines().forEach(this::putToCache);
                r.close();
            } catch (IOException e) {
                System.err.println("Could not load caches!\n" + e.getMessage());
            }

    }

    private void putToCache(String s) {
        final String[] ca = s.split(";;");
        if (ca.length != 2) {
            System.err.println("Could not load cache line " + s);
            return;
        }
        int id = -1;
        try {
            id = Integer.parseInt(ca[0]);
        } catch (NumberFormatException e) {
            System.out.println("Converting project name '" + ca[0] + "' from cache to ID...");
            try {
                for (String p : Main.cfg.IDs) {
                    final Optional<CurseProject> oProj = CurseAPI.project(Integer.parseInt(p.split(";;")[0]));
                    if (oProj.isPresent()) {
                        final CurseProject proj = oProj.get();
                        if (proj.name().equals(ca[0])) {
                            id = proj.id();
                            Main.cacheChanged = true;
                        }
                    }
                }
                Thread.sleep(2000);
            } catch (CurseException | InterruptedException ignored) {
            }
            if (id == -1) System.out.println("Cannot find project");
        }
        Main.cache.put(id, Integer.parseInt(ca[1]));

    }

    boolean isNewFile(int projID, int fileID) {
        if (!Main.cache.containsKey(projID)) return true;
        return Main.cache.get(projID) < fileID;
    }
}
