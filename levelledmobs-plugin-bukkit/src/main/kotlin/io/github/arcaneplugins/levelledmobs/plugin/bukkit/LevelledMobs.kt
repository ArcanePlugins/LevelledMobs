/*
This program is/was a part of the LevelledMobs project's source code.
Copyright (C) 2023  Lachlan Adamson (aka lokka30)
Copyright (C) 2023  LevelledMobs Contributors

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package io.github.arcaneplugins.levelledmobs.plugin.bukkit

import io.github.arcaneplugins.levelledmobs.plugin.bukkit.command.Commands
import io.github.arcaneplugins.levelledmobs.plugin.bukkit.config.ConfigManager
import io.github.arcaneplugins.levelledmobs.plugin.bukkit.listener.ListenerManager
import io.github.arcaneplugins.levelledmobs.plugin.bukkit.misc.ExceptionUtil
import io.github.arcaneplugins.levelledmobs.plugin.bukkit.misc.Log
import io.github.arcaneplugins.levelledmobs.plugin.bukkit.misc.QuickTimer
import io.github.arcaneplugins.levelledmobs.plugin.bukkit.nametag.Definitions
import io.github.arcaneplugins.levelledmobs.plugin.bukkit.nametag.ServerVersionInfo
import io.github.arcaneplugins.levelledmobs.plugin.bukkit.rule.RuleManager
import org.bukkit.command.PluginCommand
import org.bukkit.plugin.java.JavaPlugin

/**
 * The main class of LevelledMobs' plugin for Bukkit.
 *
 * @author Lachlan Adamson (lokka30)
 */
class LevelledMobs : JavaPlugin() {

    companion object {

        /**
         * An instance of this class is stored when it is initialised by Bukkit's
         * [org.bukkit.plugin.PluginManager].
         */
        lateinit var lmInstance: LevelledMobs
            private set

    }

    lateinit var configManager: ConfigManager private set
    lateinit var ruleManager: RuleManager private set

    private lateinit var listenerManager: ListenerManager
    private lateinit var commands: Commands
    lateinit var verInfo: ServerVersionInfo
    lateinit var definitions: Definitions

    override fun onLoad() {
        lmInstance = this

        //TODO assert server is running SpigotMC-based software

        try {
            configManager = ConfigManager()
            ruleManager = RuleManager()
            listenerManager = ListenerManager()
            commands = Commands()
            verInfo = ServerVersionInfo()
            definitions = Definitions()
        } catch (ex: Exception) {
            ExceptionUtil.printExceptionNicely(
                ex = ex,
                context = "An error has occurred while loading LevelledMobs"
            )
        }
    }

    override fun onEnable() {
        val timer = QuickTimer()

        try {
            configManager.load()
            ruleManager.load()
            listenerManager.load()
            registerCommands()
            verInfo.load()
            definitions.load()
        } catch(ex: Exception) {
            ExceptionUtil.printExceptionNicely(
                ex = ex,
                context = "An error has occurred while enabling LevelledMobs"
            )
        }

        Log.info("Start-up complete (took ${timer.getTimer()} ms)")
    }

    private fun registerCommands(){
        Log.info("Commands: Registering commands...")

        val levelledMobsCommand: PluginCommand? = this.getCommand("levelledmobs")
        if (levelledMobsCommand == null){
            Log.severe("Command levelledmobs is unavailable, is it not registered in plugin.yml?")
        }
        else{
            levelledMobsCommand.setExecutor(this.commands)
        }
    }

    // TODO Document function.
    fun reload() {
        Log.info("Reloading configuration files...")

        try {
            configManager.load()
        } catch (ex: Exception) {
            ExceptionUtil.printExceptionNicely(
                ex = ex,
                context = "An error has occurred whilst reloading LevelledMobs"
            )
            return
        }

        Log.info("Reload complete.")
    }

}