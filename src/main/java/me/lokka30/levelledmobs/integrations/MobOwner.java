/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.integrations;

import me.lokka30.levelledmobs.levelling.LevelledMob;

/**
 * @author lokka30
 * @since v4.0.0
 * A 'mob owner' is any plugin that manages mobs,
 * just like LevelledMobs does. By default, LevelledMobs
 * should be configured not to mess with any other Mob-Owner's
 * mobs. Example mob owners are MythicMobs, DangerousCaves mobs,
 * and so on.
 */
public interface MobOwner {

    /**
     * @param mob the mob in question
     * @return if the mob belongs to the Mob-Owner or not
     */
    boolean isMobOwner(LevelledMob mob);
    /*
    TODO
        lokka30: Add the LevelledMob class to solve the error.
     */
}
