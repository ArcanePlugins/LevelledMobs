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
package io.github.arcaneplugins.levelledmobs.plugin.bukkit.config.settings.debug

import java.util.*

/**
 * This class contains features to aid with debugging LevelledMobs.
 * Debug logging is *not* handled directly by [DebugManager] - see
 * [io.github.arcaneplugins.levelledmobs.plugin.bukkit.misc.Log.debug].
 * [DebugManager]'s role is to store which categories the server administrator is interested in
 * debugging.
 *
 * @author Lachlan Adamson (lokka30)
 */
class DebugManager {

    /**
     * A mutable [EnumSet] of enabled [DebugCategory] constants.
     */
    val enabledCategories: EnumSet<DebugCategory> = EnumSet.noneOf(DebugCategory::class.java)

    /**
     * Returns if [cat] is in [enabledCategories].
     *
     * @return if [cat] is in [enabledCategories].
     *
     * @author Lachlan Adamson (lokka30)
     */
    fun isCategoryEnabled(cat: DebugCategory): Boolean {
        return enabledCategories.contains(cat)
    }

}