/*
 * This file is Copyright (c) 2020-2022 lokka30.
 * This file is/was present in the LevelledMobs resource.
 * Repository: <https://github.com/lokka30/LevelledMobs>
 * Use of this source code is governed by the GNU GPL v3.0
 * license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.integration.plugin;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.integration.Integration;
import me.lokka30.levelledmobs.integration.MobOwner;
import me.lokka30.levelledmobs.level.LevelledMob;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

/**
 * A LevelledMobs integration.
 * Plugin:     Boss
 * Author:     kangarko
 * Link:       https://www.spigotmc.org/threads/boss.271104/
 *
 * @author lokka30
 * @since v4.0.0
 */
public class BossIntegration implements Integration, MobOwner {

    private @NotNull final LevelledMobs main;
    public BossIntegration(final @NotNull LevelledMobs main) { this.main = main; }

    @Override
    public @NotNull String getName() {
        return this.getClass().getName();
    }

    @Override
    public boolean isInstalled() {
        return Bukkit.getPluginManager().isPluginEnabled("Boss");
    }

    @Override
    public boolean isMobOwner(final @NotNull LevelledMob mob) {
        assert isEnabled(main);
        // TODO
        return false;
    }

}
