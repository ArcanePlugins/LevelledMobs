package io.github.arcaneplugins.levelledmobs.bukkit.command.levelledmobs;

import dev.jorel.commandapi.CommandAPICommand;
import io.github.arcaneplugins.levelledmobs.bukkit.command.levelledmobs.subcommand.AboutSubcommand;
import io.github.arcaneplugins.levelledmobs.bukkit.command.levelledmobs.subcommand.BackupSubcommand;
import io.github.arcaneplugins.levelledmobs.bukkit.command.levelledmobs.subcommand.ConfirmSubcommand;
import io.github.arcaneplugins.levelledmobs.bukkit.command.levelledmobs.subcommand.EggSubcommand;
import io.github.arcaneplugins.levelledmobs.bukkit.command.levelledmobs.subcommand.HelpSubcommand;
import io.github.arcaneplugins.levelledmobs.bukkit.command.levelledmobs.subcommand.KillSubcommand;
import io.github.arcaneplugins.levelledmobs.bukkit.command.levelledmobs.subcommand.ReloadSubcommand;
import io.github.arcaneplugins.levelledmobs.bukkit.command.levelledmobs.subcommand.RoutineSubcommand;
import io.github.arcaneplugins.levelledmobs.bukkit.command.levelledmobs.subcommand.SpawnerSubcommand;
import io.github.arcaneplugins.levelledmobs.bukkit.command.levelledmobs.subcommand.SummonSubcommand;

public final class LevelledMobsCommand {

    public static CommandAPICommand createInstance() {
        return new CommandAPICommand("levelledmobs")
            .withSubcommands(
                AboutSubcommand.createInstance(),
                BackupSubcommand.createInstance(),
                ConfirmSubcommand.createInstance(),
                EggSubcommand.createInstance(),
                KillSubcommand.createInstance1(),
                KillSubcommand.createInstance2(),
                ReloadSubcommand.createInstance(),
                RoutineSubcommand.createInstance(),
                SpawnerSubcommand.createInstance(),
                SummonSubcommand.createInstance()
            )
            .withSubcommands(HelpSubcommand.createInstances().toArray(new CommandAPICommand[0]))
            .withAliases("lm", "lvlmobs", "leveledmobs")
            .withPermission("levelledmobs.command.levelledmobs")
            .withShortDescription("Manage the LevelledMobs plugin.")
            .withFullDescription("Manage the LevelledMobs plugin, from re-loading the "
                + "configuration to creating a levelled mob spawn egg item with your "
                + "chosen specifications.");
    }

}
