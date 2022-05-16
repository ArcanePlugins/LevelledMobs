package me.lokka30.levelledmobs.bukkit.configs

import me.lokka30.levelledmobs.bukkit.LevelledMobs.Companion.logInf
import me.lokka30.levelledmobs.bukkit.configs.customdrops.CustomDropsCfg
import me.lokka30.levelledmobs.bukkit.configs.groups.GroupsCfg
import me.lokka30.levelledmobs.bukkit.configs.presets.PresetsCfg
import me.lokka30.levelledmobs.bukkit.configs.settings.SettingsCfg
import me.lokka30.levelledmobs.bukkit.configs.translations.TranslationHandler

class ConfigHandler {

    val customDropsCfg = CustomDropsCfg()
    val groupsCfg = GroupsCfg()
    val presetsCfg = PresetsCfg()
    val settingsCfg = SettingsCfg()

    val translationHandler = TranslationHandler()

    /*
    Attempt to (re)load all of the configs.

    Order of the function calls does matter!
     */
    fun load() {
        logInf("Loading configs...")
        setOf(
            customDropsCfg,
            groupsCfg,
            presetsCfg,
            settingsCfg
        ).forEach(Config::load)

        translationHandler.load()
    }
}