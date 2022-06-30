package me.lokka30.levelledmobs.bukkit.command;

import java.util.HashSet;
import java.util.Set;
import me.lokka30.levelledmobs.bukkit.command.levelledmobs.LevelledMobsBaseCommand;
import me.lokka30.levelledmobs.bukkit.util.Log;
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
