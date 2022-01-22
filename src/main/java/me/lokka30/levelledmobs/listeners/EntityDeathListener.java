/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.listeners;

import io.github.geniot.indexedtreemap.IndexedNavigableSet;
import io.github.geniot.indexedtreemap.IndexedTreeSet;
import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.customdrops.CustomDropResult;
import me.lokka30.levelledmobs.misc.LivingEntityWrapper;
import me.lokka30.levelledmobs.misc.NametagTimerChecker;
import me.lokka30.levelledmobs.misc.Utils;
import me.lokka30.microlib.messaging.MessageUtils;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.sql.Timestamp;
import java.util.*;

/**
 * Listens for when an entity dies so it's drops can be multiplied, manipulated, etc
 *
 * @author lokka30
 * @since 2.4.0
 */
public class EntityDeathListener implements Listener {

    private final LevelledMobs main;

    public EntityDeathListener(final LevelledMobs main) {
        this.main = main;
    }

    // These entities will be forced not to have levelled drops
    private final HashSet<String> bypassDrops = new HashSet<>(Arrays.asList("ARMOR_STAND", "ITEM_FRAME", "DROPPED_ITEM", "PAINTING"));

    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
    public void onDeath(@NotNull final EntityDeathEvent event) {
        synchronized (NametagTimerChecker.entityTarget_Lock){
            main.nametagTimerChecker.entityTargetMap.remove(event.getEntity());
        }

        if (bypassDrops.contains(event.getEntityType().toString()))
            return;

        final LivingEntityWrapper lmEntity = LivingEntityWrapper.getInstance(event.getEntity(), main);
        if (event.getEntity().getKiller() != null)
            lmEntity.playerForPermissionsCheck = event.getEntity().getKiller();

        final EntityDamageEvent damage = lmEntity.getLivingEntity().getLastDamageCause();
        if (damage != null)
            lmEntity.deathCause = damage.getCause();

        if (lmEntity.getLivingEntity().getKiller() != null && main.placeholderApiIntegration != null)
            main.placeholderApiIntegration.putPlayerOrMobDeath(lmEntity.getLivingEntity().getKiller(), lmEntity);

        if (lmEntity.isLevelled() || main.rulesManager.getRule_UseCustomDropsForMob(lmEntity).useDrops) {
            long chunkKey = Utils.getChunkKey(lmEntity);
            if (!main.entityDeathInChunkCounter.containsKey(chunkKey)) {
                main.entityDeathInChunkCounter.put(chunkKey, new IndexedTreeSet<>());
            }

            int numberOfEntityDeathInChunk = Utils.getNumberOfEntityDeathInChunk(lmEntity,main,main.maximumCoolDownTime);
            /*
             Only send message for maximum threshold and cool down time
             Only message once
             This is enabled by default
             */
            if (numberOfEntityDeathInChunk == main.maximumDeathInChunkThreshold) {
                /*
                 I'll add "maximumDeathCount much" of record in the counter
                 And prohibite new record into counter
                 Then, after cooldowntime, there's no record in the counter
                 And player can normally kill new mob
                 */
                for(int i = 0; i < main.maximumDeathInChunkThreshold; i++){
                    main.entityDeathInChunkCounter.get(chunkKey).add(new MutablePair<>(new Timestamp(System.currentTimeMillis()), lmEntity));
                }

                if (lmEntity.getLivingEntity().getKiller() != null && lmEntity.getLivingEntity().getKiller() instanceof Player &&
                        main.settingsCfg.getBoolean("exceed-kill-in-chunk-message", true)) {
                    lmEntity.getLivingEntity().getKiller().
                            sendMessage(MessageUtils.colorizeAll(main.messagesCfg.getString("other.no-drop-in-chunk")));
                }
            } else if (numberOfEntityDeathInChunk < main.maximumDeathInChunkThreshold) {
                main.entityDeathInChunkCounter.get(chunkKey).add(new MutablePair<>(new Timestamp(System.currentTimeMillis()), lmEntity));
            }
        }

        if (lmEntity.isLevelled()) {

            // Set levelled item drops
            main.levelManager.setLevelledItemDrops(lmEntity, event.getDrops());

            // Set levelled exp drops
            if (event.getDroppedExp() > 0) {
                event.setDroppedExp(main.levelManager.getLevelledExpDrops(lmEntity, event.getDroppedExp()));
            }
        } else if (main.rulesManager.getRule_UseCustomDropsForMob(lmEntity).useDrops) {
            final List<ItemStack> drops = new LinkedList<>();
            final CustomDropResult result = main.customDropsHandler.getCustomItemDrops(lmEntity, drops, false);
            if (result == CustomDropResult.HAS_OVERRIDE)
                main.levelManager.removeVanillaDrops(lmEntity, event.getDrops());

            event.getDrops().addAll(drops);
        }
        lmEntity.free();
    }
}
