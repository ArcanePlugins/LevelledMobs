package me.lokka30.levelledmobs.listeners;

import me.lokka30.levelledmobs.LevelInterface;
import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.misc.DebugType;
import me.lokka30.levelledmobs.misc.LivingEntityWrapper;
import me.lokka30.levelledmobs.misc.Utils;
import me.lokka30.levelledmobs.rules.MobTamedStatusEnum;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTameEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashSet;

/**
 * Listens when an entity is tamed so various rules can be applied
 *
 * @author stumper66
 */
public class EntityTameListener implements Listener {

    private final LevelledMobs main;
    public EntityTameListener(final LevelledMobs main) {
        this.main = main;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    private void onEntityTameEvent(@NotNull final EntityTameEvent event) {
        final LivingEntityWrapper lmEntity = new LivingEntityWrapper(event.getEntity(), main);

        if (main.rulesManager.getRule_MobTamedStatus(lmEntity) == MobTamedStatusEnum.NOT_TAMED) {
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

        if (level == -1) {
            level = main.levelInterface.generateLevel(lmEntity);
            lmEntity.invalidateCache();
        }

        main.levelInterface.applyLevelToMob(
                lmEntity,
                level,
                false,
                false,
                new HashSet<>(Collections.singletonList(LevelInterface.AdditionalLevelInformation.FROM_TAME_LISTENER))
        );
    }
}
