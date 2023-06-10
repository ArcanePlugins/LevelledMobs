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

import io.github.arcaneplugins.levelledmobs.plugin.bukkit.config.ConfigManager
import io.github.arcaneplugins.levelledmobs.plugin.bukkit.rule.RuleManager
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

    //TODO Document
    lateinit var configManager: ConfigManager

    //TODO Document
    lateinit var ruleManager: RuleManager

    override fun onLoad() {
        lmInstance = this

        //TODO assert server is running SpigotMC-based software

        configManager = ConfigManager()
        ruleManager = RuleManager()
    }

    override fun onEnable() {
        configManager.load()
        ruleManager.load()
    }

    override fun onDisable() {
        // TODO
    }

    // TODO Use
    // TODO Document
    fun reload() {
        configManager.load()
    }

}