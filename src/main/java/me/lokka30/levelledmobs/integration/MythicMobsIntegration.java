/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.integration;

import me.lokka30.levelledmobs.levelling.LevelledMob;
import org.bukkit.Bukkit;

/**
 * @author lokka30
 * @since v4.0.0
 * A LevelledMobs integration.
 * Plugin:     MythicMobs
 * Author:     Xikage
 * Link:       https://www.spigotmc.org/resources/mythicmobs.5702
 */
public class MythicMobsIntegration implements Integration, MobOwner {

    @Override
    public boolean isInstalled() {
        return Bukkit.getPluginManager().isPluginEnabled("MythicMobs");
    }

    @Override
    public boolean isEnabled() {
        // TODO
        return false;
    }

    @Override
    public boolean isMobOwner(LevelledMob mob) {
        // TODO
        return false;
    }

}