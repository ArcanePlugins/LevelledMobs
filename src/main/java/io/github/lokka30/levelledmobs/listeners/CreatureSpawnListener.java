package io.github.lokka30.levelledmobs.listeners;

import io.github.lokka30.levelledmobs.LevelledMobs;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Boss;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class CreatureSpawnListener implements Listener {

    private LevelledMobs instance;

    public CreatureSpawnListener(final LevelledMobs instance) {
        this.instance = instance;
    }

    /*
    This class assigns mob levels to each entity spawned.
    Attribute determined by: setBaseValue(default + elevated? + (increase-per-level * level)
     */
    @EventHandler
    public void onMobSpawn(final CreatureSpawnEvent e) {
        if (!e.isCancelled()) {
            final int level; //The mob's level.
            LivingEntity livingEntity = e.getEntity(); //The entity that was just spawned.

            //Check if the mob is already levelled (safarinet compatibility, etc)
            String isLevelled = livingEntity.getPersistentDataContainer().get(instance.isLevelledKey, PersistentDataType.STRING);
            if (isLevelled != null && isLevelled.equalsIgnoreCase("true")) {
                return;
            }
            if (livingEntity.getPersistentDataContainer().get(instance.levelKey, PersistentDataType.INTEGER) != null) {
                return;
            }

            //Check settings for spawn distance levelling and choose levelling method accordingly.
            if (instance.hasWorldGuard && instance.worldGuardManager.checkRegionFlags(livingEntity)) {
                level = generateRegionLevel(livingEntity);
            } else if (instance.fileCache.SETTINGS_SPAWN_DISTANCE_LEVELLING_ACTIVE) {
                level = generateLevelByDistance(livingEntity);
            } else {
                level = generateLevel(livingEntity);
            }

            if (instance.levelManager.isLevellable(livingEntity)) { //Is the mob allowed to be levelled?

                //Check the 'worlds list' to see if the mob is allowed to be levelled in the world it spawned in
                if (instance.fileCache.SETTINGS_WORLDS_LIST_ENABLED) {
                    final List<String> worldsList = instance.fileCache.SETTINGS_WORLDS_LIST_LIST;
                    final String mode = instance.fileCache.SETTINGS_WORLDS_LIST_MODE;
                    final String currentWorldName = livingEntity.getWorld().getName();
                    switch (mode) {
                        case "BLACKLIST":
                            if (worldsList.contains(currentWorldName)) {
                                return;
                            }
                            break;
                        case "WHITELIST":
                            if (!worldsList.contains(currentWorldName)) {
                                return;
                            }
                            break;
                        default:
                            throw new IllegalStateException("Unknown worlds list mode '" + mode + "', expecting 'BLACKLIST' or 'WHITELIST'. Ignoring world list due to the error.");
                    }
                }

                //Check the list of blacklisted spawn reasons. If the entity's spawn reason is in there, then we don't continue.
                //Uses a default as "NONE" as there are no blocked spawn reasons in the default config.
                for (String blacklistedReason : instance.fileCache.SETTINGS_BLACKLISTED_REASONS) {
                    if (e.getSpawnReason().toString().equalsIgnoreCase(blacklistedReason) || blacklistedReason.equals("ALL")) {
                        return;
                    }
                }

                //Set the entity's max health.
                final double baseMaxHealth = Objects.requireNonNull(e.getEntity().getAttribute(Attribute.GENERIC_MAX_HEALTH)).getBaseValue(); //TODO CHANGE TO GETDEFAULTVALUE
                final double newMaxHealth = baseMaxHealth + (baseMaxHealth * (instance.fileCache.SETTINGS_FINE_TUNING_MULTIPLIERS_MAX_HEALTH) * level);
                Objects.requireNonNull(livingEntity.getAttribute(Attribute.GENERIC_MAX_HEALTH)).setBaseValue(newMaxHealth);
                livingEntity.setHealth(newMaxHealth); //Set the entity's health to their max health, otherwise their health is still the default of 20, so they'll be just as easy to kill.

                //Set the entity's movement speed.
                if (instance.fileCache.SETTINGS_PASSIVE_MOBS_CHANGED_MOVEMENT_SPEED || livingEntity instanceof Monster || livingEntity instanceof Boss) {
                    final double baseMovementSpeed = Objects.requireNonNull(e.getEntity().getAttribute(Attribute.GENERIC_MOVEMENT_SPEED)).getBaseValue(); //TODO CHANGE TO GETDEFAULTVALUE
                    final double newMovementSpeed = baseMovementSpeed + (baseMovementSpeed * instance.fileCache.SETTINGS_FINE_TUNING_MULTIPLIERS_MOVEMENT_SPEED * level);
                    Objects.requireNonNull(livingEntity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED)).setBaseValue(newMovementSpeed);
                }

                //Checks if mobs attack damage can be modified before changing it.
                if (livingEntity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE) != null) {
                    final double baseAttackDamage = Objects.requireNonNull(e.getEntity().getAttribute(Attribute.GENERIC_ATTACK_DAMAGE)).getBaseValue(); //TODO CHANGE TO GETDEFAULTVALUE
                    final double defaultAttackDamageAddition = instance.fileCache.SETTINGS_FINE_TUNING_DEFAULT_ATTACK_DAMAGE_INCREASE;
                    final double attackDamageMultiplier = instance.fileCache.SETTINGS_FINE_TUNING_MULTIPLIERS_ATTACK_DAMAGE;
                    final double newAttackDamage = baseAttackDamage + defaultAttackDamageAddition + (attackDamageMultiplier * level);

                    Objects.requireNonNull(livingEntity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE)).setBaseValue(newAttackDamage);
                }

                //Define the mob's level so it can be accessed elsewhere.
                livingEntity.getPersistentDataContainer().set(instance.levelKey, PersistentDataType.INTEGER, level);
                livingEntity.getPersistentDataContainer().set(instance.isLevelledKey, PersistentDataType.STRING, "true");

                //Update their tag.
                instance.levelManager.updateTag(e.getEntity());

            } else if (e.getSpawnReason() == CreatureSpawnEvent.SpawnReason.CURED) {
                //Check if a zombie villager was cured. If villagers aren't levellable, then their name will be cleared,
                //otherwise their nametag is still 'Zombie Villager'. Imposter!
                e.getEntity().setCustomName("");
            }
        }
    }

    //Generates a level.
    //Uses ThreadLocalRandom.current().nextInt(min, max + 1). + 1 is because ThreadLocalRandom is usually exclusive of the uppermost value.
    public Integer generateLevel(LivingEntity livingEntity) {
        return ThreadLocalRandom.current().nextInt(instance.fileCache.getMinLevel(livingEntity), instance.fileCache.getMaxLevel(livingEntity) + 1);
    }

    //Generates a level based on distance to spawn and, if active, variance
    private Integer generateLevelByDistance(LivingEntity livingEntity) {
        final int minLevel = instance.fileCache.getMinLevel(livingEntity);
        final int maxLevel = instance.fileCache.getMaxLevel(livingEntity);

        //Get distance between entity spawn point and world spawn
        final int distance = (int) livingEntity.getWorld().getSpawnLocation().distance(livingEntity.getLocation());

        //Get the level thats meant to be at a given distance
        int finalLevel = (distance / instance.fileCache.SETTINGS_SPAWN_DISTANCE_LEVELLING_INCREASE_LEVEL_DISTANCE) + minLevel;

        //Check if there should be a variance in level
        if (instance.fileCache.SETTINGS_SPAWN_DISTANCE_LEVELLING_VARIANCE_ENABLED) {
            //The minmum amount of variation.
            final int minVariation = instance.fileCache.SETTINGS_SPAWN_DISTANCE_LEVELLING_VARIANCE_MIN;

            //The maximum amount of variation.
            final int maxVariation = instance.fileCache.SETTINGS_SPAWN_DISTANCE_LEVELLING_VARIANCE_MAX;

            //A random number between min and max which determines the amount of variation that will take place
            final int change = ThreadLocalRandom.current().nextInt(minVariation, maxVariation + 1);

            //Start variation. First check if variation is positive or negative towards the original level amount.
            if (new Random().nextBoolean()) {
                //Positive. Add the variation to the final level
                finalLevel = finalLevel + change;
            } else {
                //Negative. Subtract the variation from the final level
                finalLevel = finalLevel - change;
            }
        }

        if (finalLevel > maxLevel) {
            finalLevel = maxLevel;
        }
        if (finalLevel < minLevel) {
            finalLevel = minLevel;
        }

        return finalLevel;
    }

    private int generateRegionLevel(LivingEntity livingEntity) {
        int[] levels = instance.worldGuardManager.getRegionLevel(livingEntity, instance.fileCache.getMinLevel(livingEntity), instance.fileCache.getMaxLevel(livingEntity));
        return levels[0] + Math.round(new Random().nextFloat() * (levels[1] - levels[0]));
    }
}
