/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.integration;

import me.lokka30.levelledmobs.levelling.LevelledMob;

/**
 * A 'mob owner' is any plugin that manages mobs,
 * just like LevelledMobs does. By default, LevelledMobs
 * should be configured not to mess with any other Mob-Owner's
 * mobs. Example mob owners are MythicMobs, DangerousCaves mobs,
 * and so on.
 *
 * @author lokka30
 * @since v4.0.0
 */
public interface MobOwner {

    /**
     * Check if a mob belongs to the MobOwner.
     *
     * @param mob the mob in question
     * @return if the mob belongs to the Mob-Owner or not
     * @since v4.0.0
     */
    boolean isMobOwner(LevelledMob mob);

}
