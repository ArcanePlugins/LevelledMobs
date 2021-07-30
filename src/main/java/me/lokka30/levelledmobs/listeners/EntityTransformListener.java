/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.listeners;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.misc.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.entity.EntityTransformEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashSet;

/**
 * Listens for when a mob transforms so the applicable rules can be applied
 *
 * @author stumper66
 */
public class EntityTransformListener implements Listener {

    private final LevelledMobs main;

    public EntityTransformListener(final LevelledMobs main) {
        this.main = main;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onTransform(@NotNull final EntityTransformEvent event) {
        // is the original entity a living entity
        if (!(event.getEntity() instanceof LivingEntity)) {
            Utils.debugLog(main, DebugType.ENTITY_TRANSFORM_FAIL, event.getEntity().getType().name() + ": entity was &bnot&7 an instance of LivingEntity");
            return;
        }

        final LivingEntityWrapper lmEntity = new LivingEntityWrapper((LivingEntity) event.getEntity(), main);

        // is the original entity levelled
        if (!lmEntity.isLevelled()) {
            Utils.debugLog(main, DebugType.ENTITY_TRANSFORM_FAIL, lmEntity.getTypeName() + ": original entity was &bnot&7 levelled");
            return;
        }

        boolean useInheritance = false;
        int level = 1;

        if (main.rulesManager.getRule_MobLevelInheritance(lmEntity)){
            useInheritance = true;
            level = lmEntity.getMobLevel();
        }

        for (final Entity transformedEntity : event.getTransformedEntities()) {
            if (!(transformedEntity instanceof LivingEntity)) {
                Utils.debugLog(main, DebugType.ENTITY_TRANSFORM_FAIL, event.getEntity().getType().name() + ": entity was&b not&7 an instance of LivingEntity (loop)");
                continue;
            }

            final LivingEntityWrapper transformedLmEntity = new LivingEntityWrapper((LivingEntity) transformedEntity, main);

            final LevellableState levelledState = main.levelInterface.getLevellableState(transformedLmEntity);
            if (levelledState != LevellableState.ALLOWED) {
                Utils.debugLog(main, DebugType.ENTITY_TRANSFORM_FAIL, transformedEntity.getType().name() + ": transformed entity was &bnot&7 levellable, reason: &b" + levelledState);
                main.levelManager.updateNametag_WithDelay(transformedLmEntity);
                continue;
            }

            if (useInheritance) {
                main.levelInterface.applyLevelToMob(
                        transformedLmEntity,
                        level,
                        false,
                        false,
                        new HashSet<>(Collections.singletonList(AdditionalLevelInformation.FROM_TRANSFORM_LISTENER))
                );
            } else
                main.levelManager.entitySpawnListener.preprocessMob(transformedLmEntity, new EntitySpawnEvent(transformedEntity));

            main.levelManager.updateNametag_WithDelay(lmEntity);
        }
    }
}
