package io.github.arcaneplugins.levelledmobs.plugin.bukkit.misc

import java.util.regex.Pattern
import net.md_5.bungee.api.ChatColor
import org.bukkit.Bukkit

@Suppress("DEPRECATION")
class MessageUtils {
    companion object{
        fun colorizeAll(msg: String) : String{
            return colorizeStandardCodes(colorizeHexCodes(msg))
        }

        fun colorizeHexCodes(msg: String) : String{
            return colorizeHexCodes("&#", "", msg)
        }

        fun colorizeHexCodes(
            startTag: String,
            endTag: String,
            message: String
        ) : String{
            val hexPattern = Pattern.compile("$startTag([A-Fa-f0-9]{6})$endTag")
            val matcher = hexPattern.matcher(message)
            val buffer = StringBuilder(message.length + 4 * 8)
            val colorChar = ChatColor.COLOR_CHAR

            while (matcher.find()) {
                val group = matcher.group(1)
                matcher.appendReplacement(
                    buffer, colorChar.toString() + "x"
                            + colorChar + group[0] + colorChar + group[1]
                            + colorChar + group[2] + colorChar + group[3]
                            + colorChar + group[4] + colorChar + group[5]
                )
            }

            return matcher.appendTail(buffer).toString()
        }

        fun colorizeStandardCodes(msg: String) : String{
            return if (Bukkit.getName()
                    .equals("CraftBukkit", ignoreCase = true)
            ) org.bukkit.ChatColor.translateAlternateColorCodes('&', msg) else ChatColor.translateAlternateColorCodes(
                '&',
                msg
            )
        }
    }
}