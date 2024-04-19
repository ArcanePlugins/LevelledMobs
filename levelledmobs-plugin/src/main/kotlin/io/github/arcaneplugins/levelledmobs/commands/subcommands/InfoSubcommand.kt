package io.github.arcaneplugins.levelledmobs.commands.subcommands

import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.executors.CommandArguments
import dev.jorel.commandapi.executors.CommandExecutor
import io.github.arcaneplugins.levelledmobs.LevelledMobs
import io.github.arcaneplugins.levelledmobs.commands.MessagesHelper
import org.bukkit.command.CommandSender

/**
 * Shows LevelledMobs information such as the version number
 *
 * @author lokka30
 * @since v2.0.0
 */
object InfoSubcommand {
    fun createInstance(): CommandAPICommand? {
        @Suppress("DEPRECATION")
        return CommandAPICommand("info")
            .withPermission("levelledmobs.command.info")
            .withShortDescription("View info about the installed version of the plugin.")
            .withFullDescription("View info about the installed version of the plugin.")
            .executes(CommandExecutor { sender: CommandSender, _: CommandArguments ->
                val main = LevelledMobs.instance
                val listSeparator = main.messagesCfg.getString("command.levelledmobs.info.listSeparator", "&7, &f")!!
                MessagesHelper.showMessage(
                    sender,
                    "command.levelledmobs.info.about",
                    arrayOf(
                        "%version%",
                        "%description%",
                        "%supportedVersions%",
                        "%maintainers%",
                        "%contributors%"
                    ),
                    arrayOf(
                        main.description.version,
                        main.description.description ?: "",
                        "1.19 - 1.20",
                        main.description.authors.joinToString(listSeparator),
                        "See &8&nhttps://tinyurl.com/lm-contributors"
                    )
                )
            })
    }
}