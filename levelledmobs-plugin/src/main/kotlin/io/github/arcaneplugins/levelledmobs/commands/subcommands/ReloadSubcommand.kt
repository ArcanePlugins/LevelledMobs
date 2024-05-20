package io.github.arcaneplugins.levelledmobs.commands.subcommands

import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.executors.CommandArguments
import dev.jorel.commandapi.executors.CommandExecutor
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
object ReloadSubcommand {
    fun createInstance(): CommandAPICommand{
        return CommandAPICommand("reload")
            .withPermission("levelledmobs.command.reload")
            .withShortDescription("Reloads LM config files.")
            .withFullDescription("Reloads LevelledMobs config files.")
            .executes(CommandExecutor { sender: CommandSender, _: CommandArguments ->
                val main = LevelledMobs.instance
                main.reloadLM(sender)

                if (main.mainCompanion.hadRulesLoadError && sender is Player) {
                    sender.sendMessage(FileLoader.getFileLoadErrorMessage())
                }
            })
    }
}