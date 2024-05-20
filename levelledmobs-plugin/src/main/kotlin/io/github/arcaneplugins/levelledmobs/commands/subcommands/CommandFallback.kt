package io.github.arcaneplugins.levelledmobs.commands.subcommands

import io.github.arcaneplugins.levelledmobs.LevelledMobs
import io.github.arcaneplugins.levelledmobs.misc.FileLoader
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class CommandFallback(
    commandName: String
) : Command(commandName) {
    // these will only be used if CommandAPI fails to load which is usually only
    // if this is an unsupported version of Minecraft

    override fun execute(
        sender: CommandSender,
        label: String,
        args: Array<String>
    ): Boolean {
        val main = LevelledMobs.instance

        if (!sender.hasPermission("levelledmobs.command")) {
            main.configUtils.sendNoPermissionMsg(sender)
            return true
        }

        if (args.isEmpty()){
            sender.sendMessage("Options: reload")
            return true
        }

        if ("reload".equals(args[0], ignoreCase = true)){
            main.reloadLM(sender)

            if (main.mainCompanion.hadRulesLoadError && sender is Player) {
                sender.sendMessage(FileLoader.getFileLoadErrorMessage())
            }
        }

        return true
    }

    override fun tabComplete(
        sender: CommandSender,
        label: String,
        args: Array<String>
    ): MutableList<String> {
        return if (args.size == 1)
            mutableListOf("reload")
        else
            mutableListOf()
    }
}