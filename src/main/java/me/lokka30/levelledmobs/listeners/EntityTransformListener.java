/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.listeners;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.managers.DebugManager;
import me.lokka30.levelledmobs.misc.AdditionalLevelInformation;
import me.lokka30.levelledmobs.misc.DebugType;
import me.lokka30.levelledmobs.misc.LevellableState;
import me.lokka30.levelledmobs.wrappers.LivingEntityWrapper;
import me.lokka30.levelledmobs.rules.LevelledMobSpawnReason;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.entity.EntityTransformEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Listens for when a mob transforms so the applicable rules can be applied
 *
 * @author stumper66
 * @version 2.4.0
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
            DebugManager.log(DebugType.ENTITY_MISC, event.getEntity(), false, () ->
                    event.getEntity().getType().name() +
                ": entity was &bnot&7 an instance of LivingEntity");
            return;
        }

        // is the original entity levelled
        if (!main.levelManager.isLevelled((LivingEntity) event.getEntity())) {
            DebugManager.log(DebugType.ENTITY_MISC, event.getEntity(), false, () ->
                    event.getEntity().getName() + ": original entity was &bnot&7 levelled");
            if (event.getTransformReason() == EntityTransformEvent.TransformReason.SPLIT)
                checkForSlimeSplit((LivingEntity) event.getEntity(), event.getTransformedEntities());
            return;
        }

        final LivingEntityWrapper lmEntity = LivingEntityWrapper.getInstance(
            (LivingEntity) event.getEntity(), main);

        boolean useInheritance = false;
        int level = 1;

        if (main.rulesManager.getRuleMobLevelInheritance(lmEntity)) {
            useInheritance = true;
            level = lmEntity.getMobLevel();
        }

        for (final Entity transformedEntity : event.getTransformedEntities()) {
            if (!(transformedEntity instanceof LivingEntity)) {
                DebugManager.log(DebugType.ENTITY_MISC, event.getEntity(), false, () ->
                        event.getEntity().getType().name()
                        + ": entity was&b not&7 an instance of LivingEntity (loop)");
                continue;
            }

            final LivingEntityWrapper transformedLmEntity = LivingEntityWrapper.getInstance(
                (LivingEntity) transformedEntity, main);
            final LevellableState levelledState = main.levelInterface.getLevellableState(
                transformedLmEntity);
            if (levelledState != LevellableState.ALLOWED) {
                DebugManager.log(DebugType.ENTITY_MISC, event.getEntity(), false, () ->
                        transformedEntity.getType().name()
                        + ": transformed entity was &bnot&7 levellable, reason: &b"
                        + levelledState);
                main.levelManager.updateNametagWithDelay(transformedLmEntity);
                transformedLmEntity.free();
                continue;
            }

            DebugManager.log(DebugType.ENTITY_MISC, event.getEntity(), true, () ->
                    transformedEntity.getType().name() + ": entity was transformed");

            if (useInheritance) {
                if (lmEntity.getSpawnReason() == LevelledMobSpawnReason.LM_SPAWNER) {
                    transformedLmEntity.setSpawnReason(LevelledMobSpawnReason.SPAWNER);
                }

                main.levelInterface.applyLevelToMob(
                    transformedLmEntity,
                    level,
                    false,
                    false,
                    new HashSet<>(Collections.singletonList(
                        AdditionalLevelInformation.FROM_TRANSFORM_LISTENER))
                );
            } else {
                main.levelManager.entitySpawnListener.preprocessMob(transformedLmEntity,
                    new EntitySpawnEvent(transformedEntity));
            }

            main.levelManager.updateNametagWithDelay(lmEntity);
            transformedLmEntity.free();
        }

        lmEntity.free();
    }

    private void checkForSlimeSplit(final @NotNull LivingEntity livingEntity, final @NotNull List<Entity> transformedEntities){
        final LivingEntityWrapper parent = LivingEntityWrapper.getInstance(livingEntity, main);
        if (parent.getSpawnReason() == LevelledMobSpawnReason.DEFAULT ||
                parent.getSpawnReason() == LevelledMobSpawnReason.SLIME_SPLIT){
            parent.free();
            return;
        }

        for (final Entity transformedEntity : transformedEntities) {
            if (!(transformedEntity instanceof final LivingEntity le)) continue;

            final LivingEntityWrapper lew = LivingEntityWrapper.getInstance(le, main);
            lew.setSpawnReason(parent.getSpawnReason());
            lew.free();
        }

        parent.free();
    }
}
