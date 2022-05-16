package me.lokka30.levelledmobs.bukkit.configs.presets

import me.lokka30.levelledmobs.bukkit.LevelledMobs.Companion.logInf
import me.lokka30.levelledmobs.bukkit.LevelledMobs.Companion.logSev
import me.lokka30.levelledmobs.bukkit.configs.Config
import me.lokka30.levelledmobs.bukkit.configs.settings.SettingsCfg

class PresetsCfg : Config("presets.yml", 1) {

    companion object {

        /*
        The minimum file version which the updater is willing to migrate from.
         */
        const val updaterCutoffFileVersion = 1

    }

    override fun updateLogic(fromVersion: Int): Boolean {
        if(fromVersion < SettingsCfg.updaterCutoffFileVersion) {
            logSev("Configuration '${fileName}' is too old to be migrated: it is version" +
                    "'${fromVersion}', but the 'cutoff' version is " +
                    "'${SettingsCfg.updaterCutoffFileVersion}'.")
            return false
        }

        var currentFileVersion = fromVersion

        while (currentFileVersion < latestFileVersion) {
            logInf(
                "Updating configuration '${fileName}' from file version " +
                        "'${currentFileVersion}' to '${currentFileVersion + 1}'..."
            )

            when(currentFileVersion) {
                12345 -> {
                    // some migration code  here ...
                    currentFileVersion++
                    root!!.node("metadata", "version", "current").set(currentFileVersion)
                    loader!!.save(root)
                }
                else -> {
                    logSev("Attempted to update from file version '${currentFileVersion}' " +
                            "of configuration '${fileName}', but no updater logic is present for " +
                            "that file version. Please inform LM support, as this should be " +
                            "impossible.")
                    return false
                }
            }
        }
        return true
    }

}