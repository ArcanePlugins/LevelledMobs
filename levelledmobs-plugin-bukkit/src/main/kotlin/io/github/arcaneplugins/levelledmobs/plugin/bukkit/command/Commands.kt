package io.github.arcaneplugins.levelledmobs.plugin.bukkit.command

import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter

import java.util.Locale
import java.util.Collections

class Commands : CommandExecutor, TabCompleter {
    private val infoSubCommand = InfoSubCommand(this)
    private val reloadSubCommand = ReloadSubCommand(this)

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>?
    ): Boolean {
        if (!sender.hasPermission("levelledmobs.command")){
            sendNoPermissionMsg(sender)
        }

        if (args == null || args.isEmpty()){
            showUsage(sender)
        }
        else {
            when (args[0].lowercase(Locale.getDefault())) {
                "info" -> infoSubCommand.onCommand(sender, command, label, args)
                "reload" -> reloadSubCommand.onCommand(sender, command, label, args)
                else -> showUsage(sender)
            }
        }

        return true
    }

    fun checkPermission(sender: CommandSender, permission: String) : Boolean{
        return if (sender.hasPermission(permission)){
            true
        } else{
            sendNoPermissionMsg(sender)
            false
        }
    }

    private fun sendNoPermissionMsg(sender: CommandSender){
        sender.sendMessage("You don't have access to that.")
    }

    private fun showUsage(sender: CommandSender){
        // todo: update this message
        sender.sendMessage("This is the LevelledMobs command summary (todo)")
    }

    private val commandsToCheck = listOf("info", "reload")

    //todo: the final command set should include all of these:
    //private val commandsToCheck = listOf("debug", "egg", "help",
    //    "info", "kill", "reload", "rules", "spawner", "summon")

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>?
    ): List<String> {
        if (args == null || args.isEmpty() || args.size == 1){
            val suggestions = arrayListOf<String>()

            commandsToCheck.forEach {cmd ->
                if (sender.hasPermission("levelledmobs.command.${cmd}")){
                    suggestions.add(cmd)
                }
            }

            return suggestions
        }

        // the missing subcommands don't have tab completions, don't bother including them.
        return Collections.emptyList()
    }
}