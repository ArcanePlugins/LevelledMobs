package io.github.arcaneplugins.levelledmobs.commands

import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.executors.CommandArguments
import dev.jorel.commandapi.executors.CommandExecutor
import io.github.arcaneplugins.levelledmobs.LevelledMobs
import io.github.arcaneplugins.levelledmobs.commands.subcommands.DebugSubcommand
import io.github.arcaneplugins.levelledmobs.commands.subcommands.InfoSubcommand
import io.github.arcaneplugins.levelledmobs.commands.subcommands.ReloadSubcommand
import io.github.arcaneplugins.levelledmobs.commands.subcommands.RulesSubcommand
import io.github.arcaneplugins.levelledmobs.commands.subcommands.SpawnerEggCommand
import io.github.arcaneplugins.levelledmobs.commands.subcommands.SpawnerSubcommand
import io.github.arcaneplugins.levelledmobs.commands.subcommands.SummonSubcommand
import io.github.arcaneplugins.levelledmobs.util.Utils
import java.util.function.Consumer
import org.bukkit.command.CommandSender


/**
 * This class handles the command execution of '/levelledmobs'.
 *
 * @author lokka30
 * @since 2.4.0
 */
object LevelledMobsCommand {
    fun createInstance(): CommandAPICommand {
        return CommandAPICommand("LevelledMobs")
            .withSubcommands(
                DebugSubcommand.createInstance(),
                InfoSubcommand.createInstance(),
                ReloadSubcommand.createInstance(),
                RulesSubcommand.createInstance(),
                SpawnerEggCommand.createInstance(),
                SpawnerSubcommand.createInstance(),
                SummonSubcommand.createInstance()
            )
            .withAliases("lm", "lvlmobs", "leveledmobs")
            .withPermission("levelledmobs.command.levelledmobs")
            .withShortDescription("Manage the LevelledMobs plugin.")
            .withFullDescription("Manage the LevelledMobs plugin, from re-loading the "
                    + "configuration to creating a levelled mob spawn egg item with your "
                    + "chosen specifications.")
            .executes(CommandExecutor { sender: CommandSender, _: CommandArguments ->
                var mainUsage = LevelledMobs.instance.messagesCfg.getStringList("command.levelledmobs.main-usage")
                mainUsage = Utils.replaceAllInList(mainUsage, "%prefix%", LevelledMobs.instance.configUtils.prefix)
                mainUsage = Utils.replaceAllInList(mainUsage, "%label%", "")
                mainUsage = Utils.colorizeAllInList(mainUsage)
                mainUsage.forEach(Consumer { message: String ->
                    sender.sendMessage(message)
                })
            })
    }
}