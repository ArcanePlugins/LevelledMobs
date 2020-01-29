package io.github.lokka30.levelledmobs.listeners;

import io.github.lokka30.levelledmobs.LevelledMobs;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.persistence.PersistentDataType;

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

            if (instance.isLevellable(ent)) {
                final double baseMaxHealth = Objects.requireNonNull(e.getEntity().getAttribute(Attribute.GENERIC_MAX_HEALTH)).getBaseValue();

                //Set the max health.
                Objects.requireNonNull(ent.getAttribute(Attribute.GENERIC_MAX_HEALTH)).setBaseValue(baseMaxHealth + (instance.settings.get("fine-tuning.max_health", 1.0F) * level));

                //Only monsters should have their movement speed changed. Otherwise you would have a very fast level 10 race horse, or an untouchable bat.
                if (ent instanceof Monster) {
                    final double baseMovementSpeed = Objects.requireNonNull(e.getEntity().getAttribute(Attribute.GENERIC_MOVEMENT_SPEED)).getBaseValue();
                    Objects.requireNonNull(ent.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED)).setBaseValue(baseMovementSpeed + (instance.settings.get("fine-tuning.movement_speed", 0.02F) * level));
                }

                //These are melee mobs - their attack damage can be defined. Don't touch ranged mobs, else a NPE will occur.
                switch (ent.getType()) {
                    case ZOMBIE:
                    case HUSK:
                    case DROWNED:
                    case ZOMBIE_VILLAGER:
                    case WITHER_SKELETON:
                    case PIG_ZOMBIE:
                    case CAVE_SPIDER:
                    case SILVERFISH:
                    case SPIDER:
                    case ENDERMAN:
                    case ENDERMITE:
                    case SLIME:
                    case VINDICATOR:
                    case RAVAGER:
                    case EVOKER:
                    case IRON_GOLEM:
                        final double baseAttackDamage = Objects.requireNonNull(e.getEntity().getAttribute(Attribute.GENERIC_MOVEMENT_SPEED)).getBaseValue();
                        //add 0.5. the default attack damage is very weak.
                        Objects.requireNonNull(ent.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE)).setBaseValue(baseAttackDamage + 0.5 + (instance.settings.get("fine-tuning.attack_damage", 1.7F) * level));
                        break;
                    default:
                        break;
                }

                //Set the level.
                e.getEntity().getPersistentDataContainer().set(instance.key, PersistentDataType.INTEGER, level);

                ent.setHealth(baseMaxHealth);

                //Update their tag.
                instance.updateTag(e.getEntity());
            }
        }
    }

    //Generates a level.
    public Integer generateLevel() {
        return new Random().nextInt(instance.settings.get("fine-tuning.max-level", 10) + 1) + instance.settings.get("fine-tuning.min-level", 0);
    }
}
