/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.file.external.rules;

import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

public interface Executable {

    void run(@NotNull LivingEntity livingEntity, @NotNull String[] args);

}