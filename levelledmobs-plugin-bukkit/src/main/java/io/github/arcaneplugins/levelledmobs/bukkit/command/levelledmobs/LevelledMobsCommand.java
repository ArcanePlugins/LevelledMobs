package io.github.arcaneplugins.levelledmobs.bukkit.command.levelledmobs;

import dev.jorel.commandapi.CommandAPICommand;
import io.github.arcaneplugins.levelledmobs.bukkit.command.levelledmobs.subcommand.AboutSubcommand;
import io.github.arcaneplugins.levelledmobs.bukkit.command.levelledmobs.subcommand.BackupSubcommand;
import io.github.arcaneplugins.levelledmobs.bukkit.command.levelledmobs.subcommand.ConfirmSubcommand;
import io.github.arcaneplugins.levelledmobs.bukkit.command.levelledmobs.subcommand.EggSubcommand;
import io.github.arcaneplugins.levelledmobs.bukkit.command.levelledmobs.subcommand.KillSubcommand;
import io.github.arcaneplugins.levelledmobs.bukkit.command.levelledmobs.subcommand.ReloadSubcommand;
import io.github.arcaneplugins.levelledmobs.bukkit.command.levelledmobs.subcommand.RoutineSubcommand;
import io.github.arcaneplugins.levelledmobs.bukkit.command.levelledmobs.subcommand.SpawnerSubcommand;
import io.github.arcaneplugins.levelledmobs.bukkit.command.levelledmobs.subcommand.SummonSubcommand;
import java.util.Collection;
import java.util.List;

public class LevelledMobsCommand {

    public static final Collection<CommandAPICommand> SUBCOMMANDS = List.of(
        AboutSubcommand.INSTANCE,
        BackupSubcommand.INSTANCE,
        ConfirmSubcommand.INSTANCE,
        EggSubcommand.INSTANCE,
        KillSubcommand.INSTANCE,
        ReloadSubcommand.INSTANCE,
        RoutineSubcommand.INSTANCE,
        SpawnerSubcommand.INSTANCE,
        SummonSubcommand.INSTANCE
    );

    public static final CommandAPICommand INSTANCE =
        new CommandAPICommand("levelledmobs")
            .withSubcommands(SUBCOMMANDS.toArray(new CommandAPICommand[0]))
            .withAliases("lm", "lvlmobs", "leveledmobs")
            .withPermission("levelledmobs.command.levelledmobs")
            .withShortDescription("Manage the LevelledMobs plugin.")
            .withFullDescription("Manage the LevelledMobs plugin, from re-loading the "
                + "configuration to creating a levelled mob spawn egg item with your "
                + "chosen specifications.");

}
