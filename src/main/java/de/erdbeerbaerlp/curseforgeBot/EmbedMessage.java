package de.erdbeerbaerlp.curseforgeBot;

import com.github.rjeschke.txtmark.Processor;
import de.erdbeerbaerlp.cfcore.json.CFFileIndex;
import de.erdbeerbaerlp.curseforgeBot.projects.UpdatableProject;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.NewsChannel;
import net.dv8tion.jda.api.entities.channel.middleman.StandardGuildMessageChannel;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;


public class EmbedMessage {

    /**
     * Message with link.
     *
     * @param proj    the project
     * @param channel the channel
     */
    public static void messageWithFileLink(UpdatableProject proj, StandardGuildMessageChannel channel) {
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle(proj.getTitle(), proj.getWebURL());
        embed.setThumbnail(proj.getThumbURL());
        embed.setDescription(getMessageDescription(proj));
        embed.addField(EmbedBuilder.ZERO_WIDTH_SPACE,
                "**Release Type**: `" + proj.getReleaseType() + "`" + "\n **File Name**: `" + proj.getVersionFileName()
                        + "`" + "\n **Category**: `" + proj.getPrimaryCategory() + "`" + "\n **GameVersion**: `"
                        + proj.getGameVersions() + "`" + "\n **Website Link**: " + "[" + proj.getWebsiteName() + "](" + proj.getFileURL() + ")",
                false);
        embed.addField(EmbedBuilder.ZERO_WIDTH_SPACE,
                "**Changelog:** \n```" + getSyntax() + "\n" + formatChangelog(proj.getChangelog()) + "\n```",
                false);
        channel.sendMessage(new MessageCreateBuilder().addEmbeds(embed.build()).build()).submit().thenAccept((a) -> {
            if (channel instanceof NewsChannel) {
                a.crosspost().queue();
            }
        });
    }

    /**
     * Message with direct link.
     *
     * @param proj    the proj
     * @param file    the file
     * @param channel the channel
     */
    public static void messageWithDirectLink(UpdatableProject proj, CFFileIndex file, StandardGuildMessageChannel channel) {
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle(proj.getTitle(), proj.getWebURL());
        embed.setThumbnail(proj.getThumbURL());
        embed.setDescription(getMessageDescription(proj));
        embed.addField(EmbedBuilder.ZERO_WIDTH_SPACE,
                "**Release Type**: `" + proj.getReleaseType() + "`" + "\n **File Name**: `" + proj.getVersionFileName()
                        + "`" + "\n **Category**: `" + proj.getPrimaryCategory() + "`" + "\n **GameVersion**: `"
                        + proj.getGameVersions() + "`" + "\n **Download Link**: " + "[Download](" + proj.getDownloadURL()
                        + ")",
                false);
        embed.addField(EmbedBuilder.ZERO_WIDTH_SPACE,
                "**Changelog:** \n```" + getSyntax() + "\n" + formatChangelog(proj.getChangelog()) + "\n```",
                false);
        channel.sendMessage(new MessageCreateBuilder().addEmbeds(embed.build()).build()).submit().thenAccept((a) -> {
            if (channel instanceof NewsChannel) {
                a.crosspost().queue();
            }
        });
    }

    /**
     * Send pingable update notification.
     *
     * @param role    the role
     * @param channel the channel
     * @param proj    the proj
     */
    public static void sendPingableUpdateNotification(Role role, StandardGuildMessageChannel channel, UpdatableProject proj)
            throws InsufficientPermissionException {

        channel.sendMessage(role.getAsMention()).queue();
        EmbedMessage.messageWithFileLink(proj, channel);

    }

    /**
     * Send update notification.
     *
     * @param channel the channel
     * @param proj    the proj
     */
    public static void sendUpdateNotification(StandardGuildMessageChannel channel, UpdatableProject proj) throws InsufficientPermissionException {
        EmbedMessage.messageWithFileLink(proj, channel);
    }

    /**
     * Returns the custom message description set in bot.conf Description will be
     * set to default description if over 500 characters
     *
     * @return description
     */
    private static String getMessageDescription(UpdatableProject proj) {
        String desc = "New File detected For " + proj.getWebsiteName() + " Project";
        if (desc.length() > 500) {
            System.out.println(
                    "Your messageDescription is over 500 characters, setting to default value **PLEASE CHANGE THIS**");
            return "New File detected For " + proj.getWebsiteName() + " Project";
        } else {
            return desc;
        }
    }

    /**
     * Format changelog.
     *
     * @param s the s
     * @return the string
     */
    private static String formatChangelog(String s) {
        String string = Processor.process(s).replace("<br>", "\n").replace("&lt;", "<").replace("&gt;",
                ">").replaceAll("(?s)<[^>]*>(<[^>]*>)*", "");
        string = string.replaceAll("https.*?\\s", "");
        String out = "";
        int additionalLines = 0;
        for (final String st : string.split("\n")) {
            if ((out + st.trim() + "\n").length() > 950) {
                additionalLines++;
            } else // noinspection StringConcatenationInLoop
                out = out + st.trim() + "\n";
        }
        return out + (additionalLines > 0 ? ("... And " + additionalLines + " more lines") : "");
    }


    /**
     * returns the discord markdown syntax set in bot.conf this method does not
     * throw an error if syntax is not supported or if multiple syntax's are
     * specified.
     * <p>
     * Non-supported syntax auto default to plain text in discord
     *
     * @return discord code syntax
     */
    private static String getSyntax() {
        return "\n";

    }

}
