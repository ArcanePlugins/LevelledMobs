package io.github.arcaneplugins.levelledmobs.commands

import dev.jorel.commandapi.CommandAPI
import dev.jorel.commandapi.CommandAPIBukkitConfig
import dev.jorel.commandapi.CommandAPICommand
import io.github.arcaneplugins.levelledmobs.LevelledMobs
import io.github.arcaneplugins.levelledmobs.util.Log

object CommandHandler {
    private val COMMANDS = mutableListOf<CommandAPICommand>()

    fun load(loadingStage: LoadingStage){

        when (loadingStage) {
            LoadingStage.ON_LOAD -> {
                Log.inf("Loading commands")
                val commandCfg = CommandAPIBukkitConfig(LevelledMobs.instance)
                    .silentLogs(true)
                    .verboseOutput(false)
                CommandAPI.onLoad(commandCfg)
                registerCommands()
            }

            LoadingStage.ON_ENABLE -> {
                Log.inf("Enabling commands")
                CommandAPI.onEnable()
            }

            LoadingStage.ON_DISABLE -> {
                Log.inf("Unregistering commands")
                unregisterCommands()
            }

            LoadingStage.FORCED -> {
                Log.inf("Manually registering commands")
                registerCommands()
            }
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