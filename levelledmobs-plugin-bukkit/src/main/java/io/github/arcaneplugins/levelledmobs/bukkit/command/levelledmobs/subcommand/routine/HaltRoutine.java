package io.github.arcaneplugins.levelledmobs.bukkit.command.levelledmobs.subcommand.routine;

import dev.jorel.commandapi.CommandAPICommand;

public final class HaltRoutine {

    public static CommandAPICommand createInstance() {
        return new CommandAPICommand("halt")
            .withShortDescription("Prevents any configured Functions from triggering")
            .withPermission("levelledmobs.command.levelledmobs.routine.halt")
            .executes((sender, args) -> {
                //TODO implement
                sender.sendMessage("Not implemented.");
            });
    }

}
