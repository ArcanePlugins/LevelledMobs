package io.github.arcaneplugins.levelledmobs.commands.subcommands

import io.github.arcaneplugins.levelledmobs.LevelledMobs
import io.github.arcaneplugins.levelledmobs.commands.MessagesBase
import org.bukkit.command.CommandSender

/**
 * Shows LevelledMobs information such as the version number
 *
 * @author lokka30
 * @since v2.0.0
 */
class InfoSubcommand : MessagesBase(), Subcommand {
    override fun parseSubcommand(
        sender: CommandSender,
        label: String,
        args: Array<String>
    ) {
        commandSender = sender
        messageLabel = label

        val main = LevelledMobs.instance
        if (!sender.hasPermission("levelledmobs.command.info")) {
            main.configUtils.sendNoPermissionMsg(sender)
            return
        }

        if (args.size == 1) {
            val listSeparator = main.messagesCfg.getString("command.levelledmobs.info.listSeparator", "&7, &f")!!

            showMessage(
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
                    "1.16 - 1.20",
                    java.lang.String.join(listSeparator, main.description.authors),
                    "See &8&nhttps://github.com/lokka30/LevelledMobs/wiki/Credits"
                )
            )
        } else {
            showMessage("command.levelledmobs.info.usage")
        }
    }

    override fun parseTabCompletions(
        sender: CommandSender,
        args: Array<String>
    ): List<String> {
        // This subcommand has no tab completions.
        return emptyList()
    }
}