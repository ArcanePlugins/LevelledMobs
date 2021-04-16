package me.lokka30.levelledmobs.listeners;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.misc.Utils;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

public class PlayerDeathListener implements Listener {

    public PlayerDeathListener(final LevelledMobs main){
        this.main = main;
    }

    final private LevelledMobs main;

    /**
     * This listener handles death nametags
     *
     * @param event PlayerDeathEvent
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerDeath(final PlayerDeathEvent event) {
        if (event.getDeathMessage() == null) return;

        final String deathNametag = main.settingsCfg.getString("creature-death-nametag", "&8[&7Level %level%&8 | &f%displayname%&8]");
        if (Utils.isNullOrEmpty(deathNametag))
            return; // if they want retain the stock message they are configure it with an empty string

        final EntityDamageEvent entityDamageEvent = event.getEntity().getLastDamageCause();
        if (entityDamageEvent == null || entityDamageEvent.isCancelled() || !(entityDamageEvent instanceof EntityDamageByEntityEvent))
            return;

        final Entity damager = ((EntityDamageByEntityEvent) entityDamageEvent).getDamager();
        LivingEntity killer;

        if (damager instanceof Projectile)
            killer = (LivingEntity) ((Projectile) damager).getShooter();
        else if (!(damager instanceof LivingEntity))
            return;
        else
            killer = (LivingEntity) damager;

        if (killer == null) return;
        if (!main.levelInterface.isLevelled(killer)) return;

        event.setDeathMessage(Utils.replaceEx(event.getDeathMessage(), killer.getName(), main.levelManager.getNametag(killer, true)));
    }
}
