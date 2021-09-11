/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.levelling;

import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

/**
 * @author lokka30
 * @since v4.0.0
 * This class makes it easier to access
 * aspects about a levelled mob, such
 * as what level they are.
 */
public class LevelledMob {

    public LevelledMob(@NotNull final LivingEntity livingEntity) { this.livingEntity = livingEntity; }

    @NotNull public final LivingEntity livingEntity;

    /*
    TODO
        lokka30: Complete class with methods of course.
     */
}
