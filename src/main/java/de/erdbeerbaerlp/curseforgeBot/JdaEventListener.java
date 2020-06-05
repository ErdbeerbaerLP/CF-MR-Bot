package de.erdbeerbaerlp.curseforgeBot;

import com.therandomlabs.curseapi.project.CurseProject;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.EventListener;

import javax.annotation.Nonnull;

public class JdaEventListener implements EventListener {
    @Override
    public void onEvent(@Nonnull GenericEvent event) {
        if (event instanceof MessageReceivedEvent) {
            final MessageReceivedEvent ev = (MessageReceivedEvent) event;
            if (ev.getChannelType().equals(ChannelType.TEXT) && !ev.getAuthor().isBot() && !ev.getAuthor().isFake() && ev.getMember() != null && canRunCommands(ev.getMember())) {
                if (ev.getMessage().getContentRaw().startsWith("cf!")) {
                    final String[] command = ev.getMessage().getContentRaw().replaceFirst("cf!", "").split(" ");
                    if (command.length == 2) {
                        switch (command[0]) {
                            case "link":
                                try {
                                    final CurseProject pr = Main.addProjectFromCommand(command[1], ev.getChannel().getId());
                                    ev.getChannel().sendMessage("Bound project " + pr.name() + " (" + pr.id() + ") to this channel!").queue();
                                } catch (final Exception e) {
                                    ev.getChannel().sendMessage("Failed to add project " + command[1] + " to this channel: `" + e.getMessage() + "`").queue();
                                }
                                break;
                            case "unlink":
                                final boolean ok = Main.removeProjectFromCommand(command[1], ev.getChannel().getId());
                                if (ok)
                                    ev.getChannel().sendMessage("Removed project with id `" + command[1] + "` from this channel!").queue();
                                else
                                    ev.getChannel().sendMessage("Project is not bound to this channel!").queue();
                                break;
                        }
                    }
                }
            }
        }
    }

    private boolean canRunCommands(Member member) {
        for (final Role r : member.getRoles()) {
            if (Main.cfg.roles.contains(r.getId())) return true;
        }
        return false;
    }
}
