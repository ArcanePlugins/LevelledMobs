/*
 * This file is Copyright (c) 2020-2022 lokka30.
 * This file is/was present in the LevelledMobs resource.
 * Repository: <https://github.com/lokka30/LevelledMobs>
 * Use of this source code is governed by the GNU GPL v3.0
 * license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.rules;

import me.lokka30.levelledmobs.util.ModalList;
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
