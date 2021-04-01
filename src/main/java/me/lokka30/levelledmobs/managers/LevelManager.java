package me.lokka30.levelledmobs.managers;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.customdrops.CustomDropResult;
import me.lokka30.levelledmobs.listeners.CreatureSpawnListener;
import me.lokka30.levelledmobs.misc.*;
import me.lokka30.microlib.MessageUtils;
import org.apache.commons.lang.WordUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.*;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.inventory.AbstractHorseInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import javax.annotation.Nonnull;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author lokka30
 * @contributors stumper66, Eyrian2010, iCodinqs, deiphiz, CoolBoy, Esophose, 7smile7, Shevchik, Hugo5551
 */
public class LevelManager {

    private final LevelledMobs main;
    final public HashMap<LevelNumbersWithBias, LevelNumbersWithBias> levelNumsListCache = new HashMap<>();
    final public LinkedList<LevelNumbersWithBias> levelNumsListCacheOrder = new LinkedList<>();
    private final static int maxLevelNumsCache = 10;
    final private List<Material> vehicleNoMultiplierItems;

    public LevelManager(final LevelledMobs main) {
        this.main = main;

        levelKey = new NamespacedKey(main, "level");
        isSpawnerKey = new NamespacedKey(main, "isSpawner");

        final int factor = main.settingsCfg.getInt("fine-tuning.lower-mob-level-bias-factor", 0);
        if (factor > 0) {
            LevelNumbersWithBias lnwb = new LevelNumbersWithBias(
                    main.settingsCfg.getInt("fine-tuning.min-level", 1),
                    main.settingsCfg.getInt("fine-tuning.max-level", 10),
                    factor
            );

            lnwb.populateData();
            this.levelNumsListCache.put(lnwb, lnwb);
            this.levelNumsListCacheOrder.addFirst(lnwb);
        }

        this.vehicleNoMultiplierItems = Arrays.asList(
                Material.SADDLE,
                Material.LEATHER_HORSE_ARMOR,
                Material.IRON_HORSE_ARMOR,
                Material.GOLDEN_HORSE_ARMOR,
                Material.DIAMOND_HORSE_ARMOR
        );
    }

    public final NamespacedKey levelKey; // This stores the mob's level.
    public final NamespacedKey isSpawnerKey; //This is stored on levelled mobs to tell plugins that a mob was created from a spawner

    public final HashSet<String> forcedTypes = new HashSet<>(Arrays.asList("GHAST", "MAGMA_CUBE", "HOGLIN", "SHULKER", "PHANTOM", "ENDER_DRAGON", "SLIME", "MAGMA_CUBE", "ZOMBIFIED_PIGLIN"));

    public final static int maxCreeperBlastRadius = 100;
    public CreatureSpawnListener creatureSpawnListener;

    public boolean isLevellable(final EntityType entityType) {
        // Don't level these
        if (
                entityType == EntityType.PLAYER
                        || entityType == EntityType.UNKNOWN
                        || entityType == EntityType.ARMOR_STAND
                        || entityType == EntityType.ITEM_FRAME
                        || entityType == EntityType.DROPPED_ITEM
                        || entityType == EntityType.PAINTING
        ) return false;

        // Check if the entity is blacklisted. If not, continue.
        if (!ModalList.isEnabledInList(main.settingsCfg, "allowed-entities-list", entityType.toString()))
            return false;

        // Check if the entity is overriden. If so, force it to be levelled.
        if (main.settingsCfg.getStringList("overriden-entities").contains(entityType.toString())) return true;

        // These entities don't implement Monster or Boss and thus must be forced to return true
        if (forcedTypes.contains(entityType.toString())) {
            return true;
        }

        // Grab the Entity class, which is used to check for certain assignments
        Class<? extends Entity> entityClass = entityType.getEntityClass();
        if (entityClass == null) return false;

        return Monster.class.isAssignableFrom(entityClass)
                || Boss.class.isAssignableFrom(entityClass)
                || main.settingsCfg.getBoolean("level-passive");
    }

