package io.github.arcaneplugins.levelledmobs.bukkit.command.levelledmobs.subcommand.routine;

import dev.jorel.commandapi.CommandAPICommand;

public final class HaltRoutine {

    public static CommandAPICommand createInstance() {
        return new CommandAPICommand("halt")
            .withShortDescription("Prevents any configured LM Functions from triggering.")
            .withPermission("levelledmobs.command.levelledmobs.routine.halt")
            .executes((sender, args) -> {
                //TODO implement. basically this will toggle a boolean which is checked by
                //TODO the runFunctionWithTriggers method
                //todo make sure these persist over `/lm reload` calls
                sender.sendMessage("Not implemented.");
            });
    }

}
