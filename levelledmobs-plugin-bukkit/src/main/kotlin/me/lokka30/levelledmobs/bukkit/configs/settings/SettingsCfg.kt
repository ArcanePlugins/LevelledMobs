package me.lokka30.levelledmobs.bukkit.configs.settings

import me.lokka30.levelledmobs.bukkit.LevelledMobs.Companion.logInf
import me.lokka30.levelledmobs.bukkit.LevelledMobs.Companion.logSev
import me.lokka30.levelledmobs.bukkit.configs.Config

class SettingsCfg : Config("settings.yml", 34) {

    companion object {
        /*
        The minimum file version which the updater is willing to migrate from.
         */
        const val updaterCutoffFileVersion = 34

    }

    override fun updateLogic(fromVersion: Int): Boolean {
        if(fromVersion < updaterCutoffFileVersion) {
            logSev("Configuration '${fileName}' is too old to be migrated: it is version" +
                        "'${fromVersion}', but the 'cutoff' version is " +
                        "'${updaterCutoffFileVersion}'.")
            return false
        }

        var currentFileVersion = fromVersion

        while (currentFileVersion < latestFileVersion) {
            logInf("Updating configuration '${fileName}' from file version " +
                    "'${currentFileVersion}' to '${currentFileVersion + 1}'...")

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