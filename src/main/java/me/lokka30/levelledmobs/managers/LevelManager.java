package me.lokka30.levelledmobs.managers;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import me.lokka30.levelledmobs.LevelInterface;
import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.customdrops.CustomDropResult;
import me.lokka30.levelledmobs.listeners.EntitySpawnListener;
import me.lokka30.levelledmobs.misc.*;
import me.lokka30.levelledmobs.rules.strategies.LevellingStrategy;
import me.lokka30.levelledmobs.rules.strategies.SpawnDistanceStrategy;
import me.lokka30.levelledmobs.rules.strategies.YDistanceStrategy;
import me.lokka30.microlib.MessageUtils;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.EnchantmentTarget;
import org.bukkit.entity.*;
import org.bukkit.inventory.AbstractHorseInventory;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * TODO Describe...
 *
 * @author lokka30
 * CoolBoy, Esophose, 7smile7, Shevchik, Hugo5551,
 * limzikiki
 */
public class LevelManager {

    private final LevelledMobs main;
    final public HashMap<LevelNumbersWithBias, LevelNumbersWithBias> levelNumsListCache = new HashMap<>();
    final public LinkedList<LevelNumbersWithBias> levelNumsListCacheOrder = new LinkedList<>();
    private final static int maxLevelNumsCache = 10;
    final private List<Material> vehicleNoMultiplierItems;
    public final NamespacedKey levelKey; // This stores the mob's level.
    public final NamespacedKey spawnReasonKey; //This is stored on levelled mobs to tell how a mob was spawned
    public final NamespacedKey noLevelKey; // This key tells LM not to level the mob in future
    public final NamespacedKey wasBabyMobKey; // This key tells LM not to level the mob in future
    public final NamespacedKey overridenEntityNameKey;
    public double attributeMaxHealthMax = 2048.0;
    public double attributeMovementSpeedMax = 2048.0;
    public double attributeAttackDamageMax = 2048.0;

    public final static int maxCreeperBlastRadius = 100;
    public EntitySpawnListener entitySpawnListener;

