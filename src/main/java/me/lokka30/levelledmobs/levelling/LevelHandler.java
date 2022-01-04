/*
 * This file is Copyright (c) 2020-2022 lokka30.
 * This file is/was present in the LevelledMobs resource.
 * Repository: <https://github.com/lokka30/LevelledMobs>
 * Use of this source code is governed by the GNU GPL v3.0
 * license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.levelling;

import me.lokka30.levelledmobs.LevelledMobs;
import org.jetbrains.annotations.NotNull;

/**
 * This class handles the management
 * of mob levels in the plugin.
 *
 * @author lokka30
 * @since v4.0.0
 */
public class LevelHandler {

    private final LevelledMobs main;

    public LevelHandler(final LevelledMobs main) {
        this.main = main;
        this.levelledNamespacedKeys = new LevelledNamespacedKeys(main);
    }

    @NotNull public final LevelledNamespacedKeys levelledNamespacedKeys;

    /*
    TODO
        lokka30: Complete class body.
     */
}