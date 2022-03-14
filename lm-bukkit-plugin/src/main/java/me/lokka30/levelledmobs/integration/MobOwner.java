/*
 * This file is Copyright (c) 2020-2022 lokka30.
 * This file is/was present in the LevelledMobs resource.
 * Repository: <https://github.com/lokka30/LevelledMobs>
 * Use of this source code is governed by the GNU GPL v3.0
 * license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.integration;

import me.lokka30.levelledmobs.level.LevelledMob;
import org.jetbrains.annotations.NotNull;

/**
 * A 'mob owner' is any plugin that manages mobs, just like LevelledMobs does. By default,
 * LevelledMobs should be configured not to mess with any other Mob-Owner's mobs. Example mob owners
 * are MythicMobs, DangerousCaves mobs, and so on.
 *
 * @author lokka30
 * @version 1
 * @since 4.0.0
 */
public interface MobOwner {

    /**
     * Check if a mob belongs to the MobOwner.
     *
     * @param mob the mob in question
     * @return if the mob belongs to the Mob-Owner or not
     * @author lokka30
     * @since 4.0.0
     */
    boolean isMobOwner(@NotNull final LevelledMob mob);

}
