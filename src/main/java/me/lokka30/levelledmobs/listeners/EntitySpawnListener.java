package me.lokka30.levelledmobs.listeners;

import me.lokka30.levelledmobs.LevelInterface;
import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.misc.DebugType;
import me.lokka30.levelledmobs.misc.LivingEntityWrapper;
import me.lokka30.levelledmobs.misc.ModalList;
import me.lokka30.levelledmobs.misc.Utils;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.entity.SpawnerSpawnEvent;
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

    public EntitySpawnListener(final LevelledMobs main) {
        this.main = main;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onEntitySpawn(final EntitySpawnEvent event) {
        // Must be a LivingEntity.
        if (!(event.getEntity() instanceof LivingEntity)) return;

        if (event instanceof SpawnerSpawnEvent) {
            onSpawnerSpawn((SpawnerSpawnEvent) event);
            return;
        }
        if (event instanceof CreatureSpawnEvent){
            final CreatureSpawnEvent spawnEvent = (CreatureSpawnEvent) event;

            if (spawnEvent.getSpawnReason().equals(CreatureSpawnEvent.SpawnReason.SPAWNER) ||
                spawnEvent.getSpawnReason().equals(CreatureSpawnEvent.SpawnReason.SLIME_SPLIT))
                return;
        }

        final LivingEntityWrapper lmEntity = new LivingEntityWrapper((LivingEntity) event.getEntity(), main);

        preprocessMob(lmEntity, event);
    }

    private void onSpawnerSpawn(final SpawnerSpawnEvent event) {
        if (!(event.getEntity() instanceof LivingEntity)) return;

        final CreatureSpawner cs = event.getSpawner();
        final LivingEntityWrapper lmEntity = new LivingEntityWrapper((LivingEntity) event.getEntity(), main);

        if (!cs.getPersistentDataContainer().has(main.blockPlaceListener.keySpawner, PersistentDataType.INTEGER)){
            Utils.debugLog(main, DebugType.MOB_SPAWNER, "Spawned mob from vanilla spawner: " + event.getEntityType());
            preprocessMob(lmEntity, event);
            return;
        }

        // mob was spawned from a custom LM spawner
        createParticleEffect(cs.getLocation().add(0, 1, 0));
        final Integer minLevel = cs.getPersistentDataContainer().get(main.blockPlaceListener.keySpawner_MinLevel, PersistentDataType.INTEGER);
        final Integer maxLevel = cs.getPersistentDataContainer().get(main.blockPlaceListener.keySpawner_MaxLevel, PersistentDataType.INTEGER);
        final int useMinLevel = minLevel == null ? -1 : minLevel;
        final int useMaxLevel = maxLevel == null ? -1 : maxLevel;
        final int generatedLevel = main.levelInterface.generateLevel(lmEntity, useMinLevel, useMaxLevel);
        String customDropId = null;
        if (cs.getPersistentDataContainer().has(main.blockPlaceListener.keySpawner_CustomDropId, PersistentDataType.STRING)){
            customDropId = cs.getPersistentDataContainer().get(main.blockPlaceListener.keySpawner_CustomDropId, PersistentDataType.STRING);
            if (!Utils.isNullOrEmpty(customDropId))
                lmEntity.getPDC().set(main.blockPlaceListener.keySpawner_CustomDropId, PersistentDataType.STRING, customDropId);
        }

        Utils.debugLog(main, DebugType.MOB_SPAWNER, String.format(
                "Spawned mob from LM spawner: %s, minLevel: %s, maxLevel: %s, generatedLevel: %s%s",
                event.getEntityType(), useMinLevel, useMaxLevel, generatedLevel, (customDropId == null ? "" : ", dropid: " + customDropId)));

        main.levelInterface.applyLevelToMob(lmEntity, generatedLevel,
                false, true, new HashSet<>(Collections.singletonList(LevelInterface.AdditionalLevelInformation.NOT_APPLICABLE)));
    }

    private void createParticleEffect(final Location location){
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

        runnable.run();
    }

    public void preprocessMob(@NotNull final LivingEntityWrapper lmEntity, @NotNull final EntitySpawnEvent event){

        final LevelInterface.LevellableState levellableState = getLevellableState(lmEntity, event);
        if (levellableState == LevelInterface.LevellableState.ALLOWED) {
            main.levelInterface.applyLevelToMob(lmEntity, main.levelInterface.generateLevel(lmEntity),
                    false, false, new HashSet<>(Collections.singletonList(LevelInterface.AdditionalLevelInformation.NOT_APPLICABLE)));
        } else {
            Utils.debugLog(main, DebugType.APPLY_LEVEL_FAIL, "Entity " + event.getEntityType() + " in wo" +
                    "rld " + lmEntity.getWorldName() + " was not levelled -> Levellable state: " + levellableState);

            // Check if the mob is already levelled - if so, remove their level
            if (lmEntity.isLevelled()) {
                main.levelInterface.removeLevel(lmEntity);
            }
            else if (lmEntity.isBabyMob()) {
                // add a tag so we can potentially level the mob when/if it ages
                lmEntity.getPDC().set(main.levelManager.wasBabyMobKey, PersistentDataType.INTEGER, 1);
            }
        }
    }

    @NotNull
    private LevelInterface.LevellableState getLevellableState(final LivingEntityWrapper lmEntity, @NotNull final EntitySpawnEvent event) {
        LevelInterface.LevellableState levellableState = main.levelInterface.getLevellableState(lmEntity);

        if (levellableState != LevelInterface.LevellableState.ALLOWED) {
            return levellableState;
        }

        if (event instanceof CreatureSpawnEvent) {
            CreatureSpawnEvent creatureSpawnEvent = (CreatureSpawnEvent) event;

            // the mob gets processed via SpawnerSpawnEvent
            if (((CreatureSpawnEvent) event).getSpawnReason().equals(CreatureSpawnEvent.SpawnReason.SPAWNER))
                return LevelInterface.LevellableState.DENIED_OTHER;

            Utils.debugLog(main, DebugType.ENTITY_SPAWN, "instanceof CreatureSpawnListener: " + event.getEntityType() + ", with spawnReason " + creatureSpawnEvent.getSpawnReason() + ".");

            if (!ModalList.isEnabledInList(main.settingsCfg, "allowed-spawn-reasons-list", creatureSpawnEvent.getSpawnReason().toString())) {
                return LevelInterface.LevellableState.DENIED_CONFIGURATION_BLOCKED_SPAWN_REASON;
            }

            // Whilst we're here, unrelated, add in the spawner key to the mob
            lmEntity.getPDC().set(main.levelManager.spawnReasonKey, PersistentDataType.STRING, creatureSpawnEvent.getSpawnReason().toString());
        } else {
            Utils.debugLog(main, DebugType.ENTITY_SPAWN, "not instanceof CreatureSpawnListener: " + event.getEntityType());
        }

        return LevelInterface.LevellableState.ALLOWED;
    }
}