/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.integration;

import me.lokka30.levelledmobs.levelling.LevelledMob;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

/**
 * A LevelledMobs integration.
 * Plugin:     ItemsAdder
 * Author:     LoneDev
 * Link:       https://www.spigotmc.org/resources/itemsadder.73355/
 *
 * @author lokka30
 * @since v4.0.0
 */
public class ItemsAdderIntegration implements Integration, MobOwner {

    @Override
    @NotNull
    public String getName() {
        return this.getClass().getName();
    }

    @Override
    public boolean isInstalled() {
        return Bukkit.getPluginManager().isPluginEnabled("ItemsAdder");
    }

    @Override
    public boolean isMobOwner(LevelledMob mob) {
        // TODO
        return false;
    }

}