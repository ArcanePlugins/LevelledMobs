/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.nametag;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class NametagHandler {

    public void sendNametag(@NotNull LivingEntity livingEntity, @NotNull Player target, @NotNull String nametag) {
        //TODO
    }

    public enum NametagVisibilityMethod {
        TARGETED,
        ATTACKED,
        TRACKING
    }

}
