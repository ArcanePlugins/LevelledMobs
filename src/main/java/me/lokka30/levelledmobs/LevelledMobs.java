/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs;

import me.lokka30.levelledmobs.levelling.NamespacedKeys;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * @author lokka30
 * @since v4.0.0
 */
public class LevelledMobs extends JavaPlugin {

    @Override
    public void onLoad() {
        NamespacedKeys.register(this);
        //TODO lokka30: Complete this method's body.
    }

    /**
     * @author lokka30
     * @since v4.0.0
     * <p>
     * Called by Bukkit's plugin manager
     * when it enables the plugin.
     * Ensure reloads are factored in to
     * any code ran inside this method.
     */
    @Override
    public void onEnable() {
        //TODO lokka30: Complete this method's body.
    }

    /**
     * @author lokka30
     * @since v4.0.0
     * <p>
     * Called by Bukkit's plugin manager
     * when it enables the plugin.
     * Ensure reloads are factored in to
     * any code ran inside this method.
     */
    @Override
    public void onDisable() {
        //TODO lokka30: Complete this method's body.
    }
}
