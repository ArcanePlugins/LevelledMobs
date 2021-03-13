package io.github.lokka30.levelledmobs.managers;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import io.github.lokka30.levelledmobs.LevelledMobs;
import io.github.lokka30.levelledmobs.customdrops.CustomDropResult;
import io.github.lokka30.levelledmobs.listeners.CreatureSpawnListener;
import io.github.lokka30.levelledmobs.misc.*;
import me.lokka30.microlib.MessageUtils;
import org.apache.commons.lang.WordUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.*;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

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

    private final LevelledMobs instance;
    final public HashMap<LevelNumbersWithBias, LevelNumbersWithBias> levelNumsListCache;
    final public LinkedList<LevelNumbersWithBias> levelNumsListCacheOrder;
    private final static int maxLevelNumsCache = 10;

    public LevelManager(final LevelledMobs instance) {
        this.instance = instance;

        this.levelNumsListCache = new HashMap<>();
        this.levelNumsListCacheOrder = new LinkedList<>();

        levelKey = new NamespacedKey(instance, "level");
        isLevelledKey = new NamespacedKey(instance, "isLevelled");
        isSpawnerKey = new NamespacedKey(instance, "isSpawner");

        final int factor = instance.settingsCfg.getInt("fine-tuning.lower-mob-level-bias-factor", 0);
        if (factor > 0) {
            LevelNumbersWithBias lnwb = new LevelNumbersWithBias(
                    instance.settingsCfg.getInt("fine-tuning.min-level", 1),
                    instance.settingsCfg.getInt("fine-tuning.max-level", 10),
                    factor
            );
            lnwb.populateData();
            this.levelNumsListCache.put(lnwb, lnwb);
            this.levelNumsListCacheOrder.addFirst(lnwb);
        }
    }

    public final NamespacedKey levelKey; // This stores the mob's level.
    public final NamespacedKey isLevelledKey; //This is stored on levelled mobs to tell plugins that it is a levelled mob.
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
        if (!ModalList.isEnabledInList(instance.settingsCfg, "allowed-entities-list", entityType.toString()))
            return false;

        // Check if the entity is overriden. If so, force it to be levelled.
        if (instance.settingsCfg.getStringList("overriden-entities").contains(entityType.toString())) return true;

        // These entities don't implement Monster or Boss and thus must be forced to return true
        if (forcedTypes.contains(entityType.toString())) {
            return true;
        }

        // Grab the Entity class, which is used to check for certain assignments
        Class<? extends Entity> entityClass = entityType.getEntityClass();
        if (entityClass == null) return false;

        return Monster.class.isAssignableFrom(entityClass)
                || Boss.class.isAssignableFrom(entityClass)
                || instance.settingsCfg.getBoolean("level-passive");
    }

    //Checks if an entity can be levelled.
    public boolean isLevellable(final LivingEntity livingEntity) {

        // Ignore these entity types and metadatas
        if (
            // Entity types to ignore
                livingEntity.getType() == EntityType.PLAYER
                        || livingEntity.getType() == EntityType.UNKNOWN
                        || livingEntity.getType() == EntityType.ARMOR_STAND
                        || livingEntity.getType() == EntityType.ITEM_FRAME
                        || livingEntity.getType() == EntityType.DROPPED_ITEM
                        || livingEntity.getType() == EntityType.PAINTING

                        // DangerousCaves plugin compatibility
                        || (livingEntity.hasMetadata("DangerousCaves") && instance.externalCompatibilityManager.isExternalCompatibilityEnabled(ExternalCompatibilityManager.ExternalCompatibility.DANGEROUS_CAVES))

                        // EliteMobs plugin compatibility
                        || (livingEntity.hasMetadata("Elitemob") && instance.externalCompatibilityManager.isExternalCompatibilityEnabled(ExternalCompatibilityManager.ExternalCompatibility.ELITE_MOBS))
                        || (livingEntity.hasMetadata("Elitemobs_NPC") && instance.externalCompatibilityManager.isExternalCompatibilityEnabled(ExternalCompatibilityManager.ExternalCompatibility.ELITE_MOBS_NPCS))
                        || (livingEntity.hasMetadata("Supermob") && instance.externalCompatibilityManager.isExternalCompatibilityEnabled(ExternalCompatibilityManager.ExternalCompatibility.ELITE_MOBS_SUPER_MOBS))

                        //InfernalMobs plugin compatibility)
                        || (livingEntity.hasMetadata("infernalMetadata") && instance.externalCompatibilityManager.isExternalCompatibilityEnabled(ExternalCompatibilityManager.ExternalCompatibility.INFERNAL_MOBS))

                        // Citizens plugin compatibility
                        || (livingEntity.hasMetadata("NPC") && instance.externalCompatibilityManager.isExternalCompatibilityEnabled(ExternalCompatibilityManager.ExternalCompatibility.CITIZENS))

                        // Shopkeepers plugin compatibility
                        || (livingEntity.hasMetadata("shopkeeper") && instance.externalCompatibilityManager.isExternalCompatibilityEnabled(ExternalCompatibilityManager.ExternalCompatibility.SHOPKEEPERS))
        ) {
            return false;
        }

        // Check 'no level conditions'
        if (livingEntity.getCustomName() != null && instance.settingsCfg.getBoolean("no-level-conditions.nametagged")) {
            return false;
        }
        if (livingEntity instanceof Tameable && ((Tameable) livingEntity).isTamed() && instance.settingsCfg.getBoolean("no-level-conditions.tamed")) {
            return false;
        }

        // Check WorldGuard flag.
        if (ExternalCompatibilityManager.hasWorldGuardInstalled() && !instance.worldGuardManager.regionAllowsLevelling(livingEntity))
            return false;

        // Check for overrides
        if (instance.settingsCfg.getStringList("overriden-entities").contains(livingEntity.getType().toString()))
            return true;

        //Check allowed entities for normal entity types
        if (!ModalList.isEnabledInList(instance.settingsCfg, "allowed-entities-list", livingEntity.getType().toString()))
            return false;

        // Specific allowed entities check for BABIES
        if (Utils.isBabyZombie(livingEntity)) {
            if (!ModalList.isEnabledInList(instance.settingsCfg, "allowed-entities-list", "BABY_" + livingEntity.getName().toUpperCase()))
                return false;
        }

        return isLevellable(livingEntity.getType());
    }

    public int generateDistanceFromSpawnLevel(final LivingEntity livingEntity, final DebugInfo debugInfo, final CreatureSpawnEvent.SpawnReason spawnReason, final int minLevel, final int maxLevel) {
        final boolean isBabyEntity = Utils.isBabyZombie(livingEntity);

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
            //The maximum amount of variation.
            final int maxVariation = instance.settingsCfg.getInt("spawn-distance-levelling.variance.max");

            //A random number between min and max which determines the amount of variation that will take place
            final int change = ThreadLocalRandom.current().nextInt(0, maxVariation + 1);

            boolean useOnlyNegative = false;

            if (finalLevel >= maxLevel) {
                finalLevel = maxLevel;
                useOnlyNegative = true;
            } else if (finalLevel <= minLevel) {
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
        } else if (finalLevel < minLevel) {
            finalLevel = minLevel;
        }

        return finalLevel;
    }

    // this is now the main entry point that determines the level for all criteria
    public int generateLevel(final LivingEntity livingEntity, final DebugInfo debugInfo, final CreatureSpawnEvent.SpawnReason spawnReason) {

        final boolean isAdultEntity = !Utils.isBabyZombie(livingEntity);
        final int[] levels = getMinAndMaxLevels(livingEntity, livingEntity.getType(), isAdultEntity, livingEntity.getWorld().getName(), debugInfo, spawnReason);
        final int minLevel = levels[0];
        final int maxLevel = levels[1];

        // option 2: spawn distance levelling
        if (instance.settingsCfg.getBoolean("y-distance-levelling.active")){
            return generateDistanceFromSpawnLevel(livingEntity, debugInfo, spawnReason, minLevel, maxLevel);
        }

        // option 3: y distance levelling
        if (instance.settingsCfg.getBoolean("spawn-distance-levelling.active")) {
            return generateYCoordinateLevel(livingEntity.getLocation().getBlockY(), minLevel, maxLevel);
        }

        int biasFactor = instance.settingsCfg.getInt("fine-tuning.lower-mob-level-bias-factor", 0);

        if (minLevel == maxLevel)
            return minLevel;
        else if (biasFactor > 0){
            if (biasFactor > 10) biasFactor = 10;
            return generateLevelWithBias(minLevel, maxLevel, biasFactor);
        } else
            return ThreadLocalRandom.current().nextInt(minLevel, maxLevel + 1);
    }

    public int[] getMinAndMaxLevels(final LivingEntity le, final EntityType entityType, final boolean isAdultEntity, final String worldName,
                                    final DebugInfo debugInfo, final CreatureSpawnEvent.SpawnReason spawnReason){
        // option 1: global levelling (default)
        int minLevel = Utils.getDefaultIfNull(instance.settingsCfg, "fine-tuning.min-level", 1);
        int maxLevel = Utils.getDefaultIfNull(instance.settingsCfg, "fine-tuning.max-level", 10);

        if (debugInfo != null){
            debugInfo.minLevel = minLevel;
            debugInfo.maxLevel = maxLevel;
        }

        // world guard regions take precedence over any other min / max settings
        // le is null if passed from summon mobs command
        if (le != null && ExternalCompatibilityManager.hasWorldGuardInstalled() && instance.worldGuardManager.checkRegionFlags(le)){
            final int[] levels = generateWorldGuardRegionLevel(le, debugInfo, spawnReason);
            if (levels[0] > -1) minLevel = levels[0];
            if (levels[1] > -1) maxLevel = levels[1];
        }

        // config will decide which takes precedence; entity override or world override

        if (instance.settingsCfg.getBoolean("world-level-override.enabled")) {
            final int[] levels = getWorldLevelOverride(worldName, debugInfo);
            if (levels[0] > -1) minLevel = levels[0];
            if (levels[1] > -1) maxLevel = levels[1];
        }

        if (instance.settingsCfg.getBoolean("entitytype-level-override.enabled")) {
            final int[] levels = getEntityTypeOverride(entityType.toString(), spawnReason, isAdultEntity, debugInfo);
            if (levels[0] > -1) minLevel = levels[0];
            if (levels[1] > -1) maxLevel = levels[1];
        }

        // this will prevent an unhandled exception:
        if (minLevel > maxLevel) minLevel = maxLevel;

        return new int[]{ minLevel, maxLevel };
    }

    private int[] getWorldLevelOverride(final String worldName, final DebugInfo debugInfo){

        final int[] levels = new int[]{ -1, -1};

        if (instance.worldLevelOverride_Min.containsKey(worldName)) {
            levels[0] = Utils.getDefaultIfNull(instance.worldLevelOverride_Min, worldName, -1);
        }

        if (instance.worldLevelOverride_Max.containsKey(worldName)) {
            levels[1] = Utils.getDefaultIfNull(instance.worldLevelOverride_Max, worldName, -1);
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

        if (spawnReason == CreatureSpawnEvent.SpawnReason.REINFORCEMENTS){
            if (instance.entityTypesLevelOverride_Min.containsKey(reinforcementsStr))
                levels[0] = Utils.getDefaultIfNull(instance.entityTypesLevelOverride_Min, reinforcementsStr, levels[0]);
            if (instance.entityTypesLevelOverride_Max.containsKey(reinforcementsStr))
                levels[1] = Utils.getDefaultIfNull(instance.entityTypesLevelOverride_Max, reinforcementsStr, levels[1]);
        } else if (isAdult) {
            if (instance.entityTypesLevelOverride_Min.containsKey(entityTypeStr))
                levels[0] = Utils.getDefaultIfNull(instance.entityTypesLevelOverride_Min, entityTypeStr, levels[0]);
            if (instance.entityTypesLevelOverride_Max.containsKey(entityTypeStr))
                levels[1] = Utils.getDefaultIfNull(instance.entityTypesLevelOverride_Max, entityTypeStr, levels[1]);
        }
        // babies here:
        else if (instance.entityTypesLevelOverride_Min.containsKey("baby_" + entityTypeStr)) {
            if (instance.entityTypesLevelOverride_Min.containsKey("baby_" + entityTypeStr))
                levels[0] = Utils.getDefaultIfNull(instance.entityTypesLevelOverride_Min, "baby_" + entityTypeStr, levels[0]);
            if (instance.entityTypesLevelOverride_Max.containsKey("baby_" + entityTypeStr))
                levels[1] = Utils.getDefaultIfNull(instance.entityTypesLevelOverride_Max, "baby_" + entityTypeStr, levels[1]);
        }

        if (debugInfo != null) {
            if (levels[0] > -1) debugInfo.minLevel = levels[0];
            if (levels[1] > -1) debugInfo.maxLevel = levels[1];
            if (levels[0] > -1 || levels[1] > -1) debugInfo.rule = MobProcessReason.ENTITY;
        }

        return levels;
    }

    public int generateYCoordinateLevel(final int mobYLocation, final int minLevel, final int maxLevel) {
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

        if (variance > 0){
            boolean useOnlyNegative = false;

            if (useLevel >= maxLevel) {
                useLevel = maxLevel;
                useOnlyNegative = true;
            } else if (useLevel <= minLevel) {
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

    private int generateLevelWithBias(final int minLevel, final int maxLevel, final int factor){

        LevelNumbersWithBias levelNum = new LevelNumbersWithBias(minLevel, maxLevel, factor);

        if (this.levelNumsListCache.containsKey(levelNum)) {
            levelNum = this.levelNumsListCache.get(levelNum);
        } else {
            levelNum = new LevelNumbersWithBias(minLevel, maxLevel, factor);
            levelNum.populateData();
            this.levelNumsListCache.put(levelNum, levelNum);
            this.levelNumsListCacheOrder.addLast(levelNum);
        }

        if (this.levelNumsListCache.size() > maxLevelNumsCache) {
            LevelNumbersWithBias oldest = this.levelNumsListCacheOrder.getFirst();
            this.levelNumsListCache.remove(oldest);
            this.levelNumsListCacheOrder.removeFirst();
        }

        return levelNum.getNumberWithinLimits();
    }

    public int[] generateWorldGuardRegionLevel(final LivingEntity livingEntity, final DebugInfo debugInfo, final CreatureSpawnEvent.SpawnReason spawnReason) {
        final boolean isBabyEntity = Utils.isBabyZombie(livingEntity);
        final int[] levels = instance.worldGuardManager.getRegionLevel(livingEntity);

        if (debugInfo != null){
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
                                      final MobProcessReason processReason, final boolean override, final boolean isAdult, final DebugInfo debugInfo){

        if (!instance.settingsCfg.getBoolean("entitytype-level-override.enabled")) return -1;

        final String entityTypeStr = livingEntity.getType().toString();
        final String worldName = livingEntity.getWorld().getName();
        int minLevel = Utils.getDefaultIfNull(instance.settingsCfg, "fine-tuning.min-level", 1);
        int maxLevel = Utils.getDefaultIfNull(instance.settingsCfg, "fine-tuning.max-level", 10);
        boolean foundValue = false;

        // min level
        if (spawnReason == CreatureSpawnEvent.SpawnReason.REINFORCEMENTS && instance.entityTypesLevelOverride_Min.containsKey(entityTypeStr + "_REINFORCEMENTS")) {
            minLevel = Utils.getDefaultIfNull(instance.entityTypesLevelOverride_Min, entityTypeStr + "_REINFORCEMENTS", minLevel);
            if (debugInfo != null) {
                debugInfo.rule = MobProcessReason.ENTITY;
                debugInfo.minLevel = minLevel;
            }
            foundValue = true;
        } else if (isAdult && instance.entityTypesLevelOverride_Max.containsKey(entityTypeStr)) {
            minLevel = Utils.getDefaultIfNull(instance.entityTypesLevelOverride_Min, entityTypeStr, minLevel);
            if (debugInfo != null) {
                debugInfo.rule = MobProcessReason.ENTITY;
                debugInfo.minLevel = minLevel;
            }
            foundValue = true;
        } else if (!isAdult && instance.entityTypesLevelOverride_Max.containsKey("baby_" + entityTypeStr)) {
            minLevel = Utils.getDefaultIfNull(instance.entityTypesLevelOverride_Min, "baby_" + entityTypeStr, minLevel);
            if (debugInfo != null) {
                debugInfo.rule = MobProcessReason.ENTITY;
                debugInfo.minLevel = minLevel;
            }
            foundValue = true;
        }

        // max level
        if (spawnReason == CreatureSpawnEvent.SpawnReason.REINFORCEMENTS && instance.entityTypesLevelOverride_Min.containsKey(entityTypeStr + "_REINFORCEMENTS")) {
            maxLevel = Utils.getDefaultIfNull(instance.entityTypesLevelOverride_Max, entityTypeStr + "_REINFORCEMENTS", maxLevel);
            if (debugInfo != null){
                debugInfo.rule = MobProcessReason.ENTITY;
                debugInfo.maxLevel = maxLevel;
            }
            foundValue = true;
        } else if (isAdult && instance.entityTypesLevelOverride_Max.containsKey(entityTypeStr)) {
            maxLevel = Utils.getDefaultIfNull(instance.entityTypesLevelOverride_Max, entityTypeStr, maxLevel);
            if (debugInfo != null){
                debugInfo.rule = MobProcessReason.ENTITY;
                debugInfo.maxLevel = maxLevel;
            }
            foundValue = true;
        } else if (!isAdult && instance.entityTypesLevelOverride_Max.containsKey("baby_" + entityTypeStr)) {
            maxLevel = Utils.getDefaultIfNull(instance.entityTypesLevelOverride_Max, "baby_" + entityTypeStr, maxLevel);
            if (debugInfo != null){
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
        }.runTaskLater(instance, delay);
    }

    public void updateNametagWithDelay(final LivingEntity livingEntity, final List<Player> players, final long delay) {
        new BukkitRunnable() {
            public void run() {
                if (livingEntity == null) return; // may have died/removed after the timer.
                updateNametag(livingEntity, getNametag(livingEntity, false), players);
            }
        }.runTaskLater(instance, delay);
    }

    // This sets the levelled currentDrops on a levelled mob that just died.
    public void getLevelledItemDrops(final LivingEntity livingEntity, final List<ItemStack> currentDrops) {

        Utils.debugLog(instance, "LevelManager#getLevelledItemDrops", "1: Method called. " + currentDrops.size() + " drops will be analysed.");

        // Must be a levelled mob
        if (!livingEntity.getPersistentDataContainer().has(isLevelledKey, PersistentDataType.STRING))
            return;

        Utils.debugLog(instance, "LevelManager#getLevelledItemDrops", "2: LivingEntity is a levelled mob.");

        // Get their level
        final int level = Objects.requireNonNull(livingEntity.getPersistentDataContainer().get(levelKey, PersistentDataType.INTEGER));
        Utils.debugLog(instance, "LevelManager#getLevelledItemDrops", "3: Entity level is " + level + ".");

        final boolean doNotMultiplyDrops = instance.noDropMultiplierEntities.contains(livingEntity.getName());
        final List<ItemStack> customDrops = new ArrayList<>();

        if (instance.settingsCfg.getBoolean("use-custom-item-drops-for-mobs") &&
                instance.customDropsHandler.getCustomItemDrops(livingEntity, level, customDrops, true, false) == CustomDropResult.HAS_OVERRIDE){
            Utils.debugLog(instance, "LevelManager#getLevelledItemDrops", "4: custom drop has override");
            currentDrops.clear();
            currentDrops.addAll(customDrops);
            return;
        }

        if (!doNotMultiplyDrops) {
            // Get currentDrops added per level value
            final int addition = BigDecimal.valueOf(instance.mobDataManager.getAdditionsForLevel(livingEntity, Addition.CUSTOM_ITEM_DROP, level))
                    .setScale(0, RoundingMode.HALF_DOWN).intValueExact(); // truncate double to int
            Utils.debugLog(instance, "LevelManager#getLevelledItemDrops", "4: Item drop addition is +" + addition + ".");

            // Modify current drops
            Utils.debugLog(instance, "LevelManager#getLevelledItemDrops", "5: Scanning " + currentDrops.size() + " items...");
            for (ItemStack currentDrop : currentDrops) {
                Utils.debugLog(instance, "LevelManager#getLevelledItemDrops", "6: Scanning drop " + currentDrop.getType().toString() + " with current amount " + currentDrop.getAmount() + "...");

                if (instance.mobDataManager.isLevelledDropManaged(livingEntity.getType(), currentDrop.getType())) {
                    int useAmount = currentDrop.getAmount() + (currentDrop.getAmount() * addition);
                    if (useAmount > currentDrop.getMaxStackSize()) useAmount = currentDrop.getMaxStackSize();
                    currentDrop.setAmount(useAmount);
                    Utils.debugLog(instance, "LevelManager#getLevelledItemDrops", "7: Item was managed. New amount: " + currentDrop.getAmount() + ".");
                } else {
                    Utils.debugLog(instance, "LevelManager#getLevelledItemDrops", "7: Item was unmanaged.");
                }
            }
        }

        if (!customDrops.isEmpty()) currentDrops.addAll(customDrops);
    }

    //Calculates the XP dropped when a levellable creature dies.
    public int getLevelledExpDrops(final LivingEntity ent, final int xp) {
        if (ent.getPersistentDataContainer().has(isLevelledKey, PersistentDataType.STRING)) {
            final int level = Objects.requireNonNull(ent.getPersistentDataContainer().get(instance.levelManager.levelKey, PersistentDataType.INTEGER));

            return (int) Math.round(xp + (xp * instance.mobDataManager.getAdditionsForLevel(ent, Addition.CUSTOM_XP_DROP, level)));
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
        if (!instance.settingsCfg.getBoolean("show-label-for-default-levelled-mobs") && level == instance.settingsCfg.getInt("fine-tuning.min-level")) {
            return livingEntity.getCustomName(); // CustomName can be null, that is meant to be the case.
        }

        final AttributeInstance maxHealth = livingEntity.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        final String roundedMaxHealth = maxHealth == null ? "?" : Utils.round(maxHealth.getBaseValue()) + "";
        final String roundedMaxHealthInt = maxHealth == null ? "?" : (int) Utils.round(maxHealth.getBaseValue()) + "";

        String nametag = isDeathNametag ? instance.settingsCfg.getString("creature-death-nametag") : instance.settingsCfg.getString("creature-nametag");
        String entityName = WordUtils.capitalizeFully(livingEntity.getType().toString().toLowerCase().replaceAll("_", " "));

        // Baby zombies can have specific nametags in entity-name-override
        boolean isBabyEntity = Utils.isBabyZombie(livingEntity);
        if (isBabyEntity && livingEntity instanceof Zombie && instance.settingsCfg.contains("entity-name-override.BABY_ZOMBIE")) {
            entityName = instance.settingsCfg.getString("entity-name-override.BABY_ZOMBIE");
        } else if (instance.settingsCfg.contains("entity-name-override." + livingEntity.getType())) {
            entityName = instance.settingsCfg.getString("entity-name-override." + livingEntity.getType());
        }
        if (entityName == null || entityName.isEmpty() || entityName.equalsIgnoreCase("disabled")) return null;

        final String displayName = livingEntity.getCustomName() == null ? MessageUtils.colorizeAll(entityName) : livingEntity.getCustomName();

        // ignore if 'disabled'
        if (nametag == null || nametag.isEmpty() || nametag.equalsIgnoreCase("disabled"))
            return livingEntity.getCustomName(); // CustomName can be null, that is meant to be the case.

        // %tiered% placeholder
        int minLevel = instance.settingsCfg.getInt("fine-tuning.min-level", 1);
        int maxLevel = instance.settingsCfg.getInt("fine-tuning.max-level", 10);
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

                    if (instance.settingsCfg.getBoolean("assert-entity-validity-with-nametag-packets") && !entity.isValid())
                        return;

                    final WrappedDataWatcher dataWatcher;

                    try {
                        dataWatcher = WrappedDataWatcher.getEntityWatcher(entity).deepClone();
                    } catch (ConcurrentModificationException ex) {
                        Utils.debugLog(instance, "LevelManagerUpdateNametag", "Concurrent modification occured, skipping nametag update of " + entity.getName() + ".");
                        return;
                    }

                    final WrappedDataWatcher.Serializer chatSerializer;

                    try {
                        chatSerializer = WrappedDataWatcher.Registry.getChatComponentSerializer(true);
                    } catch (IllegalArgumentException ex) {
                        Utils.debugLog(instance, "LevelManagerUpdateNametag", "Registry is empty, skipping nametag update of " + entity.getName() + ".");
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

                    dataWatcher.setObject(3, !Utils.isNullOrEmpty(nametag) && entity.isCustomNameVisible() || instance.settingsCfg.getBoolean("creature-nametag-always-visible"));

                    final PacketContainer packet = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.ENTITY_METADATA);
                    packet.getWatchableCollectionModifier().write(0, dataWatcher.getWatchableObjects());
                    packet.getIntegers().write(0, entity.getEntityId());

                    try {
                        ProtocolLibrary.getProtocolManager().sendServerPacket(player, packet);
                    } catch (IllegalArgumentException ex) {
                        Utils.debugLog(instance, "Nametags", "IllegalArgumentException caught whilst trying to sendServerPacket");
                    } catch (InvocationTargetException ex) {
                        Utils.logger.error("Unable to update nametag packet for player &b" + player.getName() + "&7! Stack trace:");
                        ex.printStackTrace();
                    }
                }
            }
        }.runTaskAsynchronously(instance);
    }

    public BukkitTask nametagAutoUpdateTask;

    public void startNametagAutoUpdateTask() {
        Utils.logger.info("&fTasks: &7Starting async nametag auto update task...");

        final double maxDistance = Math.pow(128, 2); // square the distance we are using Location#distanceSquared. This is because it is faster than Location#distance since it does not need to sqrt which is taxing on the CPU.
        final long period = instance.settingsCfg.getInt("nametag-auto-update-task-period"); // run every ? seconds.

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
                        if (!livingEntity.getPersistentDataContainer().has(instance.levelManager.isLevelledKey, PersistentDataType.STRING))
                            continue;

                        //if within distance, update nametag.
                        if (livingEntity.getLocation().distanceSquared(location) <= maxDistance) {
                            instance.levelManager.updateNametag(livingEntity, instance.levelManager.getNametag(livingEntity, false), Collections.singletonList(player));
                        }
                    }
                }
            }
        }.runTaskTimerAsynchronously(instance, 0, 20 * period);
    }

    public void stopNametagAutoUpdateTask() {
        if (!ExternalCompatibilityManager.hasProtocolLibInstalled()) return;

        if (nametagAutoUpdateTask != null && !nametagAutoUpdateTask.isCancelled()) {
            Utils.logger.info("&fTasks: &7Stopping async nametag auto update task...");
            nametagAutoUpdateTask.cancel();
        }
    }
}
