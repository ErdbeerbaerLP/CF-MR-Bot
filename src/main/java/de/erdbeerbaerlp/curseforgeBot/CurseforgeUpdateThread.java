package de.erdbeerbaerlp.curseforgeBot;

import com.github.rjeschke.txtmark.Processor;
import com.therandomlabs.curseapi.CurseAPI;
import com.therandomlabs.curseapi.CurseException;
import com.therandomlabs.curseapi.project.CurseProject;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class CurseforgeUpdateThread extends Thread {
    private final CurseProject proj;
    private final String channelID;

    CurseforgeUpdateThread(String id) throws CurseException {
        if (id.contains(";;")) {
            channelID = id.split(";;")[1];
        } else channelID = Main.cfg.DefaultChannel;
        final Optional<CurseProject> project = CurseAPI.project(Integer.parseInt(id.split(";;")[0]));
        if (!project.isPresent()) throw new CurseException("Project not found");
        proj = project.get();
        setName("Curseforge Update Detector for " + proj.name() + " (ID: " + proj.id() + ")");
    }

    private String formatChangelog(String s) {
        String string = Processor.process(s).replace("<br>", "\n").replaceAll("(?s)<[^>]*>(<[^>]*>)*", "");
        string = string.replaceAll("https.*?\\s", "");
        String out = "";
        int additionalLines = 0;
        for (final String st : string.split("\n")) {
            if ((out + st.trim() + "\n").length() > 1000) {
                additionalLines++;
            } else //noinspection StringConcatenationInLoop
                out = out + st.trim() + "\n";
        }
        return out + (additionalLines > 0 ? ("... And " + additionalLines + " more lines") : "");
    }

    @Override
    public void run() {
        while (true) {
            try {
                System.out.println("<" + proj.name() + "> Cached: " + Main.cache.get(proj.name()) + " Newest:" + proj.files().last().id());
                if (Main.cfg.isNewFile(proj.name(), proj.files().last().id())) {
                    MessageEmbed b = new EmbedBuilder().setThumbnail(proj.avatarThumbnailURL().toString()).setDescription(
                            "New File detected for project " + proj.name() + "\n\n**File Name**: `" + proj.files().last().displayName() + "`\n**Game Version**: " + proj.files().last().gameVersionStrings().stream().findFirst()
                                                                                                                                                                        .get() + "\n" + "**Changelog**:\n```\n" + formatChangelog(
                                    proj.files().last().changelogPlainText()) + "\n```")
                                                       .setFooter("Upload time: ").setTimestamp(proj.files().last().uploadTime()).setAuthor(proj.name(), proj.url().toString())
                                                       .build();
                    try {
                        //noinspection ConstantConditions
                        Main.jda.getTextChannelById(channelID).sendMessage(b).complete();
                    } catch (NullPointerException ignored) {
                    }
                    Main.cache.put(proj.name(), proj.files().last().id());
                    Main.cacheChanged = true;
                }
                sleep(TimeUnit.SECONDS.toMillis(10));
                proj.clearFilesCache();
            } catch (InterruptedException | CurseException ignored) {
            }
        }
    }
}
