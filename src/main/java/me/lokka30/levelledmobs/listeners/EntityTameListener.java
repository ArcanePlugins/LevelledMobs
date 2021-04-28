package me.lokka30.levelledmobs.listeners;

import me.lokka30.levelledmobs.LevelInterface;
import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.misc.DebugType;
import me.lokka30.levelledmobs.misc.LivingEntityWrapper;
import me.lokka30.levelledmobs.misc.Utils;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTameEvent;
import org.bukkit.persistence.PersistentDataType;

import java.util.Collections;
import java.util.HashSet;

/**
 * TODO Describe...
 *
 * @author stumper66
 */
public class EntityTameListener implements Listener {

    private final LevelledMobs main;
    public EntityTameListener(final LevelledMobs main) {
        this.main = main;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    private void onEntityTameEvent(EntityTameEvent event) {
        final LivingEntityWrapper lmEntity = new LivingEntityWrapper(event.getEntity(), main);

        if (main.settingsCfg.getBoolean("no-level-conditions.tamed")) {
            Utils.debugLog(main, DebugType.ENTITY_TAME, "no-level-conditions.tamed = true");

            // if mob was levelled then remove it
            main.levelInterface.removeLevel(lmEntity);

            Utils.debugLog(main, DebugType.ENTITY_TAME, "Removed level of tamed mob");
            return;
        }

        Utils.debugLog(main, DebugType.ENTITY_TAME, "Applying level to tamed mob");
        int level = -1;
        if (lmEntity.isLevelled())
            level = lmEntity.getMobLevel();

        main.levelInterface.applyLevelToMob(
                lmEntity,
                level,
                false,
                false,
                new HashSet<>(Collections.singletonList(LevelInterface.AdditionalLevelInformation.FROM_TAME_LISTENER))
        );
    }
}
