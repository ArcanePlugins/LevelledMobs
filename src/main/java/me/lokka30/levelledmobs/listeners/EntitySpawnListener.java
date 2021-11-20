/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.listeners;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.managers.ExternalCompatibilityManager;
import me.lokka30.levelledmobs.misc.*;
import me.lokka30.levelledmobs.rules.LevelledMobSpawnReason;
import me.lokka30.levelledmobs.rules.NametagVisibilityEnum;
import org.bukkit.*;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.entity.SpawnerSpawnEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

/**
 * This class handles mob spawning on the server,
 * forming the starting point of the 'levelling'
 * process
 *
 * @author lokka30
 * @version 2.5.0
 */
public class EntitySpawnListener implements Listener {

    private final LevelledMobs main;
    public boolean processMobSpawns;

    public EntitySpawnListener(final LevelledMobs main) {
        this.main = main;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onEntitySpawn(@NotNull final EntitySpawnEvent event) {
        // Must be a LivingEntity.
        if (!(event.getEntity() instanceof LivingEntity)) return;

        final LivingEntityWrapper lmEntity = LivingEntityWrapper.getInstance((LivingEntity) event.getEntity(), main);

        if (event instanceof CreatureSpawnEvent && ((CreatureSpawnEvent) event).getSpawnReason() == CreatureSpawnEvent.SpawnReason.CUSTOM &&
                !lmEntity.isLevelled()) {

            lmEntity.setSpawnReason(LevelledMobSpawnReason.CUSTOM);
            if (main.configUtils.playerLevellingEnabled && lmEntity.getPlayerForLevelling() == null)
                updateMobForPlayerLevelling(lmEntity);

            delayedAddToQueue(lmEntity, event, 20);
            lmEntity.free();
            return;
        }

        if (!processMobSpawns) {
            lmEntity.free();
            return;
        }

        if (event instanceof CreatureSpawnEvent)
            lmEntity.setSpawnReason(adaptVanillaSpawnReason(((CreatureSpawnEvent) event).getSpawnReason()));

        if (main.configUtils.playerLevellingEnabled && lmEntity.getPlayerForLevelling() == null)
            updateMobForPlayerLevelling(lmEntity);

        final int mobProcessDelay = main.helperSettings.getInt(main.settingsCfg, "mob-process-delay", 0);

        if (mobProcessDelay > 0)
            delayedAddToQueue(lmEntity, event, mobProcessDelay);
        else
            main._mobsQueueManager.addToQueue(new QueueItem(lmEntity, event));

        lmEntity.free();
    }

    private void updateMobForPlayerLevelling(final @NotNull LivingEntityWrapper lmEntity){
        final int onlinePlayerCount = lmEntity.getWorld().getPlayers().size();
        final int checkDistance = main.helperSettings.getInt(main.settingsCfg, "async-task-max-blocks-from-player", 100);
        final List<org.bukkit.entity.Player> playerList = onlinePlayerCount <= 10 ?
                getPlayersOnServerNearMob(lmEntity.getLivingEntity(), checkDistance) :
                getPlayersNearMob(lmEntity.getLivingEntity(), checkDistance);

        Player closestPlayer = null;
        for (final org.bukkit.entity.Player player : playerList) {
            if (ExternalCompatibilityManager.isMobOfCitizens(player))
                continue;

            closestPlayer = player;
            break;
        }

        if (closestPlayer == null) return;

        synchronized (lmEntity.getLivingEntity().getPersistentDataContainer()) {
            lmEntity.getPDC().set(main.namespaced_keys.playerLevelling_Id, PersistentDataType.STRING, closestPlayer.getUniqueId().toString());
        }

        lmEntity.setPlayerForLevelling(closestPlayer);
        final List<NametagVisibilityEnum> nametagVisibilityEnums = main.rulesManager.getRule_CreatureNametagVisbility(lmEntity);
        if (nametagVisibilityEnums.contains(NametagVisibilityEnum.TARGETED) &&
            lmEntity.getLivingEntity().hasLineOfSight(closestPlayer))
            main.levelManager.updateNametag(lmEntity);
    }

    @NotNull
    private static List<Player> getPlayersOnServerNearMob(final @NotNull LivingEntity mob, final int checkDistance){
        final double maxDistanceSquared = checkDistance * 4;

        return mob.getWorld().getPlayers().stream()
                .filter(p -> mob.getWorld().equals(p.getWorld()))
                .filter(p -> p.getGameMode() != GameMode.SPECTATOR)
                .map(p -> Map.entry(mob.getLocation().distanceSquared(p.getLocation()), p))
                .filter(e -> e.getKey() <= maxDistanceSquared)
                .sorted(Comparator.comparingDouble(Map.Entry::getKey))
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
    }

    @NotNull
    public static List<Player> getPlayersNearMob(final @NotNull LivingEntity mob, final int checkDistance){
        return mob.getNearbyEntities(checkDistance, checkDistance, checkDistance).stream()
                .filter(e -> e instanceof org.bukkit.entity.Player)
                .filter(e -> ((Player) e).getGameMode() != GameMode.SPECTATOR)
                .map(e -> Map.entry(mob.getLocation().distanceSquared(e.getLocation()), (org.bukkit.entity.Player) e))
                .sorted(Comparator.comparingDouble(Map.Entry::getKey))
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
    }

    private LevelledMobSpawnReason adaptVanillaSpawnReason(final CreatureSpawnEvent.@NotNull SpawnReason spawnReason) {
        return LevelledMobSpawnReason.valueOf(spawnReason.toString());
    }

    private void delayedAddToQueue(final LivingEntityWrapper lmEntity, final Event event, final int delay){
        final BukkitRunnable runnable = new BukkitRunnable() {
            @Override
            public void run() {
                main._mobsQueueManager.addToQueue(new QueueItem(lmEntity, event));
            }
        };

        runnable.runTaskLater(main, delay);
    }

    private void lmSpawnerSpawn(final LivingEntityWrapper lmEntity, @NotNull final SpawnerSpawnEvent event) {
        final CreatureSpawner cs = event.getSpawner();

        // mob was spawned from a custom LM spawner
        createParticleEffect(cs.getLocation().add(0, 1, 0));

        final Integer minLevel = cs.getPersistentDataContainer().get(main.namespaced_keys.keySpawner_MinLevel, PersistentDataType.INTEGER);
        final Integer maxLevel = cs.getPersistentDataContainer().get(main.namespaced_keys.keySpawner_MaxLevel, PersistentDataType.INTEGER);
        final int useMinLevel = minLevel == null ? -1 : minLevel;
        final int useMaxLevel = maxLevel == null ? -1 : maxLevel;
        final int generatedLevel = main.levelInterface.generateLevel(lmEntity, useMinLevel, useMaxLevel);
        final String spawnerName = cs.getPersistentDataContainer().has(main.namespaced_keys.keySpawner_CustomName, PersistentDataType.STRING) ?
            cs.getPersistentDataContainer().get(main.namespaced_keys.keySpawner_CustomName, PersistentDataType.STRING) : null;
        String customDropId = null;
        if (cs.getPersistentDataContainer().has(main.namespaced_keys.keySpawner_CustomDropId, PersistentDataType.STRING)) {
            customDropId = cs.getPersistentDataContainer().get(main.namespaced_keys.keySpawner_CustomDropId, PersistentDataType.STRING);
            if (!Utils.isNullOrEmpty(customDropId)) {
                synchronized (lmEntity.getLivingEntity().getPersistentDataContainer()) {
                    lmEntity.getPDC().set(main.namespaced_keys.keySpawner_CustomDropId, PersistentDataType.STRING, customDropId);
                }
            }
        }

        lmEntity.setSourceSpawnerName(spawnerName);

        Utils.debugLog(main, DebugType.MOB_SPAWNER, String.format(
                "Spawned mob from LM spawner: &b%s&7, minLevel:&b %s&7, maxLevel: &b%s&7, generatedLevel: &b%s&b%s",
                event.getEntityType(), useMinLevel, useMaxLevel, generatedLevel, (customDropId == null ? "" : ", dropid: " + customDropId)));

        main.levelInterface.applyLevelToMob(lmEntity, generatedLevel,
                false, true, new HashSet<>(Collections.singletonList(AdditionalLevelInformation.NOT_APPLICABLE)));
    }

    private void createParticleEffect(@NotNull final Location location){
        final World world = location.getWorld();
        if (world == null) return;

        final BukkitRunnable runnable = new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    for (int i = 0; i < 10; i++) {
                        world.spawnParticle(Particle.SOUL, location, 20, 0, 0, 0, 0.1);
                        Thread.sleep(50);
                    }
                } catch (InterruptedException ignored) { }
            }
        };

        runnable.runTaskAsynchronously(main);
    }

    @SuppressWarnings("ConstantConditions")
    public void preprocessMob(final @NotNull LivingEntityWrapper lmEntity, @NotNull final Event event){
        if (!lmEntity.reEvaluateLevel && lmEntity.isLevelled())
            return;
        if (lmEntity.getLivingEntity() == null) return;

        LevelledMobSpawnReason spawnReason = LevelledMobSpawnReason.DEFAULT;
        AdditionalLevelInformation additionalInfo = AdditionalLevelInformation.NOT_APPLICABLE;

        lmEntity.setSpawnedTimeOfDay((int) lmEntity.getWorld().getTime());

        if (event instanceof SpawnerSpawnEvent) {
            final SpawnerSpawnEvent spawnEvent = (SpawnerSpawnEvent) event;

            if (spawnEvent.getSpawner() != null && spawnEvent.getSpawner().getPersistentDataContainer().has(main.namespaced_keys.keySpawner, PersistentDataType.INTEGER)) {
                lmEntity.setSpawnReason(LevelledMobSpawnReason.LM_SPAWNER);
                lmSpawnerSpawn(lmEntity, spawnEvent);
                return;
            }

            Utils.debugLog(main, DebugType.MOB_SPAWNER, "Spawned mob from vanilla spawner: &b" + spawnEvent.getEntityType());
        } else if (event instanceof CreatureSpawnEvent) {
            final CreatureSpawnEvent spawnEvent = (CreatureSpawnEvent) event;

            if (spawnEvent.getSpawnReason() == CreatureSpawnEvent.SpawnReason.SPAWNER ||
                    spawnEvent.getSpawnReason() == CreatureSpawnEvent.SpawnReason.SLIME_SPLIT)
                return;

            if (spawnEvent.getSpawnReason() == CreatureSpawnEvent.SpawnReason.CUSTOM &&
                    main.levelManager.summonedEntityType == lmEntity.getEntityType() &&
                    areLocationsTheSame(main.levelManager.summonedLocation, lmEntity.getLocation())) {
                // the mob was spawned by the summon command and will get processed directly
                return;
            }

            spawnReason = adaptVanillaSpawnReason(spawnEvent.getSpawnReason());
        } else if (event instanceof ChunkLoadEvent)
            additionalInfo = AdditionalLevelInformation.FROM_CHUNK_LISTENER;

        if (lmEntity.reEvaluateLevel && main.configUtils.playerLevellingEnabled && lmEntity.isRulesForceAll){
            synchronized (lmEntity.getLivingEntity().getPersistentDataContainer()){
                if (lmEntity.getPDC().has(main.namespaced_keys.playerLevelling_Id, PersistentDataType.STRING))
                    lmEntity.getPDC().remove(main.namespaced_keys.playerLevelling_Id);
            }
            lmEntity.setPlayerForLevelling(null);
        }

        final HashSet<AdditionalLevelInformation> additionalLevelInfo = new HashSet<>(Collections.singletonList(additionalInfo));
        final LevellableState levellableState = getLevellableState(lmEntity, event);
        if (levellableState == LevellableState.ALLOWED) {
            if (lmEntity.reEvaluateLevel && main.configUtils.playerLevellingEnabled) {
                final Object syncObj = new Object();
                final BukkitRunnable runnable = new BukkitRunnable() {
                    @Override
                    public void run() { updateMobForPlayerLevelling(lmEntity); }
                };
                runnable.runTask(main);
            }

            main.levelInterface.applyLevelToMob(lmEntity, main.levelInterface.generateLevel(lmEntity),
                    false, false, additionalLevelInfo);
        } else {
            Utils.debugLog(main, DebugType.APPLY_LEVEL_FAIL, "Entity &b" + lmEntity.getNameIfBaby() + "&7 in wo" +
                    "rld&b " + lmEntity.getWorldName() + "&7 was not levelled -> levellable state: &b" + levellableState);

            // Check if the mob is already levelled - if so, remove their level
            if (lmEntity.isLevelled())
                main.levelInterface.removeLevel(lmEntity);
            else if (lmEntity.isBabyMob()) {
                // add a tag so we can potentially level the mob when/if it ages
                synchronized (lmEntity.getLivingEntity().getPersistentDataContainer()) {
                    lmEntity.getPDC().set(main.namespaced_keys.wasBabyMobKey, PersistentDataType.INTEGER, 1);
                }
            }

            if (lmEntity.wasPreviouslyLevelled)
                main.levelManager.updateNametag(lmEntity);
        }
    }

    private static boolean areLocationsTheSame(final Location location1, final Location location2){
        if (location1 == null || location2 == null) return false;
        if (location1.getWorld() == null || location2.getWorld() == null) return false;

        return  location1.getBlockX() == location2.getBlockX() &&
                location1.getBlockY() == location2.getBlockY() &&
                location1.getBlockZ() == location2.getBlockZ();
    }

    @NotNull
    private LevellableState getLevellableState(final LivingEntityWrapper lmEntity, @NotNull final Event event) {
        LevellableState levellableState = main.levelInterface.getLevellableState(lmEntity);

        if (levellableState != LevellableState.ALLOWED)
            return levellableState;

        if (event instanceof CreatureSpawnEvent) {
            CreatureSpawnEvent creatureSpawnEvent = (CreatureSpawnEvent) event;

            // the mob gets processed via SpawnerSpawnEvent
            if (((CreatureSpawnEvent) event).getSpawnReason() == CreatureSpawnEvent.SpawnReason.SPAWNER)
                return LevellableState.DENIED_OTHER;

            Utils.debugLog(main, DebugType.ENTITY_SPAWN, "instanceof CreatureSpawnListener: &b" + creatureSpawnEvent.getEntityType() + "&7, with spawnReason &b" + creatureSpawnEvent.getSpawnReason() + "&7.");
        } else if (event instanceof EntitySpawnEvent)
            Utils.debugLog(main, DebugType.ENTITY_SPAWN, "not instanceof CreatureSpawnListener: &b" + ((EntitySpawnEvent) event).getEntityType());

        return LevellableState.ALLOWED;
    }
}
