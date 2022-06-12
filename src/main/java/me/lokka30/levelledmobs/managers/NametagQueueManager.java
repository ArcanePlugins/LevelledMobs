/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.managers;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.misc.LivingEntityWrapper;
import me.lokka30.levelledmobs.misc.NametagTimerChecker;
import me.lokka30.levelledmobs.misc.QueueItem;
import me.lokka30.levelledmobs.nms.NMSHandler;
import me.lokka30.levelledmobs.nms.NMSUtil;
import me.lokka30.levelledmobs.rules.NametagVisibilityEnum;
import me.lokka30.levelledmobs.util.Utils;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

/**
 * Queues up mob nametag updates so they can be applied in a background thread
 *
 * @author stumper66
 * @since 3.0.0
 */
public class NametagQueueManager {

    public NametagQueueManager(final LevelledMobs main) {
        this.main = main;
        this.nmsHandler = new NMSHandler(main);
        this.queue = new LinkedBlockingQueue<>();
        getNMSUtil();
    }

    private final LevelledMobs main;
    private boolean isRunning;
    private boolean doThread;
    private NMSUtil nmsUtil;
    private final LinkedBlockingQueue<QueueItem> queue;
    public final NMSHandler nmsHandler;

    private void getNMSUtil() {

        this.nmsUtil = nmsHandler.getCurrentUtil();
    }

    public boolean hasNametagSupport() {
        return this.nmsUtil != null;
    }

