package me.lokka30.levelledmobs.listeners;

import me.lokka30.levelledmobs.LevelInterface;
import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.misc.DebugType;
import me.lokka30.levelledmobs.misc.Utils;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTransformEvent;
import org.bukkit.persistence.PersistentDataType;

import java.util.Collections;
import java.util.HashSet;

/**
 * @author stumper66
 * @contributors lokka30
 */
public class EntityTransformListener implements Listener {

    private final LevelledMobs main;

    public EntityTransformListener(final LevelledMobs main) {
        this.main = main;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onTransform(final EntityTransformEvent event) {

        // is level inheritance enabled?
        if (!main.settingsCfg.getBoolean("level-inheritance")) {
            Utils.debugLog(main, DebugType.ENTITY_TAME,  event.getEntity().getType().name() + ": level-inheritance not enabled");
            return;
        }

        // is the original entity a living entity
        if (!(event.getEntity() instanceof LivingEntity)) {
            Utils.debugLog(main, DebugType.ENTITY_TAME, event.getEntity().getType().name() + ": entity was not an instance of LivingEntity");
            return;
        }

        final LivingEntity livingEntity = (LivingEntity) event.getEntity();

        // is the original entity levelled
        if (!main.levelInterface.isLevelled(livingEntity)) {
            Utils.debugLog(main, DebugType.ENTITY_TAME, livingEntity.getType().name() + ": original entity was not levelled");
            return;
        }

        final Integer level = livingEntity.getPersistentDataContainer().get(main.levelManager.levelKey, PersistentDataType.INTEGER);
        assert level != null;

        for (Entity transformedEntity : event.getTransformedEntities()) {

            if (!(transformedEntity instanceof LivingEntity)) {
                Utils.debugLog(main, DebugType.ENTITY_TAME, event.getEntity().getType().name() + ": entity was not an instance of LivingEntity (loop)");
                continue;
            }

            final LivingEntity transformedLivingEntity = (LivingEntity) transformedEntity;

            final LevelInterface.LevellableState levelledState = main.levelInterface.getLevellableState(transformedLivingEntity);
            if (levelledState != LevelInterface.LevellableState.ALLOWED) {
                Utils.debugLog(main, DebugType.ENTITY_TAME, transformedEntity.getType().name() + ": transformed entity was not levellable, reason: " + levelledState);
                main.levelManager.updateNametagWithDelay(transformedLivingEntity, null, livingEntity.getWorld().getPlayers(), 1);
                continue;
            }

            Utils.debugLog(main, DebugType.ENTITY_TAME, transformedEntity.getType().name() + ": applying level " + level + " to transformed mob");
            main.levelInterface.applyLevelToMob(
                    transformedLivingEntity,
                    level,
                    false,
                    false,
                    new HashSet<>(Collections.singletonList(LevelInterface.AdditionalLevelInformation.FROM_TRANSFORM_LISTENER))
            );
        }
    }
}
