package me.lokka30.levelledmobs.listeners;

import me.lokka30.levelledmobs.LevelInterface;
import me.lokka30.levelledmobs.LevelledMobs;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTransformEvent;
import org.bukkit.persistence.PersistentDataType;

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
        if (!main.settingsCfg.getBoolean("level-inheritance")) return;

        // is the original entity a living entity
        if (!(event.getEntity() instanceof LivingEntity)) return;

        final LivingEntity livingEntity = (LivingEntity) event.getEntity();

        // is the original entity levelled
        if (!main.levelInterface.isLevelled(livingEntity)) return;

        final Integer level = livingEntity.getPersistentDataContainer().get(main.levelManager.levelKey, PersistentDataType.INTEGER);
        assert level != null;

        for (Entity transformedEntity : event.getTransformedEntities()) {

            if (!(transformedEntity instanceof LivingEntity)) continue;

            final LivingEntity transformedLivingEntity = (LivingEntity) transformedEntity;

            if (main.levelInterface.getLevellableState(transformedLivingEntity) != LevelInterface.LevellableState.ALLOWED) {
                main.levelManager.updateNametagWithDelay(transformedLivingEntity, null, livingEntity.getWorld().getPlayers(), 1);
                continue;
            }

            main.levelInterface.applyLevelToMob(transformedLivingEntity, level, false, false);
        }
    }
}
