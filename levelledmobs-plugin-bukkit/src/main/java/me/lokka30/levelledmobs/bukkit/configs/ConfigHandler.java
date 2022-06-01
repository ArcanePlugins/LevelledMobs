package me.lokka30.levelledmobs.bukkit.configs;

import java.util.Set;
import me.lokka30.levelledmobs.bukkit.configs.customdrops.CustomDropsCfg;
import me.lokka30.levelledmobs.bukkit.configs.groups.GroupsCfg;
import me.lokka30.levelledmobs.bukkit.configs.presets.PresetsCfg;
import me.lokka30.levelledmobs.bukkit.configs.settings.SettingsCfg;
import me.lokka30.levelledmobs.bukkit.configs.translations.TranslationHandler;
import me.lokka30.levelledmobs.bukkit.utils.Log;
import org.jetbrains.annotations.NotNull;

public final class ConfigHandler {

    /* vars */

    private final CustomDropsCfg customDropsCfg = new CustomDropsCfg();
    private final GroupsCfg groupsCfg = new GroupsCfg();
    private final PresetsCfg presetsCfg = new PresetsCfg();
    private final SettingsCfg settingsCfg = new SettingsCfg();

    private final TranslationHandler translationHandler = new TranslationHandler();

    /* methods */

    /*
    Attempt to (re)load all of the configs.

    Order of the function calls does matter!
     */
    public boolean load() {
        Log.inf("Loading configs.");
        for(var cfg : Set.of(customDropsCfg, groupsCfg, presetsCfg, settingsCfg))
            if(!cfg.load())
                return false;

        return translationHandler.load();
    }

    /* getters and setters */

    @NotNull
    public CustomDropsCfg getCustomDropsCfg() { return customDropsCfg; }

    @NotNull
    public GroupsCfg getGroupsCfg() { return groupsCfg; }

    @NotNull
    public PresetsCfg getPresetsCfg() { return presetsCfg; }

    @NotNull
    public SettingsCfg getSettingsCfg() { return settingsCfg; }

    @NotNull
    public TranslationHandler getTranslationHandler() { return translationHandler; }

}
