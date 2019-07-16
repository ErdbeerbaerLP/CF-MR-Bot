package de.erdbeerbaerlp.curseforgeBot;

import com.github.rjeschke.txtmark.Processor;
import com.therandomlabs.curseapi.CurseException;
import com.therandomlabs.curseapi.project.CurseProject;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.util.concurrent.TimeUnit;

public class CurseforgeUpdateThread extends Thread {
    private final CurseProject proj;
    private final String channelID;

    CurseforgeUpdateThread(String URL) throws CurseException {
        if (URL.contains(";;")) {
            channelID = URL.split(";;")[1];
        } else channelID = Main.cfg.DefaultChannel;
        proj = CurseProject.fromURL(URL.split(";;")[0]);
        setName("Curseforge Update Detector for " + proj.title());
    }

    private String formatChangelog(String s) {
        final String string = Processor.process(s).replace("<br>", "\n").replaceAll("(?s)<[^>]*>(<[^>]*>)*", "");
        return string.replaceAll("https.*?\\s", "");
    }

    @Override
    public void run() {
        while (true) {
            try {
                System.out.println("[" + proj.title() + "]" + proj.latestFile().id());
                if (Main.cfg.isNewFile(proj.title(), proj.latestFile().id())) {
                    MessageEmbed b = new EmbedBuilder()
                            .setThumbnail(proj.thumbnailURLString())
                            .setTitle(proj.title())
                            .setDescription("New File detected for project " + proj.title() + "\n\nFile Name: `" + proj.latestFile().name() + "`\nChangelog:\n```\n" + formatChangelog(proj.latestFile().changelog()) + "\n```")
                            .setFooter("Upload time: ")
                            .setTimestamp(proj.latestFile().uploadTime())
                            .build();
                    try {
                        //noinspection ConstantConditions
                        Main.jda.getTextChannelById(channelID).sendMessage(b).complete();
                    } catch (NullPointerException ignored) {
                    }
                    Main.cache.put(proj.title(), proj.latestFile().id());
                    Main.cfg.saveCache();
                }
                sleep(TimeUnit.SECONDS.toMillis(10));
                proj.reloadFiles();
            } catch (InterruptedException | CurseException ignored) {
            }
        }
    }
}
