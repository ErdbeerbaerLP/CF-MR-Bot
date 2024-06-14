package de.erdbeerbaerlp.curseforgeBot.projects;

import de.erdbeerbaerlp.curseforgeBot.storage.json.DCChannel;

import java.util.ArrayList;

public abstract class UpdatableProject implements Runnable {
    final ArrayList<DCChannel> toRemove = new ArrayList<>();
    final ArrayList<DCChannel> channels = new ArrayList<>();

    public UpdatableProject(final DCChannel channel) {
        this.channels.add(channel);
    }

    public boolean addChannel(DCChannel channel) {
        System.out.println("Adding " + channel.channelID + " to " + getTitle());
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
