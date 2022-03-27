/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.listeners;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.customdrops.CustomDropResult;
import me.lokka30.levelledmobs.misc.ChunkKillInfo;
import me.lokka30.levelledmobs.misc.DebugType;
import me.lokka30.levelledmobs.misc.LivingEntityWrapper;
import me.lokka30.levelledmobs.misc.NametagTimerChecker;
import me.lokka30.levelledmobs.misc.Utils;
import me.lokka30.microlib.messaging.MessageUtils;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


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

    // These entity types will be forced not to be processed
    private final List<EntityType> bypassEntity = List.of(EntityType.ARMOR_STAND, EntityType.ITEM_FRAME, EntityType.DROPPED_ITEM, EntityType.PAINTING);

    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
    public void onDeath(@NotNull final EntityDeathEvent event) {
        if (event.getEntity() instanceof Player) return;
        if (bypassEntity.contains(event.getEntityType())) return;

        synchronized (NametagTimerChecker.entityTarget_Lock){
            main.nametagTimerChecker.entityTargetMap.remove(event.getEntity());
        }

        final LivingEntityWrapper lmEntity = LivingEntityWrapper.getInstance(event.getEntity(), main);
        if (event.getEntity().getKiller() != null)
            lmEntity.playerForPermissionsCheck = event.getEntity().getKiller();

        final EntityDamageEvent damage = lmEntity.getLivingEntity().getLastDamageCause();
        if (damage != null)
            lmEntity.deathCause = damage.getCause();

        if (lmEntity.getLivingEntity().getKiller() != null && main.placeholderApiIntegration != null)
            main.placeholderApiIntegration.putPlayerOrMobDeath(lmEntity.getLivingEntity().getKiller(), lmEntity);

        if (lmEntity.isLevelled() && lmEntity.getLivingEntity().getKiller() != null) {

            // Only counts if mob is killed by player
            if (hasReachedEntityDeathChunkMax(lmEntity) && main.rulesManager.disableVanillaDropsOnChunkMax(lmEntity))
                event.getDrops().clear();
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
            if (result.hasOverride)
                main.levelManager.removeVanillaDrops(lmEntity, event.getDrops());

            event.getDrops().addAll(drops);
        }

        lmEntity.free();
    }

    private boolean hasReachedEntityDeathChunkMax(final LivingEntityWrapper lmEntity){
        final long chunkKey = Utils.getChunkKey(lmEntity);
        final Map<EntityType, ChunkKillInfo> pairList = main.companion.getorAddPairForSpecifiedChunk(chunkKey);
        int numberOfEntityDeathInChunk = pairList.containsKey(lmEntity.getEntityType()) ?
                pairList.get(lmEntity.getEntityType()).getCount() : 0;

        /*
         Only send message for maximum threshold and cool down time
         Only message once
         This is enabled by default
         */

        final int maximumDeathInChunkThreshold = main.rulesManager.getMaximumDeathInChunkThreshold(lmEntity);
        final int maxCooldownTime = main.rulesManager.getMaxChunkCooldownTime(lmEntity);

        if (numberOfEntityDeathInChunk < maximumDeathInChunkThreshold){
            final ChunkKillInfo chunkKillInfo = pairList.computeIfAbsent(lmEntity.getEntityType(), k -> new ChunkKillInfo());
            chunkKillInfo.entityCounts.put(Instant.now(), maxCooldownTime);
            return false;
        }

        if (numberOfEntityDeathInChunk != maximumDeathInChunkThreshold) return true;

        Utils.debugLog(main, DebugType.CHUNK_KILL_COUNT, String.format("%s: %s, reached limit, entities recorded: %s, max: %s",
                Utils.displayChunkLocation(lmEntity.getLocation()), lmEntity.getTypeName(), numberOfEntityDeathInChunk, maximumDeathInChunkThreshold));

        final Player murderPlayer = lmEntity.getLivingEntity().getKiller();
        if (murderPlayer != null && main.helperSettings.getBoolean(main.settingsCfg, "exceed-kill-in-chunk-message", true))
            murderPlayer.sendMessage(MessageUtils.colorizeAll(main.messagesCfg.getString("other.no-drop-in-chunk")));

        final ChunkKillInfo chunkKillInfo = pairList.computeIfAbsent(lmEntity.getEntityType(), k -> new ChunkKillInfo());
        chunkKillInfo.entityCounts.put(Instant.now(), maxCooldownTime);

        return true;
    }
}