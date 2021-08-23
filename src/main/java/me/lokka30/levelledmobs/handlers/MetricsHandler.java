/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.handlers;

import me.lokka30.levelledmobs.LevelledMobs;
import org.bstats.bukkit.Metrics;

/**
 * @author lokka30
 * @since v4.0.0
 * This class handles the submission
 * of custom bStats metrics data.
 */
public class MetricsHandler {

    private final LevelledMobs main;

    public MetricsHandler(final LevelledMobs main) {
        this.main = main;
    }

    public void startMetrics() {
        new Metrics(main, 12345); //TODO Change ID
        //TODO Add custom charts
    }

    //TODO

}