    //Checks if an entity can be levelled.
    public boolean isLevellable(final LivingEntity livingEntity) {

        // Ignore these entity types and metadatas
        if (livingEntity.getType() == EntityType.PLAYER
                || livingEntity.getType() == EntityType.UNKNOWN
                || livingEntity.getType() == EntityType.ARMOR_STAND
                || livingEntity.getType() == EntityType.ITEM_FRAME
                || livingEntity.getType() == EntityType.DROPPED_ITEM
                || livingEntity.getType() == EntityType.PAINTING

                // DangerousCaves plugin compatibility
                || (livingEntity.hasMetadata("DangerousCaves") && main.externalCompatibilityManager.isExternalCompatibilityEnabled(ExternalCompatibilityManager.ExternalCompatibility.DANGEROUS_CAVES))

                // EliteMobs plugin compatibility
                || (livingEntity.hasMetadata("Elitemob") && main.externalCompatibilityManager.isExternalCompatibilityEnabled(ExternalCompatibilityManager.ExternalCompatibility.ELITE_MOBS))
                || (livingEntity.hasMetadata("Elitemobs_NPC") && main.externalCompatibilityManager.isExternalCompatibilityEnabled(ExternalCompatibilityManager.ExternalCompatibility.ELITE_MOBS_NPCS))
                || (livingEntity.hasMetadata("Supermob") && main.externalCompatibilityManager.isExternalCompatibilityEnabled(ExternalCompatibilityManager.ExternalCompatibility.ELITE_MOBS_SUPER_MOBS))

                //InfernalMobs plugin compatibility)
                || (livingEntity.hasMetadata("infernalMetadata") && main.externalCompatibilityManager.isExternalCompatibilityEnabled(ExternalCompatibilityManager.ExternalCompatibility.INFERNAL_MOBS))

                // Citizens plugin compatibility
                || (livingEntity.hasMetadata("NPC") && main.externalCompatibilityManager.isExternalCompatibilityEnabled(ExternalCompatibilityManager.ExternalCompatibility.CITIZENS))

                // Shopkeepers plugin compatibility
                || (livingEntity.hasMetadata("shopkeeper") && main.externalCompatibilityManager.isExternalCompatibilityEnabled(ExternalCompatibilityManager.ExternalCompatibility.SHOPKEEPERS))
        ) {
            return false;
        }

        // Check 'no level conditions'
        if (livingEntity.getCustomName() != null && main.settingsCfg.getBoolean("no-level-conditions.nametagged")) {
            return false;
        }
        if (livingEntity instanceof Tameable && ((Tameable) livingEntity).isTamed() && main.settingsCfg.getBoolean("no-level-conditions.tamed")) {
            return false;
        }

        // Check WorldGuard flag.
        if (ExternalCompatibilityManager.hasWorldGuardInstalled() && !main.worldGuardManager.regionAllowsLevelling(livingEntity))
            return false;

        // Check for overrides
        if (main.settingsCfg.getStringList("overriden-entities").contains(livingEntity.getType().toString()))
            return true;

        //Check allowed entities for normal entity types
        if (!ModalList.isEnabledInList(main.settingsCfg, "allowed-entities-list", livingEntity.getType().toString()))
            return false;

        // Specific allowed entities check for BABIES
        if (Utils.isBabyMob(livingEntity)) {
            if (!ModalList.isEnabledInList(main.settingsCfg, "allowed-entities-list", "BABY_" + livingEntity.getType().toString()))
                return false;
        }

        return isLevellable(livingEntity.getType());
    }

    public int generateDistanceFromSpawnLevel(final LivingEntity livingEntity, final DebugInfo debugInfo, final CreatureSpawnEvent.SpawnReason spawnReason, final int minLevel, final int maxLevel) {
        final boolean isBabyEntity = Utils.isBabyMob(livingEntity);

        if (debugInfo != null) {
            debugInfo.minLevel = minLevel;
            debugInfo.maxLevel = maxLevel;
        }

        final int distanceFromSpawn = (int) livingEntity.getWorld().getSpawnLocation().distance(livingEntity.getLocation());
        final int startDistance = main.settingsCfg.getInt("spawn-distance-levelling.start-distance", 0);
        final int levelDistance = Math.max(distanceFromSpawn - startDistance, 0);
        final int increaseLevelDistance = main.settingsCfg.getInt("spawn-distance-levelling.increase-level-distance", 200);

        int variance = main.settingsCfg.getInt("spawn-distance-levelling.variance", 2);
        if (variance != 0) {
            variance = ThreadLocalRandom.current().nextInt(0, variance + 1);
        }

        //Get the level thats meant to be at a given distance
        return Math.min((levelDistance / increaseLevelDistance) + minLevel + variance, maxLevel);
    }

    // this is now the main entry point that determines the level for all criteria
    public int generateLevel(final LivingEntity livingEntity, final DebugInfo debugInfo, final CreatureSpawnEvent.SpawnReason spawnReason) {

        final boolean isAdultEntity = !Utils.isBabyMob(livingEntity);
        final int[] levels = getMinAndMaxLevels(livingEntity, livingEntity.getType(), isAdultEntity, livingEntity.getWorld().getName(), debugInfo, spawnReason);
        final int minLevel = levels[0];
        final int maxLevel = levels[1];

        // system 2: y distance levelling
        if (main.settingsCfg.getBoolean("y-distance-levelling.active")) {
            return generateYCoordinateLevel(livingEntity.getLocation().getBlockY(), minLevel, maxLevel);
        }

        // system 3: spawn distance levelling
        if (main.settingsCfg.getBoolean("spawn-distance-levelling.active")) {
            return generateDistanceFromSpawnLevel(livingEntity, debugInfo, spawnReason, minLevel, maxLevel);
        }

        // system 1: random levelling
        int biasFactor = main.settingsCfg.getInt("fine-tuning.lower-mob-level-bias-factor", 0);

        if (minLevel == maxLevel) {
            return minLevel;
        } else if (biasFactor > 0) {
            if (biasFactor > 10) {
                biasFactor = 10;
            }

            return generateLevelWithBias(minLevel, maxLevel, biasFactor);
        } else {
            return ThreadLocalRandom.current().nextInt(minLevel, maxLevel + 1);
        }
    }

