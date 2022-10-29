package io.github.arcaneplugins.levelledmobs.bukkit.command;

import io.github.arcaneplugins.levelledmobs.bukkit.command.levelledmobs.LevelledMobsBaseCommand;
import io.github.arcaneplugins.levelledmobs.bukkit.util.Log;
import java.util.HashSet;
import java.util.Set;
import org.jetbrains.annotations.NotNull;

public final class CommandHandler {

    /* vars */

    private final Set<BaseCommandWrapper> baseCommands = new HashSet<>();

    /* constructors */

    public CommandHandler() {
        baseCommands.add(
            new LevelledMobsBaseCommand()
        );
    }

    /* methods */

    public boolean load() {
        Log.inf("Registering commands");
        for(var baseCommand : getBaseCommands()) {
            if(!baseCommand.register())
                return false;
        }
        return true;
    }

    /* getters and setters */

    @NotNull
    public Set<BaseCommandWrapper> getBaseCommands() {
        return baseCommands;
    }

}
