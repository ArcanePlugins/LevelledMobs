package io.github.arcaneplugins.levelledmobs.bukkit.config.groups

import io.github.arcaneplugins.levelledmobs.bukkit.config.Config
import io.github.arcaneplugins.levelledmobs.bukkit.util.Log.inf
import io.github.arcaneplugins.levelledmobs.bukkit.util.Log.sev
import org.spongepowered.configurate.ConfigurateException

class GroupsCfg : Config("groups.yml", 1) {
    companion object{
        /* vars */ /*
        The minimum file version which the updater is willing to migrate from.
         */
        private const val UPDATER_CUTOFF_FILE_VERSION = 1
    }

    override fun updateLogic(fromVersion: Int): Boolean {
        if (fromVersion < UPDATER_CUTOFF_FILE_VERSION) {
            sev(
                "Configuration '$fileName' is too old to be migrated: it " +
                        "is version '$fromVersion', but the 'cutoff' version is '" +
                        "$UPDATER_CUTOFF_FILE_VERSION'.", true
            )
            return false
        }

        while (fromVersion < latestFileVersion) {
            inf(
                "Updating configuration '$fileName' from file version '" +
                        "$fromVersion' to '${(fromVersion + 1)}'"
            )

            var currentFileVersion = fromVersion
            when (currentFileVersion) {
                12345 -> {
                    currentFileVersion++
                    try {
                        root!!.node("metadata", "version", "current").set(currentFileVersion)
                        loader!!.save(root)
                    } catch (ex: ConfigurateException) {
                        sev(
                            "Update failed: unable to write updates to file. "
                                    + "A stack trace is supplied below for debugging purposes.",
                            true
                        )
                        ex.printStackTrace()
                        return false
                    }
                }

                else -> {
                    sev(
                        ("Attempted to update from file version '$currentFileVersion" +
                                "' of configuration '$fileName', but no updater logic is " +
                                "present for that file version."), true
                    )
                    return false
                }
            }
        }
        return true
    }
}