package io.github.arcaneplugins.levelledmobs.bukkit.command.levelledmobs.subcommand;

import dev.jorel.commandapi.CommandAPICommand;
import io.github.arcaneplugins.levelledmobs.bukkit.command.levelledmobs.subcommand.routine.CompatibilityRoutine;
import io.github.arcaneplugins.levelledmobs.bukkit.command.levelledmobs.subcommand.routine.HaltRoutine;
import io.github.arcaneplugins.levelledmobs.bukkit.command.levelledmobs.subcommand.routine.ReloadCommandsRoutine;
import io.github.arcaneplugins.levelledmobs.bukkit.command.levelledmobs.subcommand.routine.ResetConfigsRoutine;
import io.github.arcaneplugins.levelledmobs.bukkit.command.levelledmobs.subcommand.routine.TestRoutine;
import io.github.arcaneplugins.levelledmobs.bukkit.command.levelledmobs.subcommand.routine.UnlevelAllRoutine;

public final class RoutineSubcommand {

    public static CommandAPICommand createInstance() {
        return new CommandAPICommand("routine")
            .withPermission("levelledmobs.command.levelledmobs.routine")
            .withShortDescription("[Advanced Users Only] Run the specified routine.")
            .withFullDescription("[Advanced Users Only] Run the specified routine. This is a " +
                "potentially dangerous command (depending on which routine is used). You are " +
                "advised to not use this without the instruction of a LevelledMobs maintainer.")
            .withSubcommands(
                CompatibilityRoutine.createInstance(),
                HaltRoutine.createInstance(),
                ReloadCommandsRoutine.createInstance(),
                ResetConfigsRoutine.createInstance(),
                TestRoutine.createInstance(),
                UnlevelAllRoutine.createInstance1(),
                UnlevelAllRoutine.createInstance2()
            );

        // Note: Yes, there is meant to not be any 'executes' method. This is subcommand-based. :)
    }

}
