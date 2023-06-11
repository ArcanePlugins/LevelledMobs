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

/**
 * Contains a series of debug category constants.
 *
 * In LM, debug logs are categorised to make it easier for developers to scope down what information
 * they want to be logged by the debugging system.
 *
 * Whilst testing, it's a great idea to use [TEMP]
 * so that you can debug something as you develop it, without using [System.out] or another
 * non-debug logging utility which can easily be forgotten about and find its way into production.
 *
 * One of the outliers in this enum is the [ALL] constant, which tells the [DebugManager] that the
 * server administrator wants *all* of the debug categories to be enabled.
 *
 * Debug categories are referenced by the exact name they are written in source code, i.e.,
 * their [name] property.
 *
 * @author Lachlan Adamson (lokka30)
 */
enum class DebugCategory {

    /**
     * Constant representing that all other categories should be enabled.
     */
    ALL,

    /**
     * Constant representing a temporary debug log used during rapid development.
     */
    TEMP,

}