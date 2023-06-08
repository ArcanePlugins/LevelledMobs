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
package io.github.arcaneplugins.levelledmobs.plugin.bukkit.misc

import io.github.arcaneplugins.levelledmobs.plugin.bukkit.LevelledMobs.Companion.lmInstance
import io.github.arcaneplugins.levelledmobs.plugin.bukkit.config.settings.debug.DebugCategory
import io.github.arcaneplugins.levelledmobs.plugin.bukkit.config.settings.debug.DebugHandler
import java.util.function.Supplier

/**
 * This object contains various methods to aid with logging messages to the server's console.
 *
 * @author Lachlan Adamson (lokka30)
 */
object Log {

    /**
     * Logs [msg] to the console with the `info` log level.
     *
     * @see java.util.logging.Logger.info
     *
     * @author Lachlan Adamson (lokka30)
     */
    fun info(msg: String) {
        lmInstance.logger.info(msg)
    }

    /**
     * Logs [msg] to the console with the `warning` log level.
     *
     * @see java.util.logging.Logger.warning
     *
     * @author Lachlan Adamson (lokka30)
     */
    fun warning(msg: String) {
        lmInstance.logger.warning(msg)
    }

    /**
     * Logs [msg] to the console with the `severe` log level.
     *
     * @see java.util.logging.Logger.severe
     *
     * @author Lachlan Adamson (lokka30)
     */
    fun severe(msg: String) {
        lmInstance.logger.severe(msg)
    }

    /**
     * Logs [msg] to the console if the associated debug category [cat] is enabled in
     * [io.github.arcaneplugins.levelledmobs.plugin.bukkit.config.settings.debug.DebugHandler].
     *
     * @author Lachlan Adamson (lokka30)
     */
    fun debug(msg: Supplier<String>, cat: DebugCategory) {
        val debugHandler: DebugHandler = lmInstance.configHandler.settings.debugHandler

        if(!debugHandler.isCategoryEnabled(cat)) return

        lmInstance.logger.info("[DEBUG - ${cat}] ${msg.get()}")
    }

}