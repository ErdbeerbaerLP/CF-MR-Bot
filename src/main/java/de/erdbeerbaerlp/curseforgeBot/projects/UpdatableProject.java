package de.erdbeerbaerlp.curseforgeBot.projects;

import de.erdbeerbaerlp.curseforgeBot.Main;
import de.erdbeerbaerlp.curseforgeBot.storage.json.DCChannel;
import de.erdbeerbaerlp.curseforgeBot.storage.json.Root;

import java.util.ArrayList;

public abstract class UpdatableProject implements Runnable {
    final ArrayList<DCChannel> toRemove = new ArrayList<>();
    final ArrayList<DCChannel> channels = new ArrayList<>();

    public UpdatableProject(final DCChannel channel) {
        this.channels.add(channel);
    }

    public void addChannel(long idLong, long projectID) {
        for (DCChannel c : channels)
            if (c != null && c.channelID == idLong) return;

        final DCChannel cfChannel = new DCChannel(idLong, new Root());
        cfChannel.data.projects = new Long[]{projectID};
        this.channels.add(cfChannel);
        Main.ifa.addChannelToCFProject(projectID, idLong);
    }

    public boolean addChannel(DCChannel channel) {
        System.out.println("Adding " + channel.channelID);
        return this.channels.add(channel);
    }

    public boolean removeChannel(long channelID) {
        for (final DCChannel c : channels) {
            if (c.channelID == channelID) return removeChannel(c);
        }
        return false;
    }

    public boolean removeChannel(DCChannel channel) {
        System.out.println("Removing " + channel.channelID);
        return this.channels.remove(channel);
    }

    public abstract void run();

    public abstract String getTitle();

    public abstract String getWebURL();

    public abstract String getFileURL();

    public abstract String getReleaseType();

    public abstract String getThumbURL();

    public abstract String getDescription();

    public abstract String getChangelog();

    public abstract String getVersionFileName();

    public abstract String getWebsiteName();

    public abstract String getPrimaryCategory();

    public abstract String getGameVersions();

    public abstract String getDownloadURL();
}
