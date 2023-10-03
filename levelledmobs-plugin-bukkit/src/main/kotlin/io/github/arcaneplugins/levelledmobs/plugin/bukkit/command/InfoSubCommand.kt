package io.github.arcaneplugins.levelledmobs.plugin.bukkit.command

import io.github.arcaneplugins.levelledmobs.plugin.bukkit.LevelledMobs
import java.util.Collections
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter

class InfoSubCommand(private val commands: Commands) : CommandExecutor, TabCompleter {
    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>?
    ): Boolean {
        if (!commands.checkPermission(sender, "levelledmobs.command.info")){
            return true
        }

        val main = LevelledMobs.lmInstance
        val msg = "\n" +
                "LevelledMobs v${main.description.version}\n" +
                "${main.description.description}\n" +
                "\n" +
                "Maintainers: ${main.description.authors}\n" +
                "Contributors: See https://github.com/lokka30/LevelledMobs/wiki/Credits\n" +
                "Support for: MC 1.19 - 1.20\n" +
                "\n"

        sender.sendMessage(msg)

        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>?
    ): List<String> {
        return Collections.emptyList()
    }
}