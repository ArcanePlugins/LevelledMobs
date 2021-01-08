package io.github.lokka30.levelledmobs.listeners;

import io.github.lokka30.levelledmobs.LevelledMobs;
import io.github.lokka30.levelledmobs.utils.ConfigUtils;
import io.github.lokka30.levelledmobs.utils.Utils;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Boss;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;

public class CreatureSpawnListener implements Listener {

    private final LevelledMobs instance;

    public CreatureSpawnListener(final LevelledMobs instance) {
        this.instance = instance;
    }
    
    /*
    This class assigns mob levels to each entity spawned.
    Attribute determined by: setBaseValue(default + elevated? + (increase-per-level * level)
     */
    @EventHandler
    public void onMobSpawn(final CreatureSpawnEvent e) {
        if (e.isCancelled()) return;
        
        // spawned using summon command.  It will get processed directly
        if (e.getSpawnReason() == SpawnReason.CUSTOM) return;
        
        processMobSpawn(e.getEntity(), e.getSpawnReason(), -1);
    }
    
    public void processMobSpawn(final LivingEntity livingEntity, final SpawnReason spawnReason, int level) {

    	String entityName = livingEntity.getName();

        //Check if the mob is already levelled (safarinet compatibility, etc)
        String isLevelled = livingEntity.getPersistentDataContainer().get(instance.levelManager.isLevelledKey, PersistentDataType.STRING);
        if (isLevelled != null && isLevelled.equalsIgnoreCase("true")) {
            return;
        }
        if (livingEntity.getPersistentDataContainer().get(instance.levelManager.levelKey, PersistentDataType.INTEGER) != null) {
            return;
        }

        // if spawned naturally it will be -1.  If used summon with specific level specified then it will be >= 0
        if (level == -1) {
	        //Check settings for spawn distance levelling and choose levelling method accordingly.
            if (instance.hasWorldGuard && instance.worldGuardManager.checkRegionFlags(livingEntity)) {
                level = generateRegionLevel(livingEntity);
            } else if (instance.settingsCfg.getBoolean("spawn-distance-levelling.active")) {
                level = generateLevelByDistance(livingEntity);
            } else {
                level = generateLevel(livingEntity);
            }
        }

        if (level == 1 && !instance.settingsCfg.getBoolean("show-label-for-default-levelled-mobs")) {
            if (spawnReason == SpawnReason.SLIME_SPLIT)
                livingEntity.setCustomName(""); // child slimes carry the name of their parent
            return;
        }

        if (instance.levelManager.isLevellable(livingEntity)) {

            //Check the 'worlds list' to see if the mob is allowed to be levelled in the world it spawned in
            if (instance.settingsCfg.getBoolean("worlds-list.enabled")) {
                final List<String> worldsList = instance.settingsCfg.getStringList("worlds-list.list");
                final String mode = instance.settingsCfg.getString("worlds-list.mode");
                final String currentWorldName = livingEntity.getWorld().getName();
                switch (Objects.requireNonNull(mode)) {
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
            for (String blacklistedReason : instance.settingsCfg.getStringList("blacklisted-reasons")) {
                if (spawnReason.toString().equalsIgnoreCase(blacklistedReason) || blacklistedReason.equals("ALL")) {
                    return;
                }
            }

            if (instance.settingsCfg.getBoolean("slime-children-retain-level-of-parent") &&
                    spawnReason == SpawnReason.SLIME_SPLIT && Utils.isNotNullOrEmpty(entityName)) {
                // change child level to match parent.  Only possible from parsing the custom number for the level number

                // [Level 10 | Slime]
                // §8[§7Level 3§8 | §fSlime§8]

                Matcher m = instance.slimeRegex.matcher(entityName);
                if (m.find() && m.groupCount() >= 1) {
                    // the only reason it won't match is if someone has changed the custom name syntax significantly
                    String probablyLevelNum = m.group(1);
                    if (Utils.isInteger(probablyLevelNum))
            			level = Integer.parseInt(probablyLevelNum);
            	}
            	// if we didn't match then the slime will get a random level instead of the parent's level
            	
            }
                                   
            AttributeInstance ATTRIBUTE_MAX_HEALTH = livingEntity.getAttribute(Attribute.GENERIC_MAX_HEALTH);
            AttributeInstance ATTRIBUTE_MOVEMENT_SPEED = livingEntity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
            AttributeInstance ATTRIBUTE_ATTACK_DAMAGE = livingEntity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
            AttributeInstance ATTRIBUTE_FLYING_SPEED = livingEntity.getAttribute(Attribute.GENERIC_FLYING_SPEED); // doesn't look like this is actually used :(

            //Set the entity's max health.
            if (ATTRIBUTE_MAX_HEALTH != null) {
                final double baseMaxHealth = ATTRIBUTE_MAX_HEALTH.getBaseValue(); //change to default value
                final double newMaxHealth = baseMaxHealth + (baseMaxHealth * (instance.settingsCfg.getDouble("fine-tuning.multipliers.max-health")) * level);
                ATTRIBUTE_MAX_HEALTH.setBaseValue(newMaxHealth);
                livingEntity.setHealth(newMaxHealth); //Set the entity's health to their max health, otherwise their health is still the default of 20, so they'll be just as easy to kill.
            }

            //Set the entity's movement speed.
            if (ATTRIBUTE_MOVEMENT_SPEED != null && (instance.settingsCfg.getBoolean("passive-mobs-changed-movement-speed") || livingEntity instanceof Monster || livingEntity instanceof Boss)) {
                final double baseMovementSpeed = ATTRIBUTE_MOVEMENT_SPEED.getBaseValue(); //change to default value
                final double newMovementSpeed = baseMovementSpeed + (baseMovementSpeed * instance.settingsCfg.getDouble("fine-tuning.multipliers.movement-speed") * level);
                ATTRIBUTE_MOVEMENT_SPEED.setBaseValue(newMovementSpeed);
            }

            //Checks if mobs attack damage can be modified before changing it.
            if (ATTRIBUTE_ATTACK_DAMAGE != null) {
                final double baseAttackDamage = ATTRIBUTE_ATTACK_DAMAGE.getBaseValue(); //change to default value
                final double defaultAttackDamageAddition = instance.settingsCfg.getDouble("fine-tuning.default-attack-damage-increase");
                final double attackDamageMultiplier = instance.settingsCfg.getDouble("fine-tuning.multipliers.attack-damage");
                final double newAttackDamage = baseAttackDamage + defaultAttackDamageAddition + (attackDamageMultiplier * level);
                ATTRIBUTE_ATTACK_DAMAGE.setBaseValue(newAttackDamage);
            }

            if (ATTRIBUTE_FLYING_SPEED != null) {
                final double baseFlyingSpeed = ATTRIBUTE_FLYING_SPEED.getBaseValue(); //change to default value
                final double newflyingSpeed = baseFlyingSpeed + (baseFlyingSpeed * instance.settingsCfg.getDouble("fine-tuning.multipliers.flying-speed") * level);
                assert ATTRIBUTE_MOVEMENT_SPEED != null;
                ATTRIBUTE_MOVEMENT_SPEED.setBaseValue(newflyingSpeed);
            }

            //Define the mob's level so it can be accessed elsewhere.
            livingEntity.getPersistentDataContainer().set(instance.levelManager.levelKey, PersistentDataType.INTEGER, level);
            livingEntity.getPersistentDataContainer().set(instance.levelManager.isLevelledKey, PersistentDataType.STRING, "true");

            if (ConfigUtils.SETTINGS_CREEPER_MAX_RADIUS != 3 && livingEntity instanceof Creeper) {
                // level 1 ends up with 3 (base) and anything higher becomes a percent of the max radius as specified in the config
                int blastRadius = (int) Math.floor(level / 10.0 * ((double) ConfigUtils.SETTINGS_CREEPER_MAX_RADIUS) - 3) + 3;

                // even at 100 creepers are atomic bombs but at least won't blow up the entire world
                if (blastRadius > LevelledMobs.maxCreeperBlastRadius) {
                    blastRadius = LevelledMobs.maxCreeperBlastRadius;
                }
                if (level == 1) {
                    blastRadius = 3;
                } // level 1 creepers will always have default radius
                else if (level == 0 && blastRadius > 2) {
                    blastRadius = 2;
                } // level 0 will always be less than default

                ((Creeper) livingEntity).setExplosionRadius(blastRadius);
            }
            
            //Update their tag.
            instance.levelManager.updateTag(livingEntity);

        }
        else if (spawnReason == CreatureSpawnEvent.SpawnReason.CURED) {
            //Check if a zombie villager was cured. If villagers aren't levellable, then their name will be cleared,
            //otherwise their nametag is still 'Zombie Villager'. That doesn't seem right...
        	livingEntity.setCustomName("");
        }
    }

    //Generates a level.
    //Uses ThreadLocalRandom.current().nextInt(min, max + 1). + 1 is because ThreadLocalRandom is usually exclusive of the uppermost value.
    public Integer generateLevel(LivingEntity livingEntity) {
        return ThreadLocalRandom.current().nextInt(instance.configUtils.getMinLevel(livingEntity), instance.configUtils.getMaxLevel(livingEntity) + 1);
    }

    //Generates a level based on distance to spawn and, if active, variance
    private Integer generateLevelByDistance(LivingEntity livingEntity) {
        final int minLevel = instance.configUtils.getMinLevel(livingEntity);
        final int maxLevel = instance.configUtils.getMaxLevel(livingEntity);

        //Get distance between entity spawn point and world spawn
        final int entityDistance = (int) livingEntity.getWorld().getSpawnLocation().distance(livingEntity.getLocation());

        //Make mobs start leveling from start distance
        int levelDistance = entityDistance - instance.settingsCfg.getInt("spawn-distance-levelling.start-distance");
        if (levelDistance < 0) levelDistance = 0;

        //Get the level thats meant to be at a given distance
        int finalLevel = (levelDistance / instance.settingsCfg.getInt("spawn-distance-levelling.increase-level-distance")) + minLevel;

        //Check if there should be a variance in level
        if (instance.settingsCfg.getBoolean("spawn-distance-levelling.variance.enabled")) {
            //The minmum amount of variation.
            final int minVariation = instance.settingsCfg.getInt("spawn-distance-levelling.variance.min");

            //The maximum amount of variation.
            final int maxVariation = instance.settingsCfg.getInt("spawn-distance-levelling.variance.max");

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

        //Ensure the final level is within level min/max caps
        if (finalLevel > maxLevel) {
            finalLevel = maxLevel;
        }
        if (finalLevel < minLevel) {
            finalLevel = minLevel;
        }

        return finalLevel;
    }

    private int generateRegionLevel(LivingEntity livingEntity) {
        int[] levels = instance.worldGuardManager.getRegionLevel(livingEntity, instance.configUtils.getMinLevel(livingEntity), instance.configUtils.getMaxLevel(livingEntity));
        return levels[0] + Math.round(new Random().nextFloat() * (levels[1] - levels[0]));
    }
}
