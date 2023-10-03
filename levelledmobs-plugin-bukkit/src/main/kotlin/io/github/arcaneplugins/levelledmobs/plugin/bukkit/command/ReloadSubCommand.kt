package io.github.arcaneplugins.levelledmobs.plugin.bukkit.command

import io.github.arcaneplugins.levelledmobs.plugin.bukkit.LevelledMobs
import java.util.Collections
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter

class ReloadSubCommand(private val commands: Commands) : CommandExecutor, TabCompleter {
    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>?
    ): Boolean {
        if (!commands.checkPermission(sender, "levelledmobs.command.reload")){
            return true
        }

        LevelledMobs.lmInstance.reload()

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