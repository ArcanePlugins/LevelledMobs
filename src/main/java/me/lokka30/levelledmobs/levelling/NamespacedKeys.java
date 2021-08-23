/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.levelling;

import me.lokka30.levelledmobs.LevelledMobs;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;

/**
 * @author lokka30
 * @see NamespacedKey
 * @since v4.0.0
 * This class contains a bunch of NamespacedKeys
 * that can be accessed by both LevelledMobs and any other
 * plugin that wishes to check &/or manage these Keys.
 * ALL namespaced keys in this class MUST be registered.
 */
public class NamespacedKeys {

    /**
     * @param main plugin's main class
     * @author lokka30
     * @since v4.0.0
     * Register all of LevelledMobs' namespaced keys.
     */
    public NamespacedKeys(final LevelledMobs main) {
        level = new NamespacedKey(main, "level");
    }

    /**
     * @since v4.0.0
     * What level a levelled mob is (e.g., `52`)
     */
    @NotNull
    public final NamespacedKey level;

    /*
    TODO
        lokka30: Add new namespaced-keys. Make sure they are registered - see NamespacedKeys#register(LevelledMobs).
     */
}
