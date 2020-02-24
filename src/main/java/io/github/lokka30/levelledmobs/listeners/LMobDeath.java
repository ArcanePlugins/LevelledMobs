package io.github.lokka30.levelledmobs.listeners;

import io.github.lokka30.levelledmobs.LevelledMobs;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

public class LMobDeath implements Listener {

    private LevelledMobs instance = LevelledMobs.getInstance();

    @EventHandler
    public void onDeath(final EntityDeathEvent e) {
        clearNametag(e.getEntity());
    }

    //Clear their nametag on death.
    private void clearNametag(LivingEntity ent){
        if (instance.isLevellable(ent) && instance.settings.get("fine-tuning.remove-nametag-on-death", false)) {
            ent.setCustomName(null);
        }
    }
}
