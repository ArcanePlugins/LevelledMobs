package me.lokka30.levelledmobs.listeners;

import me.lokka30.levelledmobs.LevelInterface;
import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.misc.*;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.Entity;
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

import java.util.Collections;
import java.util.HashSet;

/**
 * This class handles mob spawning on the server,
 * forming the starting point of the 'levelling'
 * process
 *
 * @author lokka30
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

        final LivingEntityWrapper lmEntity = new LivingEntityWrapper((LivingEntity) event.getEntity(), main);

        if (event instanceof CreatureSpawnEvent && ((CreatureSpawnEvent) event).getSpawnReason().equals(CreatureSpawnEvent.SpawnReason.CUSTOM) &&
            !lmEntity.isLevelled()) {
            delayedAddToQueue(lmEntity, event, 20);
            return;
        }

        if (!processMobSpawns) return;

        if (main.configUtils.playerLevellingEnabled)
            getClosestPlayer(lmEntity);

        final int mobProcessDelay = main.helperSettings.getInt(main.settingsCfg, "mob-process-delay", 0);

        if (mobProcessDelay > 0)
            delayedAddToQueue(lmEntity, event, mobProcessDelay);
        else
            main.queueManager_mobs.addToQueue(new QueueItem(lmEntity, event));
    }

    private void getClosestPlayer(final @NotNull LivingEntityWrapper lmEntity){
        Entity closestEntity = null;
        double closestRange = Double.MAX_VALUE;

        for (final Entity entity : lmEntity.getLivingEntity().getNearbyEntities(50, 50, 50)){
            if (!(entity instanceof Player)) continue;

            double range = entity.getLocation().distance(lmEntity.getLocation());
            if (range < closestRange){
                closestEntity = entity;
                closestRange = range;
            }
        }

        if (closestEntity != null) {
            synchronized (closestEntity.getPersistentDataContainer()){
                closestEntity.getPersistentDataContainer().set(main.levelManager.playerLevelling, PersistentDataType.INTEGER, 1);
            }
            Utils.logger.info("PDC key has been set");
            lmEntity.setPlayerForLevelling((Player) closestEntity);
        }
    }

    private void delayedAddToQueue(final LivingEntityWrapper lmEntity, final Event event, final int delay){
        final BukkitRunnable runnable = new BukkitRunnable() {
            @Override
            public void run() {
                main.queueManager_mobs.addToQueue(new QueueItem(lmEntity, event));
            }
        };

        runnable.runTaskLater(main, delay);
    }

    private void lmSpawnerSpawn(final LivingEntityWrapper lmEntity, @NotNull final SpawnerSpawnEvent event) {
        final CreatureSpawner cs = event.getSpawner();

        // mob was spawned from a custom LM spawner
        createParticleEffect(cs.getLocation().add(0, 1, 0));

        final Integer minLevel = cs.getPersistentDataContainer().get(main.blockPlaceListener.keySpawner_MinLevel, PersistentDataType.INTEGER);
        final Integer maxLevel = cs.getPersistentDataContainer().get(main.blockPlaceListener.keySpawner_MaxLevel, PersistentDataType.INTEGER);
        final int useMinLevel = minLevel == null ? -1 : minLevel;
        final int useMaxLevel = maxLevel == null ? -1 : maxLevel;
        final int generatedLevel = main.levelInterface.generateLevel(lmEntity, useMinLevel, useMaxLevel);
        String customDropId = null;
        if (cs.getPersistentDataContainer().has(main.blockPlaceListener.keySpawner_CustomDropId, PersistentDataType.STRING)) {
            customDropId = cs.getPersistentDataContainer().get(main.blockPlaceListener.keySpawner_CustomDropId, PersistentDataType.STRING);
            if (!Utils.isNullOrEmpty(customDropId)) {
                synchronized (lmEntity.getLivingEntity().getPersistentDataContainer()) {
                    lmEntity.getPDC().set(main.blockPlaceListener.keySpawner_CustomDropId, PersistentDataType.STRING, customDropId);
                }
            }
        }


        Utils.debugLog(main, DebugType.MOB_SPAWNER, String.format(
                "Spawned mob from LM spawner: &b%s&7, minLevel:&b %s&7, maxLevel: &b%s&7, generatedLevel: &b%s&b%s",
                event.getEntityType(), useMinLevel, useMaxLevel, generatedLevel, (customDropId == null ? "" : ", dropid: " + customDropId)));

        main.levelInterface.applyLevelToMob(lmEntity, generatedLevel,
                false, true, new HashSet<>(Collections.singletonList(LevelInterface.AdditionalLevelInformation.NOT_APPLICABLE)));
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

    public void preprocessMob(final LivingEntityWrapper lmEntity, @NotNull final Event event){

        if (!lmEntity.reEvaluateLevel && lmEntity.isLevelled())
            return;

        CreatureSpawnEvent.SpawnReason spawnReason = CreatureSpawnEvent.SpawnReason.DEFAULT;
        LevelInterface.AdditionalLevelInformation additionalInfo = LevelInterface.AdditionalLevelInformation.NOT_APPLICABLE;

        if (event instanceof SpawnerSpawnEvent) {
            SpawnerSpawnEvent spawnEvent = (SpawnerSpawnEvent) event;

            if (spawnEvent.getSpawner().getPersistentDataContainer().has(main.blockPlaceListener.keySpawner, PersistentDataType.INTEGER)){
                lmEntity.setSpawnReason(CreatureSpawnEvent.SpawnReason.SPAWNER);
                lmSpawnerSpawn(lmEntity, spawnEvent);
                return;
            }

            Utils.debugLog(main, DebugType.MOB_SPAWNER, "Spawned mob from vanilla spawner: &b" + spawnEvent.getEntityType());
            spawnReason = CreatureSpawnEvent.SpawnReason.SPAWNER;
        }
        else if (event instanceof CreatureSpawnEvent){
            final CreatureSpawnEvent spawnEvent = (CreatureSpawnEvent) event;

            if (spawnEvent.getSpawnReason().equals(CreatureSpawnEvent.SpawnReason.SPAWNER) ||
                    spawnEvent.getSpawnReason().equals(CreatureSpawnEvent.SpawnReason.SLIME_SPLIT))
                return;

            if (spawnEvent.getSpawnReason().equals(CreatureSpawnEvent.SpawnReason.CUSTOM) &&
                    main.levelManager.summonedEntityType.equals(lmEntity.getEntityType()) &&
                    areLocationsTheSame(main.levelManager.summonedLocation, lmEntity.getLocation())){
                // the mob was spawned by the summon command and will get processed directly
                return;
            }

            spawnReason = spawnEvent.getSpawnReason();
        }
        else if (event instanceof ChunkLoadEvent)
            additionalInfo = LevelInterface.AdditionalLevelInformation.FROM_CHUNK_LISTENER;

        lmEntity.setSpawnReason(spawnReason);

        final HashSet<LevelInterface.AdditionalLevelInformation> additionalLevelInfo = new HashSet<>(Collections.singletonList(additionalInfo));
        final LevelInterface.LevellableState levellableState = getLevellableState(lmEntity, event);
        if (levellableState == LevelInterface.LevellableState.ALLOWED) {
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
                    lmEntity.getPDC().set(main.levelManager.wasBabyMobKey, PersistentDataType.INTEGER, 1);
                }
            }
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
    private LevelInterface.LevellableState getLevellableState(final LivingEntityWrapper lmEntity, @NotNull final Event event) {
        LevelInterface.LevellableState levellableState = main.levelInterface.getLevellableState(lmEntity);

        if (levellableState != LevelInterface.LevellableState.ALLOWED)
            return levellableState;

        if (event instanceof CreatureSpawnEvent) {
            CreatureSpawnEvent creatureSpawnEvent = (CreatureSpawnEvent) event;

            // the mob gets processed via SpawnerSpawnEvent
            if (((CreatureSpawnEvent) event).getSpawnReason().equals(CreatureSpawnEvent.SpawnReason.SPAWNER))
                return LevelInterface.LevellableState.DENIED_OTHER;

            Utils.debugLog(main, DebugType.ENTITY_SPAWN, "instanceof CreatureSpawnListener: &b" + creatureSpawnEvent.getEntityType() + "&7, with spawnReason &b" + creatureSpawnEvent.getSpawnReason() + "&7.");
        } else if (event instanceof EntitySpawnEvent)
            Utils.debugLog(main, DebugType.ENTITY_SPAWN, "not instanceof CreatureSpawnListener: &b" + ((EntitySpawnEvent) event).getEntityType());

        return LevelInterface.LevellableState.ALLOWED;
    }
}
