package io.github.arcaneplugins.levelledmobs.commands.subcommands

import com.mojang.brigadier.Command
import com.mojang.brigadier.tree.LiteralCommandNode
import io.github.arcaneplugins.levelledmobs.LevelledMobs
import io.github.arcaneplugins.levelledmobs.commands.MessagesHelper
import io.papermc.paper.command.brigadier.CommandSourceStack
import org.bukkit.command.CommandSender

/**
 * Shows LevelledMobs information such as the version number
 *
 * @author lokka30
 * @since v2.0.0
 */
object InfoSubcommand : CommandBase("levelledmobs.command.info") {
    override val description = "View info about the installed version of the plugin."

    fun buildCommand() : LiteralCommandNode<CommandSourceStack>{
        return createLiteralCommand("info")
            .executes { ctx ->
                showInfo(ctx.source.sender)
                return@executes Command.SINGLE_SUCCESS
            }
            .build()
    }

    fun showInfo(sender: CommandSender){
        val main = LevelledMobs.instance
        val listSeparator = main.messagesCfg.getString("command.levelledmobs.info.listSeparator", "&7, &f")!!
        MessagesHelper.showMessage(sender,
            "command.levelledmobs.info.about",
            mutableListOf(
                "%version%",
                "%description%",
                "%supportedVersions%",
                "%maintainers%",
                "%contributors%"
            ),
            mutableListOf(
                main.description.version,
                main.description.description ?: "",
                "1.21",
                main.description.authors.joinToString(listSeparator),
                "See &8&nhttps://tinyurl.com/lm-contributors"
            )
        )
    }
}