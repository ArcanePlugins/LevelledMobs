package io.github.arcaneplugins.levelledmobs.bukkit.command;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandAPIConfig;
import io.github.arcaneplugins.levelledmobs.bukkit.LevelledMobs;
import io.github.arcaneplugins.levelledmobs.bukkit.command.levelledmobs.LevelledMobsCommand;
import io.github.arcaneplugins.levelledmobs.bukkit.util.Log;
import java.util.List;

public class CommandHandler {

    public static final List<CommandAPICommand> COMMANDS = List.of(
        LevelledMobsCommand.INSTANCE
    );

    public void load(final LoadingStage loadingStage) {
        switch(loadingStage) {
            case ON_LOAD -> {
                Log.inf("Loading commands");
                registerCommands();
                CommandAPI.onLoad(new CommandAPIConfig());
            }
            case ON_ENABLE -> {
                Log.inf("Enabling commands");
                CommandAPI.onEnable(LevelledMobs.getInstance());
            }
            case ON_RELOAD -> {
                Log.inf("Reloading commands");
                unregisterCommands();
                registerCommands();
            }
            case ON_DISABLE -> {
                Log.inf("Unregistering commands");
                unregisterCommands();
            }
        }
    }

    private void registerCommands() {
        COMMANDS.forEach(CommandAPICommand::register);
    }

    private void unregisterCommands() {
        COMMANDS.forEach(cmd -> CommandAPI.unregister(cmd.getName()));
    }

    public enum LoadingStage {
        ON_LOAD,
        ON_ENABLE,
        ON_RELOAD,
        ON_DISABLE
    }

}
