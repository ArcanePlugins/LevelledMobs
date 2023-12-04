package io.github.arcaneplugins.levelledmobs.bukkit

import io.github.arcaneplugins.arcaneframework.support.SupportChecker
import io.github.arcaneplugins.levelledmobs.bukkit.command.CommandHandler
import io.github.arcaneplugins.levelledmobs.bukkit.command.CommandHandler.LoadingStage
import io.github.arcaneplugins.levelledmobs.bukkit.config.ConfigHandler
import io.github.arcaneplugins.levelledmobs.bukkit.integration.IntegrationHandler
import io.github.arcaneplugins.levelledmobs.bukkit.listener.ListenerHandler
import io.github.arcaneplugins.levelledmobs.bukkit.logic.LogicHandler
import io.github.arcaneplugins.levelledmobs.bukkit.task.TaskHandler
import io.github.arcaneplugins.levelledmobs.bukkit.util.ExceptionUtil
import io.github.arcaneplugins.levelledmobs.bukkit.util.Log.inf
import org.bukkit.plugin.java.JavaPlugin

class LevelledMobs : JavaPlugin() {
    companion object {
        lateinit var lmInstance: LevelledMobs private set
    }

    //private val libLabelHandler = LabelHandler()
    lateinit var configHandler: ConfigHandler private set

    override fun onLoad() {
        lmInstance = this

        CommandHandler.load(LoadingStage.ON_LOAD)
        configHandler = ConfigHandler()
        inf("Plugin initialized")
    }

    override fun onEnable() {


        try {
            assertRunningSpigot()
            configHandler.load()
            ListenerHandler.loadPrimary()
            IntegrationHandler.load()
            LogicHandler.load()
            ListenerHandler.loadSecondary()
            CommandHandler.load(LoadingStage.ON_ENABLE)
        } catch (ex: Exception) {
            ExceptionUtil.printExceptionNicely(
                ex = ex,
                context = "An error has occurred while enabling LevelledMobs"
            )
        }
    }

    fun reload() {
        TaskHandler.stopTasks()
        LogicHandler.unload()
        configHandler.load()
        LogicHandler.load()
        TaskHandler.startTasks()
    }

    override fun onDisable() {
        TaskHandler.stopTasks()
        LogicHandler.unload()
        inf("Plugin disabled")
    }

    private fun assertRunningSpigot() {
        if (SupportChecker.SPIGOTMC_OR_DERIVATIVE) return

        throw io.github.arcaneplugins.levelledmobs.bukkit.util.SilentException(
            """
            LevelledMobs has detected that your server is not running the SpigotMC server software, or any derivative such as PaperMC.
            """
        )
    }
}