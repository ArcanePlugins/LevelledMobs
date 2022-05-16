package me.lokka30.levelledmobs.bukkit.commands.levelledmobs.subcommands

import me.lokka30.levelledmobs.bukkit.LevelledMobs
import me.lokka30.levelledmobs.bukkit.commands.CommandWrapper
import org.bukkit.ChatColor.AQUA
import org.bukkit.ChatColor.BOLD
import org.bukkit.ChatColor.DARK_GRAY
import org.bukkit.ChatColor.GRAY
import org.bukkit.ChatColor.RED
import org.bukkit.command.CommandSender

/*
[command structure]
    index.. 0   1
    size... 1   2
            :   :
          - /lm |
          - /lm about
 */
class AboutSubcommand : CommandWrapper() {

    companion object {
        const val usage = "/lm about"

        val labels = mutableSetOf(
            "about",
            "info",
            "information"
        )
    }

    override fun labels(): MutableSet<String> {
        return labels
    }

    override fun usage(): String {
        return usage
    }

    override fun run(
        sender: CommandSender,
        args: Array<String>
    ) {
        if (!hasPerm(sender, "levelledmobs.command.levelledmobs.about", true)) return

        if (args.size != 2) {
            // FIXME Make this message customizable.
            sender.sendMessage("${RED}Error: ${GRAY}Invalid usage; try '${AQUA}${usage}${GRAY}'.")
            return
        }

        // FIXME Make this message customizable.
        val pdf = LevelledMobs.instance!!.description
        sender.sendMessage(
            """
                
            ${AQUA}${BOLD}LevelledMobs${AQUA} v${pdf.version}
            
            ${GRAY}Spigot: ${DARK_GRAY}https://spigotmc.org/resources/74304
            ${GRAY}Wiki: ${DARK_GRAY}https://github.com/lokka30/LevelledMobs/Wiki
            ${GRAY}Source: ${DARK_GRAY}https://github.com/lokka30/LevelledMobs
            ${GRAY}Credits: ${DARK_GRAY}https://github.com/lokka30/LevelledMobs/wiki/Credits
            
            """.trimIndent()
        )
    }

}