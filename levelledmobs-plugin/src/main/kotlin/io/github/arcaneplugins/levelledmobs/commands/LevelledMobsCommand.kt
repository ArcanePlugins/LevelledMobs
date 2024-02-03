package io.github.arcaneplugins.levelledmobs.commands

import java.util.LinkedList
import java.util.Locale
import java.util.function.Consumer
import io.github.arcaneplugins.levelledmobs.LevelledMobs
import io.github.arcaneplugins.levelledmobs.commands.subcommands.DebugSubcommand
import io.github.arcaneplugins.levelledmobs.commands.subcommands.InfoSubcommand
import io.github.arcaneplugins.levelledmobs.commands.subcommands.KillSubcommand
import io.github.arcaneplugins.levelledmobs.commands.subcommands.ReloadSubcommand
import io.github.arcaneplugins.levelledmobs.commands.subcommands.RulesSubcommand
import io.github.arcaneplugins.levelledmobs.commands.subcommands.SpawnerEggCommand
import io.github.arcaneplugins.levelledmobs.commands.subcommands.SpawnerSubcommand
import io.github.arcaneplugins.levelledmobs.commands.subcommands.SummonSubcommand
import io.github.arcaneplugins.levelledmobs.util.PaperUtils
import io.github.arcaneplugins.levelledmobs.util.SpigotUtils
import io.github.arcaneplugins.levelledmobs.util.Utils
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.ConsoleCommandSender
import org.bukkit.command.TabCompleter

/**
 * This class handles the command execution of '/levelledmobs'.
 *
 * @author lokka30
 * @since 2.4.0
 */
class LevelledMobsCommand : CommandExecutor, TabCompleter {
    // Retain alphabetical order please.
    private val debugSubcommand = DebugSubcommand()
    private val infoSubcommand = InfoSubcommand()
    private val killSubcommand = KillSubcommand()
    private val reloadSubcommand = ReloadSubcommand()
    val rulesSubcommand = RulesSubcommand()
    private val spawnerEggCommand = SpawnerEggCommand()
    val spawnerSubCommand = SpawnerSubcommand()
    private val summonSubcommand = SummonSubcommand()

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<String>
    ) : Boolean{
        if (sender.hasPermission("levelledmobs.command")) {
            if (args.isEmpty()) {
                sendMainUsage(sender, label)
            } else {
                when (args[0].lowercase(Locale.getDefault())) {
                    "debug" -> debugSubcommand.parseSubcommand(sender, label, args)
                    "egg" -> spawnerEggCommand.parseSubcommand(sender, label, args)
                    "help" -> showHelp(sender)
                    "info" -> infoSubcommand.parseSubcommand(sender, label, args)
                    "kill" -> killSubcommand.parseSubcommand(sender, label, args)
                    "reload" -> reloadSubcommand.parseSubcommand(sender, label, args)
                    "rules" -> rulesSubcommand.parseSubcommand(sender, label, args)
                    "spawner" -> spawnerSubCommand.parseSubcommand(sender, label, args)
                    "summon" -> summonSubcommand.parseSubcommand(sender, label, args)
                    else -> sendMainUsage(sender, label)
                }
            }
        } else {
            LevelledMobs.instance.configUtils.sendNoPermissionMsg(sender)
        }
        return true
    }

    private fun sendMainUsage(sender: CommandSender, label: String) {
        var mainUsage = LevelledMobs.instance.messagesCfg.getStringList("command.levelledmobs.main-usage")
        mainUsage = Utils.replaceAllInList(mainUsage, "%prefix%", LevelledMobs.instance.configUtils.prefix)
        mainUsage = Utils.replaceAllInList(mainUsage, "%label%", label)
        mainUsage = Utils.colorizeAllInList(mainUsage)
        mainUsage.forEach(Consumer { s: String? -> sender.sendMessage(s!!) })
    }

    private fun showHelp(sender: CommandSender) {
        val message = "Click here to open the wiki FAQ"
        val url = "https://tinyurl.com/yc8xds5a"

        if (sender is ConsoleCommandSender) {
            // hyperlinks don't work on console
            sender.sendMessage("${message}: $url")
            return
        }

        if (LevelledMobs.instance.ver.isRunningPaper) {
            PaperUtils.sendHyperlink(sender, message, url)
        } else {
            SpigotUtils.sendHyperlink(sender, message, url)
        }
    }

    // Retain alphabetical order please.
    private val commandsToCheck = listOf(
        "debug", "egg", "help",
        "info", "kill", "reload", "rules", "spawner", "summon"
    )

    override fun onTabComplete(
        sender: CommandSender,
        cmd: Command,
        alias: String,
        args: Array<String>
    ): MutableList<String>? {
        if (args.size == 1) {
            val suggestions: MutableList<String> = LinkedList()

            commandsToCheck.forEach(Consumer { command: String ->
                if (sender.hasPermission("levelledmobs.command.$command")) {
                    suggestions.add(command)
                }
            })

            return suggestions
        } else {
            return when (args[0].lowercase()) {
                "kill" -> killSubcommand.parseTabCompletions(sender, args)
                "rules" -> rulesSubcommand.parseTabCompletions(sender, args)
                "spawner" -> spawnerSubCommand.parseTabCompletions(sender, args)
                "summon" -> summonSubcommand.parseTabCompletions(sender, args)
                "egg" -> spawnerEggCommand.parseTabCompletions(sender, args)
                "debug" -> debugSubcommand.parseTabCompletions(sender, args)
                else -> mutableListOf()
            }
        }
    }
}