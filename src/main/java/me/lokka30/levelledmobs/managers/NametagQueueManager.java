/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.managers;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.misc.*;
import me.lokka30.levelledmobs.rules.NametagVisibilityEnum;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Queues up mob nametag updates so they can be applied in a background thread
 *
 * @author stumper66
 * @since 3.0.0
 */
public class NametagQueueManager {

    public NametagQueueManager(final LevelledMobs main) {
        this.main = main;
        this.queue = new LinkedBlockingQueue<>();
    }

    private final LevelledMobs main;
    private boolean isRunning;
    private boolean doThread;
    private final LinkedBlockingQueue<QueueItem> queue;

    public void start(){
        if (isRunning) return;
        doThread = true;
        isRunning = true;

        final BukkitRunnable bgThread = new BukkitRunnable(){
            @Override
            public void run() {
                try {
                    main();
                } catch (InterruptedException ignored) {
                    isRunning = false;
                }
                Utils.logger.info("Nametag update queue Manager has exited");
            }
        };

        bgThread.runTaskAsynchronously(main);
    }

    public void stop(){
        doThread = false;
    }

    public void addToQueue(final @NotNull QueueItem item) {
        if (item.lmEntity.getLivingEntity() == null ||
            main.rulesManager.getRule_CreatureNametagVisbility(item.lmEntity).contains(NametagVisibilityEnum.DISABLED))
            return;

        item.lmEntity.inUseCount.getAndIncrement();
        queue.offer(item);
    }

    private void main() throws InterruptedException{
        while (doThread) {

            final QueueItem item = queue.poll(200, TimeUnit.MILLISECONDS);
            if (item == null) continue;
            if (item.lmEntity.getLivingEntity() == null){
                item.lmEntity.free();
                continue;
            }

            final int nametagTimerResetTime = item.lmEntity.getNametagCooldownTime();

            if (nametagTimerResetTime > 0) {
                synchronized (NametagTimerChecker.nametagTimer_Lock) {
                    final Map<Player, WeakHashMap<LivingEntity, Instant>> nametagCooldownQueue = main.nametagTimerChecker.getNametagCooldownQueue();

                    if (item.lmEntity.playersNeedingNametagCooldownUpdate != null) {
                        // record which players should get the cooldown for this mob
                        // public Map<Player, WeakHashMap<LivingEntity, Instant>> nametagCooldownQueue;
                        for (final Player player : item.lmEntity.playersNeedingNametagCooldownUpdate) {
                            if (!nametagCooldownQueue.containsKey(player)) continue;

                            nametagCooldownQueue.get(player).put(item.lmEntity.getLivingEntity(), Instant.now());
                            main.nametagTimerChecker.cooldownTimes.put(item.lmEntity.getLivingEntity(), item.lmEntity.getNametagCooldownTime());
                        }

                        // if any players already have a cooldown on this mob then don't remove the cooldown
                        for (final Player player : nametagCooldownQueue.keySet()){
                            if (item.lmEntity.playersNeedingNametagCooldownUpdate.contains(player)) continue;

                            if (nametagCooldownQueue.get(player).containsKey(item.lmEntity.getLivingEntity()))
                                item.lmEntity.playersNeedingNametagCooldownUpdate.add(player);
                        }
                    }
                    else{
                        // if there's any existing cooldowns we'll use them
                        for (final Player player : nametagCooldownQueue.keySet()){
                            if (nametagCooldownQueue.get(player).containsKey(item.lmEntity.getLivingEntity())) {
                                if (item.lmEntity.playersNeedingNametagCooldownUpdate == null)
                                    item.lmEntity.playersNeedingNametagCooldownUpdate = new HashSet<>();

                                item.lmEntity.playersNeedingNametagCooldownUpdate.add(player);
                            }
                        }
                    }
                }
            }
            else if (item.lmEntity.playersNeedingNametagCooldownUpdate != null)
                item.lmEntity.playersNeedingNametagCooldownUpdate = null;

            synchronized (NametagTimerChecker.entityTarget_Lock){
                if (main.nametagTimerChecker.entityTargetMap.containsKey(item.lmEntity.getLivingEntity())){
                    if (item.lmEntity.playersNeedingNametagCooldownUpdate == null)
                        item.lmEntity.playersNeedingNametagCooldownUpdate = new HashSet<>();

                    item.lmEntity.playersNeedingNametagCooldownUpdate.add(main.nametagTimerChecker.entityTargetMap.get(item.lmEntity.getLivingEntity()));
                }
            }

            updateNametag(item.lmEntity, item.nametag, item.players);
            item.lmEntity.free();
        }

        isRunning = false;
    }

