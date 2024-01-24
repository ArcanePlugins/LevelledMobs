package io.github.arcaneplugins.levelledmobs.commands.subcommands

import io.github.arcaneplugins.levelledmobs.LevelledMobs
import io.github.arcaneplugins.levelledmobs.misc.FileLoader
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

/**
 * Reloads all LevelledMobs configuration from disk
 *
 * @author lokka30
 * @since 2.0
 */
class ReloadSubcommand : Subcommand {
    override fun parseSubcommand(
        sender: CommandSender,
        label: String,
        args: Array<String>
    ) {
        val main = LevelledMobs.instance
        if (!sender.hasPermission("levelledmobs.command.reload")) {
            main.configUtils.sendNoPermissionMsg(sender)
            return
        }

        main.reloadLM(sender)

        if (main.companion.hadRulesLoadError && sender is Player) {
            sender.sendMessage(FileLoader.getFileLoadErrorMessage())
        }
    }

    override fun parseTabCompletions(
        sender: CommandSender,
        args: Array<String>
    ): List<String> {
        return mutableListOf()
    }
}