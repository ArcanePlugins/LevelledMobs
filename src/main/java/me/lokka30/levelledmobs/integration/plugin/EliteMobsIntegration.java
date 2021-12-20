/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.integration.plugin;

import me.lokka30.levelledmobs.integration.Integration;
import me.lokka30.levelledmobs.integration.MobOwner;
import me.lokka30.levelledmobs.levelling.LevelledMob;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

/**
 * A LevelledMobs integration.
 * Plugin:     EliteMobs
 * Author:     MagmaGuy
 * Link:       https://www.spigotmc.org/resources/elitemobs.40090/
 *
 * @author lokka30
 * @since v4.0.0
 */
public class EliteMobsIntegration implements Integration, MobOwner {

    @Override
    public @NotNull String getName() {
        return this.getClass().getName();
    }

    @Override
    public boolean isInstalled() {
        return Bukkit.getPluginManager().isPluginEnabled("EliteMobs");
    }

    @Override
    public boolean isMobOwner(LevelledMob mob) {
        // TODO
        return false;
    }

}