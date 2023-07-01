/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.listeners;

import java.util.Collections;
import java.util.HashSet;
import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.misc.AdditionalLevelInformation;
import me.lokka30.levelledmobs.misc.DebugType;
import me.lokka30.levelledmobs.misc.LevellableState;
import me.lokka30.levelledmobs.wrappers.LivingEntityWrapper;
import me.lokka30.levelledmobs.rules.MobTamedStatus;
import me.lokka30.levelledmobs.util.Utils;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTameEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Listens when an entity is tamed so various rules can be applied
 *
 * @author stumper66
 * @since 2.4.0
 */
public class EntityTameListener implements Listener {

    private final LevelledMobs main;

    public EntityTameListener(final LevelledMobs main) {
        this.main = main;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    private void onEntityTameEvent(@NotNull final EntityTameEvent event) {
        final LivingEntityWrapper lmEntity = LivingEntityWrapper.getInstance(event.getEntity(),
            main);
        final LevellableState levellableState = main.levelInterface.getLevellableState(lmEntity);

        if (levellableState != LevellableState.ALLOWED) {
            Utils.debugLog(main, DebugType.ENTITY_TAME, "Levelable state was " + levellableState);
            lmEntity.free();
            return;
        }

        if (main.rulesManager.getRuleMobTamedStatus(lmEntity) == MobTamedStatus.NOT_TAMED) {
            Utils.debugLog(main, DebugType.ENTITY_TAME, "no-level-conditions.tamed = &btrue");

            // if mob was levelled then remove it
            main.levelInterface.removeLevel(lmEntity);

            Utils.debugLog(main, DebugType.ENTITY_TAME, "Removed level of tamed mob");
            lmEntity.free();
            return;
        }

        Utils.debugLog(main, DebugType.ENTITY_TAME, "Applying level to tamed mob");
        int level = -1;
        if (lmEntity.isLevelled()) {
            level = lmEntity.getMobLevel();
        }

        if (level == -1) {
            level = main.levelInterface.generateLevel(lmEntity);
            lmEntity.invalidateCache();
        }

        main.levelInterface.applyLevelToMob(
            lmEntity,
            level,
            false,
            false,
            new HashSet<>(Collections.singletonList(AdditionalLevelInformation.FROM_TAME_LISTENER))
        );
        lmEntity.free();
    }
}
