package de.erdbeerbaerlp.curseforgeBot.projects;

import de.erdbeerbaerlp.curseforgeBot.EmbedMessage;
import de.erdbeerbaerlp.curseforgeBot.Main;
import de.erdbeerbaerlp.curseforgeBot.storage.json.DCChannel;
import masecla.modrinth4j.model.project.Project;
import masecla.modrinth4j.model.version.ProjectVersion;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.middleman.StandardGuildMessageChannel;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import org.apache.commons.collections4.IteratorUtils;

import java.util.Iterator;

public class ModrinthProject extends UpdatableProject {

    public Project proj;
    public ProjectVersion currentVersion;

    public ModrinthProject(Project mod, DCChannel channel) {
        super(channel);
        this.proj = mod;
    }

    @Override
    public String getTitle() {
        return proj.getTitle();
    }

    @Override
    public String getWebURL() {
        return "https://modrinth.com/project/" + proj.getId();
    }

    @Override
    public String getFileURL() {
        return getWebURL() + "/version/" + currentVersion.getId();
    }

    @Override
    public String getReleaseType() {
        return currentVersion.getVersionType().name();
    }

    @Override
    public String getThumbURL() {
        return proj.getIconUrl();
    }

    @Override
    public String getDescription() {
        return proj.getDescription();
    }

    @Override
    public String getChangelog() {
        return currentVersion.getChangelog();
    }

    @Override
    public String getVersionFileName() {
        return currentVersion.getFiles().get(0).getFilename();
    }

    @Override
    public String getWebsiteName() {
        return "Modrinth";
    }

    @Override
    public String getPrimaryCategory() {
        return proj.getCategories().get(0);
    }

    @Override
    public void run() {
        try {
            proj = Main.mrAPI.projects().get(proj.getId()).get();
            currentVersion = Main.mrAPI.versions().getVersion(proj.getVersions().get(proj.getVersions().size() - 1)).get();
            if (proj.getVersions().isEmpty()) return;
            if (Main.ifa.isNewMRFile(proj, currentVersion.getDatePublished().toEpochMilli())) {
                toRemove.forEach(this::removeChannel);
                toRemove.clear();
                Main.ifa.updateMRCache(proj.getId(), currentVersion.getDatePublished().toEpochMilli());
                for (final DCChannel c : channels) {
                    try {
                        final StandardGuildMessageChannel channel = (StandardGuildMessageChannel) Main.jda.getGuildChannelById(c.channelID);
                        if (channel == null) {
                            toRemove.add(Main.ifa.deleteChannelFromMRProject(proj.getId(), c.channelID));
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
    public String getGameVersions() {
        if (currentVersion.getGameVersions().isEmpty())
            return "UNKNOWN";
        String out = "";
        for (Iterator<String> it = IteratorUtils.arrayIterator(currentVersion.getGameVersions().toArray(new String[0])); it.hasNext(); ) {
            final String s = it.next();
            out = out + s + (it.hasNext() ? ", " : "");
        }
        return out;
    }

    @Override
    public String getDownloadURL() {
        return currentVersion.getFiles().get(0).getUrl();
    }
}
