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
package io.github.arcaneplugins.levelledmobs.plugin.bukkit.config.settings

import io.github.arcaneplugins.levelledmobs.plugin.bukkit.config.YamlConfig
import io.github.arcaneplugins.levelledmobs.plugin.bukkit.config.settings.debug.DebugCategory
import io.github.arcaneplugins.levelledmobs.plugin.bukkit.config.settings.debug.DebugHandler
import io.github.arcaneplugins.levelledmobs.plugin.bukkit.misc.DescriptiveException

// TODO Document
class SettingsConfig : YamlConfig(
    fileName = "settings",
    isResourceFile = true,
    latestFileVersion = 35
) {

    // TODO Document
    val debugHandler: DebugHandler = DebugHandler()

    // TODO Document
    override fun load() {
        super.load()
        loadDebugCategories()
    }

    // TODO Document
    override fun migrateToNextVersion() {
        val installedFv = installedFileVersion()

        // TODO Document
        if (installedFv > latestFileVersion) {
            throw DescriptiveException("Somehow your file version '$installedFv' is newer than the latest file version '$latestFileVersion'.")
        }

        // TODO Document
        when (installedFv) {
            latestFileVersion -> {
                // latest is already migrated - do nothing
                return
            }
            else -> {
                throw DescriptiveException("Installed file version '$installedFv' is not migrateable. You may need to backup the file and edit the latest version of the file, '$latestFileVersion', instead.")
            }
        }
    }

    // TODO Document
    private fun loadDebugCategories() {
        debugHandler.enabledCategories.clear()
        debugHandler.enabledCategories.addAll(
            rootNode
                .node("advanced", "debug-categories")
                .getList(DebugCategory::class.java)
                ?: emptyList() // if cnfgurate doesn't have anything for us. then add no categories.
        )
    }

}