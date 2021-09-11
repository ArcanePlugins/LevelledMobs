/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.levelling;

import me.lokka30.levelledmobs.LevelledMobs;
import org.jetbrains.annotations.NotNull;

/**
 * @author lokka30
 * @since v4.0.0
 * This class handles the management
 * of mob levels in the plugin.
 */
public class LevelHandler {

    private final LevelledMobs main;

    public LevelHandler(final LevelledMobs main) {
        this.main = main;
        this.namespacedKeys = new NamespacedKeys(main);
    }

    @NotNull public final NamespacedKeys namespacedKeys;

    /*
    TODO
        lokka30: Complete class body.
     */
}
