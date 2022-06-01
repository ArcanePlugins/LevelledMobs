package me.lokka30.levelledmobs.bukkit.commands;

import java.util.HashSet;
import java.util.Set;
import me.lokka30.levelledmobs.bukkit.commands.levelledmobs.LevelledMobsBaseCommand;
import me.lokka30.levelledmobs.bukkit.utils.Log;
import org.jetbrains.annotations.NotNull;

public final class CommandHandler {

    /* vars */

    private final Set<BaseCommandWrapper> baseCommands = new HashSet<>();

    /* constructors */

    public CommandHandler() {
        baseCommands.addAll(Set.of(
            new LevelledMobsBaseCommand()
        ));
    }

    /* methods */

    public boolean load() {
        Log.inf("Registering commands.");
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