    public int[] getMinAndMaxLevels(final LivingEntity le, final EntityType entityType, final boolean isAdultEntity, final String worldName,
                                    final DebugInfo debugInfo, final CreatureSpawnEvent.SpawnReason spawnReason) {
        // option 1: global levelling (default)
        int minLevel = Utils.getDefaultIfNull(main.settingsCfg, "fine-tuning.min-level", 1);
        int maxLevel = Utils.getDefaultIfNull(main.settingsCfg, "fine-tuning.max-level", 10);

        if (debugInfo != null) {
            debugInfo.minLevel = minLevel;
            debugInfo.maxLevel = maxLevel;
        }

        // world guard regions take precedence over any other min / max settings
        // le is null if passed from summon mobs command
        if (le != null && ExternalCompatibilityManager.hasWorldGuardInstalled() && main.worldGuardManager.checkRegionFlags(le)) {
            final int[] levels = generateWorldGuardRegionLevel(le, debugInfo, spawnReason);
            if (levels[0] > -1) minLevel = levels[0];
            if (levels[1] > -1) maxLevel = levels[1];
        }

        // config will decide which takes precedence; entity override or world override

        if (main.settingsCfg.getBoolean("world-level-override.enabled")) {
            final int[] levels = getWorldLevelOverride(worldName, debugInfo);
            if (levels[0] > -1) minLevel = levels[0];
            if (levels[1] > -1) maxLevel = levels[1];
        }

        if (main.settingsCfg.getBoolean("entitytype-level-override.enabled")) {
            final int[] levels = getEntityTypeOverride(entityType.toString(), spawnReason, isAdultEntity, debugInfo);
            if (levels[0] > -1) minLevel = levels[0];
            if (levels[1] > -1) maxLevel = levels[1];
        }

        // this will prevent an unhandled exception:
        if (minLevel > maxLevel) minLevel = maxLevel;

        return new int[]{ minLevel, maxLevel };
    }

    private int[] getWorldLevelOverride(final String worldName, final DebugInfo debugInfo) {

        final int[] levels = new int[]{-1, -1};

        if (main.configUtils.worldLevelOverride_Min.containsKey(worldName)) {
            levels[0] = Utils.getDefaultIfNull(main.configUtils.worldLevelOverride_Min, worldName, -1);
        }

        if (main.configUtils.worldLevelOverride_Max.containsKey(worldName)) {
            levels[1] = Utils.getDefaultIfNull(main.configUtils.worldLevelOverride_Max, worldName, -1);
        }

        if (debugInfo != null) {
            if (levels[0] > -1) debugInfo.minLevel = levels[0];
            if (levels[1] > -1) debugInfo.maxLevel = levels[1];
            if (levels[0] > -1 || levels[1] > -1) debugInfo.rule = MobProcessReason.ENTITY;
        }

        return levels;
    }

    private int[] getEntityTypeOverride(final String entityTypeStr, final CreatureSpawnEvent.SpawnReason spawnReason, final boolean isAdult, final DebugInfo debugInfo){
        final String reinforcementsStr = entityTypeStr + "_REINFORCEMENTS";

        final int[] levels = new int[]{ -1, -1};

        if (spawnReason == CreatureSpawnEvent.SpawnReason.REINFORCEMENTS) {
            if (main.configUtils.entityTypesLevelOverride_Min.containsKey(reinforcementsStr))
                levels[0] = Utils.getDefaultIfNull(main.configUtils.entityTypesLevelOverride_Min, reinforcementsStr, levels[0]);
            if (main.configUtils.entityTypesLevelOverride_Max.containsKey(reinforcementsStr))
                levels[1] = Utils.getDefaultIfNull(main.configUtils.entityTypesLevelOverride_Max, reinforcementsStr, levels[1]);
        } else if (isAdult) {
            if (main.configUtils.entityTypesLevelOverride_Min.containsKey(entityTypeStr))
                levels[0] = Utils.getDefaultIfNull(main.configUtils.entityTypesLevelOverride_Min, entityTypeStr, levels[0]);
            if (main.configUtils.entityTypesLevelOverride_Max.containsKey(entityTypeStr))
                levels[1] = Utils.getDefaultIfNull(main.configUtils.entityTypesLevelOverride_Max, entityTypeStr, levels[1]);
        }
        // babies here:
        else if (main.configUtils.entityTypesLevelOverride_Min.containsKey("baby_" + entityTypeStr)) {
            if (main.configUtils.entityTypesLevelOverride_Min.containsKey("baby_" + entityTypeStr))
                levels[0] = Utils.getDefaultIfNull(main.configUtils.entityTypesLevelOverride_Min, "baby_" + entityTypeStr, levels[0]);
            if (main.configUtils.entityTypesLevelOverride_Max.containsKey("baby_" + entityTypeStr))
                levels[1] = Utils.getDefaultIfNull(main.configUtils.entityTypesLevelOverride_Max, "baby_" + entityTypeStr, levels[1]);
        }

        if (debugInfo != null) {
            if (levels[0] > -1) debugInfo.minLevel = levels[0];
            if (levels[1] > -1) debugInfo.maxLevel = levels[1];
            if (levels[0] > -1 || levels[1] > -1) debugInfo.rule = MobProcessReason.ENTITY;
        }

        return levels;
    }

