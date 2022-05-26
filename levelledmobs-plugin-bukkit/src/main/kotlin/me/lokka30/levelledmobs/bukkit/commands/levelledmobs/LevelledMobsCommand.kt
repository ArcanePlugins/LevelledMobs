package me.lokka30.levelledmobs.bukkit.commands.levelledmobs

import me.lokka30.levelledmobs.bukkit.LevelledMobs.Companion.prefixSev
import me.lokka30.levelledmobs.bukkit.LevelledMobs.Companion.prefixInf
import me.lokka30.levelledmobs.bukkit.commands.BaseCommandWrapper
import me.lokka30.levelledmobs.bukkit.commands.levelledmobs.subcommands.AboutSubcommand
import me.lokka30.levelledmobs.bukkit.commands.levelledmobs.subcommands.SummonSubcommand
import org.bukkit.ChatColor.AQUA
import org.bukkit.ChatColor.BOLD
import org.bukkit.ChatColor.DARK_GRAY
import org.bukkit.ChatColor.GRAY
import org.bukkit.command.CommandSender

/*
[description]
    This class contains the code which handles the `/levelledmobs` command, the primary command in
    LevelledMobs which allows the plugin to be managed. There are several subcommands which cater to
    various different functions that server administrators may wish to use, such as creating custom
    spawners and reloading the configuration files.

[command structure]
    index.. 0   1
    size... 1   2
            :   :
      cmd.. /lm |
         .. /lm <subcommand>
 */
class LevelledMobsCommand : BaseCommandWrapper() {

    companion object {
        val subcommands = mutableSetOf(
            AboutSubcommand(),
            SummonSubcommand()
        )

        val labels = mutableSetOf(
            "levelledmobs",
            "leveledmobs",
            "lvlmobs",
            "lm"
        )
    }

    override fun labels(): MutableSet<String> {
        return labels
    }

    /*
    This builds the usage message depending on which subcommands are currently available.
     */
    override fun usage(): String {
        val usage = StringBuilder("/lm <")

        val subcommandMainLabels = mutableListOf<String>()
        subcommands.forEach { subcommandMainLabels.add(it.labels().elementAt(0)) }
        usage.append(subcommandMainLabels.joinToString("/"))
        usage.append(">")

        return usage.toString()
    }

    override fun run(sender: CommandSender, args: Array<String>) {
        if (!hasPerm(sender, "levelledmobs.command.levelledmobs", true)) return

        // make sure the user specified a command
        if (args.size <= 1) {
            sender.sendMessage(
                """
                ${AQUA}${BOLD}LevelledMobs 4${DARK_GRAY} | ${GRAY}The Ultimate RPG Mob Levelling Solution
                $DARK_GRAY • ${GRAY}For a list of available commands, run '${AQUA}/lm help${GRAY}'.
                $DARK_GRAY • ${GRAY}To learn more about LevelledMobs, run '${AQUA}/lm about${GRAY}'.
                """.trimIndent()
            )
            return
        }

        val label = args[1].lowercase()

        subcommands.forEach {
            if (it.labels().contains(label)) {
                it.run(sender, args)
                return
            }
        }

        sender.sendMessage(
            """
            ${prefixSev}Invalid subcommand '${AQUA}${args[1]}${GRAY}'.
            ${prefixInf}Run '${AQUA}/lm help${GRAY}' for a list of available commands.
            """.trimIndent()
        )
    }

    override fun suggest(sender: CommandSender, args: Array<String>): MutableList<String> {
        if (!hasPerm(sender, "levelledmobs.command.levelledmobs", false))
            return mutableListOf()

        if (args.size < 2) {
            return mutableListOf()
        } else if (args.size == 2) {
            val suggestions = mutableListOf<String>()

            subcommands.forEach {
                // we don't want to show all labels - only the base label.
                suggestions.add(it.labels().elementAt(0))
            }

            return suggestions
        } else {
            val label = args[1].lowercase()

            subcommands.forEach {
                if (it.labels().contains(label)) {
                    /*
                    Note: LevelledMobs does not care if the sender is able to access the suggested
                    subcommands or not. We don't want to make excess calls to the permission plugin
                    since these calls are ran on the main thread. This can be changed once Treasury
                    offers a permissions API, which will allow asynchronous permission checking.
                     */
                    return it.suggest(sender, args)
                }
            }

            return mutableListOf()
        }
    }

}