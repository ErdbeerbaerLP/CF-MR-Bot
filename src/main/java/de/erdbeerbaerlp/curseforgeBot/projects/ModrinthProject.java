package de.erdbeerbaerlp.curseforgeBot.projects;

import de.erdbeerbaerlp.curseforgeBot.EmbedMessage;
import de.erdbeerbaerlp.curseforgeBot.Main;
import de.erdbeerbaerlp.curseforgeBot.storage.json.DCChannel;
import masecla.modrinth4j.endpoints.version.GetProjectVersions;
import masecla.modrinth4j.model.project.Project;
import masecla.modrinth4j.model.version.ProjectVersion;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.middleman.StandardGuildMessageChannel;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import org.apache.commons.collections4.IteratorUtils;

import java.util.Iterator;
import java.util.List;

public class ModrinthProject extends UpdatableProject {

    public Project proj;
    private final String setID;
    public ProjectVersion currentVersion;

    public ModrinthProject(final Project mod, DCChannel channel, String setID) {
        super(channel);
        this.proj = mod;
        this.setID = setID;

        System.out.println("Adding " + channel.channelID + " to " + getTitle());
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
            proj = Main.mrAPI.projects().get(setID).get();
            final List<ProjectVersion> projectVersions = Main.mrAPI.versions().getProjectVersions(proj.getSlug(), GetProjectVersions.GetProjectVersionsRequest.builder().featured(false).build()).get();
            if (proj.getVersions().isEmpty()) {
                System.out.println("No versions");
                return;
            }
            currentVersion = projectVersions.get(0);
            channels.forEach((c) -> System.out.println(c.channelID));
            if (Main.ifa.isNewMRFile(proj, currentVersion.getDatePublished().toEpochMilli(), setID)) {
                toRemove.forEach(this::removeChannel);
                toRemove.clear();
                Main.ifa.updateMRCache(setID, currentVersion.getDatePublished().toEpochMilli());
                for (final DCChannel c : channels) {
                    System.out.println("Attempting to send to channel " + c.channelID);
                    try {
                        final StandardGuildMessageChannel channel = (StandardGuildMessageChannel) Main.jda.getGuildChannelById(c.channelID);
                        System.out.println(channel);
                        if (channel == null) {
                            toRemove.add(Main.ifa.deleteChannelFromMRProject(setID, c.channelID));
                            continue;
                        }
                        final Role role = c.data.settings.pingRole == 0 ? null : channel.getGuild().getRoleById(c.data.settings.pingRole);
                        System.out.println("role " + role);
                        try {
                            if (role != null) {
                                System.out.println("Sending pingable");
                                EmbedMessage.sendPingableUpdateNotification(role, channel, this);
                            } else {
                                System.out.println("Sending normal");
                                EmbedMessage.sendUpdateNotification(channel, this);
                            }
                        } catch (InsufficientPermissionException e) {
                            System.out.println(channel);
                            System.out.println(channel + ":" + e.getMessage());
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
