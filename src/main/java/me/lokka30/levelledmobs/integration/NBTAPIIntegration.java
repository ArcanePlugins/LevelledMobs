/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.integration;

import org.bukkit.Bukkit;

/**
 * @author lokka30
 * @since v4.0.0
 * A LevelledMobs integration.
 * Plugin:     NBT API
 * Author:     tr7zw
 * Link:       https://www.spigotmc.org/resources/nbt-api.7939/
 */
public class NBTAPIIntegration implements Integration {

    @Override
    public boolean isInstalled() {
        return Bukkit.getPluginManager().isPluginEnabled("NBTAPI");
    }

    @Override
    public boolean isEnabled() {
        // TODO
        return false;
    }

}
