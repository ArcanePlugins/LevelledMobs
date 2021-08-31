/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.files;

import de.leonhard.storage.Config;
import de.leonhard.storage.LightningBuilder;
import de.leonhard.storage.internal.settings.ConfigSettings;
import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.misc.Utils;
import org.jetbrains.annotations.Nullable;

import java.io.File;

/**
 * @author lokka30
 * @since v4.0.0
 * This class handles the management
 * of the LM files, internal and external.
 */
public class FileHandler {

    private final LevelledMobs main;

    public FileHandler(final LevelledMobs main) {
        this.main = main;
    }

    //TODO

    /* External Files */

    // advanced.yml
    @Nullable
    private Config advancedCfg = null;

    @Nullable
    public Config getAdvancedCfg() {
        return advancedCfg;
    }


    // customdrops.yml
    @Nullable
    private Config customDropsCfg = null;

    @Nullable
    public Config getCustomDropsCfg() {
        return customDropsCfg;
    }

    // rules.yml
    @Nullable
    private Config rulesCfg = null;

    @Nullable
    public Config getRulesCfg() {
        return rulesCfg;
    }

    // translations.yml
    @Nullable
    private Config translationsCfg = null;

    @Nullable
    public Config getTranslationsCfg() {
        return translationsCfg;
    }

    /* Internal Files */
    //TODO add internal files.

    /* Methods */

    /**
     * @author lokka30
     * @since v4.0.0
     * Load all internal files.
     * Unnecessary to run on reload - improve performance by
     * not running this method on reload. Thus it should only
     * be ran on start-up (onEnable).
     */
    public void loadInternalFiles() {
        Utils.LOGGER.info("&3FileHandler: &7Started loading internal files...");

        //TODO load static mob data file
        main.staticMobDataHandler.loadMobData();

        //TODO add the other internal files to here.

        Utils.LOGGER.info("&3FileHandler: &7All external files have been loaded.");
    }

    /**
     * @author lokka30
     * @since v4.0.0
     * (Re)load all external files.
     * This must be called on start-up and also on reload.
     */
    public void loadExternalFiles() {
        Utils.LOGGER.info("&3FileHandler: &7Started loading external files...");

        Utils.LOGGER.info("&3FileHandler: &7Loading external file '&badvanced.yml&7'...");
        advancedCfg = LightningBuilder
                .fromFile(new File(main.getDataFolder(), "advanced.yml"))
                .setConfigSettings(ConfigSettings.PRESERVE_COMMENTS)
                .createConfig();

        Utils.LOGGER.info("&3FileHandler: &7Loading external file '&bcustomdrops.yml&7'...");
        customDropsCfg = LightningBuilder
                .fromFile(new File(main.getDataFolder(), "customdrops.yml"))
                .setConfigSettings(ConfigSettings.PRESERVE_COMMENTS)
                .createConfig();

        Utils.LOGGER.info("&3FileHandler: &7Loading external file '&brules.yml&7'...");
        rulesCfg = LightningBuilder
                .fromFile(new File(main.getDataFolder(), "rules.yml"))
                .setConfigSettings(ConfigSettings.PRESERVE_COMMENTS)
                .createConfig();

        Utils.LOGGER.info("&3FileHandler: &7Loading external file '&btranslations.yml&7'...");
        translationsCfg = LightningBuilder
                .fromFile(new File(main.getDataFolder(), "translations.yml"))
                .setConfigSettings(ConfigSettings.PRESERVE_COMMENTS)
                .createConfig();

        Utils.LOGGER.info("&3FileHandler: &7Loading external file '&blicense.txt&7'...");
        main.saveResource("license.txt", true);

        Utils.LOGGER.info("&3FileHandler: &7All external files have been loaded.");
    }

}
