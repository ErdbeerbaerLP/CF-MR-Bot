package de.erdbeerbaerlp.curseforgeBot;

import com.therandomlabs.curseapi.CurseAPI;
import com.therandomlabs.curseapi.CurseException;
import com.therandomlabs.curseapi.project.CurseProject;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class CurseforgeUpdateThread extends Thread {
    private final CurseProject proj;
    private final String channelID;
    private String roleID = "";

    CurseforgeUpdateThread(String id) throws CurseException {
        if (id.contains(";;")) {
            String[] ids = id.split(";;");
            channelID = ids[1];
            if (ids.length == 3) {
                System.out.println(ids.length);
                roleID = ids[2];
            }
        } else {
            roleID = Main.cfg.mentionRole;
            channelID = Main.cfg.DefaultChannel;
        }
        final Optional<CurseProject> project = CurseAPI.project(Integer.parseInt(id.split(";;")[0]));
        if (!project.isPresent()) throw new CurseException("Project not found");
        proj = project.get();
        setName("Curseforge Update Detector for " + proj.name() + " (ID: " + proj.id() + ")");
        Main.threads.add(this);
    }

    @Override
    public void run() {
        while (true) {
            try {
                if (Main.debug)
                    System.out.println("<" + proj.name() + "> Cached: " + Main.cache.get(proj.id()) + " Newest:" + proj.files().first().id());
                if (Main.cfg.isNewFile(proj.id(), proj.files().first().id())) {
                    TextChannel channel = Main.jda.getTextChannelById(channelID);
                    //noinspection ConstantConditions
                    final Role role = roleID.isEmpty() ? null : channel.getGuild().getRoleById(roleID);
                    if (role != null) {
                        EmbedMessage.sendPingableUpdateNotification(role, channel, proj);
                    } else EmbedMessage.sendUpdateNotification(channel, proj);
                    Main.cache.put(proj.id(), proj.files().first().id());
                    Main.cacheChanged = true;
                }
                sleep(TimeUnit.SECONDS.toMillis(30));
                proj.refreshFiles();
            } catch (InterruptedException | CurseException e) {
                e.printStackTrace();
            }
        }
    }

}
