package io.github.arcaneplugins.levelledmobs.bukkit.config;

import io.github.arcaneplugins.levelledmobs.bukkit.config.customdrops.CustomDropsCfg;
import io.github.arcaneplugins.levelledmobs.bukkit.config.groups.GroupsCfg;
import io.github.arcaneplugins.levelledmobs.bukkit.config.presets.PresetsCfg;
import io.github.arcaneplugins.levelledmobs.bukkit.config.settings.SettingsCfg;
import io.github.arcaneplugins.levelledmobs.bukkit.config.translations.TranslationHandler;
import io.github.arcaneplugins.levelledmobs.bukkit.debug.DebugHandler;
import io.github.arcaneplugins.levelledmobs.bukkit.util.Log;
import java.util.Set;
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
    public void load() {
        Log.inf("Loading configuration");

        for(var cfg : Set.of(customDropsCfg, groupsCfg, presetsCfg, settingsCfg))
            cfg.load();

        DebugHandler.load();

        getTranslationHandler().load();
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
