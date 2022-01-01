/*
 * This file is Copyright (c) 2020-2022 lokka30.
 * This file is/was present in the LevelledMobs resource.
 * Repository: <https://github.com/lokka30/LevelledMobs>
 * Use of this source code is governed by the GNU GPL v3.0
 * license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.integration.plugin;

import me.lokka30.levelledmobs.integration.Integration;
import me.lokka30.levelledmobs.integration.MobOwner;
import me.lokka30.levelledmobs.levelling.LevelledMob;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A LevelledMobs integration.
 * Plugin:     Dangerous Caves 2
 * Author:     imDaniX (v2 maintainer), OkDexter12 (v1 maintainer)
 * Link:       https://www.spigotmc.org/resources/dangerous-caves-2.76212/
 *
 * @author lokka30
 * @since v4.0.0
 */
public class DangerousCavesIntegration implements Integration, MobOwner {

    @Nullable private NamespacedKey mobTypeKey = null;

    @Override
    public @NotNull String getName() {
        return this.getClass().getName();
    }

    @Override
    public boolean isInstalled() {
        return Bukkit.getPluginManager().isPluginEnabled("DangerousCaves");
    }

    @Override
    public boolean isMobOwner(LevelledMob mob) {

        // get the other plugin's main class
        final Plugin dangerousCavesPlugin = Bukkit.getPluginManager().getPlugin("DangerousCaves");

        // make sure it is installed
        if(dangerousCavesPlugin == null) return false;

        // if the key is not set, set it
        if(mobTypeKey == null) mobTypeKey = new NamespacedKey(dangerousCavesPlugin, "mob-type");

        // check if the entity belongs to the other plugin
        return mob.livingEntity.getPersistentDataContainer().has(mobTypeKey, PersistentDataType.STRING);
    }

}