package io.github.arcaneplugins.levelledmobs.bukkit.command.levelledmobs.subcommand;

import dev.jorel.commandapi.CommandAPICommand;

public class RoutineSubcommand {

    public static final CommandAPICommand INSTANCE =
        new CommandAPICommand("routine")
            .withPermission("levelledmobs.command.levelledmobs.routine")
            .withShortDescription("Advanced users only: run the specified routine.")
            .withFullDescription("Advanced users only: run the specified routine. Routines " +
                "contain miscellaneous code which is often used during testing and when " +
                "support staff are assisting a server owner with a situation.");

}