    public LevelManager(final LevelledMobs main) {
        this.main = main;

        levelKey = new NamespacedKey(main, "level");
        spawnReasonKey = new NamespacedKey(main, "spawnReason");
        noLevelKey = new NamespacedKey(main, "noLevel");
        wasBabyMobKey = new NamespacedKey(main, "wasBabyMob");
        overridenEntityNameKey = new NamespacedKey(main, "overridenEntityName");

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

    public int generateDistanceFromSpawnLevel(final LivingEntityWrapper lmEntity, final int minLevel, final int maxLevel, final SpawnDistanceStrategy spawnDistanceStrategy) {
        Location spawnLocation = lmEntity.getLivingEntity().getWorld().getSpawnLocation();

        if (spawnDistanceStrategy.spawnLocation_Z != null || spawnDistanceStrategy.spawnLocation_X != null) {
            final double useX = spawnDistanceStrategy.spawnLocation_X == null ? spawnLocation.getX() : spawnDistanceStrategy.spawnLocation_X;
            final double useZ = spawnDistanceStrategy.spawnLocation_Z == null ? spawnLocation.getX() : spawnDistanceStrategy.spawnLocation_Z;

            spawnLocation = new Location(
                    lmEntity.getLivingEntity().getWorld(),
                    useX,
                    spawnLocation.getY(),
                    useZ);
        }

        final int distanceFromSpawn = (int) spawnLocation.distance(lmEntity.getLivingEntity().getLocation());
        final int startDistance = spawnDistanceStrategy.startDistance;
        final int levelDistance = Math.max(distanceFromSpawn - startDistance, 0);
        final int increaseLevelDistance = spawnDistanceStrategy.increaseLevelDistance;

        int variance = spawnDistanceStrategy.variance;
        if (variance != 0) {
            variance = ThreadLocalRandom.current().nextInt(0, variance + 1);
        }

        //Get the level thats meant to be at a given distance
        final int spawnDistanceAssignment = Math.min((levelDistance / increaseLevelDistance) + minLevel + variance, maxLevel);
        if (!spawnDistanceStrategy.blendedLevellingEnabled)
            return spawnDistanceAssignment;

        return generateBlendedLevel(lmEntity, spawnDistanceStrategy, spawnDistanceAssignment, minLevel, maxLevel);
    }

    private int generateBlendedLevel(final LivingEntityWrapper lmEntity, SpawnDistanceStrategy spawnDistanceStrategy,
                                     final int spawnDistanceLevelAssignment, final int minLevel, final int maxLevel){
        final int currentYPos = lmEntity.getLivingEntity().getLocation().getBlockY();
        final Location spawnLocation = lmEntity.getLivingEntity().getWorld().getSpawnLocation();

        double result;

        if (spawnDistanceStrategy.scaleDownward) {
            result = ((((
                    (double) spawnDistanceStrategy.transition_Y_Height - (double) currentYPos) /
                    (double) spawnDistanceStrategy.multiplierPeriod) * spawnDistanceStrategy.lvlMultiplier)
                    * (double) spawnDistanceLevelAssignment);
        }
        else {
            result = ((((
                    (double) spawnDistanceStrategy.transition_Y_Height - (double) currentYPos) /
                    (double) spawnDistanceStrategy.multiplierPeriod) * (spawnDistanceStrategy.lvlMultiplier * -1.0))
                    * (double) spawnDistanceLevelAssignment);
        }

        result = Utils.round(Math.floor(result) + spawnDistanceLevelAssignment);
        if (result < minLevel) result = minLevel;
        else if (result > maxLevel) result = maxLevel;

        return (int) result;
    }

    // this is now the main entry point that determines the level for all criteria
    public int generateLevel(final LivingEntityWrapper lmEntity) {
        return generateLevel(lmEntity, -1, -1);
    }

    public int generateLevel(final LivingEntityWrapper lmEntity, final int minLevel_Pre, final int maxLevel_Pre) {
        int minLevel = minLevel_Pre;
        int maxLevel = maxLevel_Pre;

        if (minLevel == -1 || maxLevel == -1) {
            final int[] levels = getMinAndMaxLevels(lmEntity);
            if (minLevel == -1) minLevel = levels[0];
            if (maxLevel == -1) maxLevel = levels[1];
        }

        LevellingStrategy levellingStrategy = main.rulesManager.getRule_LevellingStrategy(lmEntity);

        if (levellingStrategy instanceof YDistanceStrategy)
            return generateYCoordinateLevel(lmEntity, minLevel, maxLevel, (YDistanceStrategy) levellingStrategy);
        else if (levellingStrategy instanceof SpawnDistanceStrategy)
            return generateDistanceFromSpawnLevel(lmEntity, minLevel, maxLevel, (SpawnDistanceStrategy)levellingStrategy);

        // system 1: random levelling

        if (minLevel == maxLevel)
            return minLevel;

        LevelNumbersWithBias levelNumbersWithBias = main.rulesManager.getRule_LowerMobLevelBiasFactor(lmEntity, minLevel, maxLevel);

        if (levelNumbersWithBias != null)
            return levelNumbersWithBias.getNumberWithinLimits();
        else
            return ThreadLocalRandom.current().nextInt(minLevel, maxLevel + 1);
    }

    public int[] getMinAndMaxLevels(final @Nullable LivingEntityWrapper lmEntity) {
        // final EntityType entityType, final boolean isAdultEntity, final String worldName
        // if called from summon command then lmEntity is null

        // TODO: ignore all these null
        int minLevel = main.rulesManager.getRule_MobMinLevel(lmEntity);
        int maxLevel = main.rulesManager.getRule_MobMaxLevel(lmEntity);

        // world guard regions take precedence over any other min / max settings
        // livingEntity is null if passed from summon mobs command
        if (lmEntity != null && ExternalCompatibilityManager.hasWorldGuardInstalled() && main.worldGuardManager.checkRegionFlags(lmEntity.getLivingEntity())) {
            final int[] levels = generateWorldGuardRegionLevel(lmEntity);
            if (levels[0] > -1) minLevel = levels[0];
            if (levels[1] > -1) maxLevel = levels[1];
        }

        // this will prevent an unhandled exception:
        if (minLevel > maxLevel) minLevel = maxLevel;

        return new int[]{ minLevel, maxLevel };
    }

    // public int generateYCoordinateLevel(final int mobYLocation, final int minLevel, final int maxLevel) {
    public int generateYCoordinateLevel(final LivingEntityWrapper lmEntity, final int minLevel, final int maxLevel, final YDistanceStrategy yDistanceStrategy) {

        int mobYLocation = lmEntity.getLivingEntity().getLocation().getBlockY();

        final int yPeriod = yDistanceStrategy.yPeriod;
        final int variance = yDistanceStrategy.yPeriod;
        int yStart = yDistanceStrategy.startingYLevel;
        int yEnd = yDistanceStrategy.endingYLevel;

        final boolean isAscending = (yEnd > yStart);
        if (!isAscending) {
            yStart = yEnd;
            yEnd = yDistanceStrategy.startingYLevel;
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

    public int[] generateWorldGuardRegionLevel(final LivingEntityWrapper lmEntity) {
        return main.worldGuardManager.getRegionLevel(lmEntity.getLivingEntity());

        //TODO Do we need the code below?

        // standard issue, generate random levels based upon max and min flags in worldguard

//        final int biasFactor = instance.settingsCfg.getInt("fine-tuning.lower-mob-level-bias-factor", 0);
//        if (biasFactor > 0)
//            return generateLevelWithBias(levels[0], levels[1], biasFactor);
//        else
//            return levels[0] + Math.round(new Random().nextFloat() * (levels[1] - levels[0]));


        // generate level based on y distance but use min and max values from world guard
        //return instance.levelManager.generateYCoordinateLevel(livingEntity.getLocation().getBlockY(), levels[0], levels[1]);
    }

    public void updateNametagWithDelay(final LivingEntityWrapper lmEntity, final String nametag, final long delay) {
        new BukkitRunnable() {
            public void run() {
                if (lmEntity == null) return; // may have died/removed after the timer.
                updateNametag(lmEntity, nametag);
            }
        }.runTaskLater(main, delay);
    }

    public void updateNametagWithDelay(final LivingEntityWrapper lmEntity, final long delay) {
        new BukkitRunnable() {
            public void run() {
                if (lmEntity == null) return; // may have died/removed after the timer.
                updateNametag(lmEntity, getNametag(lmEntity, false));
            }
        }.runTaskLater(main, delay);
    }

    public void updateNametagWithDelay(final LivingEntityWrapper lmEntity, final List<Player> playerList, final long delay) {
        new BukkitRunnable() {
            public void run() {
                if (lmEntity == null) return; // may have died/removed after the timer.
                updateNametag(lmEntity, getNametag(lmEntity, false), playerList);
            }
        }.runTaskLater(main, delay);
    }

    // This sets the levelled currentDrops on a levelled mob that just died.
    public void setLevelledItemDrops(final LivingEntityWrapper lmEntity, final List<ItemStack> currentDrops) {

        // this accomodates chested animals, saddles and armor on ridable creatures
        final List<ItemStack> dropsToMultiply = getDropsToMultiply(lmEntity, currentDrops);
        final List<ItemStack> customDrops = new LinkedList<>();
        currentDrops.clear();

        Utils.debugLog(main, DebugType.SET_LEVELLED_ITEM_DROPS, "1: Method called. " + dropsToMultiply.size() + " drops will be analysed.");

        // Get their level
        final int level = lmEntity.getMobLevel();
        Utils.debugLog(main, DebugType.SET_LEVELLED_ITEM_DROPS, "3: Entity " + lmEntity.getTypeName() + " level is " + level + ".");

        final boolean doNotMultiplyDrops = !main.rulesManager.getRule_CheckIfNoDropMultiplierEntitiy(lmEntity);

        if (main.rulesManager.getRule_UseCustomDropsForMob(lmEntity)) {
            // custom drops also get multiplied in the custom drops handler
            final CustomDropResult dropResult = main.customDropsHandler.getCustomItemDrops(lmEntity, customDrops, false);

            if (dropResult == CustomDropResult.HAS_OVERRIDE) {
                Utils.debugLog(main, DebugType.SET_LEVELLED_ITEM_DROPS, "4: custom drop has override");
                removeVanillaDrops(lmEntity, dropsToMultiply);
            }
        }

        if (!doNotMultiplyDrops && !dropsToMultiply.isEmpty()) {
            // Get currentDrops added per level value
            final int addition = BigDecimal.valueOf(main.mobDataManager.getAdditionsForLevel(lmEntity, Addition.CUSTOM_ITEM_DROP))
                    .setScale(0, RoundingMode.HALF_DOWN).intValueExact(); // truncate double to int
            Utils.debugLog(main, DebugType.SET_LEVELLED_ITEM_DROPS, "4: Item drop addition is +" + addition + ".");

            // Modify current drops
            Utils.debugLog(main, DebugType.SET_LEVELLED_ITEM_DROPS, "5: Scanning " + dropsToMultiply.size() + " items...");
            for (final ItemStack currentDrop : dropsToMultiply)
                multiplyDrop(lmEntity, currentDrop, addition, false);
        }

        if (!customDrops.isEmpty()) currentDrops.addAll(customDrops);
        if (!dropsToMultiply.isEmpty()) currentDrops.addAll(dropsToMultiply);
    }

    public void multiplyDrop(LivingEntityWrapper lmEntity, final ItemStack currentDrop, final int addition, final boolean isCustomDrop){
        Utils.debugLog(main, DebugType.SET_LEVELLED_ITEM_DROPS, "6: Scanning drop " + currentDrop.getType() + " with current amount " + currentDrop.getAmount() + "...");

        if (isCustomDrop || main.mobDataManager.isLevelledDropManaged(lmEntity.getLivingEntity().getType(), currentDrop.getType())) {
            int useAmount = currentDrop.getAmount() + (currentDrop.getAmount() * addition);
            if (useAmount > currentDrop.getMaxStackSize()) useAmount = currentDrop.getMaxStackSize();
            currentDrop.setAmount(useAmount);
            Utils.debugLog(main, DebugType.SET_LEVELLED_ITEM_DROPS, "7: Item was managed. New amount: " + currentDrop.getAmount() + ".");
        } else {
            Utils.debugLog(main, DebugType.SET_LEVELLED_ITEM_DROPS, "7: Item was unmanaged.");
        }
    }

    @Nonnull
    private List<ItemStack> getDropsToMultiply(final LivingEntityWrapper lmEntity, final List<ItemStack> drops){
        final List<ItemStack> results = new ArrayList<>(drops.size());
        results.addAll(drops);

        // we only need to check for chested animals and 'vehicles' since they can have saddles and armor
        // those items shouldn't get multiplied

        if (lmEntity.getLivingEntity() instanceof ChestedHorse && ((ChestedHorse)lmEntity.getLivingEntity()).isCarryingChest()){
            final AbstractHorseInventory inv = ((ChestedHorse) lmEntity.getLivingEntity()).getInventory();
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

        if (!(lmEntity.getLivingEntity() instanceof Vehicle)) return results;

        for (int i = results.size() - 1; i >= 0; i--){
            // remove horse armor or saddles
            final ItemStack item = results.get(i);
            if (this.vehicleNoMultiplierItems.contains(item.getType())) // saddle or horse armor
                results.remove(i);
        }

        return results;
    }

    public void removeVanillaDrops(final LivingEntityWrapper lmEntity, final List<ItemStack> drops){
        boolean hadSaddle = false;
        List<ItemStack> chestItems = null;

        if (lmEntity.getLivingEntity() instanceof ChestedHorse && ((ChestedHorse)lmEntity.getLivingEntity()).isCarryingChest()){
            final AbstractHorseInventory inv = ((ChestedHorse) lmEntity.getLivingEntity()).getInventory();
            chestItems = new LinkedList<>();
            Collections.addAll(chestItems, inv.getContents());
            chestItems.add(new ItemStack(Material.CHEST));
        }
        else if (lmEntity.getLivingEntity() instanceof Vehicle){
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
    public int getLevelledExpDrops(final LivingEntityWrapper lmEntity, final int xp) {
        if (lmEntity.isLevelled()) {
            final int level = lmEntity.getMobLevel();
            return (int) Math.round(xp + (xp * main.mobDataManager.getAdditionsForLevel(lmEntity, Addition.CUSTOM_XP_DROP)));
        } else {
            return xp;
        }
    }

    /**
     * Executes commands that are suitable for this entity
     *
     * @param lmEntity     entity that was killed
     * @author limzikiki
     */
    public void execCommands(final LivingEntityWrapper lmEntity){
        if(lmEntity.isLevelled()){
            //Get section that contains all data related to mob kill commands
            final ConfigurationSection configs = main.customCommandsCfg;
            if(configs != null) {
                final Set<String> entities = configs.getKeys(false);
                for (String entityType: entities) {
                    if (entityType.equals("file-version")) continue;
                    if (entityType.equals("ALL") || EntityType.valueOf(entityType) == lmEntity.getLivingEntity().getType()) {
                        serilalizeConfigsForCommands(lmEntity, configs, entityType);
                    }
                }
            }else{
                throw new Error("Error reading 'customCommands.yml'");
            }
        }
    }

    private void serilalizeConfigsForCommands(LivingEntityWrapper lmEntity, ConfigurationSection configs, String entityType) {
        final List<Map<?, ?>> entityConfigs = configs.getMapList(entityType);
        final ConfigurationSection commandsName = configs.getConfigurationSection(entityType);

        Utils.debugLog(main, DebugType.CUSTOM_COMMANDS, entityConfigs.toString());

        entityConfigs.forEach(elem -> {
            Utils.debugLog(main, DebugType.CUSTOM_COMMANDS, elem.keySet().toString());
            final String commandName = (String) elem.keySet().iterator().next();
            final Map<String, Object> commandConfigs = (Map<String, Object>) elem.get(commandName);
            Utils.debugLog(main, DebugType.CUSTOM_COMMANDS, commandConfigs.keySet().toString());
            final String command = (String) commandConfigs.get("command");
            int minLevel = (int) commandConfigs.getOrDefault("minLevel", 0);
            int maxLevel = (int) commandConfigs.getOrDefault("maxLevel", Integer.MAX_VALUE);
            double chance = (double) commandConfigs.getOrDefault("chance", 1.0);
            boolean playerCaused = (boolean) commandConfigs.getOrDefault("playerCaused", true);

            final CustomMobCommand commandInstance = new CustomMobCommand(commandName, command, minLevel, maxLevel, chance, playerCaused);

            execCommands(lmEntity, commandInstance);

        });
    }

    /**
     * Executes commands that are suitable for this entity,
     * configs should contain only levels and corresponding commands
     *
     * @param lmEntity     entity that was killed
     * @param configs    configs containing only levels and corresponding commands
     * @author limzikiki
     */
    public void execCommands(final LivingEntityWrapper lmEntity, @Nonnull final CustomMobCommand configs) {

        final int entityLevel = lmEntity.getMobLevel();
        //noinspection ConstantConditions
        final boolean isPlayerCaused = (lmEntity.getLivingEntity().getKiller() != null) && (lmEntity.getLivingEntity().getKiller() instanceof Player);

        final String commandName = configs.commandName;

        final String command = configs.command;
        final int minLevel = configs.minLevel;
        final int maxLevel = configs.maxLevel;
        final boolean playerCaused = configs.playerCaused;
        final double chance = configs.chance;

        if (chance < 1.0) {
            double chanceRole = ThreadLocalRandom.current().nextInt(0, 100001) * 0.0001;
            if (1.0 - chanceRole >= chance) return;
        }

        if (isPlayerCaused != playerCaused) return;

        if (minLevel >= entityLevel || maxLevel <= entityLevel) return;

        final Player player = lmEntity.getLivingEntity().getKiller();

        // Replace placeholders
        String finalCommand = command;

        if (isPlayerCaused) {
            // %player% placeholder
            finalCommand = Utils.replaceEx(finalCommand, "%player%", player.getName());
        }

        // %level% placeholder
        finalCommand = Utils.replaceEx(finalCommand, "%level%", String.valueOf(entityLevel));

        // %world% placeholder
        finalCommand = Utils.replaceEx(finalCommand, "%world%", lmEntity.getWorldName());

        // %location% placeholder
        final String location =
                lmEntity.getLivingEntity().getLocation().getBlockX() + " " +
                lmEntity.getLivingEntity().getLocation().getBlockY() + " " +
                lmEntity.getLivingEntity().getLocation().getBlockZ();
        finalCommand = Utils.replaceEx(finalCommand, "%location%", location);

        Utils.debugLog(main, DebugType.CUSTOM_COMMANDS, "Command: " + finalCommand);
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCommand);
    }


    // When the persistent data container levelled key has been set on the entity already (i.e. when they are damaged)
    public String getNametag(final LivingEntityWrapper lmEntity, final boolean isDeathNametag) {
        return getNametag(lmEntity, lmEntity.getMobLevel(), isDeathNametag);
    }

    // When the persistent data container levelled key has not been set on the entity yet (i.e. for use in EntitySpawnListener)
    public String getNametag(final LivingEntityWrapper lmEntity, final int level, final boolean isDeathNametag) {

        final AttributeInstance maxHealth = lmEntity.getLivingEntity().getAttribute(Attribute.GENERIC_MAX_HEALTH);
        final String roundedMaxHealth = maxHealth == null ? "?" : Utils.round(maxHealth.getBaseValue()) + "";
        final String roundedMaxHealthInt = maxHealth == null ? "?" : (int) Utils.round(maxHealth.getBaseValue()) + "";

        String nametag = isDeathNametag ? main.rulesManager.getRule_Nametag_CreatureDeath(lmEntity) : main.rulesManager.getRule_Nametag(lmEntity);
        String entityName = Utils.capitalize(lmEntity.getTypeName()).toLowerCase().replaceAll("_", " ");

        // Baby zombies can have specific nametags in entity-name-override

        final String overridenName = lmEntity.hasOverridenEntityName() ?
                lmEntity.getOverridenEntityName() :
                main.rulesManager.getRule_EntityOverriddenName(lmEntity);

        if (overridenName != null) entityName = overridenName;
        if (entityName.isEmpty() || entityName.equalsIgnoreCase("disabled")) return null;
        final String displayName = lmEntity.getLivingEntity().getCustomName() == null ? MessageUtils.colorizeAll(entityName) : lmEntity.getLivingEntity().getCustomName();

        // ignore if 'disabled'
        if (nametag.isEmpty() || nametag.equalsIgnoreCase("disabled") || nametag.equalsIgnoreCase("none"))
            return lmEntity.getLivingEntity().getCustomName(); // CustomName can be null, that is meant to be the case.

        // %tiered% placeholder
        String tieredPlaceholder = main.rulesManager.getRule_TieredPlaceholder(lmEntity);
        if (tieredPlaceholder == null) tieredPlaceholder = "";

        // replace them placeholders ;)
        nametag = nametag.replace("%mob-lvl%", level + "");
        nametag = nametag.replace("%entity-name%", entityName);
        nametag = nametag.replace("%entity-health%", Utils.round(lmEntity.getLivingEntity().getHealth()) + "");
        nametag = nametag.replace("%entity-health-rounded%", (int) Utils.round(lmEntity.getLivingEntity().getHealth()) + "");
        nametag = nametag.replace("%entity-max-health%", roundedMaxHealth);
        nametag = nametag.replace("%entity-max-health-rounded%", roundedMaxHealthInt);
        nametag = nametag.replace("%heart_symbol%", "❤");
        nametag = nametag.replace("%tiered%", tieredPlaceholder);
        nametag = nametag.replace("%wg_region%", lmEntity.getWGRegionName());
        nametag = MessageUtils.colorizeAll(nametag);

        // This is after colorize so that color codes in nametags dont get translated
        nametag = nametag.replace("%displayname%", displayName);

        return nametag;
    }

    public void updateNametag(final @NotNull LivingEntityWrapper lmEntity, final String nametag) {
        updateNametag(lmEntity, nametag, lmEntity.getLivingEntity().getWorld().getPlayers());
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
    public void updateNametag(final @NotNull LivingEntityWrapper lmEntity, final String nametag, final List<Player> players) {

        if (!ExternalCompatibilityManager.hasProtocolLibInstalled()) return;
        if (main.settingsCfg.getBoolean("assert-entity-validity-with-nametag-packets") && !lmEntity.getLivingEntity().isValid())
            return;

        final WrappedDataWatcher dataWatcher;
        final WrappedDataWatcher.Serializer chatSerializer;

        try {
            dataWatcher = WrappedDataWatcher.getEntityWatcher(lmEntity.getLivingEntity()).deepClone();
        } catch (ConcurrentModificationException ex) {
            Utils.debugLog(main, DebugType.UPDATE_NAMETAG_FAIL, "Concurrent modification occured, skipping nametag update of " + lmEntity.getLivingEntity().getName() + ".");
            return;
        }

        try {
            chatSerializer = WrappedDataWatcher.Registry.getChatComponentSerializer(true);
        } catch (ConcurrentModificationException ex) {
            Utils.debugLog(main, DebugType.UPDATE_NAMETAG_FAIL, "ConcurrentModificationException caught, skipping nametag update of " + lmEntity.getLivingEntity().getName() + ".");
            return;
        } catch (IllegalArgumentException ex) {
            Utils.debugLog(main, DebugType.UPDATE_NAMETAG_FAIL, "Registry is empty, skipping nametag update of " + lmEntity.getLivingEntity().getName() + ".");
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
        dataWatcher.setObject(3, !Utils.isNullOrEmpty(nametag) && lmEntity.getLivingEntity().isCustomNameVisible() || main.rulesManager.getRule_CreatureNametagAlwaysVisible(lmEntity));

        final PacketContainer packet = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.ENTITY_METADATA);
        packet.getWatchableCollectionModifier().write(0, dataWatcher.getWatchableObjects());
        packet.getIntegers().write(0, lmEntity.getLivingEntity().getEntityId());

        for (Player player : players) {
            if (!player.isOnline()) continue;
            if (!lmEntity.getLivingEntity().isValid()) return;

            try {
                Utils.debugLog(main, DebugType.UPDATE_NAMETAG_SUCCESS, "Nametag packet sent for " + lmEntity.getLivingEntity().getName() + " to " + player.getName() + ".");
                ProtocolLibrary.getProtocolManager().sendServerPacket(player, packet);
            } catch (IllegalArgumentException ex) {
                Utils.debugLog(main, DebugType.UPDATE_NAMETAG_FAIL, "IllegalArgumentException caught whilst trying to sendServerPacket");
            } catch (InvocationTargetException ex) {
                Utils.logger.error("Unable to update nametag packet for player &b" + player.getName() + "&7! Stack trace:");
                ex.printStackTrace();
            }
        }
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

                        if (!entity.isValid()) continue; // async task, entity can despawn whilst it is running

                        // Mob must be a livingentity that is ...living.
                        if (!(entity instanceof LivingEntity)) continue;
                        LivingEntityWrapper lmEntity = new LivingEntityWrapper((LivingEntity) entity, main);

                        // if the mob isn't levelled then see if it qualifies to be levelled
                        if (!lmEntity.isLevelled() &&
                                !lmEntity.isBabyMob() &&
                                lmEntity.getPDC().has(main.levelManager.wasBabyMobKey, PersistentDataType.INTEGER) &&
                                main.levelInterface.getLevellableState(lmEntity) == LevelInterface.LevellableState.ALLOWED) {
                            // if the mob was a baby at some point, aged and now is eligable for levelling, we'll apply a level to it now
                            Utils.debugLog(main, DebugType.ENTITY_MISC, lmEntity.getLivingEntity().getName() + " was a baby and is now an adult, applying levelling rules");
                            // can't apply the level from an async task
                            applyLevelToMobFromAsync(lmEntity);
                        }
                        else if (lmEntity.isLevelled() && lmEntity.getLivingEntity().getLocation().distanceSquared(location) <= maxDistance){
                            //if within distance, update nametag.
                            main.levelManager.updateNametag(
                                    lmEntity, main.levelManager.getNametag(lmEntity, false), Collections.singletonList(player)
                            );
                        }
                    }
                }
            }
        }.runTaskTimerAsynchronously(main, 0, 20 * period);
    }

    private void applyLevelToMobFromAsync(final LivingEntityWrapper lmEntity){
        BukkitRunnable applyLevelTask = new BukkitRunnable() {
            @Override
            public void run() {
                lmEntity.getPDC().remove(main.levelManager.wasBabyMobKey);

                main.levelInterface.applyLevelToMob(
                        lmEntity,
                        main.levelInterface.generateLevel(lmEntity),
                        false,
                        false,
                        new HashSet<>(Collections.singletonList(LevelInterface.AdditionalLevelInformation.NOT_APPLICABLE))
                );
            }
        };

        applyLevelTask.runTask(main);
    }

    public void stopNametagAutoUpdateTask() {
        if (!ExternalCompatibilityManager.hasProtocolLibInstalled()) return;

        if (nametagAutoUpdateTask != null && !nametagAutoUpdateTask.isCancelled()) {
            Utils.logger.info("&fTasks: &7Stopping async nametag auto update task...");
            nametagAutoUpdateTask.cancel();
        }
    }

    public void applyLevelledAttributes(final LivingEntityWrapper lmEntity, final Addition addition) {
        assert lmEntity.isLevelled();

        // This functionality should be added into the enum.
        Attribute attribute;
        switch (addition) {
            case ATTRIBUTE_MAX_HEALTH:
                attribute = Attribute.GENERIC_MAX_HEALTH;
                break;
            case ATTRIBUTE_ATTACK_DAMAGE:
                attribute = Attribute.GENERIC_ATTACK_DAMAGE;
                break;
            case ATTRIBUTE_MOVEMENT_SPEED:
                attribute = Attribute.GENERIC_MOVEMENT_SPEED;
                break;
            default:
                throw new IllegalStateException("Addition must be an Attribute, if so, it has not been considered in this method");
        }

        // Attr instance for the mob
        final AttributeInstance attrInst = lmEntity.getLivingEntity().getAttribute(attribute);

        // Don't try to apply an addition to their attribute if they don't have it
        if (attrInst == null) return;

        // For the MAX_HEALTH attr, must use the base value for Slimes
        final boolean useBaseValue = (lmEntity.getLivingEntity() instanceof Slime) && attribute == Attribute.GENERIC_MAX_HEALTH;

        // Apply additions
        main.mobDataManager.setAdditionsForLevel(lmEntity, attribute, addition, useBaseValue);

        // MAX_HEALTH specific: set health to max health
        if (attribute == Attribute.GENERIC_MAX_HEALTH) {
            //noinspection ConstantConditions
            lmEntity.getLivingEntity().setHealth(lmEntity.getLivingEntity().getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue());
        }
    }

    public void applyCreeperBlastRadius(LivingEntityWrapper lmEntity, int level) {
        final int creeperMaxDamageRadius = main.rulesManager.getRule_CreeperMaxBlastRadius(lmEntity);
        Creeper creeper = (Creeper) lmEntity.getLivingEntity();

        if (creeperMaxDamageRadius == 3) return;

        final int minMobLevel = main.rulesManager.getRule_MobMinLevel(lmEntity);
        final double levelDiff = main.rulesManager.getRule_MobMaxLevel(lmEntity) - minMobLevel;
        final double maxBlastDiff = creeperMaxDamageRadius - 3;
        final double useLevel = level - minMobLevel;
        final double percent = useLevel / levelDiff;
        int blastRadius = (int) Math.round(maxBlastDiff * percent) + 3;

        // don't let it go too high, for the server owner's sanity
        blastRadius = Math.min(LevelManager.maxCreeperBlastRadius, blastRadius);

        if (level == 1) {
            // level 1 creepers will always have default
            blastRadius = 3;
        } else if (level == 0 && blastRadius > 2) {
            // level 0 will always be less than default
            blastRadius = 2;
        }

        creeper.setExplosionRadius(blastRadius);
    }

    public void updateNametag(LivingEntityWrapper lmEntity, int level) {
        if (!main.settingsCfg.getBoolean("show-label-for-default-levelled-mobs") && level == 1) {
            // Don't show the label for default levelled mobs, since the server owner configured it this way
            main.levelManager.updateNametag(lmEntity, main.levelManager.getNametag(lmEntity, level, false));
            return;
        }

        final String nametag = main.levelManager.getNametag(lmEntity, level, false);
        main.levelManager.updateNametagWithDelay(lmEntity, nametag, 1);
    }

    /**
     * Add configured equipment to the levelled mob
     * LivingEntity MUST be a levelled mob
     * <p>
     * Thread-safety unknown.
     *
     * @param lmEntity a levelled mob to apply levelled equipment to
     * @param level        the level of the levelled mob
     */
    public void applyLevelledEquipment(@NotNull final LivingEntityWrapper lmEntity, final int level) {
        if (!lmEntity.isLevelled()) {
            // if you summon a mob and it isn't levelled due to a config rule (baby zombies exempt for example)
            // then we'll be here with a non-levelled entity
            return;
        }
        assert level >= 0;

        // Custom Drops must be enabled.
        if (!main.rulesManager.getRule_UseCustomDropsForMob(lmEntity)) return;

        List<ItemStack> items = new ArrayList<>();
        main.customDropsHandler.getCustomItemDrops(lmEntity, items, true);
        if (items.isEmpty()) return;

        EntityEquipment equipment = lmEntity.getLivingEntity().getEquipment();
        if (equipment == null) return;

        boolean hadMainItem = false;

        for (ItemStack itemStack : items) {
            Material material = itemStack.getType();
            if (EnchantmentTarget.ARMOR_FEET.includes(material)) {
                equipment.setBoots(itemStack, true);
                equipment.setBootsDropChance(0);
            } else if (EnchantmentTarget.ARMOR_LEGS.includes(material)) {
                equipment.setLeggings(itemStack, true);
                equipment.setLeggingsDropChance(0);
            } else if (EnchantmentTarget.ARMOR_TORSO.includes(material)) {
                equipment.setChestplate(itemStack, true);
                equipment.setChestplateDropChance(0);
            } else if (EnchantmentTarget.ARMOR_HEAD.includes(material)) {
                equipment.setHelmet(itemStack, true);
                equipment.setHelmetDropChance(0);
            } else {
                if (!hadMainItem) {
                    equipment.setItemInMainHand(itemStack);
                    equipment.setItemInMainHandDropChance(0);
                    hadMainItem = true;
                } else {
                    equipment.setItemInOffHand(itemStack);
                    equipment.setItemInOffHandDropChance(0);
                }
            }
        }
    }
}

/**
 * TODO Describe...
 *
 * @author limzikiki
 */
class CustomMobCommand {
    public final String commandName, command;
    public final int minLevel, maxLevel;
    public final double chance;
    public final boolean playerCaused;

    public CustomMobCommand(String commandName, String command, int minLevel, int maxLevel, double chance, boolean playerCaused) {
        this.commandName = commandName;
        this.command = command;
        this.minLevel = minLevel;
        this.maxLevel = maxLevel;
        this.chance = chance;
        this.playerCaused = playerCaused;
    }
}