package io.github.arcaneplugins.levelledmobs.bukkit.command.levelledmobs.subcommand;

import dev.jorel.commandapi.CommandAPICommand;

public class RoutineSubcommand {

    public static final CommandAPICommand INSTANCE =
        new CommandAPICommand("routine")
            .withPermission("levelledmobs.command.levelledmobs.routine")
            .executes((sender, args) -> {
                sender.sendMessage("routine not implemented");
                //TODO impl
            });

}
