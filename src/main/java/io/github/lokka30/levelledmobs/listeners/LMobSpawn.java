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
                final double newMaxHealth = baseMaxHealth + (baseMaxHealth * (instance.settings.get("fine-tuning.multipliers.max-health", 1.5F) - 1) * level);
                Objects.requireNonNull(ent.getAttribute(Attribute.GENERIC_MAX_HEALTH)).setBaseValue(newMaxHealth);
                ent.setHealth(newMaxHealth);

                //Only monsters should have their movement speed changed. Otherwise you would have a very fast level 10 race horse, or an untouchable bat.
                if (ent instanceof Monster) {
                    final double baseMovementSpeed = Objects.requireNonNull(e.getEntity().getAttribute(Attribute.GENERIC_MOVEMENT_SPEED)).getBaseValue();
                    final double newMovementSpeed = baseMovementSpeed + (instance.settings.get("fine-tuning.multipliers.movement-speed", 0.01F) * level);
                    Objects.requireNonNull(ent.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED)).setBaseValue(newMovementSpeed);
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
                        final double baseAttackDamage = Objects.requireNonNull(e.getEntity().getAttribute(Attribute.GENERIC_ATTACK_DAMAGE)).getBaseValue();
                        final double defaultAttackDamageAddition = instance.settings.get("fine-tuning.default-attack-damage", 0.5D);
                        final double attackDamageMultiplier = instance.settings.get("fine-tuning.multipliers.attack-damage", 1.7D);
                        final double newAttackDamage = baseAttackDamage + defaultAttackDamageAddition + (attackDamageMultiplier * level);

                        Objects.requireNonNull(ent.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE)).setBaseValue(newAttackDamage);
                        break;
                    default:
                        break;
                }

                //Set the level.
                e.getEntity().getPersistentDataContainer().set(instance.key, PersistentDataType.INTEGER, level);

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
