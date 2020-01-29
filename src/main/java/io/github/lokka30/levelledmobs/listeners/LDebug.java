package io.github.lokka30.levelledmobs.listeners;

import io.github.lokka30.levelledmobs.LevelledMobs;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Objects;
import java.util.UUID;

public class LDebug implements Listener {

    ArrayList<UUID> delay = new ArrayList<>();
    private LevelledMobs instance = LevelledMobs.getInstance();

    @EventHandler
    public void onEntityDamageByEntity(final EntityDamageByEntityEvent e) {
        if (e.getEntity() instanceof LivingEntity && e.getDamager() instanceof Player && instance.settings.get("debug", false)) {
            final Player p = (Player) e.getDamager();
            final UUID uuid = p.getUniqueId();

            if (p.isOp() && !delay.contains(uuid)) {
                final LivingEntity ent = (LivingEntity) e.getEntity();

                p.sendMessage(instance.colorize("&a&lLevelledMobs: &7Debug Information for &a" + ent.getType().toString() + "&7: "));
                p.sendMessage(instance.colorize("&8 - &fAttribute.GENERIC_MAX_HEALTH = " + Objects.requireNonNull(ent.getAttribute(Attribute.GENERIC_MAX_HEALTH)).getBaseValue()));
                p.sendMessage(instance.colorize("&8 - &fCurrent Health = " + ent.getHealth()));
                p.sendMessage(instance.colorize("&8 - &fAttribute.GENERIC_MOVEMENT_SPEED = " + Objects.requireNonNull(ent.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED)).getBaseValue()));
                p.sendMessage(instance.colorize("&8 - &fAttribute.GENERIC_ATTACK_DAMAGE = " + Objects.requireNonNull(ent.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE)).getBaseValue()));

                p.playSound(p.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1.0F, 1.0F);

                delay.add(uuid);
                new BukkitRunnable() {
                    public void run() {
                        delay.remove(uuid);
                        p.playSound(p.getLocation(), Sound.ITEM_BOOK_PUT, 1.0F, 1.0F);
                    }
                }.runTaskLater(instance, 40L);
            }
        }
    }
}
