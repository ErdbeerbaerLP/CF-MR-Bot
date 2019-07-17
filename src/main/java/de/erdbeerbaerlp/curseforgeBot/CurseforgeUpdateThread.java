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

    CurseforgeUpdateThread(String id) throws CurseException {
        if (id.contains(";;")) {
            channelID = id.split(";;")[1];
        } else channelID = Main.cfg.DefaultChannel;
        proj = CurseProject.fromID(id.split(";;")[0]);
        setName("Curseforge Update Detector for " + proj.title() + " (ID: " + proj.id() + ")");
    }

    private String formatChangelog(String s) {
        String string = Processor.process(s).replace("<br>", "\n").replaceAll("(?s)<[^>]*>(<[^>]*>)*", "");
        string = string.replaceAll("https.*?\\s", "");
        String out = "";
        int additionalLines = 0;
        for (final String st : string.split("\n")) {
            if ((out + st.trim() + "\n").length() > 1000) {
                additionalLines++;
            } else out = out + st.trim() + "\n";
        }
        return out + (additionalLines > 0 ? ("... And " + additionalLines + " more lines") : "");
    }

    @Override
    public void run() {
        while (true) {
            try {
                Main.logger.debug("<" + proj.title() + "> Cached: " + Main.cache.get(proj.title()) + " Newest:" + proj.latestFile().id());
                if (Main.cfg.isNewFile(proj.title(), proj.latestFile().id())) {
                    MessageEmbed b = new EmbedBuilder()
                            .setThumbnail(proj.thumbnailURLString())
                            .setDescription("New File detected for project " + proj.title() + "\n\n**File Name**: `" + proj.latestFile().name() + "`\n**Game Version(s)**: " + proj.latestFile().gameVersionStrings() + "\n**Changelog**:\n```\n" + formatChangelog(proj.latestFile().changelog()) + "\n```")
                            .setFooter("Upload time: ")
                            .setTimestamp(proj.latestFile().uploadTime())
                            .setAuthor(proj.title(), proj.urlString())
                            .build();
                    try {
                        //noinspection ConstantConditions
                        Main.jda.getTextChannelById(channelID).sendMessage(b).complete();
                    } catch (NullPointerException ignored) {
                    }
                    Main.cache.put(proj.title(), proj.latestFile().id());
                    Main.cacheChanged = true;
                }
                sleep(TimeUnit.SECONDS.toMillis(10));
                proj.reloadFiles();
                proj.reload();
            } catch (InterruptedException | CurseException ignored) {
            }
        }
    }
}