    public void start() {
        if (isRunning) {
            return;
        }
        doThread = true;
        isRunning = true;

        final BukkitRunnable bgThread = new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    main();
                } catch (final InterruptedException ignored) {
                    isRunning = false;
                }
                Utils.logger.info("Nametag update queue Manager has exited");
            }
        };

        bgThread.runTaskAsynchronously(main);
    }

    public void stop() {
        doThread = false;
    }

    void addToQueue(final @NotNull QueueItem item) {
        if (Bukkit.getOnlinePlayers().size() == 0) {
            return;
        }
        if (item.lmEntity.getLivingEntity() == null ||
            main.rulesManager.getRuleCreatureNametagVisbility(item.lmEntity)
                .contains(NametagVisibilityEnum.DISABLED)) {
            return;
        }

        item.lmEntity.inUseCount.getAndIncrement();
        queue.offer(item);
    }

    private void main() throws InterruptedException {
        while (doThread) {

            final QueueItem item = queue.poll(200, TimeUnit.MILLISECONDS);
            if (item == null) {
                continue;
            }
            if (item.lmEntity.getLivingEntity() == null) {
                item.lmEntity.free();
                continue;
            }

            String lastEntityType = null;
            try {
                lastEntityType = item.lmEntity.getNameIfBaby();
                processItem(item);
            } catch (final Exception ex) {
                final var entityName = lastEntityType == null ? "Unknown Entity" : lastEntityType;

                Utils.logger.error("Unable to process nametag update for '" + entityName + "'. " +
                    "This is usually caused by not running the correct ProtocolLib version for " +
                    "your server's Minecraft version (e.g., MC 1.18 requires PL v4.8.0).");
                ex.printStackTrace();
            } finally {
                item.lmEntity.free();
            }
        }

        isRunning = false;
    }

    private void processItem(final @NotNull QueueItem item) {
        if (this.nmsUtil == null) {
            // this would happen if the Minecraft version isn't supported directly by NMS
            // and ProtocolLib is not installed
            return;
        }

        final long nametagTimerResetTime = item.lmEntity.getNametagCooldownTime();

        if (nametagTimerResetTime > 0L && !Utils.isNullOrEmpty(item.nametag)) {
            synchronized (NametagTimerChecker.nametagTimer_Lock) {
                final Map<Player, WeakHashMap<LivingEntity, Instant>> nametagCooldownQueue = main.nametagTimerChecker.getNametagCooldownQueue();

                if (item.lmEntity.playersNeedingNametagCooldownUpdate != null) {
                    // record which players should get the cooldown for this mob
                    // public Map<Player, WeakHashMap<LivingEntity, Instant>> nametagCooldownQueue;
                    for (final Player player : item.lmEntity.playersNeedingNametagCooldownUpdate) {
                        if (!nametagCooldownQueue.containsKey(player)) {
                            continue;
                        }

                        nametagCooldownQueue.get(player)
                            .put(item.lmEntity.getLivingEntity(), Instant.now());
                        main.nametagTimerChecker.cooldownTimes.put(item.lmEntity.getLivingEntity(),
                            item.lmEntity.getNametagCooldownTime());
                    }

                    // if any players already have a cooldown on this mob then don't remove the cooldown
                    for (final Map.Entry<Player, WeakHashMap<LivingEntity, Instant>> coolDown : nametagCooldownQueue.entrySet()) {
                        final Player player = coolDown.getKey();
                        if (item.lmEntity.playersNeedingNametagCooldownUpdate.contains(player)) {
                            continue;
                        }

                        if (coolDown.getValue().containsKey(item.lmEntity.getLivingEntity())) {
                            item.lmEntity.playersNeedingNametagCooldownUpdate.add(player);
                        }
                    }
                } else {
                    // if there's any existing cooldowns we'll use them
                    for (final Map.Entry<Player, WeakHashMap<LivingEntity, Instant>> coolDown : nametagCooldownQueue.entrySet()) {
                        if (coolDown.getValue().containsKey(item.lmEntity.getLivingEntity())) {
                            if (item.lmEntity.playersNeedingNametagCooldownUpdate == null) {
                                item.lmEntity.playersNeedingNametagCooldownUpdate = new HashSet<>();
                            }

                            item.lmEntity.playersNeedingNametagCooldownUpdate.add(
                                coolDown.getKey());
                        }
                    }
                }
            }
        } else if (item.lmEntity.playersNeedingNametagCooldownUpdate != null) {
            item.lmEntity.playersNeedingNametagCooldownUpdate = null;
        }

        synchronized (NametagTimerChecker.entityTarget_Lock) {
            if (main.nametagTimerChecker.entityTargetMap.containsKey(
                item.lmEntity.getLivingEntity())) {
                if (item.lmEntity.playersNeedingNametagCooldownUpdate == null) {
                    item.lmEntity.playersNeedingNametagCooldownUpdate = new HashSet<>();
                }

                item.lmEntity.playersNeedingNametagCooldownUpdate.add(
                    main.nametagTimerChecker.entityTargetMap.get(item.lmEntity.getLivingEntity()));
            }
        }

        if (!item.lmEntity.getIsPopulated()) {
            return;
        }

        if (main.helperSettings.getBoolean(main.settingsCfg, "use-customname-for-mob-nametags")) {
            updateNametagCustomName(item.lmEntity, item.nametag);
            return;
        }

        if (main.helperSettings.getBoolean(main.settingsCfg,
            "assert-entity-validity-with-nametag-packets") && !item.lmEntity.getLivingEntity()
            .isValid()) {
            return;
        }

        updateNametag(item.lmEntity, item.nametag, item.players);
    }

    private void updateNametag(final @NotNull LivingEntityWrapper lmEntity, final String nametag,
        final List<Player> players) {
        final int loopCount = lmEntity.playersNeedingNametagCooldownUpdate == null ?
            1 : 2;

        for (int i = 0; i < loopCount; i++) {
            // will loop again to update with nametag cooldown for only the specified players

            final List<NametagVisibilityEnum> nametagVisibilityEnum = main.rulesManager.getRuleCreatureNametagVisbility(
                lmEntity);
            final boolean doAlwaysVisible = i == 1 ||
                    nametag != null && !nametag.isEmpty() && lmEntity.getLivingEntity().isCustomNameVisible() ||
                nametagVisibilityEnum.contains(NametagVisibilityEnum.ALWAYS_ON);

            if (i == 0) {
                // these players are not getting always on nametags unless always on has been configured for the mob
                for (final Player player : players) {
                    if (lmEntity.playersNeedingNametagCooldownUpdate != null
                        && lmEntity.playersNeedingNametagCooldownUpdate.contains(player)) {
                        continue;
                    }

                    nmsUtil.sendNametag(lmEntity.getLivingEntity(), nametag, player,
                        doAlwaysVisible);
                }
            } else {
                // these players are getting always on nametags
                for (final Player player : lmEntity.playersNeedingNametagCooldownUpdate) {
                    nmsUtil.sendNametag(lmEntity.getLivingEntity(), nametag, player, true);
                }
            }
        }
    }

    private void updateNametagCustomName(final @NotNull LivingEntityWrapper lmEntity,
        final String nametag) {
        synchronized (lmEntity.getLivingEntity().getPersistentDataContainer()) {
            if (lmEntity.getPDC()
                .has(main.namespacedKeys.hasCustomNameTag, PersistentDataType.INTEGER)) {
                return;
            }
        }

        final boolean hadCustomName = lmEntity.getLivingEntity().getCustomName() != null;

        lmEntity.getLivingEntity().setCustomName(nametag);
        lmEntity.getLivingEntity().setCustomNameVisible(true);

        final boolean isTamable = (lmEntity.getLivingEntity() instanceof Tameable);

        if (!hadCustomName && !isTamable && !lmEntity.getTypeName().equalsIgnoreCase("Axolotl")) {
            lmEntity.getLivingEntity().setRemoveWhenFarAway(true);
        }
    }
}
