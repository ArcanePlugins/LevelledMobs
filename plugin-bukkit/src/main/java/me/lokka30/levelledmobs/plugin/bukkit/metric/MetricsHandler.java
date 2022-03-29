/*
 * This file is Copyright (c) 2020-2022 lokka30.
 * This file is/was present in the LevelledMobs resource.
 * Repository: <https://github.com/lokka30/LevelledMobs>
 * Use of this source code is governed by the GNU GPL v3.0
 * license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.plugin.bukkit.metric;

import me.lokka30.levelledmobs.plugin.bukkit.LevelledMobs;
import org.bstats.bukkit.Metrics;

/**
 * @author lokka30
 * @since 4.0.0 This class handles the submission of custom bStats metrics data.
 */
public class MetricsHandler {

    //TODO: make metrics work again
    public void startMetrics() {
        final Metrics metrics = new Metrics(LevelledMobs.getInstance(), 6269);
//        metrics.addCustomChart(new SimplePie("maxlevel_used", metricsInfo::getMaxLevelRange));
//        metrics.addCustomChart(new SimplePie("custom_rules_used", metricsInfo::getCustomRulesUsed));
//        metrics.addCustomChart(new SimplePie("custom_drops_enabled", metricsInfo::getUsesCustomDrops));
//        metrics.addCustomChart(new SimplePie("health_indicator_enabled", metricsInfo::getUsesHealthIndicator));
//        metrics.addCustomChart(new SimplePie("levelling_strategy", metricsInfo::getLevellingStrategy));
//        metrics.addCustomChart(new SimplePie("autoupdate_checker_enabled", metricsInfo::usesAutoUpdateChecker));
//        metrics.addCustomChart(new SimplePie("level_mobs_upon_spawn", metricsInfo::levelMobsUponSpawn));
//        metrics.addCustomChart(new SimplePie("check_mobs_on_chunk_load", metricsInfo::checkMobsOnChunkLoad));
//        metrics.addCustomChart(new SimplePie("custom-entity-names", metricsInfo::customEntityNamesCount));
//        metrics.addCustomChart(new SimplePie("utilizes-nbtdata", metricsInfo::usesNbtData));
//        metrics.addCustomChart(new SimplePie("utilizes_player_levelling", metricsInfo::usesPlayerLevelling));
//        metrics.addCustomChart(new SimplePie("nametag_visibility", metricsInfo::nametagVisibility));
//        metrics.addCustomChart(new SimpleBarChart("enabled-compatibility", metricsInfo::enabledCompats));
    }

}
