package me.lokka30.levelledmobs.bukkit.commands

import me.lokka30.levelledmobs.bukkit.LevelledMobs.Companion.logInf
import me.lokka30.levelledmobs.bukkit.commands.levelledmobs.LevelledMobsCommand

class CommandHandler {

    /*
    A set of base commands which are provided by LevelledMobs.
     */
    val baseCommands = mutableSetOf<BaseCommandWrapper>(LevelledMobsCommand())

    /*
    Register all base commands for LevelledMobs.

    Important note: Each command must be registered in plugin.yml, otherwise Bukkit will allow it to
    be registered.
     */
    fun load() {
        logInf("Registering commands...")
        baseCommands.forEach(BaseCommandWrapper::register)
    }

}