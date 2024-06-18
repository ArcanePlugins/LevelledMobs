package io.github.arcaneplugins.levelledmobs.commands

import dev.jorel.commandapi.CommandAPI
import dev.jorel.commandapi.CommandAPIBukkitConfig
import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.exceptions.UnsupportedVersionException
import io.github.arcaneplugins.levelledmobs.LevelledMobs
import io.github.arcaneplugins.levelledmobs.commands.subcommands.CommandFallback
import io.github.arcaneplugins.levelledmobs.util.Log
import org.bukkit.Bukkit
import org.bukkit.command.CommandMap

object CommandHandler {
    private val COMMANDS = mutableListOf<CommandAPICommand>()
    var hadErrorLoading = false
        private set

    fun load(loadingStage: LoadingStage){
        when (loadingStage) {
            LoadingStage.ON_LOAD -> {
                try{
                    Log.inf("Loading commands")
                    val commandCfg = CommandAPIBukkitConfig(LevelledMobs.instance)
                        .silentLogs(true)
                        .verboseOutput(false)
                        .skipReloadDatapacks(true)
                    CommandAPI.onLoad(commandCfg)
                    registerCommands()
                }
                catch (e: UnsupportedVersionException){
                    hadErrorLoading = true
                    Log.sev(e.message!!)
                    Log.war("The plugin may continue to work with limited commands. Please see if an updated version of LevelledMobs is available.")
                    loadFallbackCommands()
                }
                catch (e: Exception){
                    hadErrorLoading = true
                    loadFallbackCommands()
                    throw e
                }
            }

            LoadingStage.ON_ENABLE -> {
                if (!hadErrorLoading){
                    Log.inf("Enabling commands")
                    CommandAPI.onEnable()
                }
            }

            LoadingStage.ON_DISABLE -> {
                if (!hadErrorLoading){
                    Log.inf("Unregistering commands")
                    unregisterCommands()
                }
            }

            LoadingStage.FORCED -> {
                if (!hadErrorLoading){
                    Log.inf("Manually registering commands")
                    registerCommands()
                }
            }
        }
    }

    private fun loadFallbackCommands() {
        val fallbackCommands = CommandFallback("levelledmobs")
        val aliases = mutableListOf("levelledmobs", "lm", "lvlmobs", "leveledmobs")
        val fieldCommandMap = Bukkit.getServer()::class.java.getDeclaredField("commandMap")
        fieldCommandMap.trySetAccessible()
        val commandMap = fieldCommandMap.get(Bukkit.getServer()) as CommandMap

        for (alias in aliases){
            commandMap.register(
                alias, LevelledMobs.instance.name, fallbackCommands
            )
        }
    }

    private fun registerCommands(){
        COMMANDS.clear()
        COMMANDS.add(LevelledMobsCommand.createInstance())
        COMMANDS.forEach { cmd: CommandAPICommand -> cmd.register() }
    }

    private fun unregisterCommands(){
        COMMANDS.forEach { cmd: CommandAPICommand -> CommandAPI.unregister(cmd.name) }
    }

    enum class LoadingStage {
        ON_LOAD,
        ON_ENABLE,
        ON_DISABLE,
        FORCED
    }
}