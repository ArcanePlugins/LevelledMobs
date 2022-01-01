/*
 * This file is Copyright (c) 2020-2022 lokka30.
 * This file is/was present in the LevelledMobs resource.
 * Repository: <https://github.com/lokka30/LevelledMobs>
 * Use of this source code is governed by the GNU GPL v3.0
 * license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.levelling;

import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

/**
 * This class makes it easier to access
 * aspects about a levelled mob, such
 * as what level they are.
 *
 * @author lokka30
 * @since v4.0.0
 */
public class LevelledMob {

    public LevelledMob(@NotNull final LivingEntity livingEntity) { this.livingEntity = livingEntity; }

    @NotNull public final LivingEntity livingEntity;

    /*
    TODO
        lokka30: Complete class with methods of course.
     */
}
