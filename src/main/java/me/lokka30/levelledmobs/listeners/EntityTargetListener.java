/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.listeners;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.misc.LivingEntityWrapper;
import me.lokka30.levelledmobs.misc.NametagTimerChecker;
import me.lokka30.levelledmobs.misc.QueueItem;
import me.lokka30.levelledmobs.rules.NametagVisibilityEnum;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTargetEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Used as a workaround to ensure mob nametags are properly updated
 *
 * @author stumper66
 * @since 2.4.0
 */
public class EntityTargetListener implements Listener {

    private final LevelledMobs main;

    public EntityTargetListener(final LevelledMobs main) {
        this.main = main;
    }

    /**
     * This event is listened to update the nametag of a mob when they start targeting a player.
     * Should provide another band-aid for packets not appearing sometimes for mob nametags.
     *
     * @param event EntityTargetEvent
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onTarget(@NotNull final EntityTargetEvent event) {
        if (!(event.getEntity() instanceof LivingEntity)) return;

        if (event.getTarget() == null){
            synchronized (NametagTimerChecker.entityTarget_Lock){
                main.nametagTimerChecker.entityTargetMap.remove((LivingEntity) event.getEntity());
            }
            return;
        }

        // Must target a player and must be a living entity
        if (!(event.getTarget() instanceof Player)) return;

        final LivingEntityWrapper lmEntity = LivingEntityWrapper.getInstance((LivingEntity) event.getEntity(), main);

        // Must be a levelled entity
        if (!lmEntity.isLevelled()){
            if (main.levelManager.entitySpawnListener.processMobSpawns) {
                lmEntity.free();
                return;
            }

            if (lmEntity.getMobLevel() < 0) lmEntity.reEvaluateLevel = true;
            main._mobsQueueManager.addToQueue(new QueueItem(lmEntity, event));
            return;
        }

        if (main.rulesManager.getRule_CreatureNametagVisbility(lmEntity).contains(NametagVisibilityEnum.TRACKING)) {
            synchronized (NametagTimerChecker.entityTarget_Lock) {
                main.nametagTimerChecker.entityTargetMap.put(lmEntity.getLivingEntity(), (Player) event.getTarget());
            }
        }

        // Update the nametag.
        main.levelManager.updateNametag(lmEntity);
        lmEntity.free();
    }
}
