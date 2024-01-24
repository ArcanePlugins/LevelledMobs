package io.github.arcaneplugins.levelledmobs.commands.subcommands

import org.bukkit.command.CommandSender

/**
 * Provides the interface for parsing commands sent to LevelledMobs
 *
 * @author lokka30
 * @since 2.4.0
 */
interface Subcommand {
    fun parseSubcommand(
        sender: CommandSender,
        label: String,
        args: Array<String>
    )

    fun parseTabCompletions(
        sender: CommandSender,
        args: Array<String>
    ): List<String>?
}