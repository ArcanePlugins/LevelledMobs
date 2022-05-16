package me.lokka30.levelledmobs.bukkit

import me.lokka30.levelledmobs.bukkit.commands.CommandHandler
import me.lokka30.levelledmobs.bukkit.configs.ConfigHandler
import me.lokka30.levelledmobs.bukkit.integrations.IntegrationHandler
import me.lokka30.levelledmobs.bukkit.listeners.ListenerHandler
import me.lokka30.levelledmobs.bukkit.logic.LogicHandler
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
        Sends an [INFO] log to the console.
         */
        fun logInf(msg: String) { instance!!.logger.info(msg) }

        /*
        Sends a [WARNING] log to the console.
         */
        fun logWar(msg: String) { instance!!.logger.warning(msg) }

        /*
        Sends a [SEVERE] log to the console.
         */
        fun logSev(msg: String) { instance!!.logger.severe(msg) }
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