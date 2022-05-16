package me.lokka30.levelledmobs.bukkit.configs

import me.lokka30.levelledmobs.bukkit.LevelledMobs
import me.lokka30.levelledmobs.bukkit.LevelledMobs.Companion.logInf
import me.lokka30.levelledmobs.bukkit.LevelledMobs.Companion.logSev
import me.lokka30.levelledmobs.bukkit.LevelledMobs.Companion.logWar
import org.spongepowered.configurate.CommentedConfigurationNode
import org.spongepowered.configurate.ConfigurateException
import org.spongepowered.configurate.yaml.YamlConfigurationLoader
import java.io.File
import java.lang.Integer.max
import java.nio.file.Path
import kotlin.io.path.exists

/*
FIXME comment
 */
abstract class Config(
    val fileName: String,
    val latestFileVersion: Int
) {

    /*
    FIXME comment
     */
    protected var loader: YamlConfigurationLoader? = null

    /*
    FIXME comment
     */
    var root: CommentedConfigurationNode? = null
        private set

    /*
    FIXME comment
     */
    fun load() {
        logInf("Loading file '${fileName}'...")

        saveDefaultFile(false)

        if(loader == null) {
            loader = YamlConfigurationLoader.builder()
                .path(getAbsolutePath())
                .build()
        }

        try {
            root = loader!!.load()
        } catch (ex: ConfigurateException) {
            logSev(
                "Unable to load configuration '${fileName}'. This is usually a " +
                        "user-caused error caused from YAML syntax errors inside the file, such as an " +
                        "incorrect indent or stray symbol. We recommend that you use a YAML parser " +
                        "website - such as the one linked here - to help locate where these errors " +
                        "are appearing. --> https://www.yaml-online-parser.appspot.com/ <-- A stack trace " +
                        "will be printed below for debugging purposes."
            )
            ex.printStackTrace()
            return
        }

        update()
    }

    /*
    FIXME comment
     */
    fun save() {
        try {
            loader!!.save(root)
        } catch (ex: ConfigurateException) {
            logSev(
                """
                LevelledMobs was unable to save data to the configuration '${fileName}'. Please contact the LM
                support team regarding this rare issue via the methods described on the resource page (Discord/PMs).
                A stack trace will be printed below for debugging purposes.
                """.trimIndent()
            )
            ex.printStackTrace()
        }
    }

    /*
    FIXME comment
     */
    private fun update() {
        val currentFileVersion = getCurrentFileVersion()

        if (currentFileVersion == 0) {
            logSev("Unable to detect the file version of configuration '${fileName}'. " +
                    "Was it modified?")
        } else if (currentFileVersion > latestFileVersion) {
            logWar("Configuration '${fileName}' is somehow newer than the latest compatible " +
                        "file version. How did we get here?")
        } else if (currentFileVersion < latestFileVersion) {
            logInf("Update detected for '${fileName}' - updating...")
            if (updateLogic(currentFileVersion)) {
                logInf("Configuration '${fileName}' has been updated.")
            } else {
                logSev("Update for configuration '${fileName}' failed.")
            }
        }
    }

    /*
    Run abstracted update logic handled per-file.
    Returns if the file update was successful (no exception occurred).
    This function should only be called by the `update` function.
     */
    protected abstract fun updateLogic(fromVersion: Int): Boolean

    /*
    Get the current (installed) file version of this config.
     */
    fun getCurrentFileVersion(): Int {
        val fileVersionLM4 = root!!.node("metadata", "version", "current").getInt(0)

        val currentFileVersion = max(
            0, if (fileVersionLM4 == 0) {
                /*
                Either the user has tampered with the file, or they are migrating from an old LM
                revision. Let's check the file version at the classic LM 1/2/3 node. If even that is
                missing, then it'll default to `0` which will send a severe warning to the console.

                Using the `max` method since we don't want numbers lower than 0.
                 */
                logInf("Fallback to classic file version node for configuration '${fileName}'.")
                root!!.node("file-version").getInt(0)
            } else {
                /*
                Configuration has the file version specified, and it'. LM is happy :)
                 */
                fileVersionLM4
            }
        )

        if (currentFileVersion == 0)
            logSev(
                "Unable to retrieve file-version for configuration " +
                        "'${fileName}'. Was it modified?"
            )

        return currentFileVersion
    }

    fun saveDefaultFile(replace: Boolean) {
        if (!replace) {
            if (getAbsolutePath().exists()) {
                return
            } else {
                logInf("Saving default file of configuration '${fileName}'.")
            }
        }

        LevelledMobs.instance!!.saveResource(fileName, replace)
    }

    fun getAbsolutePath(): Path {
        return Path.of(LevelledMobs.instance!!.dataFolder.absolutePath +
                File.separator + fileName)
    }

}