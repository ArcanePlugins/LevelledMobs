package io.github.lokka30.levelledmobs.listeners;

import io.github.lokka30.levelledmobs.LevelManager;
import io.github.lokka30.levelledmobs.LevelledMobs;
import io.github.lokka30.levelledmobs.utils.*;
import me.lokka30.microlib.MicroUtils;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.persistence.PersistentDataType;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class CreatureSpawnListener implements Listener {

    private final LevelledMobs instance;
    private final HashSet<String> forcedTypes = new HashSet<>(Arrays.asList("ENDER_DRAGON", "PHANTOM"));

    public CreatureSpawnListener(final LevelledMobs instance) {
        this.instance = instance;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerDeath(final PlayerDeathEvent event) {
        String nametag = instance.settingsCfg.getString("creature-death-nametag", "&8[&7Level %level%&8 | &f%displayname%&8]");
        if (Utils.isNullOrEmpty(nametag)) return; // if they want retain the stock message they are configure it with an empty string

        final EntityDamageEvent entityDamageEvent = event.getEntity().getLastDamageCause();
        if (entityDamageEvent == null || entityDamageEvent.isCancelled() || !(entityDamageEvent instanceof EntityDamageByEntityEvent)){
            return;
        }

        Entity damager = ((EntityDamageByEntityEvent) entityDamageEvent).getDamager();
        LivingEntity killer;

        if (damager instanceof Projectile) {
            killer = (LivingEntity) ((Projectile)damager).getShooter();
        }
        else if (!(damager instanceof LivingEntity)) return;
        else{
            killer = (LivingEntity) damager;
        }
        
        if (killer == null) return;

        final Object levelTemp = killer.getPersistentDataContainer().get(instance.levelManager.levelKey, PersistentDataType.INTEGER);
        if (levelTemp == null) return;

        final int level = (int)levelTemp;
        final String entityName = instance.configUtils.getEntityName(killer.getType());
        final String displayName = killer.getCustomName() == null ? entityName : killer.getCustomName();
        final AttributeInstance maxHealth = killer.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        final String health = maxHealth == null ? "?" : Utils.round(maxHealth.getBaseValue()) + "";

        nametag = nametag.replace("%level%", level + "");
        nametag = nametag.replace("%displayname%", displayName);
        nametag = nametag.replace("%typename%", entityName);
        nametag = nametag.replace("%health%", Utils.round(killer.getHealth()) + "");
        nametag = nametag.replace("%max_health%", health);
        nametag = nametag.replace("%heart_symbol%", "❤");
        nametag = MicroUtils.colorize(nametag);

        final String newMessage = Utils.replaceEx(event.getDeathMessage(), killer.getName(), nametag);
        event.setDeathMessage(newMessage);
    }

    /**
     * This listens for entities that are not passed thrrough CreatureSpawnEvent,
     * such as Phantoms and Ender Dragons, which server owners may want to have levelled.
     *
     * @param event EntitySpawnEvent, the event to listen to
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onEntitySpawn(final EntitySpawnEvent event) {
        if (!(event.getEntity() instanceof LivingEntity)) return;

        if (!forcedTypes.contains(event.getEntityType().toString())) return;

        final LivingEntity livingEntity = (LivingEntity) event.getEntity();
        processMobSpawn(livingEntity, SpawnReason.DEFAULT, -1, MobProcessReason.NONE);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onChunkLoad(final ChunkLoadEvent event){
        for (final Entity entity : event.getChunk().getEntities()){
            if (!(entity instanceof LivingEntity)) continue;

            LivingEntity livingEntity = (LivingEntity) entity;
            processMobSpawn(livingEntity, SpawnReason.DEFAULT, -1, MobProcessReason.NONE);
        }
    }

    /**
     * This listens to most mobs that spawn in.
     *
     * @param event CreatureSpawnEvent, the event to listen to
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onCreatureSpawn(final CreatureSpawnEvent event) {
        // spawned using summon command.  It will get processed directly
        if (event.getSpawnReason() == SpawnReason.CUSTOM) return;

        processMobSpawn(event.getEntity(), event.getSpawnReason(), -1, MobProcessReason.NONE);
    }



    public void processMobSpawn(final LivingEntity livingEntity, final SpawnReason spawnReason, int level, final MobProcessReason processReason) {

        //Check if the mob is already levelled
        if (livingEntity.getPersistentDataContainer().has(instance.levelManager.isLevelledKey, PersistentDataType.STRING))
            return;
        if (livingEntity.getPersistentDataContainer().has(instance.levelManager.levelKey, PersistentDataType.INTEGER))
            return;

        //Check if the mob was spawned by MythicMobs
        if (instance.hasMythicMobsInstalled && !instance.settingsCfg.getBoolean("allow-mythic-mobs") && instance.mythicMobsHelper.isMythicMob(livingEntity)){
            return;
        }

        //Check the 'worlds list' to see if the mob is allowed to be levelled in the world it spawned in
        if (!ModalList.isEnabledInList(instance.settingsCfg, "allowed-worlds-list", livingEntity.getWorld().getName())) {
            if (instance.settingsCfg.getBoolean("debug-show-mobs-not-levellable")) {
                Utils.logger.info("&b" + livingEntity.getName() + "&7 spawned but is not levellable - not in allowed-worlds-list");
            }
            return;
        }

        if (spawnReason == SpawnReason.SPAWNER)
            livingEntity.getPersistentDataContainer().set(instance.levelManager.isSpawnerKey, PersistentDataType.STRING, "true");

        boolean isSpawner = livingEntity.getPersistentDataContainer().has(instance.levelManager.isSpawnerKey, PersistentDataType.STRING);

        //Check the list of blacklisted spawn reasons. If the entity's spawn reason is in there, then we don't continue.
        //Uses a default as "NONE" as there are no blocked spawn reasons in the default config.
        if (!ModalList.isEnabledInList(instance.settingsCfg, "allowed-spawn-reasons-list", spawnReason.toString()) ||
                isSpawner && !ModalList.isEnabledInList(instance.settingsCfg, "allowed-spawn-reasons-list", SpawnReason.SPAWNER.toString())) {
            if (instance.settingsCfg.getBoolean("debug-show-mobs-not-levellable")) {
                Utils.logger.info("&b" + livingEntity.getName() + "&7 spawned but is not levellable - not in allowed-spawn-reasons-list");
            }
            return;
        }

        final DebugInfo debugInfo = instance.settingsCfg.getBoolean("debug-show-spawned-mobs") ?
                new DebugInfo() : null;

        if (instance.levelManager.isLevellable(livingEntity)) {

            // if spawned naturally it will be -1.  If used summon with specific level specified or if using the slime child system then it will be >= 0
            if (level == -1) {
                //Check settings for spawn distance levelling and choose levelling method accordingly.
                if (instance.hasWorldGuardInstalled && instance.worldGuardManager.checkRegionFlags(livingEntity)) {
                    level = generateRegionLevel(livingEntity, debugInfo);
                } else if (instance.settingsCfg.getBoolean("spawn-distance-levelling.active")) {
                    level = generateDistanceFromSpawnLevel(livingEntity, debugInfo);
                } else {
                    level = generateLevel(livingEntity, debugInfo);
                }
            }

            //Define the mob's level so it can be accessed elsewhere.
            livingEntity.getPersistentDataContainer().set(instance.levelManager.levelKey, PersistentDataType.INTEGER, level);
            livingEntity.getPersistentDataContainer().set(instance.levelManager.isLevelledKey, PersistentDataType.STRING, "true");

            // Modify their maximum health attribute. This changes the maximum health the levelled mob has.
            // Makes sure the levelled mob has this attribute. If not, skip setting it.
            if (livingEntity.getAttribute(Attribute.GENERIC_MAX_HEALTH) != null) {

                // This sets the max health value.
                instance.mobDataManager.setAdditionsForLevel(livingEntity, Attribute.GENERIC_MAX_HEALTH, Addition.ATTRIBUTE_MAX_HEALTH, level);

                // Need to set their actual health otherwise their current health != max health.
                livingEntity.setHealth(Objects.requireNonNull(livingEntity.getAttribute(Attribute.GENERIC_MAX_HEALTH)).getBaseValue());
            }

            // Modify their movement speed attribute. This changes the movement speed the levelled mob has.
            // Makes sure the levelled mob has this attribute. If not, skip setting it.
            if (livingEntity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED) != null) {
                //This sets the movement speed value.
                instance.mobDataManager.setAdditionsForLevel(livingEntity, Attribute.GENERIC_MOVEMENT_SPEED, Addition.ATTRIBUTE_MOVEMENT_SPEED, level);
            }

            // Modify their attack damage attribute. This changes the attack damage the levelled mob has.
            // Makes sure the levelled mob has this attribute. If not, skip setting it.
            if (livingEntity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE) != null) {
                //This sets the movement speed value.
                instance.mobDataManager.setAdditionsForLevel(livingEntity, Attribute.GENERIC_ATTACK_DAMAGE, Addition.ATTRIBUTE_ATTACK_DAMAGE, level);
            }

            if (livingEntity instanceof Creeper && instance.settingsCfg.getInt("creeper-max-damage-radius", 3) != 3) {

                // level 1 ends up with 3 (base) and anything higher becomes a percent of the max radius as specified in the config
                //int blastRadius = (int) Math.floor(level / 10.0 * ((double) instance.settingsCfg.getInt("creeper-max-damage-radius")) - 3) + 3;

                final double levelDiff = instance.settingsCfg.getInt("fine-tuning.max-level", 10) - instance.settingsCfg.getInt("fine-tuning.min-level", 1);
                final double maxBlastDiff = instance.settingsCfg.getInt("creeper-max-damage-radius", 3) - 3;
                final double useLevel = level - instance.settingsCfg.getInt("fine-tuning.min-level", 1);
                final double percent = useLevel / levelDiff;
                int blastRadius = (int)Math.round(maxBlastDiff * percent) + 3;

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
            final String nameTag;
            if (level != 1 || instance.settingsCfg.getBoolean("show-label-for-default-levelled-mobs")) {
                nameTag = instance.levelManager.getNametag(livingEntity, level);
                instance.levelManager.updateNametagWithDelay(livingEntity, nameTag, livingEntity.getWorld().getPlayers(), 1);
            }

            // Debug Info
            if (debugInfo != null) {
                boolean isBabyZombie = livingEntity instanceof Zombie && !Utils.isZombieBaby((Zombie) livingEntity);

                if (spawnReason == SpawnReason.CUSTOM) {
                    debugInfo.minLevel = level;
                    debugInfo.maxLevel = level;
                }

                if (processReason == MobProcessReason.SUMMON) {
                    debugInfo.rule = MobProcessReason.SUMMON;
                }

                String babyOrAdult = !isBabyZombie ? "Adult" : "Baby";
                String rule;
                switch (debugInfo.rule) {
                    case WORLD:
                        rule = " - World rule";
                        break;
                    case ENTITY:
                        rule = " - Entity rule";
                        break;
                    case WORLD_GUARD:
                        rule = " - WG rule";
                        break;
                    case SUMMON:
                        rule = " - summon rule";
                        break;
                    case TRANSFORM:
                        rule = " - transform rule";
                        break;
                    default:
                        rule = "";
                }

                Utils.logger.info(String.format("Spawned a &fLvl.%s &b%s &8(&7%s&8) min: %s, max: %s%s",
                        level, livingEntity.getName(), babyOrAdult, debugInfo.minLevel, debugInfo.maxLevel, rule));
            }

        } else if (spawnReason == CreatureSpawnEvent.SpawnReason.CURED) {
            //Check if a zombie villager was cured. If villagers aren't levellable, then their name will be cleared,
            //otherwise their nametag is still 'Zombie Villager'.
            instance.levelManager.updateNametagWithDelay(livingEntity, null, livingEntity.getWorld().getPlayers(), 1);
        } else {
            if (instance.settingsCfg.getBoolean("debug-show-mobs-not-levellable")) {
                Utils.logger.info("&b" + livingEntity.getName() + "&7 spawned but is not levellable");
            }
        }

    }

    //Generates a level.
    //Uses ThreadLocalRandom.current().nextInt(min, max + 1). + 1 is because ThreadLocalRandom is usually exclusive of the uppermost value.
    public Integer generateLevel(final LivingEntity livingEntity, final DebugInfo debugInfo) {

        final boolean isBabyZombie = livingEntity instanceof Zombie && !Utils.isZombieBaby((Zombie) livingEntity);

        if (instance.settingsCfg.getBoolean("y-distance-levelling.active")){
            return generateYCoordinateLevel(
                    livingEntity.getLocation().getBlockY(),
                    instance.configUtils.getMinLevel(livingEntity.getType(), livingEntity.getWorld(), isBabyZombie, debugInfo),
                    instance.configUtils.getMaxLevel(livingEntity.getType(), livingEntity.getWorld(), isBabyZombie, debugInfo)
            );
        }

        // normal return:
        int minLevel = instance.configUtils.getMinLevel(livingEntity.getType(),
                livingEntity.getWorld(), isBabyZombie, debugInfo);
        final int maxLevel = instance.configUtils.getMaxLevel(livingEntity.getType(),
                livingEntity.getWorld(), isBabyZombie, debugInfo) + 1;

        // this will prevent an unhandled exception:
        if (minLevel > maxLevel) minLevel = maxLevel;
        
        return minLevel == maxLevel ? minLevel : ThreadLocalRandom.current().nextInt(minLevel, maxLevel);
    }

    //Generates a level based on distance to spawn and, if active, variance
    private Integer generateDistanceFromSpawnLevel(final LivingEntity livingEntity, final DebugInfo debugInfo) {
        final boolean isBabyZombie = livingEntity instanceof Zombie && !Utils.isZombieBaby((Zombie) livingEntity);
        final int minLevel = instance.configUtils.getMinLevel(livingEntity.getType(), livingEntity.getWorld(), !isBabyZombie, debugInfo);
        final int maxLevel = instance.configUtils.getMaxLevel(livingEntity.getType(), livingEntity.getWorld(), !isBabyZombie, debugInfo);

        if (debugInfo != null) {
            debugInfo.minLevel = minLevel;
            debugInfo.maxLevel = maxLevel;
        }

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

            boolean useOnlyNegative = false;

            if (finalLevel >= maxLevel) {
                finalLevel = maxLevel;
                useOnlyNegative = true;
            }
            else if (finalLevel <= minLevel) {
                finalLevel = minLevel;
            }

            //Start variation. First check if variation is positive or negative towards the original level amount.
            if (!useOnlyNegative || ThreadLocalRandom.current().nextBoolean()) {
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

    private int generateRegionLevel(final LivingEntity livingEntity, final DebugInfo debugInfo) {
        final boolean isBabyZombie = livingEntity instanceof Zombie && !Utils.isZombieBaby((Zombie) livingEntity);
        final int[] levels = instance.worldGuardManager.getRegionLevel(livingEntity,
                instance.configUtils.getMinLevel(livingEntity.getType(), livingEntity.getWorld(), !isBabyZombie, debugInfo),
                instance.configUtils.getMaxLevel(livingEntity.getType(), livingEntity.getWorld(), !isBabyZombie, debugInfo));

        if (debugInfo != null){
            debugInfo.rule = MobProcessReason.WORLD_GUARD;
            debugInfo.minLevel = levels[0];
            debugInfo.maxLevel = levels[1];
        }

        if (!instance.settingsCfg.getBoolean("y-distance-levelling.active")){
            // standard issue, generate random levels based upon max and min flags in worldguard
            return levels[0] + Math.round(new Random().nextFloat() * (levels[1] - levels[0]));
        }

        // generate level based on y distance but use min and max values from world guard
        return generateYCoordinateLevel(livingEntity.getLocation().getBlockY(), levels[0], levels[1]);
    }

    private int generateYCoordinateLevel(final int mobYLocation, final int minLevel, final int maxLevel) {
        final int yPeriod = instance.settingsCfg.getInt("y-distance-levelling.y-period", 0);
        final int variance = instance.settingsCfg.getInt("y-distance-levelling.variance", 0);
        int yStart = instance.settingsCfg.getInt("y-distance-levelling.starting-y-level", 100);
        int yEnd = instance.settingsCfg.getInt("y-distance-levelling.ending-y-level", 20);

        final boolean isAscending = (yEnd > yStart);
        if (!isAscending) {
            yStart = yEnd;
            yEnd = instance.settingsCfg.getInt("y-distance-levelling.starting-y-level", 100);
        }

        int useLevel = minLevel;
        boolean skipYPeriod = false;

        if (mobYLocation >= yEnd){
            useLevel = maxLevel;
            skipYPeriod = true;
        }
        else if (mobYLocation <= yStart){
            skipYPeriod = true;
        }

        if (!skipYPeriod) {
            final double diff = yEnd - yStart;
            double useMobYLocation =  mobYLocation - yStart;

            if (yPeriod > 0) {
                useLevel = (int) (useMobYLocation / (double) yPeriod);
            } else {
                double percent = useMobYLocation / diff;
                useLevel = (int) Math.ceil((maxLevel - minLevel + 1) * percent);
            }
        }

        if (!isAscending) {
            useLevel = maxLevel - useLevel + 1;
        }

        if (variance > 0){
            boolean useOnlyNegative = false;

            if (useLevel >= maxLevel) {
                useLevel = maxLevel;
                useOnlyNegative = true;
            }
            else if (useLevel <= minLevel) {
                useLevel = minLevel;
            }

            final int change = ThreadLocalRandom.current().nextInt(0, variance + 1);

            //Start variation. First check if variation is positive or negative towards the original level amount.
            if (!useOnlyNegative || ThreadLocalRandom.current().nextBoolean()) {
                //Positive. Add the variation to the final level
                useLevel += change;
            } else {
                //Negative. Subtract the variation from the final level
                useLevel -= change;
            }
        }

        if (useLevel < minLevel) useLevel = minLevel;
        else if (useLevel > maxLevel) useLevel = maxLevel;

        return useLevel;
    }
}
