/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.levelling;

import org.bukkit.entity.LivingEntity;

/**
 * @author lokka30
 * @since v4.0.0
 * This class makes it easier to access
 * aspects about a levelled mob, such
 * as what level they are.
 */
public class LevelledMob {

    private final LivingEntity livingEntity;

    public LevelledMob(final LivingEntity livingEntity) {
        this.livingEntity = livingEntity;
    }

    public LivingEntity getLivingEntity() {
        return livingEntity;
    }

    /*
    TODO
        lokka30: Complete class with methods of course.
     */
}
