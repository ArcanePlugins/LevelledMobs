package io.github.arcaneplugins.levelledmobs.bukkit.config

import io.github.arcaneplugins.levelledmobs.bukkit.LevelledMobs
import io.github.arcaneplugins.levelledmobs.bukkit.util.Log.inf
import io.github.arcaneplugins.levelledmobs.bukkit.util.Log.war
import org.spongepowered.configurate.CommentedConfigurationNode
import org.spongepowered.configurate.ConfigurateException
import org.spongepowered.configurate.yaml.YamlConfigurationLoader
import java.io.File
import java.nio.file.Path
import kotlin.math.max

abstract class Config(
    val fileName: String,
    val latestFileVersion: Int
){
    var loader: YamlConfigurationLoader? = null
    var root: CommentedConfigurationNode? = null

    fun load(){
        saveDefaultFile(false)

        if (loader == null) {
            loader = YamlConfigurationLoader.builder().path(getAbsolutePath()).build()
        }

        root = try {
            loader!!.load()
        } catch (ex: ConfigurateException) {
            throw RuntimeException(ex)
        }

        update()
    }

    fun save(){
        try {
            loader!!.save(root)
        } catch (ex: ConfigurateException) {
            throw java.lang.RuntimeException(ex)
        }
    }

    fun update(){
        val currentFileVersion: Int = getCurrentFileVersion()

        if (currentFileVersion == 0) {
            throw IllegalStateException(
                "Unable to detect the file version of configuration '" +
                        fileName + "'. Was the file metadata modified?"
            )
        } else if (currentFileVersion > latestFileVersion) {
            throw IllegalStateException(
                ("Configuration '" + fileName +
                        "' is somehow newer than the latest " +
                        "compatible file version. Was it modified by the user?")
            )
        } else if (currentFileVersion < latestFileVersion) {
            inf("Update detected for configuration '$fileName'; updating")
            if (updateLogic(currentFileVersion)) {
                inf("Configuration '$fileName' has been updated")
            }
        }
    }

    /*
    Run abstracted update logic handled per-file.
    Returns if the file update was successful (no exception occurred).
    This method should only be called by the `update` method.
     */
    protected abstract fun updateLogic(fromVersion: Int): Boolean

    fun getCurrentFileVersion(): Int{
        val fileVersionLm4 = max(0.0,
            root!!.node("metadata", "version", "current").getInt(0).toDouble()
        ).toInt()

        val fileVersionLm3 = max(0.0,
            root!!.node("file-version").getInt(0).toDouble()
        ).toInt()

        if (fileVersionLm4 == 0) {
            /*
            Either the user has tampered with the file, or they are migrating from an old LM
            revision. Let's check the file version at the classic LM 1/2/3 node. If even that is
            missing, then it'll default to `0` which will send a severe warning to the console.

            Using the `max` method since we don't want numbers lower than 0.
             */
            if (fileVersionLm3 == 0) {
                throw java.lang.IllegalStateException(
                    "Unable to retrieve current file version of config '" + fileName +
                            "'. Was it removed or modified by the user?"
                )
            } else {
                war(
                    ("LM4-style file version not found for config '" + fileName +
                            "'; falling back to the LM3-style file version until the file is updated")
                )
                return fileVersionLm3
            }
        } else {
            return fileVersionLm4
        }
    }

    fun saveDefaultFile(replaceExistingFile: Boolean) {
        if (!replaceExistingFile && getAbsolutePath().toFile().exists()) return
        inf("Saving default file '$fileName'")
        LevelledMobs.lmInstance.saveResource(fileName, replaceExistingFile)
    }

    fun getAbsolutePath(): Path {
        return Path.of(
            LevelledMobs.lmInstance
                .dataFolder.absolutePath + File.separator + fileName
        )
    }
}

