package me.lokka30.levelledmobs.util;

import java.util.logging.Logger;

import me.lokka30.levelledmobs.LevelledMobs;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

@SuppressWarnings("deprecation")
public class MicroLogger {

    final boolean serverIsSpigot;
    private final Logger logger;
    private String prefix;

    /**
     * Create a new instance of MicroLogger with a custom prefix
     *
     * @param prefix the prefix to use
     * @author lokka30
     * @since unknown
     */
    public MicroLogger(String prefix) {
        this.prefix = prefix;
        this.logger = Bukkit.getLogger();
        this.serverIsSpigot = LevelledMobs.getInstance().getVerInfo().getIsRunningSpigot();
    }

    /**
     * @return the current prefix
     * @author lokka30
     * @since unknown
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * @param prefix the prefix that should be set
     * @author lokka30
     * @since unknown
     */
    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    /**
     * @param message message to send with INFO log-level
     * @author lokka30, stumper66
     * @since unknown
     */
    public void info(String message) {
        if (serverIsSpigot)
            Bukkit.getServer().getConsoleSender().sendMessage(MessageUtils.colorizeAll(prefix + message));
        else
            logger.info(MessageUtils.colorizeAll(prefix + message));
    }

    /**
     * @param message message to send with WARNING log-level
     * @author lokka30, stumper66
     * @since unknown
     */
    public void warning(String message) {
        if (serverIsSpigot)
            Bukkit.getServer().getConsoleSender().sendMessage(MessageUtils.colorizeAll(ChatColor.YELLOW + "[WARN] " + ChatColor.RESET + prefix + message));
        else
            logger.warning(MessageUtils.colorizeAll(prefix + message));
    }

    /**
     * @param message message to send with ERROR log-level
     * @author lokka30, stumper66
     * @since unknown
     */
    public void error(String message) {
        if (serverIsSpigot)
            Bukkit.getServer().getConsoleSender().sendMessage(MessageUtils.colorizeAll(ChatColor.RED + "[ERROR] " + ChatColor.RESET + prefix + message));
        else
            logger.severe(MessageUtils.colorizeAll(prefix + message));
    }
}
