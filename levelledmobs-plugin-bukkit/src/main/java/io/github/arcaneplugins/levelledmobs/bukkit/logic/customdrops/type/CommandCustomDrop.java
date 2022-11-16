package io.github.arcaneplugins.levelledmobs.bukkit.logic.customdrops.type;

import io.github.arcaneplugins.levelledmobs.bukkit.logic.customdrops.CustomDrop;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.customdrops.recipient.CustomDropRecipient;
import java.util.Collection;
import java.util.HashSet;
import javax.annotation.Nonnull;

public class CommandCustomDrop extends CustomDrop {

    private final String command;

    private final Collection<String> commandRunEvents = new HashSet<>();
    private int commandDelay = 0;

    public CommandCustomDrop(
        final @Nonnull String command,
        final @Nonnull CustomDropRecipient recipient
    ) {
        super(StandardCustomDropType.COMMAND.name(), recipient);
        this.command = command;
    }

    /* getters and setters */

    public int getCommandDelay() {
        return commandDelay;
    }

    public @Nonnull CommandCustomDrop withCommandDelay(final int commandDelay) {
        this.commandDelay = commandDelay;
        return this;
    }

    public @Nonnull Collection<String> getCommandRunEvents() {
        return commandRunEvents;
    }

    public @Nonnull CommandCustomDrop withCommandRunEvents(final @Nonnull Collection<String> runEvents) {
        getCommandRunEvents().addAll(runEvents);
        return this;
    }

    public @Nonnull String getCommand() {
        assert command != null;
        return command;
    }

}
