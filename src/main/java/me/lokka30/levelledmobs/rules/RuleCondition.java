/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.rules;

import me.lokka30.levelledmobs.misc.ModalList;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public record RuleCondition(
        @NotNull Optional<Boolean>                                      isLevelled,
        @NotNull Optional<ModalList<CreatureSpawnEvent.SpawnReason>>    spawnReasons,
        @NotNull Optional<ModalList<String>>                            worldNames
) {

    public boolean appliesTo(@NotNull final LivingEntity livingEntity) {
        //TODO
        return false;
    }
}
