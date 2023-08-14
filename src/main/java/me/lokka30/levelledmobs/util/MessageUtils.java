package me.lokka30.levelledmobs.util;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.nametag.ServerVersionInfo;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;

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
public class MessageUtils {

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
    public static @NotNull String colorizeAll(String msg) {
        return colorizeStandardCodes(colorizeHexCodes(msg));
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
    public static String colorizeHexCodes(String msg) {
        return colorizeHexCodes("&#", "", msg);
    }

    /**
     * (WARNING!) This does NOT colorize standard codes, ONLY hex codes.
     * This translates all hex codes in a message. Hex codes are prefixed by '&#', e.g. '&#abcdef'.
     * This method ensures the version is 1.16 or newer before translating - else, it will not translate the message.
     *
     * @author Elementeral @SpigotMC.org and imDaniX @ SpigotMC.org ~ <a href="https://www.spigotmc.org/threads/hex-color-code-translate.449748/#post-3867804">...</a>
     *
     * @param startTag what the tag should begin with - '&#' is recommended
     * @param endTag   what the tag should end with - '' (nothing) is recommended
     * @param message  the message that should be translated
     * @return the translated string
     *
     * @since unknown
     */
    public static String colorizeHexCodes(String startTag, String endTag, String message) {
        final ServerVersionInfo ver = LevelledMobs.getInstance().getVerInfo();

        if (ver.getMinecraftVersion() < 1.16 || !ver.getIsRunningSpigot()) return message;

        final Pattern hexPattern = Pattern.compile(startTag + "([A-Fa-f0-9]{6})" + endTag);
        Matcher matcher = hexPattern.matcher(message);
        StringBuilder buffer = new StringBuilder(message.length() + 4 * 8);
        final char colorChar = net.md_5.bungee.api.ChatColor.COLOR_CHAR;

        while (matcher.find()) {
            String group = matcher.group(1);
            matcher.appendReplacement(buffer, colorChar + "x"
                    + colorChar + group.charAt(0) + colorChar + group.charAt(1)
                    + colorChar + group.charAt(2) + colorChar + group.charAt(3)
                    + colorChar + group.charAt(4) + colorChar + group.charAt(5)
            );
        }
        return matcher.appendTail(buffer).toString();
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
    public static @NotNull String colorizeStandardCodes(String msg) {
        if (Bukkit.getName().equalsIgnoreCase("CraftBukkit"))
            return org.bukkit.ChatColor.translateAlternateColorCodes('&', msg);
        else
            return net.md_5.bungee.api.ChatColor.translateAlternateColorCodes('&', msg);
    }
}
