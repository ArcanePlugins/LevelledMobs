package io.github.arcaneplugins.levelledmobs.bukkit.command.levelledmobs.subcommand.routine;

import dev.jorel.commandapi.CommandAPICommand;
import io.github.arcaneplugins.levelledmobs.bukkit.command.levelledmobs.subcommand.RoutineSubcommand;

@SuppressWarnings("unused")
public class HaltRoutine {

    static {
        RoutineSubcommand.INSTANCE.withSubcommands(
            new CommandAPICommand("halt")
                .withShortDescription("Prevents any configured Functions from triggering")
                .withPermission("levelledmobs.command.levelledmobs.routine.halt")
                .executes((sender, args) -> {
                    //TODO implement
                })
        );
    }
}
