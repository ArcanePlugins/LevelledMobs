/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.file;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.util.Utils;

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
    //TODO add external files.

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
        Utils.LOGGER.info("Started loading internal files...");

        Utils.LOGGER.info("All internal files have been loaded.");
    }

    /**
     * @author lokka30
     * @since v4.0.0
     * (Re)load all external files.
     * This must be called on start-up and also on reload.
     */
    public void loadExternalFiles() {
        Utils.LOGGER.info("Started loading external files...");

        Utils.LOGGER.info("All external files have been loaded.");
    }

}
