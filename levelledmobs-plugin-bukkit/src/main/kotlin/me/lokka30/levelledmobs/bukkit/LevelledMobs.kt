package me.lokka30.levelledmobs.bukkit

import me.lokka30.levelledmobs.bukkit.commands.CommandHandler
import me.lokka30.levelledmobs.bukkit.configs.ConfigHandler
import me.lokka30.levelledmobs.bukkit.integrations.IntegrationHandler
import me.lokka30.levelledmobs.bukkit.listeners.ListenerHandler
import me.lokka30.levelledmobs.bukkit.logic.LogicHandler
import org.bukkit.ChatColor.AQUA
import org.bukkit.ChatColor.BOLD
import org.bukkit.ChatColor.GRAY
import org.bukkit.ChatColor.RED
import org.bukkit.ChatColor.YELLOW
import org.bukkit.plugin.java.JavaPlugin

class LevelledMobs : JavaPlugin() {

    val commandHandler = CommandHandler()
    val configHandler = ConfigHandler()
    val integrationHandler = IntegrationHandler()
    val listenerHandler = ListenerHandler()
    val logicHandler = LogicHandler()

    companion object {
        /*
        Stores an instance of the class loaded by Bukkit so it can be referenced elsewhere.
         */
        var instance: LevelledMobs? = null
            private set

        /*
        Short hand logging methods.
         */

        // INFO
        fun logInf(msg: String) { instance!!.logger.info(msg) }

        // WARNING
        fun logWar(msg: String) { instance!!.logger.warning(msg) }

        // SEVERE
        fun logSev(msg: String) { instance!!.logger.severe(msg) }

        /*
        Temporary chat message prefixes used for snapshot builds.
         */

        // INFO
        val prefixInf = "${AQUA}${BOLD}LM:${GRAY} "

        // WARNING
        val prefixWar = "${YELLOW}${BOLD}LM:${GRAY} "

        // SEVERE
        val prefixSev = "${RED}${BOLD}LM:${GRAY} "
    }

    override fun onLoad() {
        instance = this
        logger.info("Plugin initialized.")
    }

    override fun onEnable() {
        configHandler.load()
        logicHandler.load()
        listenerHandler.load()
        commandHandler.load()
        logger.info("Plugin enabled.")
    }

    override fun onDisable() {
        logger.info("Plugin disabled.")
    }
}