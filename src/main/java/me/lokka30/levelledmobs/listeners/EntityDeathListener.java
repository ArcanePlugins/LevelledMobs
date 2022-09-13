/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.listeners;

import java.time.Instant;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.customdrops.CustomDropResult;
import me.lokka30.levelledmobs.misc.ChunkKillInfo;
import me.lokka30.levelledmobs.misc.DebugType;
import me.lokka30.levelledmobs.misc.LivingEntityWrapper;
import me.lokka30.levelledmobs.misc.NametagTimerChecker;
import me.lokka30.levelledmobs.result.AdjacentChunksResult;
import me.lokka30.levelledmobs.util.Utils;
import me.lokka30.microlib.messaging.MessageUtils;
import org.bukkit.Chunk;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


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
    private final List<EntityType> bypassEntity = List.of(EntityType.ARMOR_STAND,
        EntityType.ITEM_FRAME, EntityType.DROPPED_ITEM, EntityType.PAINTING);

    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
    public void onDeath(@NotNull final EntityDeathEvent event) {
        if (event.getEntity() instanceof Player) {
            return;
        }
        if (bypassEntity.contains(event.getEntityType())) {
            return;
        }

        synchronized (NametagTimerChecker.entityTarget_Lock) {
            main.nametagTimerChecker.entityTargetMap.remove(event.getEntity());
        }

        final LivingEntityWrapper lmEntity = LivingEntityWrapper.getInstance(event.getEntity(),
            main);
        if (event.getEntity().getKiller() != null) {
            lmEntity.playerForPermissionsCheck = event.getEntity().getKiller();
        }

        final EntityDamageEvent damage = lmEntity.getLivingEntity().getLastDamageCause();
        if (damage != null) {
            lmEntity.deathCause = damage.getCause();
        }

        if (lmEntity.getLivingEntity().getKiller() != null
            && main.placeholderApiIntegration != null) {
            main.placeholderApiIntegration.putPlayerOrMobDeath(
                lmEntity.getLivingEntity().getKiller(), lmEntity, false);
        }

        if (lmEntity.isLevelled() && lmEntity.getLivingEntity().getKiller() != null
            && main.rulesManager.getMaximumDeathInChunkThreshold(lmEntity) > 0) {

            // Only counts if mob is killed by player
            if (hasReachedEntityDeathChunkMax(lmEntity, lmEntity.getLivingEntity().getKiller())
                && main.rulesManager.disableVanillaDropsOnChunkMax(lmEntity)) {
                event.getDrops().clear();
            }
        }

        if (lmEntity.getPDC().has(main.namespacedKeys.lockSettings, PersistentDataType.INTEGER)
                && lmEntity.getPDC().has(main.namespacedKeys.lockedDropRules, PersistentDataType.STRING)){
            final String lockedDropRules = lmEntity.getPDC().get(main.namespacedKeys.lockedDropRules, PersistentDataType.STRING);
            if (lockedDropRules != null) {
                lmEntity.lockedCustomDrops = new LinkedList<>(List.of(lockedDropRules.split(";")));
            }
            if (lmEntity.getPDC().has(main.namespacedKeys.lockedDropRulesOverride, PersistentDataType.INTEGER)){
                final Integer lockedOverride = lmEntity.getPDC().get(main.namespacedKeys.lockedDropRulesOverride, PersistentDataType.INTEGER);
                lmEntity.hasLockedDropsOverride = (lockedOverride != null && lockedOverride == 1);
            }
        }

        if (lmEntity.isLevelled()) {
            // Set levelled item drops
            main.levelManager.setLevelledItemDrops(lmEntity, event.getDrops());

            // Set levelled exp drops
            if (event.getDroppedExp() > 0) {
                event.setDroppedExp(
                    main.levelManager.getLevelledExpDrops(lmEntity, event.getDroppedExp()));
            }
        } else if (lmEntity.lockedCustomDrops != null || main.rulesManager.getRuleUseCustomDropsForMob(lmEntity).useDrops) {
            final List<ItemStack> drops = new LinkedList<>();
            final CustomDropResult result = main.customDropsHandler.getCustomItemDrops(lmEntity,
                drops, false);
            if (result.hasOverride()) {
                main.levelManager.removeVanillaDrops(lmEntity, event.getDrops());
            }

            event.getDrops().addAll(drops);
        }

        lmEntity.free();
    }

    private boolean hasReachedEntityDeathChunkMax(final @NotNull LivingEntityWrapper lmEntity,
        final @NotNull Player player) {
        final long chunkKey = Utils.getChunkKey(lmEntity.getLocation().getChunk());
        final Map<EntityType, ChunkKillInfo> pairList = main.companion.getorAddPairForSpecifiedChunk(
            chunkKey);
        int numberOfEntityDeathInChunk = pairList.containsKey(lmEntity.getEntityType()) ?
            pairList.get(lmEntity.getEntityType()).getCount() : 0;

        final AdjacentChunksResult adjacentChunksResult = getNumberOfEntityDeathsInAdjacentChunks(
            lmEntity);
        if (adjacentChunksResult != null) {
            numberOfEntityDeathInChunk += adjacentChunksResult.entities;
            adjacentChunksResult.chunkKeys.add(chunkKey);
        }

        lmEntity.chunkKillcount = numberOfEntityDeathInChunk;
        final int maximumDeathInChunkThreshold = main.rulesManager.getMaximumDeathInChunkThreshold(
            lmEntity);
        final int maxCooldownTime = main.rulesManager.getMaxChunkCooldownTime(lmEntity);

        if (numberOfEntityDeathInChunk < maximumDeathInChunkThreshold) {
            final ChunkKillInfo chunkKillInfo = pairList.computeIfAbsent(lmEntity.getEntityType(),
                k -> new ChunkKillInfo());
            chunkKillInfo.entityCounts.put(Instant.now(), maxCooldownTime);
            return false;
        }

        if (main.helperSettings.getBoolean(main.settingsCfg, "exceed-kill-in-chunk-message",
            true)) {
            final List<Long> chunkKeys = adjacentChunksResult != null ?
                adjacentChunksResult.chunkKeys : List.of(chunkKey);
            if (main.companion.doesUserHaveCooldown(chunkKeys, player.getUniqueId())) {
                return true;
            }

            Utils.debugLog(main, DebugType.CHUNK_KILL_COUNT,
                String.format("%s: player: %s, entity: %s, reached chunk kill limit, max: %s",
                    Utils.displayChunkLocation(lmEntity.getLocation()), player.getName(),
                    lmEntity.getTypeName(), maximumDeathInChunkThreshold));

            final String prefix = main.configUtils.getPrefix();
            final String msg = main.messagesCfg.getString("other.no-drop-in-chunk");

            if (msg != null) {
                player.sendMessage(MessageUtils.colorizeAll(msg.replace("%prefix%", prefix)));
            }

            main.companion.addUserCooldown(chunkKeys, player.getUniqueId());
        }

        return true;
    }

    @Nullable private AdjacentChunksResult getNumberOfEntityDeathsInAdjacentChunks(
        final @NotNull LivingEntityWrapper lmEntity) {
        final int adjacentChunksToCheck = main.rulesManager.getAdjacentChunksToCheck(lmEntity);
        if (adjacentChunksToCheck <= 0) {
            return null;
        }

        final Chunk startingChunk = lmEntity.getLocation().getChunk();
        final AdjacentChunksResult result = new AdjacentChunksResult();

        for (int x = -adjacentChunksToCheck; x < adjacentChunksToCheck; x++) {
            for (int z = -adjacentChunksToCheck; z < adjacentChunksToCheck; z++) {
                if (x == 0 && z == 0) {
                    continue;
                }

                final Chunk chunk = lmEntity.getWorld()
                    .getChunkAt(startingChunk.getX() + x, startingChunk.getZ() + z);
                if (!chunk.isLoaded()) {
                    continue;
                }

                result.chunkKeys.add(Utils.getChunkKey(chunk));
            }
        }

        final List<Map<EntityType, ChunkKillInfo>> pairLists = main.companion.getorAddPairForSpecifiedChunks(
            result.chunkKeys);
        for (final Map<EntityType, ChunkKillInfo> pairList : pairLists) {
            result.entities += pairList.containsKey(lmEntity.getEntityType()) ? pairList.get(
                lmEntity.getEntityType()).getCount() : 0;
        }

        return result;
    }
}