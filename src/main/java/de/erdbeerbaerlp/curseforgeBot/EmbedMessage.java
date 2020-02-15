package de.erdbeerbaerlp.curseforgeBot;

import com.github.rjeschke.txtmark.Processor;
import com.therandomlabs.curseapi.CurseException;
import com.therandomlabs.curseapi.file.CurseFile;
import com.therandomlabs.curseapi.project.CurseProject;

import java.util.Iterator;
import java.util.stream.Stream;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;


public class EmbedMessage {

    /**
     * Message without link.
     *
     * @param proj the proj
     * @param file the file
     * @param channel the channel
     * @throws CurseException the curse exception
     */
    public static void messageWithoutLink(CurseProject proj, CurseFile file, TextChannel channel)
	    throws CurseException {
	EmbedBuilder embed = new EmbedBuilder();
	embed.setTitle(proj.name(), proj.url().toString());
	embed.setThumbnail(proj.logo().thumbnailURL().toString());
	embed.setDescription(getMessageDescription());
	embed.addField(EmbedBuilder.ZERO_WIDTH_SPACE,
		"**Release Type**: `" + file.releaseType().name() + "`" + "\n **File Name**: `" + file.displayName()
			+ "`" + "\n **Category**: `" + proj.categorySection().name() + "`" + "\n **GameVersion**: `"
			+ getGameVersions(proj) + "`",false);
	embed.addField(EmbedBuilder.ZERO_WIDTH_SPACE,
		"**Changelog:** \n```" + getSyntax() + "\n" + formatChangelog(file.changelogPlainText(1000)) + "\n```",
		false);
	channel.sendMessage(embed.build()).queue();
    }

    /**
     * Message with curse link.
     *
     * @param proj the proj
     * @param file the file
     * @param channel the channel
     * @throws CurseException the curse exception
     */
    public static void messageWithCurseLink(CurseProject proj, CurseFile file, TextChannel channel)
	    throws CurseException {
	EmbedBuilder embed = new EmbedBuilder();
	embed.setTitle(proj.name(), proj.url().toString());
	embed.setThumbnail(proj.logo().thumbnailURL().toString());
	embed.setDescription(getMessageDescription());
	embed.addField(EmbedBuilder.ZERO_WIDTH_SPACE,
		"**Release Type**: `" + file.releaseType().name() + "`" + "\n **File Name**: `" + file.displayName()
			+ "`" + "\n **Category**: `" + proj.categorySection().name() + "`" + "\n **GameVersion**: `"
			+ getGameVersions(proj) + "`" + "\n **Website Link**: " + "[CurseForge](" + getUrl(proj) + ")",
		false);
	embed.addField(EmbedBuilder.ZERO_WIDTH_SPACE,
		"**Changelog:** \n```" + getSyntax() + "\n" + formatChangelog(file.changelogPlainText(1000)) + "\n```",
		false);
	channel.sendMessage(embed.build()).queue();
    }

    /**
     * Message with direct link.
     *
     * @param proj the proj
     * @param file the file
     * @param channel the channel
     * @throws CurseException the curse exception
     */
    public static void messageWithDirectLink(CurseProject proj, CurseFile file, TextChannel channel)
	    throws CurseException {
	EmbedBuilder embed = new EmbedBuilder();
	embed.setTitle(proj.name(), proj.url().toString());
	embed.setThumbnail(proj.logo().thumbnailURL().toString());
	embed.setDescription(getMessageDescription());
	embed.addField(EmbedBuilder.ZERO_WIDTH_SPACE,
		"**Release Type**: `" + file.releaseType().name() + "`" + "\n **File Name**: `" + file.displayName()
			+ "`" + "\n **Category**: `" + proj.categorySection().name() + "`" + "\n **GameVersion**: `"
			+ getGameVersions(proj) + "`" + "\n **Download Link**: " + "[Download](" + file.downloadURL()
			+ ")",false);
	embed.addField(EmbedBuilder.ZERO_WIDTH_SPACE,
		"**Changelog:** \n```" + getSyntax() + "\n" + formatChangelog(file.changelogPlainText(1000)) + "\n```",
		false);
	channel.sendMessage(embed.build()).queue();
    }

    /**
     * Send pingable update notification.
     *
     * @param role the role
     * @param channel the channel
     * @param proj the proj
     * @throws CurseException the curse exception
     */
    public static void sendPingableUpdateNotification(Role role, TextChannel channel, CurseProject proj)
	    throws CurseException {
	if (Main.cfg.updateFileLink.equals("nolink")) {
	    channel.sendMessage(role.getAsMention()).queue();
	    EmbedMessage.messageWithoutLink(proj, proj.files().first(), channel);
	}
	if (Main.cfg.updateFileLink.equals("curse")) {
	    channel.sendMessage(role.getAsMention()).queue();
	    EmbedMessage.messageWithCurseLink(proj, proj.files().first(), channel);
	}
	if (Main.cfg.updateFileLink.equals("direct")) {
	    channel.sendMessage(role.getAsMention()).queue();
	    EmbedMessage.messageWithDirectLink(proj, proj.files().first(), channel);
	}
    }

    /**
     * Send update notification.
     *
     * @param channel the channel
     * @param proj the proj
     * @throws CurseException the curse exception
     */
    public static void sendUpdateNotification(TextChannel channel, CurseProject proj) throws CurseException {
	if (Main.cfg.updateFileLink.equals("nolink")) {
	    EmbedMessage.messageWithoutLink(proj, proj.files().first(), channel);
	}
	if (Main.cfg.updateFileLink.equals("curse")) {
	    EmbedMessage.messageWithCurseLink(proj, proj.files().first(), channel);
	}
	if (Main.cfg.updateFileLink.equals("direct")) {
	    EmbedMessage.messageWithDirectLink(proj, proj.files().first(), channel);
	}
    }

    /**
     * Returns the custom message description set in bot.conf Description will be
     * set to default description if over 500 characters
     * 
     * @return description
     */
    private static String getMessageDescription() {
	String desc = Main.cfg.messageDescription;
	if (desc.length() > 500) {
	    System.out.println(
		    "Your messageDescription is over 500 characters, setting to default value **PLEASE CHANGE THIS**");
	    return "New File Detected For CurseForge Project";
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
     * @param proj the proj
     * @return the game versions
     * @throws CurseException the curse exception
     */
    private static String getGameVersions(final CurseProject proj) throws CurseException {
	if (proj.files().first().gameVersionStrings().isEmpty())
	    return "UNKNOWN";
	String out = "";
	final Stream<String> stream = proj.files().first().gameVersionStrings().stream().sorted();
	for (Iterator<String> it = stream.iterator(); it.hasNext();) {
	    final String s = it.next();
	    out = out + s + (it.hasNext() ? ", " : "");
	}
	return out;
    }

    /**
     * returns the discord markdown syntax set in bot.conf this method does not
     * throw an error if syntax is not supported or if multiple syntax's are
     * specified.
     * 
     * Non-supported syntax auto default to plain text in discord
     * 
     * @return discord code syntax
     */
    private static String getSyntax() {
	String md = Main.cfg.changlogDiscordFormat;
	if (!(md == "Syntax")) {
	    return md + "\n";
	} else {
	    return "\n";
	}
    }

    /**
     * Return the newest file curseforge page url to embed into message.
     *
     * @param proj the proj
     * @return url link to file page
     * @throws CurseException the curse exception
     */
    private static String getUrl(final CurseProject proj) throws CurseException {
	String urlPre = proj.url().toString();
	int id = proj.files().first().id();
	String out = urlPre + "/files/" + id;
	return out;
    }
}
