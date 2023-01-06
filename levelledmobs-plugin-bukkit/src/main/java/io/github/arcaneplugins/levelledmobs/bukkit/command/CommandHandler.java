package io.github.arcaneplugins.levelledmobs.bukkit.command;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandAPIConfig;
import io.github.arcaneplugins.levelledmobs.bukkit.LevelledMobs;
import io.github.arcaneplugins.levelledmobs.bukkit.command.levelledmobs.LevelledMobsCommand;
import io.github.arcaneplugins.levelledmobs.bukkit.util.Log;
import java.util.Collection;
import java.util.LinkedList;

public final class CommandHandler {

    private static final Collection<CommandAPICommand> COMMANDS = new LinkedList<>();

    public static final CommandAPIConfig CMD_CONFIG = new CommandAPIConfig()
        .silentLogs(false); //TODO set to `true` when commands are 100% complete.

    public void load(final LoadingStage loadingStage) {
        switch(loadingStage) {
            case ON_LOAD -> {
                Log.inf("Loading commands");
                CommandAPI.onLoad(CMD_CONFIG);
                registerCommands();
            }
            case ON_ENABLE -> {
                Log.inf("Enabling commands");
                CommandAPI.onEnable(LevelledMobs.getInstance());
            }
            case ON_DISABLE -> {
                Log.inf("Unregistering commands");
                unregisterCommands();
            }
            case FORCED -> {
                Log.inf("Manually registering commands");
                registerCommands();
            }
        }
    }

    private void registerCommands() {
        COMMANDS.clear();
        COMMANDS.add(LevelledMobsCommand.createInstance());
        COMMANDS.forEach(CommandAPICommand::register);
    }

    private void unregisterCommands() {
        COMMANDS.forEach(cmd -> CommandAPI.unregister(cmd.getName()));
    }

    public enum LoadingStage {
        ON_LOAD,
        ON_ENABLE,
        ON_DISABLE,
        FORCED
    }

}
