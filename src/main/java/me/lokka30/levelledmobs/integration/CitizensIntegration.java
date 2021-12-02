/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.integration;

import me.lokka30.levelledmobs.levelling.LevelledMob;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

/**
 * A LevelledMobs integration.
 * Plugin:     Citizens
 * Author:     fullwall
 * Link:       https://www.spigotmc.org/resources/citizens.13811/
 *
 * @author lokka30
 * @since v4.0.0
 */
public class CitizensIntegration implements Integration, MobOwner {

    @Override
    public @NotNull String getName() {
        return this.getClass().getName();
    }

    @Override
    public boolean isInstalled() {
        return Bukkit.getPluginManager().isPluginEnabled("Citizens");
    }

    @Override
    public boolean isMobOwner(LevelledMob mob) {
        return mob.livingEntity.hasMetadata("NPC");
    }

}