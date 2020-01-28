package io.github.lokka30.levelledmobs.listeners;

import io.github.lokka30.levelledmobs.LevelledMobs;
import org.apache.commons.lang.StringUtils;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.persistence.PersistentDataType;

import java.util.Objects;

public class EntityDamage implements Listener {

    private LevelledMobs instance = LevelledMobs.getInstance();
    private NamespacedKey key = new NamespacedKey(instance, "level");

    //whenever an entity is damaged, update its custom name to reflect its current health
    @EventHandler
    public void onEntityDamage(EntityDamageEvent e) {
        if (e.getEntity() instanceof LivingEntity) {
            LivingEntity entity = (LivingEntity) e.getEntity();
            String name = StringUtils.capitalize(entity.getType().name().toLowerCase());

            e.getEntity().setCustomName(instance.colorize(instance.settings.get("creature-nametag", "&8[&7Level %level%&8 | &f%name%&8]")
                    .replaceAll("%level%", entity.getPersistentDataContainer().get(key, PersistentDataType.INTEGER) + "")
                    .replaceAll("%name%", name)
                    .replaceAll("%health%", String.valueOf((int) entity.getHealth()))
                    .replaceAll("%max_health%", String.valueOf((int) Objects.requireNonNull(entity.getAttribute(Attribute.GENERIC_MAX_HEALTH)).getBaseValue()))));
        }
    }

}
