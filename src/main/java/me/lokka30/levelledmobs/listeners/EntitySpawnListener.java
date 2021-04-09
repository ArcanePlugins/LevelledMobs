package me.lokka30.levelledmobs.listeners;

import me.lokka30.levelledmobs.LevelInterface;
import me.lokka30.levelledmobs.LevelledMobs;
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
        String deathNametag = main.settingsCfg.getString("creature-death-nametag", "&8[&7Level %level%&8 | &f%displayname%&8]");
        if (Utils.isNullOrEmpty(deathNametag))
            return; // if they want retain the stock message they are configure it with an empty string

        final EntityDamageEvent entityDamageEvent = event.getEntity().getLastDamageCause();
        if (entityDamageEvent == null || entityDamageEvent.isCancelled() || !(entityDamageEvent instanceof EntityDamageByEntityEvent)) {
            return;
        }

        Entity damager = ((EntityDamageByEntityEvent) entityDamageEvent).getDamager();
        LivingEntity killer;

        if (damager instanceof Projectile) {
            killer = (LivingEntity) ((Projectile) damager).getShooter();
        } else if (!(damager instanceof LivingEntity)) {
            return;
        } else {
            killer = (LivingEntity) damager;
        }

        if (killer == null) return;
        if (!main.levelInterface.isLevelled(killer)) return;

        event.setDeathMessage(Utils.replaceEx(event.getDeathMessage(), killer.getName(), main.levelManager.getNametag(killer, true)));
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

        final LevelInterface.LevellableState levellableState = getLevellableState(event);
        if (levellableState == LevelInterface.LevellableState.ALLOWED) {
            main.levelInterface.applyLevelToMob(livingEntity, main.levelInterface.generateLevel(livingEntity), false, false);
        } else {
            Utils.debugLog(main, "ApplyLevelFail", "Entity " + event.getEntityType() + " in wo" +
                    "rld " + livingEntity.getWorld().getName() + " was not levelled -> Levellable state: " + levellableState.toString());

            // Check if the mob is already levelled - if so, remove their level
            if (main.levelInterface.isLevelled(livingEntity)) {
                main.levelInterface.removeLevel(livingEntity);
            }
        }
    }

    private LevelInterface.LevellableState getLevellableState(EntitySpawnEvent event) {
        assert event.getEntity() instanceof LivingEntity;
        LivingEntity livingEntity = (LivingEntity) event.getEntity();

        LevelInterface.LevellableState levellableState = main.levelInterface.getLevellableState(livingEntity);

        if (levellableState != LevelInterface.LevellableState.ALLOWED) {
            return levellableState;
        }

        if (event instanceof CreatureSpawnEvent) {
            CreatureSpawnEvent creatureSpawnEvent = (CreatureSpawnEvent) event;

            if (!ModalList.isEnabledInList(main.settingsCfg, "allowed-spawn-reasons-list", creatureSpawnEvent.getSpawnReason().toString())) {
                return LevelInterface.LevellableState.DENIED_CONFIGURATION_BLOCKED_SPAWN_REASON;
            }

            // Whilst we're here, unrelated, add in the spawner key to the mob
            livingEntity.getPersistentDataContainer().set(main.levelManager.spawnReasonKey, PersistentDataType.STRING, creatureSpawnEvent.getSpawnReason().toString());
        }

        return LevelInterface.LevellableState.ALLOWED;
    }
}