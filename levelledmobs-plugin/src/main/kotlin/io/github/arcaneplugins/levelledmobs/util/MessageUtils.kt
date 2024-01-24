@file:Suppress("DEPRECATION")

package io.github.arcaneplugins.levelledmobs.util

import java.util.regex.Pattern
import io.github.arcaneplugins.levelledmobs.LevelledMobs
import net.md_5.bungee.api.ChatColor
import org.bukkit.Bukkit

/**
 * This class contains a bunch of methods which
 * make it very easy to translate '&'-based color
 * codes in messages. You can colorize standard codes
 * (&a, &b, &1, &2, etc), and even hex codes (&#abccdef),
 * and also both in one method :)
 *
 * @author lokka30, Sullivan_Bognar, imDaniX
 * @since 1.0.0
 */
@SuppressWarnings("deprecation")
object MessageUtils {
    /**
     * Colorize a message, using '&' color codes - e.g. '&a' for ChatColor.GREEN.
     * If the server is 1.16 or newer, then it will also translate hex codes - e.g. '&#abcdef'.
     *
     * @param msg the message to translate color codes from.
     * @return the color-translated message.
     * @author lokka30
     * @see MessageUtils#colorizeHexCodes(String)
     * @see MessageUtils#colorizeStandardCodes(String)
     * @since unknown
     */
    fun colorizeAll(
        msg: String?
    ): String {
        if (msg == null) return ""
        return colorizeStandardCodes(colorizeHexCodes(msg))
    }

    /**
     * This defaults the 'startTag' to '&#' and endTag to '' (nothing) to colorizeHexCodes.
     *
     * @param msg message to translate
     * @return the translated string
     * @author lokka30
     * @see MessageUtils#colorizeHexCodes(String, String, String)
     * @since unknown
     */
    fun colorizeHexCodes(
        msg: String
    ): String {
        return colorizeHexCodes("&#", "", msg)
    }

    fun colorizeHexCodes(
        startTag: String,
        endTag: String,
        message: String
    ): String {
        val ver = LevelledMobs.instance.ver

        if (ver.minecraftVersion < 1.16 || !ver.isRunningSpigot) return message

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

    /**
     * This does NOT colorize hex codes, ONLY standard codes.
     * This translated all standard codes in a message. Standard codes are prefixed by '&', e.g. '&a'.
     *
     * @author lokka30
     *
     * @param msg the message to translate standard color codes from.
     * @return the color-translated message.
     *
     * @since unknown
     */
    fun colorizeStandardCodes(msg: String?): String {
        return if (Bukkit.getName()
                .equals("CraftBukkit", ignoreCase = true)
        ) org.bukkit.ChatColor.translateAlternateColorCodes(
            '&',
            msg!!
        )
        else ChatColor.translateAlternateColorCodes('&', msg)
    }
}