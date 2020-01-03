package de.erdbeerbaerlp.curseforgeBot;

import com.therandomlabs.curseapi.CurseAPI;
import com.therandomlabs.curseapi.CurseException;
import com.therandomlabs.curseapi.project.CurseProject;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import net.dv8tion.jda.api.entities.TextChannel;

public class CurseforgeUpdateThread extends Thread
{
    private final CurseProject proj;
    private final String channelID;
    
    
    CurseforgeUpdateThread(String id) throws CurseException {
        if (id.contains(";;")) {
            channelID = id.split(";;")[1];
        }
        else channelID = Main.cfg.DefaultChannel;
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
                    String roleid = Main.cfg.mentionRole;
                    if (Main.cfg.updateFileLink.equals("nolink")) {
                	if (!roleid.equals("000000000")) {
                    	channel.sendMessage(EmbedMessage.getRoleAsMention(roleid, channel)).queue();
			}
                        EmbedMessage.messageWithoutLink(proj, proj.files().first(), channel);
		    }
                    if (Main.cfg.updateFileLink.equals("curse")) {
                	if (!roleid.equals("000000000")) {
                        	channel.sendMessage(EmbedMessage.getRoleAsMention(roleid, channel)).queue();
    			}
                        EmbedMessage.messageWithCurseLink(proj, proj.files().first(), channel);
		    }
                    if (Main.cfg.updateFileLink.equals("direct")) {
                	if (!roleid.equals("000000000")) {
                        	channel.sendMessage(EmbedMessage.getRoleAsMention(roleid, channel)).queue();
    			}
                        EmbedMessage.messageWithDirectLink(proj, proj.files().first(), channel);
		    }
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
