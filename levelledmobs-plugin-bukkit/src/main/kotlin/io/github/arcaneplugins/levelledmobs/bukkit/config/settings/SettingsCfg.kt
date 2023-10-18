package io.github.arcaneplugins.levelledmobs.bukkit.config.settings

import io.github.arcaneplugins.levelledmobs.bukkit.config.Config
import io.github.arcaneplugins.levelledmobs.bukkit.util.Log.inf
import io.github.arcaneplugins.levelledmobs.bukkit.util.Log.sev
import org.spongepowered.configurate.ConfigurateException

class SettingsCfg : Config("settings.yml", 34) {
    /*
    TODO Make it update from LM3 by just deleting the file
     */

    /* vars */

    /*
    The minimum file version which the updater is willing to migrate from.
     */
    private val UPDATER_CUTOFF_FILE_VERSION = 34

    override fun updateLogic(fromVersion: Int): Boolean {
        if (fromVersion < UPDATER_CUTOFF_FILE_VERSION) {
            sev(
                "Configuration '$fileName' is too old to be migrated: it " +
                        "is version '$fromVersion', but the 'cutoff' version is " +
                        "'$UPDATER_CUTOFF_FILE_VERSION'.", true
            )
            return false
        }

        var currentFileVersion = fromVersion
        while (currentFileVersion < latestFileVersion) {
            inf(
                "Updating configuration '$fileName' from file version '" +
                        "$currentFileVersion' to '${(currentFileVersion + 1)}'")


            when (currentFileVersion) {
                12345 -> {
                    currentFileVersion++
                    try {
                        root?.node("metadata", "version", "current")?.set(currentFileVersion)
                        loader?.save(root)
                    } catch (ex: ConfigurateException) {
                        sev(
                            "Update failed: unable to write updates to file. " +
                                    "A stack trace has been supplied below for debugging purposes.",
                            true
                        )
                        ex.printStackTrace()
                        return false
                    }
                }

                else -> {
                    sev(
                        "Attempted to update from file version '$currentFileVersion' " +
                                "of configuration '$fileName', but no updater logic is " +
                                "present for that file version.", true
                    )
                    return false
                }
            }
            
            return true
        }

        return true
    }
}