    public int generateYCoordinateLevel(final int mobYLocation, final int minLevel, final int maxLevel) {
        final int yPeriod = main.settingsCfg.getInt("y-distance-levelling.y-period", 0);
        final int variance = main.settingsCfg.getInt("y-distance-levelling.variance", 0);
        int yStart = main.settingsCfg.getInt("y-distance-levelling.starting-y-level", 100);
        int yEnd = main.settingsCfg.getInt("y-distance-levelling.ending-y-level", 20);

        final boolean isAscending = (yEnd > yStart);
        if (!isAscending) {
            yStart = yEnd;
            yEnd = main.settingsCfg.getInt("y-distance-levelling.starting-y-level", 100);
        }

        int useLevel = minLevel;
        boolean skipYPeriod = false;

        if (mobYLocation >= yEnd){
            useLevel = maxLevel;
            skipYPeriod = true;
        } else if (mobYLocation <= yStart){
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

        if (variance > 0) {
            boolean useOnlyNegative = false;

            if (useLevel >= maxLevel) {
                useLevel = maxLevel;
                useOnlyNegative = true;
            } else if (useLevel <= minLevel) {
                useLevel = minLevel;
            }

            final int change = ThreadLocalRandom.current().nextInt(0, variance + 1);

            // Start variation. First check if variation is positive or negative towards the original level amount.
            if (!useOnlyNegative || ThreadLocalRandom.current().nextBoolean()) {
                // Positive. Add the variation to the final level
                useLevel += change;
            } else {
                // Negative. Subtract the variation from the final level
                useLevel -= change;
            }
        }

        if (useLevel < minLevel) {
            useLevel = minLevel;
        } else if (useLevel > maxLevel) {
            useLevel = maxLevel;
        }

        return useLevel;
    }

    private int generateLevelWithBias(final int minLevel, final int maxLevel, final int factor) {

        LevelNumbersWithBias levelNum = new LevelNumbersWithBias(minLevel, maxLevel, factor);

        if (levelNumsListCache.containsKey(levelNum)) {
            levelNum = levelNumsListCache.get(levelNum);
        } else {
            levelNum = new LevelNumbersWithBias(minLevel, maxLevel, factor);
            levelNum.populateData();
            levelNumsListCache.put(levelNum, levelNum);
            levelNumsListCacheOrder.addLast(levelNum);
        }

        if (levelNumsListCache.size() > maxLevelNumsCache) {
            LevelNumbersWithBias oldest = levelNumsListCacheOrder.getFirst();
            levelNumsListCache.remove(oldest);
            levelNumsListCacheOrder.removeFirst();
        }

        return levelNum.getNumberWithinLimits();
    }

    public int[] generateWorldGuardRegionLevel(final LivingEntity livingEntity, final DebugInfo debugInfo, final CreatureSpawnEvent.SpawnReason spawnReason) {
        final boolean isBabyEntity = Utils.isBabyMob(livingEntity);
        final int[] levels = main.worldGuardManager.getRegionLevel(livingEntity);

        if (debugInfo != null) {
            debugInfo.rule = MobProcessReason.WORLD_GUARD;
            debugInfo.minLevel = levels[0];
            debugInfo.maxLevel = levels[1];
        }

        return levels;

        // standard issue, generate random levels based upon max and min flags in worldguard

//        final int biasFactor = instance.settingsCfg.getInt("fine-tuning.lower-mob-level-bias-factor", 0);
//        if (biasFactor > 0)
//            return generateLevelWithBias(levels[0], levels[1], biasFactor);
//        else
//            return levels[0] + Math.round(new Random().nextFloat() * (levels[1] - levels[0]));


        // generate level based on y distance but use min and max values from world guard
        //return instance.levelManager.generateYCoordinateLevel(livingEntity.getLocation().getBlockY(), levels[0], levels[1]);
    }

    public int getEntityOverrideLevel(final LivingEntity livingEntity, final CreatureSpawnEvent.SpawnReason spawnReason, int level,
                                      final MobProcessReason processReason, final boolean override, final boolean isAdult, final DebugInfo debugInfo) {

        if (!main.settingsCfg.getBoolean("entitytype-level-override.enabled")) return -1;

        final String entityTypeStr = livingEntity.getType().toString();
        final String worldName = livingEntity.getWorld().getName();
        int minLevel = Utils.getDefaultIfNull(main.settingsCfg, "fine-tuning.min-level", 1);
        int maxLevel = Utils.getDefaultIfNull(main.settingsCfg, "fine-tuning.max-level", 10);
        boolean foundValue = false;

        // min level
        if (spawnReason == CreatureSpawnEvent.SpawnReason.REINFORCEMENTS && main.configUtils.entityTypesLevelOverride_Min.containsKey(entityTypeStr + "_REINFORCEMENTS")) {
            minLevel = Utils.getDefaultIfNull(main.configUtils.entityTypesLevelOverride_Min, entityTypeStr + "_REINFORCEMENTS", minLevel);
            if (debugInfo != null) {
                debugInfo.rule = MobProcessReason.ENTITY;
                debugInfo.minLevel = minLevel;
            }
            foundValue = true;
        } else if (isAdult && main.configUtils.entityTypesLevelOverride_Max.containsKey(entityTypeStr)) {
            minLevel = Utils.getDefaultIfNull(main.configUtils.entityTypesLevelOverride_Min, entityTypeStr, minLevel);
            if (debugInfo != null) {
                debugInfo.rule = MobProcessReason.ENTITY;
                debugInfo.minLevel = minLevel;
            }
            foundValue = true;
        } else if (!isAdult && main.configUtils.entityTypesLevelOverride_Max.containsKey("baby_" + entityTypeStr)) {
            minLevel = Utils.getDefaultIfNull(main.configUtils.entityTypesLevelOverride_Min, "baby_" + entityTypeStr, minLevel);
            if (debugInfo != null) {
                debugInfo.rule = MobProcessReason.ENTITY;
                debugInfo.minLevel = minLevel;
            }
            foundValue = true;
        }

        // max level
        if (spawnReason == CreatureSpawnEvent.SpawnReason.REINFORCEMENTS && main.configUtils.entityTypesLevelOverride_Min.containsKey(entityTypeStr + "_REINFORCEMENTS")) {
            maxLevel = Utils.getDefaultIfNull(main.configUtils.entityTypesLevelOverride_Max, entityTypeStr + "_REINFORCEMENTS", maxLevel);
            if (debugInfo != null) {
                debugInfo.rule = MobProcessReason.ENTITY;
                debugInfo.maxLevel = maxLevel;
            }
            foundValue = true;
        } else if (isAdult && main.configUtils.entityTypesLevelOverride_Max.containsKey(entityTypeStr)) {
            maxLevel = Utils.getDefaultIfNull(main.configUtils.entityTypesLevelOverride_Max, entityTypeStr, maxLevel);
            if (debugInfo != null) {
                debugInfo.rule = MobProcessReason.ENTITY;
                debugInfo.maxLevel = maxLevel;
            }
            foundValue = true;
        } else if (!isAdult && main.configUtils.entityTypesLevelOverride_Max.containsKey("baby_" + entityTypeStr)) {
            maxLevel = Utils.getDefaultIfNull(main.configUtils.entityTypesLevelOverride_Max, "baby_" + entityTypeStr, maxLevel);
            if (debugInfo != null) {
                debugInfo.rule = MobProcessReason.ENTITY;
                debugInfo.maxLevel = maxLevel;
            }
            foundValue = true;
        }

        if (foundValue)
            return ThreadLocalRandom.current().nextInt(minLevel, maxLevel + 1);
        else
            return -1;
    }

    public void updateNametagWithDelay(final LivingEntity livingEntity, final String nametag, final List<Player> players, final long delay) {
        new BukkitRunnable() {
            public void run() {
                if (livingEntity == null) return; // may have died/removed after the timer.
                updateNametag(livingEntity, nametag, players);
            }
        }.runTaskLater(main, delay);
    }

    public void updateNametagWithDelay(final LivingEntity livingEntity, final List<Player> players, final long delay) {
        new BukkitRunnable() {
            public void run() {
                if (livingEntity == null) return; // may have died/removed after the timer.
                updateNametag(livingEntity, getNametag(livingEntity, false), players);
            }
        }.runTaskLater(main, delay);
    }

    // This sets the levelled currentDrops on a levelled mob that just died.
    public void getLevelledItemDrops(final LivingEntity livingEntity, final List<ItemStack> currentDrops) {

        // this accomodates chested animals, saddles and armor on ridable creatures
        final List<ItemStack> dropsToMultiply = getDropsToMultiply(livingEntity, currentDrops);
        final List<ItemStack> customDrops = new LinkedList<>();
        currentDrops.clear();

        Utils.debugLog(main, "LevelManager#getLevelledItemDrops", "1: Method called. " + dropsToMultiply.size() + " drops will be analysed.");

        //Utils.debugLog(main, "LevelManager#getLevelledItemDrops", "2: LivingEntity is a levelled mob.");

        // Get their level
        final int level = Objects.requireNonNull(livingEntity.getPersistentDataContainer().get(levelKey, PersistentDataType.INTEGER));
        Utils.debugLog(main, "LevelManager#getLevelledItemDrops", "3: Entity level is " + level + ".");

        final boolean doNotMultiplyDrops =
                (Utils.isBabyMob(livingEntity) && main.configUtils.noDropMultiplierEntities.contains("BABY_" + livingEntity.getType().toString())) ||
                        main.configUtils.noDropMultiplierEntities.contains(livingEntity.getType().toString());

        if (main.settingsCfg.getBoolean("use-custom-item-drops-for-mobs")) {
            // custom drops also get multiplied in the custom drops handler
            final CustomDropResult dropResult = main.customDropsHandler.getCustomItemDrops(livingEntity, level, customDrops, true, false);

            if (dropResult == CustomDropResult.HAS_OVERRIDE) {
                Utils.debugLog(main, "LevelManager#getLevelledItemDrops", "4: custom drop has override");
                removeVanillaDrops(livingEntity, dropsToMultiply);
            }
        }

        if (!doNotMultiplyDrops && !dropsToMultiply.isEmpty()) {
            // Get currentDrops added per level value
            final int addition = BigDecimal.valueOf(main.mobDataManager.getAdditionsForLevel(livingEntity, Addition.CUSTOM_ITEM_DROP, level))
                    .setScale(0, RoundingMode.HALF_DOWN).intValueExact(); // truncate double to int
            Utils.debugLog(main, "LevelManager#getLevelledItemDrops", "4: Item drop addition is +" + addition + ".");

            // Modify current drops
            Utils.debugLog(main, "LevelManager#getLevelledItemDrops", "5: Scanning " + dropsToMultiply.size() + " items...");
            for (final ItemStack currentDrop : dropsToMultiply)
                multiplyDrop(livingEntity, currentDrop, addition, false);
        }

        if (!customDrops.isEmpty()) currentDrops.addAll(customDrops);
        if (!dropsToMultiply.isEmpty()) currentDrops.addAll(dropsToMultiply);
    }

    public void multiplyDrop(LivingEntity livingEntity, final ItemStack currentDrop, final int addition, final boolean isCustomDrop){
        Utils.debugLog(main, "LevelManager#getLevelledItemDrops", "6: Scanning drop " + currentDrop.getType().toString() + " with current amount " + currentDrop.getAmount() + "...");

        if (isCustomDrop || main.mobDataManager.isLevelledDropManaged(livingEntity.getType(), currentDrop.getType())) {
            int useAmount = currentDrop.getAmount() + (currentDrop.getAmount() * addition);
            if (useAmount > currentDrop.getMaxStackSize()) useAmount = currentDrop.getMaxStackSize();
            currentDrop.setAmount(useAmount);
            Utils.debugLog(main, "LevelManager#getLevelledItemDrops", "7: Item was managed. New amount: " + currentDrop.getAmount() + ".");
        } else {
            Utils.debugLog(main, "LevelManager#getLevelledItemDrops", "7: Item was unmanaged.");
        }
    }

    @Nonnull
    private List<ItemStack> getDropsToMultiply(final LivingEntity livingEntity, final List<ItemStack> drops){
        final List<ItemStack> results = new ArrayList<>(drops.size());
        results.addAll(drops);

        // we only need to check for chested animals and 'vehicles' since they can have saddles and armor
        // those items shouldn't get multiplied

        if (livingEntity instanceof ChestedHorse && ((ChestedHorse)livingEntity).isCarryingChest()){
            final AbstractHorseInventory inv = ((ChestedHorse) livingEntity).getInventory();
            final ItemStack[] chestItems = inv.getContents();
            // look thru the animal's inventory for leather. That is the only item that will get duplicated
            for (final ItemStack item : chestItems){
                if (item.getType().equals(Material.LEATHER))
                    return Collections.singletonList(item);
            }

            // if we made it here it didn't drop leather so don't return anything
            results.clear();
            return results;
        }

        if (!(livingEntity instanceof Vehicle)) return results;

        for (int i = results.size() - 1; i >= 0; i--){
            // remove horse armor or saddles
            final ItemStack item = results.get(i);
            if (this.vehicleNoMultiplierItems.contains(item.getType())) // saddle or horse armor
                results.remove(i);
        }

        return results;
    }

    public void removeVanillaDrops(final LivingEntity livingEntity, final List<ItemStack> drops){
        boolean hadSaddle = false;
        List<ItemStack> chestItems = null;

        if (livingEntity instanceof ChestedHorse && ((ChestedHorse)livingEntity).isCarryingChest()){
            final AbstractHorseInventory inv = ((ChestedHorse) livingEntity).getInventory();
            chestItems = Arrays.asList(inv.getContents());
        }
        else if (livingEntity instanceof Vehicle){
            for (final ItemStack itemStack : drops){
                if (itemStack.getType().equals(Material.SADDLE)){
                    hadSaddle = true;
                    break;
                }
            }
        }

        drops.clear();
        if (chestItems != null) drops.addAll(chestItems);
        if (hadSaddle) drops.add(new ItemStack(Material.SADDLE));
    }

    //Calculates the XP dropped when a levellable creature dies.
    public int getLevelledExpDrops(final LivingEntity livingEntity, final int xp) {
        if (main.levelInterface.isLevelled(livingEntity)) {
            final int level = Objects.requireNonNull(livingEntity.getPersistentDataContainer().get(main.levelManager.levelKey, PersistentDataType.INTEGER));
            return (int) Math.round(xp + (xp * main.mobDataManager.getAdditionsForLevel(livingEntity, Addition.CUSTOM_XP_DROP, level)));
        } else {
            return xp;
        }
    }

    // When the persistent data container levelled key has been set on the entity already (i.e. when they are damaged)
    public String getNametag(final LivingEntity livingEntity, final boolean isDeathNametag) {
        return getNametag(livingEntity, Objects.requireNonNull(
                livingEntity.getPersistentDataContainer().get(levelKey, PersistentDataType.INTEGER)), isDeathNametag);
    }

    // When the persistent data container levelled key has not been set on the entity yet (i.e. for use in CreatureSpawnListener)
    public String getNametag(final LivingEntity livingEntity, final int level, final boolean isDeathNametag) {
        // If show label for default levelled mobs is disabled and the mob is the min level, then don't modify their tag.
        if (!main.settingsCfg.getBoolean("show-label-for-default-levelled-mobs") && level == main.settingsCfg.getInt("fine-tuning.min-level")) {
            return livingEntity.getCustomName(); // CustomName can be null, that is meant to be the case.
        }

        final AttributeInstance maxHealth = livingEntity.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        final String roundedMaxHealth = maxHealth == null ? "?" : Utils.round(maxHealth.getBaseValue()) + "";
        final String roundedMaxHealthInt = maxHealth == null ? "?" : (int) Utils.round(maxHealth.getBaseValue()) + "";

        String nametag = isDeathNametag ? main.settingsCfg.getString("creature-death-nametag") : main.settingsCfg.getString("creature-nametag");
        String entityName = WordUtils.capitalizeFully(livingEntity.getType().toString().toLowerCase().replaceAll("_", " "));

        // Baby zombies can have specific nametags in entity-name-override
        if (Utils.isBabyMob(livingEntity) && main.settingsCfg.contains("entity-name-override.BABY_" + livingEntity.getType().toString())) {
            entityName = main.settingsCfg.getString("entity-name-override.BABY_" + livingEntity.getType().toString());
        } else if (main.settingsCfg.contains("entity-name-override." + livingEntity.getType().toString())) {
            entityName = main.settingsCfg.getString("entity-name-override." + livingEntity.getType().toString());
        }
        if (entityName == null || entityName.isEmpty() || entityName.equalsIgnoreCase("disabled")) return null;

        final String displayName = livingEntity.getCustomName() == null ? MessageUtils.colorizeAll(entityName) : livingEntity.getCustomName();

        // ignore if 'disabled'
        if (nametag == null || nametag.isEmpty() || nametag.equalsIgnoreCase("disabled"))
            return livingEntity.getCustomName(); // CustomName can be null, that is meant to be the case.

        // %tiered% placeholder
        int minLevel = main.settingsCfg.getInt("fine-tuning.min-level", 1);
        int maxLevel = main.settingsCfg.getInt("fine-tuning.max-level", 10);
        double levelPercent = (double) level / (double) (maxLevel - minLevel);
        ChatColor tier = ChatColor.GREEN;
        if (levelPercent >= 0.66666666) tier = ChatColor.RED;
        else if (levelPercent >= 0.33333333) tier = ChatColor.GOLD;

        // replace them placeholders ;)
        nametag = nametag.replace("%level%", level + "");
        nametag = nametag.replace("%typename%", entityName);
        nametag = nametag.replace("%health%", Utils.round(livingEntity.getHealth()) + "");
        nametag = nametag.replace("%health_rounded%", (int) Utils.round(livingEntity.getHealth()) + "");
        nametag = nametag.replace("%max_health%", roundedMaxHealth);
        nametag = nametag.replace("%max_health_rounded%", roundedMaxHealthInt);
        nametag = nametag.replace("%heart_symbol%", "❤");
        nametag = nametag.replace("%tiered%", tier.toString());
        nametag = MessageUtils.colorizeAll(nametag);

        // This is after colorize so that color codes in nametags dont get translated
        nametag = nametag.replace("%displayname%", displayName);

        return nametag;
    }

    /*
     * Credit
     * - Thread: https://www.spigotmc.org/threads/changing-an-entitys-nametag-with-packets.482855/
     *
     * - Users:
     *   - @CoolBoy (https://www.spigotmc.org/members/CoolBoy.102500/)
     *   - @Esophose (https://www.spigotmc.org/members/esophose.34168/)
     *   - @7smile7 (https://www.spigotmc.org/members/7smile7.43809/)
     */
    public void updateNametag(final LivingEntity entity, final String nametag, final List<Player> players) {
        if (!ExternalCompatibilityManager.hasProtocolLibInstalled()) return;

        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {

                for (Player player : players) {

                    // async task, so make sure the player & entity are valid
                    if (!player.isOnline()) continue;
                    if (entity == null) return;

                    if (main.settingsCfg.getBoolean("assert-entity-validity-with-nametag-packets") && !entity.isValid())
                        return;

                    final WrappedDataWatcher dataWatcher;

                    try {
                        dataWatcher = WrappedDataWatcher.getEntityWatcher(entity).deepClone();
                    } catch (ConcurrentModificationException ex) {
                        Utils.debugLog(main, "LevelManagerUpdateNametag", "Concurrent modification occured, skipping nametag update of " + entity.getName() + ".");
                        return;
                    }

                    final WrappedDataWatcher.Serializer chatSerializer;

                    try {
                        chatSerializer = WrappedDataWatcher.Registry.getChatComponentSerializer(true);
                    } catch (IllegalArgumentException ex) {
                        Utils.debugLog(main, "LevelManagerUpdateNametag", "Registry is empty, skipping nametag update of " + entity.getName() + ".");
                        return;
                    }

                    final WrappedDataWatcher.WrappedDataWatcherObject watcherObject = new WrappedDataWatcher.WrappedDataWatcherObject(2, chatSerializer);

                    Optional<Object> optional;
                    if (Utils.isNullOrEmpty(nametag)) {
                        optional = Optional.empty();
                    } else {
                        optional = Optional.of(WrappedChatComponent.fromChatMessage(nametag)[0].getHandle());
                    }

                    dataWatcher.setObject(watcherObject, optional);

                    dataWatcher.setObject(3, !Utils.isNullOrEmpty(nametag) && entity.isCustomNameVisible() || main.settingsCfg.getBoolean("creature-nametag-always-visible"));

                    final PacketContainer packet = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.ENTITY_METADATA);
                    packet.getWatchableCollectionModifier().write(0, dataWatcher.getWatchableObjects());
                    packet.getIntegers().write(0, entity.getEntityId());

                    try {
                        ProtocolLibrary.getProtocolManager().sendServerPacket(player, packet);
                    } catch (IllegalArgumentException ex) {
                        Utils.debugLog(main, "Nametags", "IllegalArgumentException caught whilst trying to sendServerPacket");
                    } catch (InvocationTargetException ex) {
                        Utils.logger.error("Unable to update nametag packet for player &b" + player.getName() + "&7! Stack trace:");
                        ex.printStackTrace();
                    }
                }
            }
        }.runTaskAsynchronously(main);
    }

    public BukkitTask nametagAutoUpdateTask;

    public void startNametagAutoUpdateTask() {
        Utils.logger.info("&fTasks: &7Starting async nametag auto update task...");

        final double maxDistance = Math.pow(128, 2); // square the distance we are using Location#distanceSquared. This is because it is faster than Location#distance since it does not need to sqrt which is taxing on the CPU.
        final long period = main.settingsCfg.getInt("nametag-auto-update-task-period"); // run every ? seconds.

        nametagAutoUpdateTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (final Player player : Bukkit.getOnlinePlayers()) {
                    final Location location = player.getLocation();

                    for (final Entity entity : player.getWorld().getEntities()) {

                        if (entity == null) continue; // async task, entity can despawn whilst it is running

                        // Mob must be a livingentity that is ...living.
                        if (!(entity instanceof LivingEntity)) continue;
                        final LivingEntity livingEntity = (LivingEntity) entity;

                        // Mob must be levelled
                        if (!main.levelInterface.isLevelled(livingEntity)) continue;

                        //if within distance, update nametag.
                        if (livingEntity.getLocation().distanceSquared(location) <= maxDistance) {
                            main.levelManager.updateNametag(livingEntity, main.levelManager.getNametag(livingEntity, false), Collections.singletonList(player));
                        }
                    }
                }
            }
        }.runTaskTimerAsynchronously(main, 0, 20 * period);
    }

    public void stopNametagAutoUpdateTask() {
        if (!ExternalCompatibilityManager.hasProtocolLibInstalled()) return;

        if (nametagAutoUpdateTask != null && !nametagAutoUpdateTask.isCancelled()) {
            Utils.logger.info("&fTasks: &7Stopping async nametag auto update task...");
            nametagAutoUpdateTask.cancel();
        }
    }
}
