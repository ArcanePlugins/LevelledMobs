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

import io.github.arcaneplugins.levelledmobs.plugin.bukkit.LevelledMobs.Companion.lmInstance
import io.github.arcaneplugins.levelledmobs.plugin.bukkit.misc.DescriptiveException
import org.spongepowered.configurate.CommentedConfigurationNode
import java.io.File
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.createFile
import kotlin.io.path.exists

/**
 * TODO Description
 *
 * @author Lachlan Adamson (lokka30)
 */
abstract class Config(
    val fileName: String,
    val fileExtension: String,
    val isResourceFile: Boolean,
    val relativePath: String = "", // todo note that this does not include an ending file separator
    val latestFileVersion: Int,
) {

    // TODO Description
    val path: Path = Path(
        lmInstance.dataFolder.absolutePath +
                File.separator +
                relativePath +
                fileName +
                fileExtension
    )

    // TODO Description
    lateinit var rootNode: CommentedConfigurationNode

    // TODO Description
    open fun load() {
        saveIfNotExists()
        loadRootNode()
        setLatestInstalledPluginVersion()
        migrate()
    }

    protected abstract fun loadRootNode()

    // TODO Description
    abstract fun save()

    //TODO description
    protected fun saveIfNotExists() {
        if(exists()) return

        if(isResourceFile) {
            lmInstance.saveResource(
                relativePath + fileName + fileExtension,
                false
            )
        } else {
            path.createFile()
        }
    }

    //TODO description
    fun exists(): Boolean {
        return path.exists()
    }

    //TODO describe
    fun installedFileVersion(): Int {
        return rootNode
            .node("advanced", "metadata", "version", "file", "installed")
            .int
    }

    //TODO describe
    fun initialInstalledFileVersion(): Int {
        return rootNode
            .node("advanced", "metadata", "version", "file", "initial")
            .int
    }

    //TODO description
    private fun setLatestInstalledPluginVersion() {
        rootNode
            .node("advanced", "metadata", "version", "plugin", "installed")
            .set(lmInstance.description.version)
    }

    //TODO describe
    protected fun migrate() {
        while(installedFileVersion() < latestFileVersion) {
            try {
                migrateToNextVersion()
            } catch(ex: DescriptiveException) {
                throw DescriptiveException(
                    "Unable to migrate '${fileName}' file to next version: ${ex.message}",
                    ex
                )
            }
        }
    }

    //TODO describe
    protected abstract fun migrateToNextVersion()

    //TODO describe
    protected fun setFileVersion(newFileVersion: Int) {
        rootNode
            .node("advanced", "metadata", "version", "file", "installed")
            .set(newFileVersion)
    }

}