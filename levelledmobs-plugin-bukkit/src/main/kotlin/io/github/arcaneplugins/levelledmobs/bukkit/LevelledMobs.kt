package io.github.arcaneplugins.levelledmobs.bukkit

import io.github.arcaneplugins.levelledmobs.bukkit.command.CommandHandler
import io.github.arcaneplugins.levelledmobs.bukkit.config.ConfigHandler
import io.github.arcaneplugins.levelledmobs.bukkit.util.Log.inf
import org.bukkit.plugin.java.JavaPlugin

class LevelledMobs : JavaPlugin() {
    companion object{
        lateinit var lmInstance: LevelledMobs private set
    }

    //private val libLabelHandler = LabelHandler()
    lateinit var configHandler: ConfigHandler private set

    override fun onLoad() {
        lmInstance = this

        CommandHandler.load(CommandHandler.LoadingStage.ON_LOAD)
        configHandler = ConfigHandler()
        inf("Plugin initialized")
    }

    override fun onEnable() {

    }

    fun reload(){

    }

    override fun onDisable() {

    }

    fun assertRunningSpigot(){

    }
}