package io.github.arcaneplugins.levelledmobs.bukkit.command.levelledmobs.subcommand.routine;

import dev.jorel.commandapi.CommandAPICommand;

public final class ResetConfigsRoutine {

    public static CommandAPICommand createInstance() {
        return new CommandAPICommand("reset-configs")
            .withShortDescription("Backup and reset config files")
            .withPermission("levelledmobs.command.levelledmobs.routine.reset-configs")
            .executes((sender, args) -> {
                //TODO
                sender.sendMessage("Not implemented");
            });
    }

}
