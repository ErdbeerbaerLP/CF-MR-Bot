package de.erdbeerbaerlp.curseforgeBot;

import com.therandomlabs.curseapi.CurseAPI;
import com.therandomlabs.curseapi.CurseException;
import com.therandomlabs.curseapi.project.CurseProject;
import net.dv8tion.jda.api.entities.TextChannel;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class CurseforgeUpdateThread extends Thread {
    private final CurseProject proj;
    private String channelID;

    CurseforgeUpdateThread(String id) throws CurseException {
        if (id.contains(";;")) {
            String[] ids = id.split(";;");
            channelID = ids[1];
        } else {
            //Just throw the message so it won't be used
            throw new CurseException("No channel ID specified!");
        }
        final Optional<CurseProject> project = CurseAPI.project(Integer.parseInt(id.split(";;")[0]));
        if (!project.isPresent()) throw new CurseException("Project not found");
        proj = project.get();
        setName("Curseforge Update Detector for " + proj.name() + " (ID: " + proj.id() + ")");
        Main.threads.put(proj.id() + "", this);
    }

    @Override
    public void run() {
        while (true) {
            try {
                System.out.println("<" + proj.name() + "> Cached: " + Main.cache.get(proj.id()) + " Newest:" + proj.files().first().id());
                if (Main.cfg.isNewFile(proj.id(), proj.files().first().id())) {
                    final TextChannel channel = Main.jda.getTextChannelById(channelID);
                    EmbedMessage.sendUpdateNotification(channel, proj);
                    Main.cache.put(proj.id(), proj.files().first().id());
                    Main.cacheChanged = true;
                }
                sleep(TimeUnit.SECONDS.toMillis(20));
                proj.clearFilesCache();
            } catch (CurseException ignored) {
            } catch (InterruptedException e) {
                return;
            }
        }
    }
}
