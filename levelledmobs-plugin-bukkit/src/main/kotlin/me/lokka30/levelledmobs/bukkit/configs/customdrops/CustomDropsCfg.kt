package me.lokka30.levelledmobs.bukkit.configs.customdrops

import me.lokka30.levelledmobs.bukkit.LevelledMobs.Companion.logInf
import me.lokka30.levelledmobs.bukkit.LevelledMobs.Companion.logSev
import me.lokka30.levelledmobs.bukkit.configs.Config
import me.lokka30.levelledmobs.bukkit.configs.settings.SettingsCfg

class CustomDropsCfg : Config("customdrops.yml", 11) {

    companion object {

        /*
        The latest file version of 'customdrops.yml' in the previous LevelledMobs revision (LM3).
         */
        const val latestLM3CustomDropsFileVersion = 10

        /*
        The minimum file version which the updater is willing to migrate from.
         */
        const val updaterCutoffFileVersion = latestLM3CustomDropsFileVersion

    }

    @Suppress("CanBeVal") // TODO do not make this a val unlike what IntelliJ suggests
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
                latestLM3CustomDropsFileVersion -> {
                    //TODO some migration code  here ...
                    /*
                    currentFileVersion++
                    root!!.node("metadata", "version", "current").set(currentFileVersion)
                    loader!!.save(root)
                     */
                    logSev("Update logic is not yet available for LM3 Custom Drops.")
                    return false
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