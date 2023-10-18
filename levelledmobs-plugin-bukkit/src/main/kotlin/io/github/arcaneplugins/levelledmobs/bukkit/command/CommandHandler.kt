package io.github.arcaneplugins.levelledmobs.bukkit.command

import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.CommandAPIConfig
import io.github.arcaneplugins.levelledmobs.bukkit.util.Log

class CommandHandler {
    companion object{
        fun load(loadingStage: LoadingStage){
            when (loadingStage){
                LoadingStage.ON_LOAD ->{
                    Log
                }
                LoadingStage.ON_ENABLE -> {

                }
                LoadingStage.ON_DISABLE -> {

                }
                LoadingStage.FORCED ->{

                }
            }
        }
    }

    private val commands: MutableList<CommandAPICommand> = mutableListOf()
    val cmdConfig = CommandAPIConfig()
        .verboseOutput(false)
        .silentLogs(true)

    enum class LoadingStage{
        ON_LOAD,
        ON_ENABLE,
        ON_DISABLE,
        FORCED
    }
}