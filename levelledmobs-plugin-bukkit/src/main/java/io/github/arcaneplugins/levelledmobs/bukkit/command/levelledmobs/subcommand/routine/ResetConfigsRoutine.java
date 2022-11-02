package io.github.arcaneplugins.levelledmobs.bukkit.command.levelledmobs.subcommand.routine;

import dev.jorel.commandapi.CommandAPICommand;
import io.github.arcaneplugins.levelledmobs.bukkit.command.levelledmobs.subcommand.RoutineSubcommand;

@SuppressWarnings("unused")
final class ResetConfigsRoutine {

    static {
        RoutineSubcommand.INSTANCE.withSubcommand(
            new CommandAPICommand("reset-configs")
                .withShortDescription("Backup and reset config files")
                .withPermission("levelledmobs.command.levelledmobs.routine.reset-configs")
                .executes((sender, args) -> {
                    //TODO
                    sender.sendMessage("Not implemented");
                })
        );
    }

}
