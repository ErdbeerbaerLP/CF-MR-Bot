package de.erdbeerbaerlp.curseforgeBot.projects;

import de.erdbeerbaerlp.cfcore.CFCoreAPI;
import de.erdbeerbaerlp.cfcore.json.CFMod;
import de.erdbeerbaerlp.curseforgeBot.EmbedMessage;
import de.erdbeerbaerlp.curseforgeBot.Main;
import de.erdbeerbaerlp.curseforgeBot.storage.json.DCChannel;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.middleman.StandardGuildMessageChannel;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import org.apache.commons.collections4.IteratorUtils;

import java.util.Iterator;

public class CurseforgeProject extends UpdatableProject {

    public CFMod proj;

    public CurseforgeProject(CFMod project, DCChannel channel) {
        super(channel);
        this.proj = project;
    }


    @Override
    public void run() {
        try {
            proj = CFCoreAPI.getModFromID(proj.id);
            if (proj.latestFiles.length == 0) return;
            if (Main.ifa.isNewCFFile(proj)) {
                toRemove.forEach(this::removeChannel);
                toRemove.clear();
                Main.ifa.updateCFCache(proj.id, proj.latestFilesIndexes[0].fileId);
                for (final DCChannel c : channels) {
                    try {
                        final StandardGuildMessageChannel channel = (StandardGuildMessageChannel) Main.jda.getGuildChannelById(c.channelID);
                        if (channel == null) {
                            toRemove.add(Main.ifa.deleteChannelFromCFProject(proj.id, c.channelID));
                            continue;
                        }
                        final Role role = c.data.settings.pingRole == 0 ? null : channel.getGuild().getRoleById(c.data.settings.pingRole);
                        try {
                            if (role != null) {
                                EmbedMessage.sendPingableUpdateNotification(role, channel, this);
                            } else
                                EmbedMessage.sendUpdateNotification(channel, this);
                        } catch (InsufficientPermissionException e) {
                            System.out.println(channel);
                            System.out.println(channel.getName() + ":" + e.getMessage());
                            final Guild guild = channel.getGuild();
                            guild.retrieveOwner().submit().thenAccept((ow) -> {
                                System.out.println(ow);
                                if (ow != null)
                                    ow.getUser().openPrivateChannel().submit().thenAccept((dm) -> {
                                        dm.sendMessage("I tried posting an update notification, but I am missing required permission for channel " + channel.getAsMention() + "\n> `" + e.getMessage() + "`").queue();
                                    });
                            });

                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getTitle() {
        return proj.name;
    }

    @Override
    public String getWebURL() {
        return proj.links.websiteUrl;
    }

    @Override
    public String getFileURL() {
        String urlPre = getWebURL();
        long id = proj.latestFilesIndexes[0].fileId;
        return urlPre + "/files/" + id;
    }

    @Override
    public String getReleaseType() {
        return proj.latestFilesIndexes[0].releaseType.name();
    }

    @Override
    public String getThumbURL() {
        return proj.logo.thumbnailUrl;
    }

    @Override
    public String getDescription() {
        return proj.summary;
    }

    @Override
    public String getChangelog() {
        return CFCoreAPI.getChangelog(proj.id, proj.latestFilesIndexes[0].fileId);
    }

    @Override
    public String getVersionFileName() {
        return proj.latestFilesIndexes[0].filename;
    }

    @Override
    public String getWebsiteName() {
        return "CurseForge";
    }

    @Override
    public String getPrimaryCategory() {
        return proj.categories[proj.categories.length - 1].name;
    }

    @Override
    public String getGameVersions() {
        if (proj.latestFiles[0].gameVersions.length == 0)
            return "UNKNOWN";
        String out = "";
        for (Iterator<String> it = IteratorUtils.arrayIterator(proj.latestFiles[0].gameVersions); it.hasNext(); ) {
            final String s = it.next();
            out = out + s + (it.hasNext() ? ", " : "");
        }
        return out;
    }

    @Override
    public String getDownloadURL() {
        return proj.latestFiles[0].downloadUrl;
    }
}
