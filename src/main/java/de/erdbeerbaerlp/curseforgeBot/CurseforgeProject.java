package de.erdbeerbaerlp.curseforgeBot;

import com.therandomlabs.curseapi.CurseException;
import com.therandomlabs.curseapi.project.CurseProject;
import de.erdbeerbaerlp.curseforgeBot.storage.json.Root;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;

import java.sql.SQLException;
import java.util.ArrayList;

public class CurseforgeProject implements Runnable {
    public final CurseProject proj;
    private final ArrayList<CFChannel> channels = new ArrayList<>();


    public CurseforgeProject(CurseProject project, CFChannel channel) throws CurseException {
        this.channels.add(channel);
        this.proj = project;
    }

    public void addChannel(long idLong, int projectID) {
        for (CFChannel c : channels)
            if (c != null && c.channelID == idLong) return;

        final CFChannel cfChannel = new CFChannel(idLong, new Root());
        cfChannel.data.projects = new Integer[]{projectID};
        this.channels.add(cfChannel);
        Main.ifa.addChannelToProject(projectID, idLong);
    }

    public boolean addChannel(CFChannel channel) {
        System.out.println("Adding " + channel.channelID);
        return this.channels.add(channel);
    }

    public boolean removeChannel(CFChannel channel) {
        System.out.println("Removing " + channel.channelID);
        return this.channels.remove(channel);
    }

    final ArrayList<CFChannel> toRemove = new ArrayList<CFChannel>();
    @Override
    public void run() {
        try {
            proj.refreshFiles();
            if (proj.files().isEmpty()) return;
            if (Main.ifa.isNewFile(proj)) {
                toRemove.forEach(this::removeChannel);
                toRemove.clear();
                for (CFChannel c : channels) {
                    final TextChannel channel = Main.jda.getTextChannelById(c.channelID);
                    if (channel == null) {
                        toRemove.add(Main.ifa.deleteChannelFromProject(proj.id(), c.channelID));
                        return;
                    }
                    final Role role = c.data.settings.pingRole == 0 ? null : channel.getGuild().getRoleById(c.data.settings.pingRole);
                    try {
                        if (role != null) {
                            EmbedMessage.sendPingableUpdateNotification(role, channel, proj);
                        } else
                            EmbedMessage.sendUpdateNotification(channel, proj);
                    } catch (InsufficientPermissionException e) {
                        System.out.println(channel);
                        if (channel != null) {
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
                    }
                }
                Main.ifa.updateCache(proj.id(), proj.files().first().id());
            }
        } catch (CurseException | SQLException e) {
            e.printStackTrace();
        }
    }

    public static class CFChannel {

        public final long channelID;
        public final Root data;

        public CFChannel(long channelID, Root json) {
            this.channelID = channelID;
            this.data = json;
        }

        @Override
        public String toString() {
            return "CFChannel{" +
                    "channelID=" + channelID +
                    ", data=" + data +
                    '}';
        }
    }
}
