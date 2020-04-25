package io.github.lokka30.levelledmobs.listeners;

import io.github.lokka30.levelledmobs.LevelledMobs;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.persistence.PersistentDataType;

import java.util.Collections;
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
            /* Code seems to not work.
            String isLevelled = livingEntity.getPersistentDataContainer().get(instance.isLevelledKey, PersistentDataType.STRING);
            if (isLevelled != null && isLevelled.equalsIgnoreCase("true")) {
                return;
            }
            */

            //Check if the mob is already levelled (safarinet compatibility, etc)
            if (livingEntity.getPersistentDataContainer().get(instance.levelKey, PersistentDataType.INTEGER) != null) {
                return;
            }

            //Check settings for spawn distance levelling and choose levelling method accordingly.
            if (instance.hasWorldGuard && instance.worldGuardManager.checkRegionFlags(livingEntity)) {
                level = generateRegionLevel(livingEntity);
            } else if (instance.settings.get("spawn-distance-levelling.active", false)) {
                level = generateLevelByDistance(livingEntity);
            } else {
                level = generateLevel();
            }

            if (instance.levelManager.isLevellable(livingEntity)) { //Is the mob allowed to be levelled?

                //Check the list of blacklisted worlds. If the entity's world is in here, then we don't continue.
                for (String blacklistedWorld : instance.settings.get("blacklisted-worlds", Collections.singletonList("BLACKLISTED_WORLD"))) {
                    if (e.getEntity().getWorld().getName().equalsIgnoreCase(blacklistedWorld) || blacklistedWorld.equals("ALL")) {
                        return;
                    }
                }

                //Check the list of blacklisted spawn reasons. If the entity's spawn reason is in there, then we don't continue.
                //Uses a default as "NONE" as there are no blocked spawn reasons in the default config.
                for (String blacklistedReason : instance.settings.get("blacklisted-reasons", Collections.singletonList("NONE"))) {
                    if (e.getSpawnReason().toString().equalsIgnoreCase(blacklistedReason) || blacklistedReason.equals("ALL")) {
                        return;
                    }
                }

                //Check if mob is already levelled. Fixes portal doubling and other related issues.
                if (e.getEntity().getPersistentDataContainer().get(instance.levelKey, PersistentDataType.INTEGER) != null) {
                    return;
                }

                //Set the entity's max health.
                final double baseMaxHealth = Objects.requireNonNull(e.getEntity().getAttribute(Attribute.GENERIC_MAX_HEALTH)).getBaseValue();
                final double newMaxHealth = baseMaxHealth + (baseMaxHealth * (instance.settings.get("fine-tuning.multipliers.max-health", 0.2F)) * level);
                Objects.requireNonNull(livingEntity.getAttribute(Attribute.GENERIC_MAX_HEALTH)).setBaseValue(newMaxHealth);
                livingEntity.setHealth(newMaxHealth); //Set the entity's health to their max health, otherwise their health is still the default of 20, so they'll be just as easy to kill.

                //Set the entity's movement speed.
                //Only monsters should have their movement speed changed. Otherwise you would have a very fast level 10 race horse, or an untouchable bat.
                if (livingEntity instanceof Monster) {
                    final double baseMovementSpeed = Objects.requireNonNull(e.getEntity().getAttribute(Attribute.GENERIC_MOVEMENT_SPEED)).getBaseValue();
                    final double newMovementSpeed = baseMovementSpeed + (baseMovementSpeed * instance.settings.get("fine-tuning.multipliers.movement-speed", 0.065F) * level);
                    Objects.requireNonNull(livingEntity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED)).setBaseValue(newMovementSpeed);
                }

                //Checks if mobs attack damage can be modified before changing it.
                if (livingEntity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE) != null) {
                    final double baseAttackDamage = Objects.requireNonNull(e.getEntity().getAttribute(Attribute.GENERIC_ATTACK_DAMAGE)).getBaseValue();
                    final double defaultAttackDamageAddition = instance.settings.get("fine-tuning.default-attack-damage-increase", 1.0F);
                    final double attackDamageMultiplier = instance.settings.get("fine-tuning.multipliers.attack-damage", 1.5F);
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
                // otherwise their nametag is still 'Zombie Villager'. Imposter!
                e.getEntity().setCustomName("");
            }
        }
    }

    //Generates a level.
    //Uses ThreadLocalRandom.current().nextInt(min, max + 1). + 1 is because ThreadLocalRandom is usually exclusive of the uppermost value.
    public Integer generateLevel() {
        return ThreadLocalRandom.current().nextInt(instance.settings.get("fine-tuning.min-level", 1), instance.settings.get("fine-tuning.max-level", 10) + 1);
    }

    //Generates a level based on distance to spawn and, if active, variance
    private Integer generateLevelByDistance(LivingEntity livingEntity) {
        int minLevel, maxLevel, defaultLevel, finalLevel, levelSpan, distance;

        minLevel = instance.settings.get("fine-tuning.min-level", 1);
        maxLevel = instance.settings.get("fine-tuning.max-level", 10);
        finalLevel = -1;

        //Calculate amount of available levels
        levelSpan = (maxLevel + 1) - minLevel;

        //Get distance between entity spawn point and world spawn
        distance = (int) livingEntity.getWorld().getSpawnLocation().distance(livingEntity.getLocation());

        //Get the level thats meant to be at a given distance
        defaultLevel = (distance / instance.settings.get("spawn-distance-levelling.increase-level-distance", 200)) + minLevel;
        if (defaultLevel > maxLevel)
            defaultLevel = maxLevel;

        //Check if there should be a variance in level
        if (instance.settings.get("spawn-distance-levelling.variance", true)) {
            double binomialp, randomnumber;
            double[] levelarray, weightedlevelarray;


            //Create array with chances for each level
            levelarray = new double[levelSpan];
            binomialp = (1.0D / levelSpan / 2.0D) + ((1.0D - (1.0D / levelSpan)) / levelSpan * (defaultLevel - minLevel));
            for (int i = 0; i < levelSpan; i++) {
                levelarray[i] = instance.utils.binomialDistribution(levelSpan, i, binomialp);
            }

            //Create weighted array for choosing a level
            weightedlevelarray = instance.utils.createWeightedArray(levelarray);

            //Choose a level based on the weight of a level
            randomnumber = new Random().nextDouble() * weightedlevelarray[weightedlevelarray.length - 1];
            for (int i = 0; i < weightedlevelarray.length; i++)
                if (randomnumber <= weightedlevelarray[i]) {
                    finalLevel = i + minLevel;
                    break;
                }

        } else
            finalLevel = defaultLevel;

        finalLevel = finalLevel == -1 ? 0 : finalLevel;

        return finalLevel;
    }

    private int generateRegionLevel(LivingEntity ent) {
        //Fallback values
        int minlevel = instance.settings.get("fine-tuning.min-level", 1);
        int maxlevel = instance.settings.get("fine-tuning.max-level", 10);

        int[] levels = instance.worldGuardManager.getRegionLevel(ent, minlevel, maxlevel);

        return levels[0] + Math.round(new Random().nextFloat() * (levels[1] - levels[0]));
    }
}
