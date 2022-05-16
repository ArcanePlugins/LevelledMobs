package me.lokka30.levelledmobs.bukkit.commands

import me.lokka30.levelledmobs.bukkit.LevelledMobs
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor

/*
This is a CommandWrapper variant for base commands only (i.e.,
`/lm`, but not `/lm about`: `about` is a subcommand).
 */
abstract class BaseCommandWrapper : CommandWrapper(), TabExecutor {

    /*
    Wrap over TabExecutor's onCommand function
     */
    override fun onCommand(
        sender: CommandSender,
        cmd: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        run(sender, bukkitArgsToWrapperArgs(label, args))
        return true
    }

    /*
    Wrap over TabExecutor's onTabComplete function
     */
    override fun onTabComplete(
        sender: CommandSender,
        cmd: Command,
        label: String,
        args: Array<out String>
    ): MutableList<String>? {
        return suggest(sender, bukkitArgsToWrapperArgs(label, args))
    }

    /*
    Converts Bukkit-style args to CommandWrapper-style args.

    CommandWrapper args have the label as the first element, and the bukkitArgs as the rest of the
    elements.
     */
    fun bukkitArgsToWrapperArgs(label: String, bukkitArgs: Array<out String>): Array<String> {
        val args = mutableListOf(label)
        args.addAll(bukkitArgs)
        return args.toTypedArray()
    }

    /*
    Registers the command to the server, if it is a base command. If this command is not a base
    command, then it does nothing.
     */
    fun register() {
        LevelledMobs.instance!!.getCommand(labels().elementAt(0))!!.setExecutor(this)
    }

}