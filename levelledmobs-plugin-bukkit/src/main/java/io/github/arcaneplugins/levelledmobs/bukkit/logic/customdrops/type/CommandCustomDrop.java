package io.github.arcaneplugins.levelledmobs.bukkit.logic.customdrops.type;

import io.github.arcaneplugins.levelledmobs.bukkit.LevelledMobs;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.LogicHandler;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.Context;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.customdrops.CustomDrop;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.customdrops.recipient.CustomDropRecipient;
import java.util.Collection;
import java.util.HashSet;
import javax.annotation.Nonnull;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

public class CommandCustomDrop extends CustomDrop {

    private final Collection<String> commands;

    private final Collection<String> commandRunEvents = new HashSet<>();
    private long commandDelay = 0;

    public CommandCustomDrop(
        final @Nonnull Collection<String> commands,
        final @Nonnull CustomDropRecipient recipient
    ) {
        super(StandardCustomDropType.COMMAND.name(), recipient);
        this.commands = commands;
    }

    /* methods */

    public void execute(
        final @Nonnull Context context
    ) {
        if(getCommandDelay() == 0L) {
            executeImmediately(context);
        } else {
            new BukkitRunnable() {
                @Override
                public void run() {
                    execute(context);
                }
            }.runTaskLater(LevelledMobs.getInstance(), getCommandDelay());
        }
    }

    private void executeImmediately(
        final @Nonnull Context context
    ) {
        for(final String command : getCommands()) {
            Bukkit.dispatchCommand(
                Bukkit.getConsoleSender(),
                LogicHandler.replacePapiAndContextPlaceholders(command, context)
            );
        }
    }

    /* getters and setters */

    /**
     * @return delay before running the commands (measured in Minecraft ticks)
     */
    public long getCommandDelay() {
        return commandDelay;
    }

    public @Nonnull CommandCustomDrop withCommandDelay(final long commandDelay) {
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

    public @Nonnull Collection<String> getCommands() {
        return commands;
    }

}
