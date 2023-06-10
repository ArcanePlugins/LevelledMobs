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
package io.github.arcaneplugins.levelledmobs.plugin.bukkit.config

import io.github.arcaneplugins.levelledmobs.plugin.bukkit.config.settings.RulesConfig
import io.github.arcaneplugins.levelledmobs.plugin.bukkit.config.settings.SettingsConfig

class ConfigManager {

    //TODO Describe
    val settings: SettingsConfig = SettingsConfig()

    //TODO Describe
    val rules: RulesConfig = RulesConfig()

    //TODO Describe
    val allConfigs: Array<Config> = arrayOf(
        settings, // do not change order
        rules,    // do not change order
        // ... do not insert nodes above this line
        // ... do not insert nodes below this line
    )

    //TODO Describe
    fun load() {
        allConfigs.forEach(Config::load)
    }

}