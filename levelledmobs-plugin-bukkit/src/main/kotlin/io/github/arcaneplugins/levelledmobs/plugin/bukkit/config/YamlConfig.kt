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

import org.spongepowered.configurate.yaml.YamlConfigurationLoader

//todo doc
abstract class YamlConfig(
    fileName: String,
    isResourceFile: Boolean,
    relativePath: String = "",
    latestFileVersion: Int
) : Config(
    fileName = fileName,
    fileExtension = FILE_EXTENSION,
    isResourceFile = isResourceFile,
    relativePath = relativePath,
    latestFileVersion = latestFileVersion
) {

    companion object {
        const val FILE_EXTENSION = ".yml"
    }

    val loader: YamlConfigurationLoader = YamlConfigurationLoader
        .builder()
        .path(path)
        .build()

    override fun loadRootNode() {
        rootNode = loader.load()
    }

    override fun save() {
        loader.save(rootNode)
    }

}