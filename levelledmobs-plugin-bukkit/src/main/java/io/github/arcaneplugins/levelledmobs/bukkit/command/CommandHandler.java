package io.github.arcaneplugins.levelledmobs.bukkit.command;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandAPIConfig;
import dev.jorel.commandapi.arguments.Argument;
import io.github.arcaneplugins.levelledmobs.bukkit.LevelledMobs;
import io.github.arcaneplugins.levelledmobs.bukkit.command.levelledmobs.LevelledMobsCommand;
import io.github.arcaneplugins.levelledmobs.bukkit.util.Log;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import org.bukkit.ChatColor;
import org.jetbrains.annotations.NotNull;

public final class CommandHandler {

    public static final Collection<CommandAPICommand> COMMANDS = new LinkedList<>();

    public static final CommandAPIConfig CMD_CONFIG = new CommandAPIConfig()
        .silentLogs(false); //TODO set to `true` when commands are 100% complete.

    public static String createUsageStringFromArgs(
        final @NotNull List<Argument<?>> args
    ) {
        Objects.requireNonNull(args, "args");

        final StringBuilder sb = new StringBuilder(
            ChatColor.BLUE.toString() + ChatColor.ITALIC
        );

        for(int i = 0; i < args.size(); i++) {
            final Argument<?> arg = args.get(i);
            sb.append("<")
                .append(arg.getNodeName())
                .append(">");
            if(i < args.size() - 1) sb.append(" ");
        }

        return sb.toString();
    }

    public void load(final LoadingStage loadingStage) {
        switch(loadingStage) {
            case ON_LOAD -> {
                Log.inf("Loading commands");

                CommandAPI.onLoad(CMD_CONFIG);

                /*
                add command objects to commands list
                 */
                COMMANDS.add(LevelledMobsCommand.createInstance());

                /*
                register commands in commands list
                 */
                COMMANDS.forEach(CommandAPICommand::register);
            }
            case ON_ENABLE -> {
                Log.inf("Enabling commands");
                CommandAPI.onEnable(LevelledMobs.getInstance());
            }
            case ON_DISABLE -> {
                Log.inf("Unregistering commands");
                unregisterCommands();
            }
        }
    }

    private void unregisterCommands() {
        COMMANDS.forEach(cmd -> CommandAPI.unregister(cmd.getName()));
    }

    public enum LoadingStage {
        ON_LOAD,
        ON_ENABLE,
        ON_DISABLE
    }

}
