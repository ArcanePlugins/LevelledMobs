package io.github.arcaneplugins.levelledmobs.bukkit.config

import io.github.arcaneplugins.levelledmobs.bukkit.config.customdrops.CustomDropsCfg
import io.github.arcaneplugins.levelledmobs.bukkit.config.groups.GroupsCfg
import io.github.arcaneplugins.levelledmobs.bukkit.config.presets.PresetsCfg
import io.github.arcaneplugins.levelledmobs.bukkit.config.settings.SettingsCfg
import io.github.arcaneplugins.levelledmobs.bukkit.config.translations.TranslationHandler
import io.github.arcaneplugins.levelledmobs.bukkit.debug.DebugHandler
import io.github.arcaneplugins.levelledmobs.bukkit.util.Log.inf

class ConfigHandler {

    /* vars */
    val customDropsCfg = CustomDropsCfg()
    val groupsCfg = GroupsCfg()
    val presetsCfg = PresetsCfg()
    val settingsCfg = SettingsCfg()

    private val translationHandler = TranslationHandler()

    /* methods */

    /*
    Attempt to (re)load all of the configs.

    Order of the function calls does matter!
     */

    fun load(){
        inf("Loading configuration")

        for (cfg in mutableSetOf(customDropsCfg, groupsCfg, presetsCfg, settingsCfg)) {
            cfg.load()
        }

        DebugHandler.load()
        translationHandler.load()
    }
}