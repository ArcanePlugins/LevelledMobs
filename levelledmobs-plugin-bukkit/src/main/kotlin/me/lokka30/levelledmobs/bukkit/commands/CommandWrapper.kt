package me.lokka30.levelledmobs.bukkit.commands

import org.bukkit.ChatColor
import org.bukkit.command.CommandSender

/*
This abstract class is a wrapper for Bukkit's TabExecutor, which makes writing commands easier,
without giving up the high degree of logic control which TabExecutor provides (unlike what most
'command framework' libraries do).
 */
abstract class CommandWrapper {

    /*
    Get the list of labels that this command can be ran by. This includes the base command (e.g.
    `/levelledmobs`), and all aliases (e.g. `/lvlmobs`, `/leveledmobs`, `/lm`).

    The base label is the first entry. It must be present.
     */
    abstract fun labels(): MutableSet<String>

    /*
    Get the usage message for the command, which shows what parameters should be specified to
    successfully run it.
     */
    abstract fun usage(): String

    /*
    Make the CommandSender run the command with given args.

    Remember to factor in whether the sender has the required permission(s).
     */
    abstract fun run(
        sender: CommandSender,
        args: Array<String>
    )

    /*
    Generate a list of suggestions for the CommandSender's current context. For example, if they
    have typed `/lm`, suggest them a list of subcommands which they can run.

    Remember to factor in whether the sender has the required permission(s).
     */
    open fun suggest(
        sender: CommandSender,
        args: Array<String>
    ): MutableList<String> {
        return mutableListOf()
    }

    /*
    Check if the sender has the given permission. If warn is enabled, then the sender will be warned
    about their inability to access something if they lack the permission.
     */
    fun hasPerm(
        sender: CommandSender,
        permission: String,
        warn: Boolean
    ): Boolean {
        if (sender.hasPermission(permission))
            return true

        if (warn) {
            // FIXME Make this message customisable.
            sender.sendMessage("${ChatColor.RED}You don't have access to that. [Permission " +
                    "required: '${permission}']")
        }

        return false
    }

}