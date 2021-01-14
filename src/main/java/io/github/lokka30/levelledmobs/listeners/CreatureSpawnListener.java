package io.github.lokka30.levelledmobs.listeners;

import io.github.lokka30.levelledmobs.LevelManager;
import io.github.lokka30.levelledmobs.LevelledMobs;
import io.github.lokka30.levelledmobs.enums.ModalList;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.persistence.PersistentDataType;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class CreatureSpawnListener implements Listener {

    private final LevelledMobs instance;

    public CreatureSpawnListener(final LevelledMobs instance) {
        this.instance = instance;
    }

    /**
     * This listens for entities that are not passed thrrough CreatureSpawnEvent,
     * such as Phantoms and Ender Dragons, which server owners may want to have levelled.
     *
     * @param event EntitySpawnEvent, the event to listen to
     */
    @EventHandler
    public void onEntitySpawn(final EntitySpawnEvent event) {
        if (event.isCancelled()) return;

        if (!(event.getEntity() instanceof LivingEntity)) return;

        LivingEntity livingEntity = (LivingEntity) event.getEntity();

        List<EntityType> forcedTypes = Arrays.asList(EntityType.ENDER_DRAGON, EntityType.PHANTOM);
        if (forcedTypes.contains(event.getEntityType())) {
            processMobSpawn(livingEntity, SpawnReason.DEFAULT, -1);
        }
    }

    /**
     * Thos listens to most mobs that spawn in.
     *
     * @param event CreatureSpawnEvent, the event to listen to
     */
    @EventHandler
    public void onCreatureSpawn(final CreatureSpawnEvent event) {
        if (event.isCancelled()) return;

        // spawned using summon command.  It will get processed directly
        if (event.getSpawnReason() == SpawnReason.CUSTOM) return;

        processMobSpawn(event.getEntity(), event.getSpawnReason(), -1);
    }
    
    public void processMobSpawn(final LivingEntity livingEntity, final SpawnReason spawnReason, int level) {

        //Check if the mob is already levelled
        if (livingEntity.getPersistentDataContainer().get(instance.levelManager.levelKey, PersistentDataType.INTEGER) != null) {
            return;
        }

        // if spawned naturally it will be -1.  If used summon with specific level specified then it will be >= 0
        if (level == -1) {
	        //Check settings for spawn distance levelling and choose levelling method accordingly.
            if (instance.hasWorldGuardInstalled && instance.worldGuardManager.checkRegionFlags(livingEntity)) {
                level = generateRegionLevel(livingEntity);
            } else if (instance.settingsCfg.getBoolean("spawn-distance-levelling.active")) {
                level = generateLevelByDistance(livingEntity);
            } else {
                level = generateLevel(livingEntity);
            }
        }

        /* TODO
        if (level == 1 && !instance.settingsCfg.getBoolean("show-label-for-default-levelled-mobs")) {
            if (spawnReason == SpawnReason.SLIME_SPLIT)
                livingEntity.setCustomName(""); // child slimes carry the name of their parent
            return;
        }
         */

        if (instance.levelManager.isLevellable(livingEntity)) {

            //Check the 'worlds list' to see if the mob is allowed to be levelled in the world it spawned in
            if (!ModalList.isEnabledInList(instance.settingsCfg, "allowed-worlds-list", livingEntity.getWorld().getName())) {
                return;
            }

            //Check the list of blacklisted spawn reasons. If the entity's spawn reason is in there, then we don't continue.
            //Uses a default as "NONE" as there are no blocked spawn reasons in the default config.
            if (!ModalList.isEnabledInList(instance.settingsCfg, "allowed-spawn-reasons-list", spawnReason.toString())) {
                return;
            }

            /* TODO
            // change child level to match parent.  Only possible from parsing the custom number for the level number
            if (spawnReason == SpawnReason.SLIME_SPLIT && Utils.isNotNullOrEmpty(entityName)) {
                if (instance.settingsCfg.getBoolean("slime-children-retain-level-of-parent")) {
                    // [Level 10 | Slime]

                    Matcher m = instance.levelManager.slimeRegex.matcher(entityName);
                    if (m.find() && m.groupCount() >= 1) {
                        // the only reason it won't match is if someone has changed the custom name syntax significantly
                        String probablyLevelNum = m.group(1);
                        if (Utils.isInteger(probablyLevelNum))
                            level = Integer.parseInt(probablyLevelNum);
                    }

                    // if we didn't match then the slime will get a random level instead of the parent's level
                }
            }

             */

            //Define the mob's level so it can be accessed elsewhere.
            livingEntity.getPersistentDataContainer().set(instance.levelManager.levelKey, PersistentDataType.INTEGER, level);
            livingEntity.getPersistentDataContainer().set(instance.levelManager.isLevelledKey, PersistentDataType.STRING, "true");

            // Max Health attribute
            if (livingEntity.getAttribute(Attribute.GENERIC_MAX_HEALTH) != null) {
                double multiplier = instance.settingsCfg.getDouble("fine-tuning.additions.attributes.max-health");
                instance.attributeManager.setAddedValue(livingEntity, Attribute.GENERIC_MAX_HEALTH, multiplier, level);
                //noinspection ConstantConditions
                livingEntity.setHealth(livingEntity.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue());
            }

            // Movement Speed attribute
            if (livingEntity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED) != null) {
                double multiplier = instance.settingsCfg.getDouble("fine-tuning.additions.attributes.movement-speed");
                instance.attributeManager.setAddedValue(livingEntity, Attribute.GENERIC_MOVEMENT_SPEED, multiplier, level);
            }

            // Attack Damage attribute
            if (livingEntity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE) != null) {
                double multiplier = instance.settingsCfg.getDouble("fine-tuning.additions.attributes.attack-damage");
                instance.attributeManager.setAddedValue(livingEntity, Attribute.GENERIC_ATTACK_DAMAGE, multiplier, level);
            }

            if (livingEntity instanceof Creeper && instance.settingsCfg.getInt("creeper-max-damage-radius", 3) != 3) {
                //TODO: update this method to properly scale blast radius when min or max levels have been changed from the default of 1 and 10

                // level 1 ends up with 3 (base) and anything higher becomes a percent of the max radius as specified in the config
                int blastRadius = (int) Math.floor(level / 10.0 * ((double) instance.settingsCfg.getInt("creeper-max-damage-radius")) - 3) + 3;

                // even at 100 creepers are atomic bombs but at least won't blow up the entire world
                if (blastRadius > LevelManager.maxCreeperBlastRadius) {
                    blastRadius = LevelManager.maxCreeperBlastRadius;
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
            instance.levelManager.updateNametag(livingEntity, instance.levelManager.getNametag(livingEntity));

        }
        else if (spawnReason == CreatureSpawnEvent.SpawnReason.CURED) {
            //Check if a zombie villager was cured. If villagers aren't levellable, then their name will be cleared,
            //otherwise their nametag is still 'Zombie Villager'. That doesn't seem right...
            instance.levelManager.updateNametag(livingEntity, "");
        }
    }

    //Generates a level.
    //Uses ThreadLocalRandom.current().nextInt(min, max + 1). + 1 is because ThreadLocalRandom is usually exclusive of the uppermost value.
    public Integer generateLevel(final LivingEntity livingEntity) {

        if (instance.settingsCfg.getBoolean("y-distance-levelling.active")){
            return getMobLevelFromYDistance(
                    livingEntity.getLocation().getBlockY(),
                    instance.configUtils.getMinLevel(livingEntity.getType(), livingEntity.getWorld()),
                    instance.configUtils.getMaxLevel(livingEntity.getType(), livingEntity.getWorld())
            );
        }

        // normal return:
        return ThreadLocalRandom.current().nextInt(
                instance.configUtils.getMinLevel(livingEntity.getType(),
                        livingEntity.getWorld()),
                instance.configUtils.getMaxLevel(livingEntity.getType(),
                        livingEntity.getWorld()) + 1
        );
    }

    //Generates a level based on distance to spawn and, if active, variance
    private Integer generateLevelByDistance(final LivingEntity livingEntity) {
        final int minLevel = instance.configUtils.getMinLevel(livingEntity.getType(), livingEntity.getWorld());
        final int maxLevel = instance.configUtils.getMaxLevel(livingEntity.getType(), livingEntity.getWorld());

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
            if (ThreadLocalRandom.current().nextBoolean()) {
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
        else if (finalLevel < minLevel) {
            finalLevel = minLevel;
        }

        return finalLevel;
    }

    private int generateRegionLevel(final LivingEntity livingEntity) {
        final int[] levels = instance.worldGuardManager.getRegionLevel(livingEntity, instance.configUtils.getMinLevel(livingEntity.getType(), livingEntity.getWorld()), instance.configUtils.getMaxLevel(livingEntity.getType(), livingEntity.getWorld()));

        if (!instance.settingsCfg.getBoolean("y-distance-levelling.active")){
            // standard issue, generate random levels based upon max and min flags in worldguard
            return levels[0] + Math.round(new Random().nextFloat() * (levels[1] - levels[0]));
        }

        // generate level based on y distance but use min and max values from world guard
        return getMobLevelFromYDistance(livingEntity.getLocation().getBlockY(), levels[0], levels[1]);
    }

    private int getMobLevelFromYDistance(final int mobYLocation, final int minLevel, final int maxLevel) {
        final int yPeriod = instance.settingsCfg.getInt("y-distance-levelling.y-period", 0);
        final int variance = instance.settingsCfg.getInt("y-distance-levelling.variance", 0);
        int yStart = instance.settingsCfg.getInt("y-distance-levelling.starting-y-level", 100);
        int yEnd = instance.settingsCfg.getInt("y-distance-levelling.ending-y-level", 20);

        final boolean isAscending = (yEnd > yStart);
        if (!isAscending){
            yStart = yEnd;
            yEnd = instance.settingsCfg.getInt("y-distance-levelling.starting-y-level", 100);
        }

        if (mobYLocation <= yStart){
            return isAscending ? minLevel : maxLevel;
        }
        else if (mobYLocation > yEnd){
            return isAscending ? maxLevel : minLevel;
        }

        final double diff = yEnd - yStart;
        double useMobYLocation = mobYLocation - yStart;
        int useLevel;

        if (yPeriod > 0){
            useLevel = (int)(useMobYLocation / (double) yPeriod);
        }
        else {
            if (useMobYLocation < yStart) {
                useMobYLocation = 1.0;
            } else if (useMobYLocation > yEnd) {
                useMobYLocation = yEnd;
            }

            useLevel = (int) Math.ceil(useMobYLocation / diff * maxLevel);
        }

        if (variance > 0){
            final int change = ThreadLocalRandom.current().nextInt(0, variance);
            //Start variation. First check if variation is positive or negative towards the original level amount.
            if (ThreadLocalRandom.current().nextBoolean()) {
                //Positive. Add the variation to the final level
                useLevel += change;
            } else {
                //Negative. Subtract the variation from the final level
                useLevel -= change;
            }
        }

        if (!isAscending) {
            useLevel = maxLevel - useLevel + 1;
        }

        if (useLevel < minLevel){ useLevel = minLevel; }
        else if (useLevel > maxLevel){ useLevel = maxLevel; }

        return useLevel;
    }
}
