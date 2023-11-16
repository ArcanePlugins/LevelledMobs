package io.github.arcaneplugins.levelledmobs.bukkit.config.customdrops

import io.github.arcaneplugins.levelledmobs.bukkit.config.Config
import io.github.arcaneplugins.levelledmobs.bukkit.util.Log.inf
import io.github.arcaneplugins.levelledmobs.bukkit.util.Log.sev

class CustomDropsCfg : Config("customdrops.yml", 11) {
    companion object{
        /* vars */ /*
        The latest file version of 'customdrops.yml' in the previous LevelledMobs revision (LM3).
         */
        private const val LATEST_LM_3_CUSTOM_DROPS_FILE_VERSION = 10

        /*
        The minimum file version which the updater is willing to migrate from.
         */
        private const val UPDATER_CUTOFF_FILE_VERSION = LATEST_LM_3_CUSTOM_DROPS_FILE_VERSION
    }

    override fun updateLogic(fromVersion: Int): Boolean {
        if (fromVersion < UPDATER_CUTOFF_FILE_VERSION) {
            sev(
                "Configuration '$fileName' is too old to be migrated: it " +
                        "is version '$fromVersion', but the 'cutoff' version is '" +
                        "$UPDATER_CUTOFF_FILE_VERSION'", true
            )
            return false
        }

        while (fromVersion < latestFileVersion) {
            inf(
                "Updating configuration '" + fileName + "' from file version '" +
                        fromVersion + "' to '" + (fromVersion + 1) + "'"
            )

            when (fromVersion) {
                LATEST_LM_3_CUSTOM_DROPS_FILE_VERSION -> {
                    //TODO some migration code  here ...
                    /*
                    currentFileVersion++
                    root node("metadata", "version", "current").set(currentFileVersion)
                    loader save(root)
                     */
                    sev("Update logic is not yet available for LM3 Custom Drops", true)
                    return false
                }

                else -> {
                    sev(
                        ("Attempted to update from file version '$fromVersion'" +
                                " of configuration '$fileName', but no updater logic is " +
                                "present for that file version"), true
                    )
                    return false
                }
            }
        }
        return true
    }
}