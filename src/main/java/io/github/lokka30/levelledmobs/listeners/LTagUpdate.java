package io.github.lokka30.levelledmobs.listeners;

import io.github.lokka30.levelledmobs.LevelledMobs;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class LTagUpdate implements Listener {

    private LevelledMobs instance = LevelledMobs.getInstance();

    //Update their tag on damage.
    @EventHandler
    public void onDamage(final EntityDamageEvent e) {
        instance.updateTag(e.getEntity());

        new BukkitRunnable() {
            public void run() {
                instance.updateTag(e.getEntity());
            }
        }.runTaskLater(instance, 10L);
    }

    //Clear their nametag on death.
    @EventHandler
    public void onDeath(final EntityDeathEvent e) {
        final LivingEntity livingEntity = e.getEntity();
        if (instance.isLevellable(livingEntity) && instance.settings.get("fine-tuning.remove-nametag-on-death", false)) {
            livingEntity.setCustomName(null);
        }
    }
}
