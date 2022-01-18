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
import me.lokka30.levelledmobs.level.LevelledMob;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A LevelledMobs integration.
 * Plugin:     EcoBosses
 * Author:     Auxilor
 * Link:       https://www.spigotmc.org/resources/ecobosses.86576/
 *
 * @author lokka30
 * @since 4.0.0
 */
public class EcoBossesIntegration implements Integration, MobOwner {

    @Nullable private NamespacedKey bossKey = null;

    @Override
    public @NotNull String getName() {
        return this.getClass().getName();
    }

    @Override
    public boolean isInstalled() {
        return Bukkit.getPluginManager().isPluginEnabled("EcoBosses");
    }

    @Override
    public boolean isMobOwner(@NotNull LevelledMob mob) {
        assert isEnabled();

        // if the key is not set, set it
        if(bossKey == null) {
            bossKey = new NamespacedKey(
                    Bukkit.getPluginManager().getPlugin("EcoBosses"),
                    "boss"
            );
        }

        // check if the entity belongs to the other plugin
        return mob.getPDC().has(bossKey, PersistentDataType.STRING);
    }

}