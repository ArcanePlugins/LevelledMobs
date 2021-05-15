package me.lokka30.levelledmobs.listeners;

import me.lokka30.levelledmobs.LevelInterface;
import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.misc.DebugType;
import me.lokka30.levelledmobs.misc.LivingEntityWrapper;
import me.lokka30.levelledmobs.misc.Utils;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.entity.EntityTransformEvent;

import java.util.Collections;
import java.util.HashSet;

/**
 * TODO Describe...
 *
 * @author stumper66
 */
public class EntityTransformListener implements Listener {

    private final LevelledMobs main;

    public EntityTransformListener(final LevelledMobs main) {
        this.main = main;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onTransform(final EntityTransformEvent event) {

        // is the original entity a living entity
        if (!(event.getEntity() instanceof LivingEntity)) {
            Utils.debugLog(main, DebugType.ENTITY_TRANSFORM_FAIL, event.getEntity().getType().name() + ": entity was not an instance of LivingEntity");
            return;
        }

        LivingEntityWrapper lmEntity = new LivingEntityWrapper((LivingEntity) event.getEntity(), main);

        // is the original entity levelled
        if (!lmEntity.isLevelled()) {
            Utils.debugLog(main, DebugType.ENTITY_TRANSFORM_FAIL, lmEntity.getTypeName() + ": original entity was not levelled");
            return;
        }

        boolean useInheritance = false;
        int level = 1;

        if (main.rulesManager.getRule_MobLevelInheritance(lmEntity)){
            useInheritance = true;
            level = lmEntity.getMobLevel();
        }

        for (Entity transformedEntity : event.getTransformedEntities()) {
            if (!(transformedEntity instanceof LivingEntity)) {
                Utils.debugLog(main, DebugType.ENTITY_TRANSFORM_FAIL, event.getEntity().getType().name() + ": entity was not an instance of LivingEntity (loop)");
                continue;
            }

            final LivingEntityWrapper transformedLmEntity = new LivingEntityWrapper((LivingEntity) transformedEntity, main);

            final LevelInterface.LevellableState levelledState = main.levelInterface.getLevellableState(transformedLmEntity);
            if (levelledState != LevelInterface.LevellableState.ALLOWED) {
                Utils.debugLog(main, DebugType.ENTITY_TRANSFORM_FAIL, transformedEntity.getType().name() + ": transformed entity was not levellable, reason: " + levelledState);
                main.levelManager.updateNametagWithDelay(transformedLmEntity, 1);
                continue;
            }

            if (useInheritance) {
                main.levelInterface.applyLevelToMob(
                        transformedLmEntity,
                        level,
                        false,
                        false,
                        new HashSet<>(Collections.singletonList(LevelInterface.AdditionalLevelInformation.FROM_TRANSFORM_LISTENER))
                );
            }
            else
                main.levelManager.entitySpawnListener.preprocessMob(transformedLmEntity, new EntitySpawnEvent(transformedEntity));

            main.levelManager.updateNametag(lmEntity);
        }
    }
}
