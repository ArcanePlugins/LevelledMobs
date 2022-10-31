package io.github.arcaneplugins.levelledmobs.bukkit.command;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandAPIConfig;
import dev.jorel.commandapi.arguments.ListArgument;
import dev.jorel.commandapi.arguments.ListArgumentBuilder;
import io.github.arcaneplugins.levelledmobs.bukkit.LevelledMobs;
import io.github.arcaneplugins.levelledmobs.bukkit.command.levelledmobs.LevelledMobsCommand;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.generator.WorldInfo;

public class CommandHandler {

    public static final List<CommandAPICommand> COMMANDS = List.of(
        LevelledMobsCommand.INSTANCE
    );

    public void load(final LoadingStage loadingStage) {
        switch(loadingStage) {
            case ON_LOAD -> {
                registerCommands();
                CommandAPI.onLoad(new CommandAPIConfig());
            }
            case ON_ENABLE -> {
                CommandAPI.onEnable(LevelledMobs.getInstance());
            }
        }
    }

    private void registerCommands() {
        COMMANDS.forEach(CommandAPICommand::register);
    }

    public static ListArgument<World> createWorldListArgument(String nodeName) {
        return new ListArgumentBuilder<World>(nodeName)
            .withList(Bukkit.getWorlds())
            .withMapper(WorldInfo::getName)
            .build();
    }

    public enum LoadingStage {
        ON_LOAD,
        ON_ENABLE
    }

}