    private void updateNametag(final @NotNull LivingEntityWrapper lmEntity, final String nametag, final List<Player> players) {
        if (!lmEntity.getIsPopulated()) return;

        if (main.helperSettings.getBoolean(main.settingsCfg, "use-customname-for-mob-nametags")){
            updateNametag_CustomName(lmEntity, nametag);
            return;
        }

        if (!ExternalCompatibilityManager.hasProtocolLibInstalled()) return;
        if (main.helperSettings.getBoolean(main.settingsCfg, "assert-entity-validity-with-nametag-packets") && !lmEntity.getLivingEntity().isValid())
            return;

        final int loopCount = lmEntity.playersNeedingNametagCooldownUpdate == null ?
                1 : 2;

        for (int i = 0; i < loopCount; i++) {
            // will loop again to update with nametag cooldown for only the specified players

            final WrappedDataWatcher dataWatcher;
            final WrappedDataWatcher.Serializer chatSerializer;

            try {
                dataWatcher = WrappedDataWatcher.getEntityWatcher(lmEntity.getLivingEntity()).deepClone();
            } catch (ConcurrentModificationException ex) {
                Utils.debugLog(main, DebugType.UPDATE_NAMETAG_FAIL, "&bConcurrentModificationException &7caught, skipping nametag update of &b" + lmEntity.getLivingEntity().getName() + "&7.");
                return;
            }

            try {
                chatSerializer = WrappedDataWatcher.Registry.getChatComponentSerializer(true);
            } catch (ConcurrentModificationException ex) {
                Utils.debugLog(main, DebugType.UPDATE_NAMETAG_FAIL, "&bConcurrentModificationException &7caught, skipping nametag update of &b" + lmEntity.getLivingEntity().getName() + "&7.");
                return;
            } catch (IllegalArgumentException ex) {
                Utils.debugLog(main, DebugType.UPDATE_NAMETAG_FAIL, "Registry is empty (&bIllegalArgumentException&7 caught), skipping nametag update of &b" + lmEntity.getLivingEntity().getName() + "&7.");
                return;
            }

            final WrappedDataWatcher.WrappedDataWatcherObject watcherObject = new WrappedDataWatcher.WrappedDataWatcherObject(2, chatSerializer);
            final int objectIndex = 3;
            final int fieldIndex = 0;
            final Optional<Object> optional = Utils.isNullOrEmpty(nametag) ?
                    Optional.empty() : Optional.of(WrappedChatComponent.fromChatMessage(nametag)[0].getHandle());

            dataWatcher.setObject(watcherObject, optional);

            if (nametag == null)
                dataWatcher.setObject(objectIndex, false);
            else {
                final List<NametagVisibilityEnum> nametagVisibilityEnum = main.rulesManager.getRule_CreatureNametagVisbility(lmEntity);
                final boolean doAlwaysVisible = i == 1 ||
                        !"".equals(nametag) && lmEntity.getLivingEntity().isCustomNameVisible() ||
                        nametagVisibilityEnum.contains(NametagVisibilityEnum.ALWAYS_ON);

                dataWatcher.setObject(objectIndex, doAlwaysVisible);
            }

            final PacketContainer packet = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.ENTITY_METADATA);
            packet.getWatchableCollectionModifier().write(fieldIndex, dataWatcher.getWatchableObjects());
            packet.getIntegers().write(fieldIndex, lmEntity.getLivingEntity().getEntityId());

            if (i == 0){
                // these players are not getting always on nametags unless always on has been configured for the mob
                for (final Player player : players) {
                    if (lmEntity.playersNeedingNametagCooldownUpdate != null && lmEntity.playersNeedingNametagCooldownUpdate.contains(player))
                        continue;

                    if (!sendPacket(player, lmEntity, packet)) return;
                }
            }
            else{
                // these players are getting always on nametags
                for (final Player player : lmEntity.playersNeedingNametagCooldownUpdate)
                    if (!sendPacket(player, lmEntity, packet)) return;
            }
        }
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean sendPacket(final @NotNull Player player, final LivingEntityWrapper lmEntity, final PacketContainer packet){
        if (!player.isOnline()) return true;
        if (!lmEntity.getLivingEntity().isValid()) return false;

        try {
            Utils.debugLog(main, DebugType.UPDATE_NAMETAG_SUCCESS, "Nametag packet sent for &b" + lmEntity.getLivingEntity().getName() + "&7 to &b" + player.getName() + "&7.");
            ProtocolLibrary.getProtocolManager().sendServerPacket(player, packet);
        } catch (IllegalArgumentException ex) {
            Utils.debugLog(main, DebugType.UPDATE_NAMETAG_FAIL, "&bIllegalArgumentException&7 caught whilst trying to sendServerPacket");
        } catch (InvocationTargetException ex) {
            Utils.logger.error("Unable to update nametag packet for player &b" + player.getName() + "&7; Stack trace:");
            ex.printStackTrace();
        }

        return true;
    }

    private void updateNametag_CustomName(final @NotNull LivingEntityWrapper lmEntity, final String nametag){
        synchronized (lmEntity.getLivingEntity().getPersistentDataContainer()) {
            if (lmEntity.getPDC().has(main.namespaced_keys.hasCustomNameTag, PersistentDataType.INTEGER))
                return;
        }

        final boolean hadCustomName = lmEntity.getLivingEntity().getCustomName() != null;

        lmEntity.getLivingEntity().setCustomName(nametag);
        lmEntity.getLivingEntity().setCustomNameVisible(true);

        final boolean isTamable = (lmEntity.getLivingEntity() instanceof Tameable);

        if (!hadCustomName && !isTamable && !lmEntity.getTypeName().equalsIgnoreCase("Axolotl"))
            lmEntity.getLivingEntity().setRemoveWhenFarAway(true);
    }
}
