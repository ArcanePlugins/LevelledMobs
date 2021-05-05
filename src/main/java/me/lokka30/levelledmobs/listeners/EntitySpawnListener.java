package me.lokka30.levelledmobs.listeners;

import me.lokka30.levelledmobs.LevelInterface;
import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.misc.DebugType;
import me.lokka30.levelledmobs.misc.ModalList;
import me.lokka30.levelledmobs.misc.Utils;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashSet;

/**
 * @author lokka30
 * @contributors stumper66
 */
public class EntitySpawnListener implements Listener {

    private final LevelledMobs main;

    public EntitySpawnListener(final LevelledMobs main) {
        this.main = main;
    }

    /**
     * This listener handles death nametags
     *
     * @param event PlayerDeathEvent
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerDeath(final PlayerDeathEvent event) {
        final String deathMessage = event.getDeathMessage();
        if (Utils.isNullOrEmpty(deathMessage)) return;

        if (main.settingsCfg.getString("creature-death-nametag", "disabled").equalsIgnoreCase("disabled")) return;

        final EntityDamageEvent entityDamageEvent = event.getEntity().getLastDamageCause();
        if (entityDamageEvent == null || entityDamageEvent.isCancelled() || !(entityDamageEvent instanceof EntityDamageByEntityEvent))
            return;

        final Entity damager = ((EntityDamageByEntityEvent) entityDamageEvent).getDamager();
        LivingEntity killer;

        if (damager instanceof Projectile) {
            killer = (LivingEntity) ((Projectile) damager).getShooter();
        } else if (damager instanceof LivingEntity) {
            killer = (LivingEntity) damager;
        } else {
            return;
        }
        if (killer == null) return;
        if (!main.levelInterface.isLevelled(killer)) return;

        if (Utils.isNullOrEmpty(killer.getName())) {
            return;
        }

        final String deathNametag = main.levelManager.getNametag(killer, true);
        if (Utils.isNullOrEmpty(deathNametag)) return;

        event.setDeathMessage(Utils.replaceEx(deathMessage, killer.getName(), deathNametag));
    }

    /**
     * This listens for entities that spawn in the server
     *
     * @param event EntitySpawnEvent
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onEntitySpawn(final EntitySpawnEvent event) {

        // Must be a LivingEntity.
        if (!(event.getEntity() instanceof LivingEntity)) return;
        final LivingEntity livingEntity = (LivingEntity) event.getEntity();

        // when spawned from spawner it creates two events, SpawnerSpawnEvent and CreatureSpawnEvent
        if (event instanceof SpawnerSpawnEvent) return;

        final LevelInterface.LevellableState levellableState = getLevellableState(event);
        if (levellableState == LevelInterface.LevellableState.ALLOWED) {
            main.levelInterface.applyLevelToMob(livingEntity, main.levelInterface.generateLevel(livingEntity),
                    false, false, new HashSet<>(Collections.singletonList(LevelInterface.AdditionalLevelInformation.NOT_APPLICABLE)));
        } else {
            Utils.debugLog(main, DebugType.APPLY_LEVEL_FAIL, "Entity " + event.getEntityType() + " in wo" +
                    "rld " + livingEntity.getWorld().getName() + " was not levelled -> Levellable state: " + levellableState);

            // Check if the mob is already levelled - if so, remove their level
            if (main.levelInterface.isLevelled(livingEntity)) {
                main.levelInterface.removeLevel(livingEntity);
            }
            else if (Utils.isBabyMob(livingEntity)) {
                // add a tag so we can potentially level the mob when/if it ages
                livingEntity.getPersistentDataContainer().set(main.levelManager.wasBabyMobKey, PersistentDataType.INTEGER, 1);
            }
        }
    }

    @NotNull
    private LevelInterface.LevellableState getLevellableState(@NotNull final EntitySpawnEvent event) {
        assert event.getEntity() instanceof LivingEntity;
        LivingEntity livingEntity = (LivingEntity) event.getEntity();

        LevelInterface.LevellableState levellableState = main.levelInterface.getLevellableState(livingEntity);

        if (levellableState != LevelInterface.LevellableState.ALLOWED) {
            return levellableState;
        }

        if (event instanceof CreatureSpawnEvent) {
            CreatureSpawnEvent creatureSpawnEvent = (CreatureSpawnEvent) event;

            Utils.debugLog(main, DebugType.ENTITY_SPAWN, "instanceof CreatureSpawnListener: " + event.getEntityType() + ", with spawnReason " + creatureSpawnEvent.getSpawnReason() + ".");

            if (!ModalList.isEnabledInList(main.settingsCfg, "allowed-spawn-reasons-list", creatureSpawnEvent.getSpawnReason().toString())) {
                return LevelInterface.LevellableState.DENIED_CONFIGURATION_BLOCKED_SPAWN_REASON;
            }

            // Whilst we're here, unrelated, add in the spawner key to the mob
            livingEntity.getPersistentDataContainer().set(main.levelManager.spawnReasonKey, PersistentDataType.STRING, creatureSpawnEvent.getSpawnReason().toString());
        } else {
            Utils.debugLog(main, DebugType.ENTITY_SPAWN, "not instanceof CreatureSpawnListener: " + event.getEntityType());
        }

        return LevelInterface.LevellableState.ALLOWED;
    }
}