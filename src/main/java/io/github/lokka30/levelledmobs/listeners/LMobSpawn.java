package io.github.lokka30.levelledmobs.listeners;

import io.github.lokka30.levelledmobs.LevelledMobs;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;

import java.util.Objects;
import java.util.Random;

public class LMobSpawn implements Listener {

    private LevelledMobs instance = LevelledMobs.getInstance();

    /*
    This class assigns mob levels to each entity spawned.
    Attribute determined by: setBaseValue(default + elevated? + (increase-per-level * level)
     */
    @EventHandler
    public void onMobSpawn(final CreatureSpawnEvent e) {
        if (!e.isCancelled()) {
            final int level = generateLevel();
            LivingEntity ent = e.getEntity();


            final double baseMaxHealth = Objects.requireNonNull(e.getEntity().getAttribute(Attribute.GENERIC_MAX_HEALTH)).getBaseValue();
            Objects.requireNonNull(ent.getAttribute(Attribute.GENERIC_MAX_HEALTH)).setBaseValue(baseMaxHealth + (instance.settings.getFloat("max_health") * level));

            if (ent instanceof Monster) {
                final double baseMovementSpeed = Objects.requireNonNull(e.getEntity().getAttribute(Attribute.GENERIC_MOVEMENT_SPEED)).getBaseValue();
                Objects.requireNonNull(ent.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED)).setBaseValue(baseMovementSpeed + (instance.settings.getFloat("movement_speed") * level));
            }

            final EntityType type = ent.getType();

            //would use a neat switch statement here, but I'd prefer to support java 8 users :)
            //these entities aren't ranged, so their attack damage can be defined. otherwise it would produce an error.
            if (type == EntityType.ZOMBIE
                    || type == EntityType.HUSK
                    || type == EntityType.DROWNED
                    || type == EntityType.ZOMBIE_VILLAGER
                    || type == EntityType.WITHER_SKELETON
                    || type == EntityType.PIG_ZOMBIE
                    || type == EntityType.CAVE_SPIDER
                    || type == EntityType.SILVERFISH
                    || type == EntityType.SPIDER
                    || type == EntityType.ENDERMAN
                    || type == EntityType.ENDERMITE
                    || type == EntityType.SLIME
                    || type == EntityType.VINDICATOR
                    || type == EntityType.RAVAGER
                    || type == EntityType.EVOKER
                    || type == EntityType.IRON_GOLEM) {

                final double baseAttackDamage = Objects.requireNonNull(e.getEntity().getAttribute(Attribute.GENERIC_MOVEMENT_SPEED)).getBaseValue();
                //add 0.5. the default attack damage is very weak.
                Objects.requireNonNull(ent.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE)).setBaseValue(baseAttackDamage + 0.5 + (instance.settings.getFloat("attack_damage") * level));
            }

            String name;
            if (e.getEntity().getCustomName() == null) {
                name = e.getEntity().getName();
            } else {
                name = e.getEntity().getCustomName();
            }
            e.getEntity().setCustomName(instance.colorize("&8[&7Level " + level + "&8 | &f" + name + "&8]"));
            e.getEntity().setCustomName(instance.colorize(instance.settings.get("creature-nametag", "&8[&7Level %level%&8 | &f%name%&8]")
                    .replaceAll("%level%", level + "")
                    .replaceAll("%name%", name)));
        }
    }

    /*
    Generate a level for the creature.
     */
    public Integer generateLevel() {
        return new Random().nextInt(instance.settings.getInt("max-level") + 1) + instance.settings.getInt("min-level");
    }
}
