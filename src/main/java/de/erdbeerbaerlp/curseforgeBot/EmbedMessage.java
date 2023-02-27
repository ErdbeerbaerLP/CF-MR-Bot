package de.erdbeerbaerlp.curseforgeBot;

import com.github.rjeschke.txtmark.Processor;
import de.erdbeerbaerlp.cfcore.CFCoreAPI;
import de.erdbeerbaerlp.cfcore.json.CFFile;
import de.erdbeerbaerlp.cfcore.json.CFFileIndex;
import de.erdbeerbaerlp.cfcore.json.CFMod;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.NewsChannel;
import net.dv8tion.jda.api.entities.channel.middleman.StandardGuildMessageChannel;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import org.apache.commons.collections4.IteratorUtils;

import java.util.Iterator;


public class EmbedMessage {

    /**
     * Message without link.
     *
     * @param proj    the proj
     * @param file    the file
     * @param channel the channel
     */
    public static void messageWithoutLink(CFMod proj, CFFileIndex file, StandardGuildMessageChannel channel) {
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle(proj.name, proj.links.websiteUrl);
        embed.setThumbnail(proj.logo.thumbnailUrl);
        embed.setDescription(getMessageDescription());
        final CFFile f = CFCoreAPI.getFileFromID(proj.id, file.fileId);
        embed.addField(EmbedBuilder.ZERO_WIDTH_SPACE,
                "**Release Type**: `" + f.releaseType.name() + "`" + "\n **File Name**: `" + f.displayName
                        + "`" + "\n **Category**: `" + proj.categories[proj.categories.length - 1].name + "`" + "\n **GameVersion**: `"
                        + getGameVersions(f),
                false);
        embed.addField(EmbedBuilder.ZERO_WIDTH_SPACE,
                "**Changelog:** \n```" + getSyntax() + "\n" + formatChangelog(CFCoreAPI.getChangelog(proj.id, file.fileId)) + "\n```",
                false);
        channel.sendMessage(new MessageCreateBuilder().addEmbeds(embed.build()).build()).submit().thenAccept((a) -> {
            if (channel instanceof NewsChannel) {
                a.crosspost().queue();
            }
        });
    }

    /**
     * Message with curse link.
     *
     * @param proj    the proj
     * @param file    the file
     * @param channel the channel
     */
    public static void messageWithCurseLink(CFMod proj, CFFileIndex file, StandardGuildMessageChannel channel) {
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle(proj.name, proj.links.websiteUrl);
        embed.setThumbnail(proj.logo.thumbnailUrl);
        embed.setDescription(getMessageDescription());
        final CFFile f = CFCoreAPI.getFileFromID(proj.id, file.fileId);
        embed.addField(EmbedBuilder.ZERO_WIDTH_SPACE,
                "**Release Type**: `" + f.releaseType.name() + "`" + "\n **File Name**: `" + f.displayName
                        + "`" + "\n **Category**: `" + proj.categories[proj.categories.length - 1].name + "`" + "\n **GameVersion**: `"
                        + getGameVersions(f) + "`" + "\n **Website Link**: " + "[CurseForge](" + getUrl(proj) + ")",
                false);
        embed.addField(EmbedBuilder.ZERO_WIDTH_SPACE,
                "**Changelog:** \n```" + getSyntax() + "\n" + formatChangelog(CFCoreAPI.getChangelog(proj.id, file.fileId)) + "\n```",
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
    public static void messageWithDirectLink(CFMod proj, CFFileIndex file, StandardGuildMessageChannel channel) {
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle(proj.name, proj.links.websiteUrl);
        embed.setThumbnail(proj.logo.thumbnailUrl);
        embed.setDescription(getMessageDescription());
        final CFFile f = CFCoreAPI.getFileFromID(proj.id, file.fileId);
        embed.addField(EmbedBuilder.ZERO_WIDTH_SPACE,
                "**Release Type**: `" + file.releaseType.name() + "`" + "\n **File Name**: `" + f.displayName
                        + "`" + "\n **Category**: `" + proj.categories[proj.categories.length - 1].name + "`" + "\n **GameVersion**: `"
                        + getGameVersions(f) + "`" + "\n **Download Link**: " + "[Download](" + f.downloadUrl
                        + ")", false);
        embed.addField(EmbedBuilder.ZERO_WIDTH_SPACE,
                "**Changelog:** \n```" + getSyntax() + "\n" + formatChangelog(CFCoreAPI.getChangelog(proj.id, file.fileId)) + "\n```",
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
    public static void sendPingableUpdateNotification(Role role, StandardGuildMessageChannel channel, CFMod proj)
            throws InsufficientPermissionException {

        channel.sendMessage(role.getAsMention()).queue();
        EmbedMessage.messageWithCurseLink(proj, proj.latestFilesIndexes[0], channel);

    }

    /**
     * Send update notification.
     *
     * @param channel the channel
     * @param proj    the proj
     */
    public static void sendUpdateNotification(StandardGuildMessageChannel channel, CFMod proj) throws InsufficientPermissionException {
        EmbedMessage.messageWithCurseLink(proj, proj.latestFilesIndexes[0], channel);
    }

    /**
     * Returns the custom message description set in bot.conf Description will be
     * set to default description if over 500 characters
     *
     * @return description
     */
    private static String getMessageDescription() {
        String desc = "New File detected For CurseForge Project";
        if (desc.length() > 500) {
            System.out.println(
                    "Your messageDescription is over 500 characters, setting to default value **PLEASE CHANGE THIS**");
            return "New File detected For CurseForge Project";
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
     * Gets the game versions.
     *
     * @param f File to get versions from
     * @return the game versions
     */
    @SuppressWarnings("StringConcatenationInLoop")
    private static String getGameVersions(final CFFile f) {
        if (f.gameVersions.length == 0)
            return "UNKNOWN";
        String out = "";
        for (Iterator<String> it = IteratorUtils.arrayIterator(f.gameVersions); it.hasNext(); ) {
            final String s = it.next();
            out = out + s + (it.hasNext() ? ", " : "");
        }
        return out;
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

    /**
     * Return the newest file curseforge page url to embed into message.
     *
     * @param proj the proj
     * @return url link to file page
     */
    private static String getUrl(final CFMod proj) {
        String urlPre = proj.links.websiteUrl;
        long id = proj.latestFilesIndexes[0].fileId;
        return urlPre + "/files/" + id;
    }
}
