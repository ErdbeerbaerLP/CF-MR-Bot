package de.erdbeerbaerlp.curseforgeBot;

import com.therandomlabs.curseapi.CurseAPI;
import com.therandomlabs.curseapi.CurseException;
import com.therandomlabs.curseapi.project.CurseProject;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;

public class CurseforgeUpdateThread extends Thread
{
    private final CurseProject proj;
    private String channelID;
    private String roleID;
    
    CurseforgeUpdateThread(String id) throws CurseException {
        if (id.contains(";;")) {
            String[] ids = id.split(";;");
            channelID = ids[1];
            if (ids.length == 3) {
        	System.out.println(ids.length);
        	roleID = ids[2];
            }
        }
        else
    	roleID = Main.cfg.mentionRole;
        channelID = Main.cfg.DefaultChannel;
        final Optional<CurseProject> project = CurseAPI.project(Integer.parseInt(id.split(";;")[0]));
        if (!project.isPresent()) throw new CurseException("Project not found");
        proj = project.get();
        setName("Curseforge Update Detector for " + proj.name() + " (ID: " + proj.id() + ")");
    }

    @Override
    public void run() {
        while (true) {
            try {
                System.out.println("<" + proj.name() + "> Cached: " + Main.cache.get(proj.name()) + " Newest:" + proj.files().first().id());
                if (Main.cfg.isNewFile(proj.name(), proj.files().first().id())) {
                    TextChannel channel = Main.jda.getTextChannelById(channelID);
                    Role role = channel.getGuild().getRoleById(roleID);
                    if(!(role == null)) {
                	EmbedMessage.sendPingableUpdateNotification(role, channel, proj);
                    } 
                    else EmbedMessage.sendUpdateNotification(channel, proj);
                    Main.cache.put(proj.name(), proj.files().first().id());
                    Main.cacheChanged = true;
                }
                sleep(TimeUnit.SECONDS.toMillis(10));
                proj.clearFilesCache();
            } catch (InterruptedException | CurseException ignored) {
            }
        }
    }
}
