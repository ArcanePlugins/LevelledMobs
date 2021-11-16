/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.managers;

import me.lokka30.levelledmobs.LevelInterface;
import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.LivingEntityInterface;
import me.lokka30.levelledmobs.customdrops.CustomDropResult;
import me.lokka30.levelledmobs.events.MobPostLevelEvent;
import me.lokka30.levelledmobs.events.MobPreLevelEvent;
import me.lokka30.levelledmobs.events.SummonedMobPreLevelEvent;
import me.lokka30.levelledmobs.listeners.EntitySpawnListener;
import me.lokka30.levelledmobs.misc.*;
import me.lokka30.levelledmobs.rules.*;
import me.lokka30.levelledmobs.rules.strategies.LevellingStrategy;
import me.lokka30.levelledmobs.rules.strategies.RandomLevellingStrategy;
import me.lokka30.levelledmobs.rules.strategies.SpawnDistanceStrategy;
import me.lokka30.levelledmobs.rules.strategies.YDistanceStrategy;
import me.lokka30.microlib.messaging.MessageUtils;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Generates levels and manages other functions related to levelling mobs
 *
 * @author lokka30, stumper66, CoolBoy, Esophose, 7smile7, Shevchik, Hugo5551, limzikiki
 * @since 2.4.0
 */
public class LevelManager implements LevelInterface {

    public LevelManager(final LevelledMobs main) {
        this.main = main;
        this.randomLevellingCache = new TreeMap<>();
        this.summonedOrSpawnEggs = new WeakHashMap<>();

        this.vehicleNoMultiplierItems = Arrays.asList(
                Material.SADDLE,
                Material.LEATHER_HORSE_ARMOR,
                Material.IRON_HORSE_ARMOR,
                Material.GOLDEN_HORSE_ARMOR,
                Material.DIAMOND_HORSE_ARMOR
        );
    }

    private final LevelledMobs main;
    private final static int maxLevelNumsCache = 10;
    final private List<Material> vehicleNoMultiplierItems;
    public double attributeMaxHealthMax = 2048.0;
    public double attributeMovementSpeedMax = 2048.0;
    public double attributeAttackDamageMax = 2048.0;
    public final Map<LivingEntity, Object> summonedOrSpawnEggs;
    public static final Object summonedOrSpawnEggs_Lock = new Object();
    private boolean hasMentionedNBTAPI_Missing;
    private final Map<String, RandomLevellingStrategy> randomLevellingCache;
    public final static int maxCreeperBlastRadius = 100;
    public EntitySpawnListener entitySpawnListener;

    /**
     * The following entity types *MUST NOT* be levellable.
     * Stored as Strings since older versions may not contain certain entity type constants
     */
    public final HashSet<String> FORCED_BLOCKED_ENTITY_TYPES = new HashSet<>(Arrays.asList(
            "AREA_EFFECT_CLOUD", "ARMOR_STAND", "ARROW", "BOAT", "DRAGON_FIREBALL", "DROPPED_ITEM",
            "EGG", "ENDER_CRYSTAL", "ENDER_PEARL", "ENDER_SIGNAL", "EXPERIENCE_ORB",
            "FALLING_BLOCK", "FIREBALL", "FIREWORK", "FISHING_HOOK", "GLOW_ITEM_FRAME",
            "ITEM_FRAME", "LEASH_HITCH", "LIGHTNING", "LLAMA_SPIT", "MARKER", "MINECART",
            "MINECART_CHEST", "MINECART_COMMAND", "MINECART_FURNACE", "MINECART_HOPPER",
            "MINECART_MOB_SPAWNER", "MINECART_TNT", "NPC", "PAINTING", "PLAYER", "PRIMED_TNT",
            "SMALL_FIREBALL", "SNOWBALL", "SPECTRAL_ARROW", "SPLASH_POTION", "THROWN_EXP_BOTTLE",
            "TRIDENT", "UNKNOWN", "WITHER_SKULL"
    ));

    /**
     * The following entity types must be manually ALLOWED in 'getLevellableState',
     * as they are not instanceof Monster or Boss
     * Stored as Strings since older versions may not contain certain entity type constants
     */
    public final HashSet<String> OTHER_HOSTILE_MOBS = new HashSet<>(Arrays.asList("GHAST", "HOGLIN", "SHULKER", "PHANTOM", "ENDER_DRAGON", "SLIME", "MAGMA_CUBE", "ZOMBIFIED_PIGLIN"));

    public void clearRandomLevellingCache(){
        this.randomLevellingCache.clear();
    }

    /**
     * This method generates a level for the mob. It utilises the levelling mode
     * specified by the administrator through the settings.yml configuration.
     *
     * Thread-safety intended, but not tested.
     *
     * @param lmEntity the entity to generate a level for
     * @return a level for the entity
     */
    public int generateLevel(final LivingEntityWrapper lmEntity) {
        return generateLevel(lmEntity, -1, -1);
    }

    /**
     * This method generates a level for the mob. It utilises the levelling mode
     * specified by the administrator through the settings.yml configuration.
     *
     * Thread-safety intended, but not tested.
     *
     * @param lmEntity the entity to generate a level for
     * @param minLevel_Pre the minimum level to be used for the mob
     * @param maxLevel_Pre the maximum level to be used for the mob
     * @return a level for the entity
     */
    public int generateLevel(final LivingEntityWrapper lmEntity, final int minLevel_Pre, final int maxLevel_Pre) {
        int minLevel = minLevel_Pre;
        int maxLevel = maxLevel_Pre;

        if (minLevel == -1 || maxLevel == -1) {
            final int[] levels = getMinAndMaxLevels(lmEntity);
            if (minLevel == -1) minLevel = levels[0];
            if (maxLevel == -1) maxLevel = levels[1];
        }

        final LevellingStrategy levellingStrategy = main.rulesManager.getRule_LevellingStrategy(lmEntity);

        if (levellingStrategy instanceof YDistanceStrategy || levellingStrategy instanceof SpawnDistanceStrategy)
            return levellingStrategy.generateLevel(lmEntity, minLevel, maxLevel);

        // if no levelling strategy was selected then we just use a random number between min and max

        if (minLevel == maxLevel)
            return minLevel;

        final RandomLevellingStrategy randomLevelling = (levellingStrategy instanceof RandomLevellingStrategy) ?
                (RandomLevellingStrategy) levellingStrategy : null;

        return generateRandomLevel(randomLevelling, minLevel, maxLevel);
    }

    private int generateRandomLevel(RandomLevellingStrategy randomLevelling, final int minLevel, final int maxLevel){
        if (randomLevelling == null) {
            // used the caches defaults if it exists, otherwise add it to the cache
            if (this.randomLevellingCache.containsKey("default"))
                randomLevelling = this.randomLevellingCache.get("default");
            else {
                randomLevelling = new RandomLevellingStrategy();
                this.randomLevellingCache.put("default", randomLevelling);
            }
        } else {
            // used the caches one if it exists, otherwise add it to the cache
            final String checkName = String.format("%s-%s: %s", minLevel, maxLevel, randomLevelling);

            if (this.randomLevellingCache.containsKey(checkName))
                randomLevelling = this.randomLevellingCache.get(checkName);
            else {
                randomLevelling.populateWeightedRandom(minLevel, maxLevel);
                this.randomLevellingCache.put(checkName, randomLevelling);
            }
        }

        return randomLevelling.generateLevel(minLevel, maxLevel);
    }

    private int @Nullable [] getPlayerLevels(final @NotNull LivingEntityWrapper lmEntity){
        final PlayerLevellingOptions options = main.rulesManager.getRule_PlayerLevellingOptions(lmEntity);
        if (options == null) return null;

        final Player player = lmEntity.getPlayerForLevelling();
        if (player == null) return null;

        int levelSource;
        final String variableToUse = Utils.isNullOrEmpty(options.variable) ? "%level%" : options.variable;
        final double scale = options.playerLevelScale != null ? options.playerLevelScale : 1.0;
        final boolean usePlayerMax = options.usePlayerMaxLevel != null && options.matchPlayerLevel;
        final boolean matchPlayerLvl = options.matchPlayerLevel != null && options.matchPlayerLevel;
        final double origLevelSource = getPlayerLevelSourceNumber(lmEntity.getPlayerForLevelling(), variableToUse);

        levelSource = (int) Math.round(origLevelSource * scale);
        if (levelSource < 1) levelSource = 1;
        final int[] results = new int[]{ 1, 1};
        String tierMatched = null;
        final String capDisplay = options.levelCap == null ? "" : "cap: " + options.levelCap + ", ";

        if (options.usePlayerMaxLevel) {
            results[0] = levelSource;
            results[1] = results[0];
        } else if (options.matchPlayerLevel) {
            results[1] = levelSource;
        } else {
            boolean foundMatch = false;
            for (final LevelTierMatching tier : options.levelTiers) {
                final boolean meetsMin = (tier.minLevel == null || levelSource >= tier.minLevel);
                final boolean meetsMax = (tier.maxLevel == null || levelSource <= tier.maxLevel);

                if (meetsMin && meetsMax) {
                    if (tier.valueRanges[0] > 0) results[0] = tier.valueRanges[0];
                    if (tier.valueRanges[1] > 0) results[1] = tier.valueRanges[1];
                    tierMatched = tier.toString();
                    foundMatch = true;
                    break;
                }
            }

            if (!foundMatch) {
                Utils.debugLog(main, DebugType.PLAYER_LEVELLING, String.format(
                        "mob: %s, player: %s, lvl-src: %s, lvl-scale: %s, %sno tiers matched",
                        lmEntity.getNameIfBaby(), player.getName(), origLevelSource, levelSource, capDisplay));
                return null;
            }
        }

        if (options.levelCap != null){
            if (results[0] > options.levelCap)
                results[0] = options.levelCap;
            if (results[1] > options.levelCap)
                results[1] = options.levelCap;
        }

        if (tierMatched == null) {
            Utils.debugLog(main, DebugType.PLAYER_LEVELLING, String.format(
                    "mob: %s, player: %s, lvl-src: %s, lvl-scale: %s, %sresult: %s",
                    lmEntity.getNameIfBaby(), player.getName(), origLevelSource, levelSource, capDisplay, Arrays.toString(results)));
        } else {
            Utils.debugLog(main, DebugType.PLAYER_LEVELLING, String.format(
                    "mob: %s, player: %s, lvl-src: %s, lvl-scale: %s, tier: %s, %sresult: %s",
                    lmEntity.getNameIfBaby(), player.getName(), origLevelSource, levelSource, tierMatched, capDisplay, Arrays.toString(results)));
        }

        lmEntity.playerLevellingAllowDecrease = options.decreaseLevel;

        return results;
    }

    public int getPlayerLevelSourceNumber(final Player player, final String variableToUse){
        if (player == null) return 1;

        double origLevelSource;

        if (variableToUse.equalsIgnoreCase("%level%"))
            origLevelSource = player.getLevel();
        else if (variableToUse.equalsIgnoreCase("%exp%"))
            origLevelSource = player.getExp();
        else if (variableToUse.equalsIgnoreCase("%exp-to-level%"))
            origLevelSource = player.getExpToLevel();
        else if (variableToUse.equalsIgnoreCase("%total-exp%"))
            origLevelSource = player.getTotalExperience();
        else if (variableToUse.equalsIgnoreCase("%world_time_ticks%"))
            origLevelSource = player.getWorld().getTime();
        else{
            boolean usePlayerLevel = false;
            String PAPIResult = null;

            if (ExternalCompatibilityManager.hasPAPI_Installed()) {
                PAPIResult = ExternalCompatibilityManager.getPAPI_Placeholder(player, variableToUse);
                if (Utils.isNullOrEmpty(PAPIResult)) {
                    Utils.logger.warning("Got blank result for '" + variableToUse + "' from PAPI");
                    usePlayerLevel = true;
                }
                if (!Utils.isDouble(PAPIResult)) {
                    Utils.logger.warning("Got invalid number for '" + variableToUse + "' from PAPI");
                    usePlayerLevel = true;
                }
            } else {
                Utils.logger.warning("PlaceHolderAPI is not installed, unable to get variable " + variableToUse);
                usePlayerLevel = true;
            }

            if (usePlayerLevel)
                origLevelSource = player.getLevel();
            else
                origLevelSource = (int) Double.parseDouble(PAPIResult);
        }

        return (int) Math.round(origLevelSource);
    }

    public int[] getMinAndMaxLevels(final @NotNull LivingEntityInterface lmInterface) {
        // final EntityType entityType, final boolean isAdultEntity, final String worldName
        // if called from summon command then lmEntity is null

        int minLevel = main.rulesManager.getRule_MobMinLevel(lmInterface);
        int maxLevel = main.rulesManager.getRule_MobMaxLevel(lmInterface);

        if (main.configUtils.playerLevellingEnabled && lmInterface instanceof LivingEntityWrapper &&
                ((LivingEntityWrapper)lmInterface).getPlayerForLevelling() != null) {
            final int[] playerLevellingResults = getPlayerLevels((LivingEntityWrapper) lmInterface);
            if (playerLevellingResults != null){
                minLevel = playerLevellingResults[0];
                maxLevel = playerLevellingResults[1];
            }
        }

        // this will prevent an unhandled exception:
        if (minLevel < 1) minLevel = 1;
        if (maxLevel < 1) maxLevel = 1;

        if (minLevel > maxLevel) minLevel = maxLevel;

        return new int[]{ minLevel, maxLevel };
    }

    // This sets the levelled currentDrops on a levelled mob that just died.
    public void setLevelledItemDrops(final LivingEntityWrapper lmEntity, final List<ItemStack> currentDrops) {

        // this accomodates chested animals, saddles and armor on ridable creatures
        final List<ItemStack> dropsToMultiply = getDropsToMultiply(lmEntity, currentDrops);
        final List<ItemStack> customDrops = new LinkedList<>();
        currentDrops.clear();

        Utils.debugLog(main, DebugType.SET_LEVELLED_ITEM_DROPS, "&81:&7 Method called. &b" + dropsToMultiply.size() + "&7 drops will be analysed.");

        Utils.debugLog(main, DebugType.SET_LEVELLED_ITEM_DROPS, "&82:&7 Level of the &b" + lmEntity.getTypeName() + "&7 entity is &b" + lmEntity.getMobLevel() + "&7.");

        final boolean doNotMultiplyDrops = main.rulesManager.getRule_CheckIfNoDropMultiplierEntitiy(lmEntity);

        if (main.rulesManager.getRule_UseCustomDropsForMob(lmEntity).useDrops) {
            // custom drops also get multiplied in the custom drops handler
            final CustomDropResult dropResult = main.customDropsHandler.getCustomItemDrops(lmEntity, customDrops, false);

            if (dropResult == CustomDropResult.HAS_OVERRIDE) {
                Utils.debugLog(main, DebugType.SET_LEVELLED_ITEM_DROPS, "&83:&7 Custom drop has override.");
                removeVanillaDrops(lmEntity, dropsToMultiply);
            }
        }

        if (!doNotMultiplyDrops && !dropsToMultiply.isEmpty()) {
            // Get currentDrops added per level value
            final int addition = BigDecimal.valueOf(main.mobDataManager.getAdditionsForLevel(lmEntity, Addition.CUSTOM_ITEM_DROP, 2.0))
                    .setScale(0, RoundingMode.HALF_DOWN).intValueExact(); // truncate double to int
            Utils.debugLog(main, DebugType.SET_LEVELLED_ITEM_DROPS, "&84:&7 Item drop addition is &b+" + addition + "&7.");

            // Modify current drops
            Utils.debugLog(main, DebugType.SET_LEVELLED_ITEM_DROPS, "&85:&7 Scanning &b" + dropsToMultiply.size() + "&7 items...");
            for (final ItemStack currentDrop : dropsToMultiply)
                multiplyDrop(lmEntity, currentDrop, addition, false);
        }

        if (!customDrops.isEmpty()) currentDrops.addAll(customDrops);
        if (!dropsToMultiply.isEmpty()) currentDrops.addAll(dropsToMultiply);
    }

    public void multiplyDrop(LivingEntityWrapper lmEntity, @NotNull final ItemStack currentDrop, final int addition, final boolean isCustomDrop){
        Utils.debugLog(main, DebugType.SET_LEVELLED_ITEM_DROPS, "&86:&7 Scanning drop &b" + currentDrop.getType() + "&7 with current amount &b" + currentDrop.getAmount() + "&7...");

        if (isCustomDrop || main.mobDataManager.isLevelledDropManaged(lmEntity.getLivingEntity().getType(), currentDrop.getType())) {
            int useAmount = currentDrop.getAmount() + (currentDrop.getAmount() * addition);
            if (useAmount > currentDrop.getMaxStackSize()) useAmount = currentDrop.getMaxStackSize();
            currentDrop.setAmount(useAmount);
            Utils.debugLog(main, DebugType.SET_LEVELLED_ITEM_DROPS, "&87:&7 Item was managed. New amount: &b" + currentDrop.getAmount() + "&7.");
        } else {
            Utils.debugLog(main, DebugType.SET_LEVELLED_ITEM_DROPS, "&87:&7 Item was unmanaged.");
        }
    }

    @NotNull
    private List<ItemStack> getDropsToMultiply(@NotNull final LivingEntityWrapper lmEntity, @NotNull final List<ItemStack> drops){
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

    public void removeVanillaDrops(@NotNull final LivingEntityWrapper lmEntity, final List<ItemStack> drops){
        boolean hadSaddle = false;
        List<ItemStack> chestItems = null;

        if (lmEntity.getLivingEntity() instanceof ChestedHorse && ((ChestedHorse) lmEntity.getLivingEntity()).isCarryingChest()) {
            final AbstractHorseInventory inv = ((ChestedHorse) lmEntity.getLivingEntity()).getInventory();
            chestItems = new LinkedList<>();
            Collections.addAll(chestItems, inv.getContents());
            chestItems.add(new ItemStack(Material.CHEST));
        } else if (lmEntity.getLivingEntity() instanceof Vehicle) {
            for (final ItemStack itemStack : drops) {
                if (itemStack.getType().equals(Material.SADDLE)) {
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
    public int getLevelledExpDrops(@NotNull final LivingEntityWrapper lmEntity, final int xp) {
        if (lmEntity.isLevelled()) {
            final int newXp = (int) Math.round(xp + (xp * main.mobDataManager.getAdditionsForLevel(lmEntity, Addition.CUSTOM_XP_DROP, 3.0)));
            Utils.debugLog(main, DebugType.SET_LEVELLED_XP_DROPS, String.format("%s: lvl: %s, xp-vanilla: %s, new-xp: %s",
                    lmEntity.getNameIfBaby(), lmEntity.getMobLevel(), xp, newXp));
            return newXp;
        } else
            return xp;
    }

    @Nullable
    public String getNametag(final LivingEntityWrapper lmEntity, final boolean isDeathNametag) {
        String nametag = isDeathNametag ? main.rulesManager.getRule_Nametag_CreatureDeath(lmEntity) : main.rulesManager.getRule_Nametag(lmEntity);
        if ("disabled".equalsIgnoreCase(nametag) || "none".equalsIgnoreCase(nametag)) return null;

        final boolean useCustomNameForNametags = main.helperSettings.getBoolean(main.settingsCfg, "use-customname-for-mob-nametags");
        // ignore if 'disabled'
        if (nametag.isEmpty()) {
            if (useCustomNameForNametags)
                return lmEntity.getTypeName();
            else
                return lmEntity.getLivingEntity().getCustomName(); // CustomName can be null, that is meant to be the case.
        }
        if (!lmEntity.isLevelled())
            nametag = "";

        return updateNametag(lmEntity, nametag, useCustomNameForNametags);
    }

    @NotNull
    public String updateNametag(final LivingEntityWrapper lmEntity, @NotNull String nametag, final boolean useCustomNameForNametags) {
        if ("".equals(nametag)) return nametag;
        final String overridenName = main.rulesManager.getRule_EntityOverriddenName(lmEntity, useCustomNameForNametags);

        String displayName = overridenName == null ?
                Utils.capitalize(lmEntity.getTypeName().replaceAll("_", " ")) :
                MessageUtils.colorizeAll(overridenName);

        if (lmEntity.getLivingEntity().getCustomName() != null && !useCustomNameForNametags)
            displayName = lmEntity.getLivingEntity().getCustomName();

        nametag = replaceStringPlaceholders(nametag, lmEntity, displayName);

        // This is after colorize so that color codes in nametags dont get translated
        nametag = nametag.replace("%displayname%", displayName);

        if (nametag.toLowerCase().contains("%health-indicator%"))
            nametag = nametag.replace("%health-indicator%", formatHealthIndicator(lmEntity));

        if (nametag.contains("%") && ExternalCompatibilityManager.hasPAPI_Installed())
            nametag = ExternalCompatibilityManager.getPAPI_Placeholder(null, nametag);

        return nametag;
    }

    @NotNull
    private String formatHealthIndicator(final LivingEntityWrapper lmEntity){
        final HealthIndicator indicator = main.rulesManager.getRule_NametagIndicator(lmEntity);
        final double mobHealth = lmEntity.getLivingEntity().getHealth();

        if (indicator == null || mobHealth == 0.0) return "";

        final StringBuilder sb = new StringBuilder();
        final int maxIndicators = indicator.maxIndicators != null ? indicator.maxIndicators : 10;
        final String indicatorStr = indicator.indicator != null ? indicator.indicator : "▐";
        final double scale = indicator.scale != null ? indicator.scale : 5.0;
        final double healthPerTier = scale * maxIndicators;

        int indicatorsToUse = scale == 0 ?
                (int) Math.ceil(mobHealth) : (int) Math.ceil(mobHealth / scale);
        final int tiersToUse = (int) Math.ceil((double) indicatorsToUse / (double) maxIndicators);
        int toRecolor = 0;
        if (tiersToUse > 0)
            toRecolor = indicatorsToUse % maxIndicators;

        String primaryColor = "";
        String secondaryColor = "";

        if (indicator.tiers != null){
            if (indicator.tiers.containsKey(tiersToUse))
                primaryColor = indicator.tiers.get(tiersToUse);
            else if (indicator.tiers.containsKey(0))
                primaryColor = indicator.tiers.get(0);

            if (tiersToUse > 0 && indicator.tiers.containsKey(tiersToUse - 1))
                secondaryColor = indicator.tiers.get(tiersToUse - 1);
            else if (indicator.tiers.containsKey(0))
                secondaryColor = indicator.tiers.get(0);
        }

        String result = primaryColor;

        if (tiersToUse < 2) {
            boolean useHalf = false;
            if (indicator.indicatorHalf != null && indicatorsToUse < maxIndicators) {
                useHalf = scale / 2.0 <= (indicatorsToUse * scale) - mobHealth;
                if (useHalf && indicatorsToUse > 0) indicatorsToUse--;
            }

            result += indicatorStr.repeat(indicatorsToUse);
            if (useHalf) result += indicator.indicatorHalf;
        } else {
            if (toRecolor == 0)
                result += primaryColor + indicatorStr.repeat(maxIndicators);
            else {
                result += primaryColor + indicatorStr.repeat(toRecolor);
                result += secondaryColor + indicatorStr.repeat(maxIndicators - toRecolor);
            }
        }

        return MessageUtils.colorizeAll(result);
    }

    public String replaceStringPlaceholders(final String nametag, @NotNull final LivingEntityWrapper lmEntity, final String displayName){
        String result = nametag;

        final double maxHealth = getMobAttributeValue(lmEntity, Attribute.GENERIC_MAX_HEALTH);
        final double entityHealth = getMobHealth(lmEntity);
        final int entityHealthRounded = entityHealth < 1.0 && entityHealth > 0.0 ?
                1 : (int) Utils.round(entityHealth);
        final String roundedMaxHealth = Utils.round(maxHealth) + "";
        final String roundedMaxHealthInt = (int) Utils.round(maxHealth) + "";
        final double percentHealthTemp = Math.round(entityHealth / maxHealth * 100.0);
        final int percentHealth = percentHealthTemp < 1.0 ? 1 : (int) percentHealthTemp;

        String tieredPlaceholder = main.rulesManager.getRule_TieredPlaceholder(lmEntity);
        if (tieredPlaceholder == null) tieredPlaceholder = "";

        final String locationStr = String.format("%s %s %s",
                lmEntity.getLivingEntity().getLocation().getBlockX(),
                lmEntity.getLivingEntity().getLocation().getBlockY(),
                lmEntity.getLivingEntity().getLocation().getBlockZ());

        // replace them placeholders ;)
        result = result.replace("%mob-lvl%", lmEntity.getMobLevel() + "");
        result = result.replace("%entity-name%", Utils.capitalize(lmEntity.getNameIfBaby().replace("_", " ")));
        result = result.replace("%entity-health%", Utils.round(entityHealth) + "");
        result = result.replace("%entity-health-rounded%", entityHealthRounded + "");
        result = result.replace("%entity-max-health%", roundedMaxHealth);
        result = result.replace("%entity-max-health-rounded%", roundedMaxHealthInt);
        result = result.replace("%heart_symbol%", "❤");
        result = result.replace("%tiered%", tieredPlaceholder);
        result = result.replace("%wg_region%", lmEntity.getWGRegionName());
        result = result.replace("%world%", lmEntity.getWorldName());
        result = result.replace("%location%", locationStr);
        result = result.replace("%health%-percent%", percentHealth + "");
        result = result.replace("%x%", lmEntity.getLivingEntity().getLocation().getBlockX() + "");
        result = result.replace("%y%", lmEntity.getLivingEntity().getLocation().getBlockY() + "");
        result = result.replace("%z%", lmEntity.getLivingEntity().getLocation().getBlockZ() + "");

        if (result.contains("%") && ExternalCompatibilityManager.hasPAPI_Installed())
            result = ExternalCompatibilityManager.getPAPI_Placeholder(null, result);

        result = MessageUtils.colorizeAll(result);

        return result;
    }

    public void updateNametag_WithDelay(final @NotNull LivingEntityWrapper lmEntity){
        final BukkitRunnable runnable = new BukkitRunnable() {
            @Override
            public void run() {
                updateNametag(lmEntity);

                lmEntity.free();
            }
        };

        lmEntity.inUseCount.getAndIncrement();
        runnable.runTaskLater(main, 1L);
    }

    public void updateNametag(final LivingEntityWrapper lmEntity){
        final QueueItem queueItem = new QueueItem(
                lmEntity,
                getNametag(lmEntity, false),
                lmEntity.getLivingEntity().getWorld().getPlayers()
        );

        main.nametagQueueManager_.addToQueue(queueItem);
    }

    public void updateNametag(final @NotNull LivingEntityWrapper lmEntity, final String nametag) {
        updateNametag(lmEntity, nametag, lmEntity.getLivingEntity().getWorld().getPlayers());
    }

    public void updateNametag(final @NotNull LivingEntityWrapper lmEntity, final String nametag, final List<Player> players) {
        main.nametagQueueManager_.addToQueue(new QueueItem(lmEntity, nametag, players));
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

    public BukkitTask nametagAutoUpdateTask;
    public BukkitTask nametagTimerTask;

    public void startNametagAutoUpdateTask() {
        Utils.logger.info("&fTasks: &7Starting async nametag auto update task...");

        final long period = main.helperSettings.getInt(main.settingsCfg, "async-task-update-period", 6); // run every ? seconds.

        nametagAutoUpdateTask = new BukkitRunnable() {
            @Override
            public void run() {
                final Map<Player, List<Entity>> entitiesPerPlayer = new LinkedHashMap<>();
                final int checkDistance = main.helperSettings.getInt(main.settingsCfg,"async-task-max-blocks-from-player", 100);

                for (final Player player : Bukkit.getOnlinePlayers()) {
                    final List<Entity> entities = player.getNearbyEntities(checkDistance, checkDistance, checkDistance);
                    entitiesPerPlayer.put(player, entities);
                }

                final BukkitRunnable runnable = new BukkitRunnable() {
                    @Override
                    public void run() {
                        runNametagCheck_aSync(entitiesPerPlayer);
                    }
                };

                runnable.runTaskAsynchronously(main);
            }
        }.runTaskTimer(main, 0, 20 * period);
    }

    public void startNametagTimer(){
        nametagTimerTask = new BukkitRunnable() {
            @Override
            public void run() {
                final BukkitRunnable runnable = new BukkitRunnable() {
                    @Override
                    public void run() {
                        main.nametagTimerChecker.checkNametags();
                    }
                };

                runnable.runTaskAsynchronously(main);
            }
        }.runTaskTimer(main, 0, 20);
    }

    private void runNametagCheck_aSync(final @NotNull Map<Player,List<Entity>> entitiesPerPlayer){
        final Map<LivingEntityWrapper, List<Player>> entityToPlayer = new LinkedHashMap<>();

        for (final Player player : entitiesPerPlayer.keySet()) {
            for (final Entity entity : entitiesPerPlayer.get(player)) {

                if (!entity.isValid()) continue; // async task, entity can despawn whilst it is running

                // Mob must be a livingentity that is ...living.
                if (!(entity instanceof LivingEntity) || entity instanceof Player || !entity.isValid()) continue;

                boolean wrapperHasReference = false;
                final LivingEntityWrapper lmEntity = LivingEntityWrapper.getInstance((LivingEntity) entity, main);
                lmEntity.playerForPermissionsCheck = player;

                if (lmEntity.isLevelled()) {
                    if (main.configUtils.playerLevellingEnabled) {
                        final boolean hasKey = entityToPlayer.containsKey(lmEntity);
                        final List<Player> players = hasKey ?
                                entityToPlayer.get(lmEntity) : new LinkedList<>();
                        players.add(player);
                        if (!hasKey) entityToPlayer.put(lmEntity, players);
                        wrapperHasReference = true;
                    }

                    boolean useResetTimer = false;
                    if (lmEntity.getLivingEntity() == null) continue;
                    final List<NametagVisibilityEnum> nametagVisibilityEnums = main.rulesManager.getRule_CreatureNametagVisbility(lmEntity);
                    final int nametagVisibleTime = lmEntity.getNametagCooldownTime();
                   if (nametagVisibleTime > 0 &&
                            nametagVisibilityEnums.contains(NametagVisibilityEnum.TARGETED) &&
                            lmEntity.getLivingEntity().hasLineOfSight(player)) {

                        if (lmEntity.playersNeedingNametagCooldownUpdate == null)
                            lmEntity.playersNeedingNametagCooldownUpdate = new HashSet<>();
                        lmEntity.playersNeedingNametagCooldownUpdate.add(player);
                    }

                    checkLevelledEntity(lmEntity, player);
                } else {
                    boolean wasBabyMob;
                    synchronized (lmEntity.getLivingEntity().getPersistentDataContainer()) {
                        wasBabyMob = lmEntity.getPDC().has(main.namespaced_keys.wasBabyMobKey, PersistentDataType.INTEGER);
                    }
                    if (lmEntity.getLivingEntity() != null) { // a hack to prevent a null exception that was reported
                        final LevellableState levellableState = main.levelInterface.getLevellableState(lmEntity);
                        if (!lmEntity.isBabyMob() &&
                                wasBabyMob &&
                                levellableState == LevellableState.ALLOWED) {
                            // if the mob was a baby at some point, aged and now is eligable for levelling, we'll apply a level to it now
                            Utils.debugLog(main, DebugType.ENTITY_MISC, "&b" + lmEntity.getTypeName() + " &7was a baby and is now an adult, applying levelling rules");

                            main._mobsQueueManager.addToQueue(new QueueItem(lmEntity, null));
                        } else if (levellableState == LevellableState.ALLOWED) {
                            Utils.logger.info("async, levelling mob 2");
                            main._mobsQueueManager.addToQueue(new QueueItem(lmEntity, null));
                        }
                    }
                }

                if (!wrapperHasReference)
                    lmEntity.free();
            }
        }

        for (final LivingEntityWrapper lmEntity : entityToPlayer.keySet()) {
            if (entityToPlayer.containsKey(lmEntity))
                checkEntityForPlayerLevelling(lmEntity, entityToPlayer.get(lmEntity));

            lmEntity.free();
        }
    }

    private void checkEntityForPlayerLevelling(final @NotNull LivingEntityWrapper lmEntity, final @NotNull List<Player> players){
        final LivingEntity mob = lmEntity.getLivingEntity();
        final List<Player> sortedPlayers = players.stream()
                .filter(p -> mob.getWorld().equals(p.getWorld()))
                .filter(p -> !p.getGameMode().equals(GameMode.SPECTATOR))
                .map(p -> Map.entry(mob.getLocation().distanceSquared(p.getLocation()), p))
                .sorted(Comparator.comparingDouble(Map.Entry::getKey))
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());

        Player closestPlayer = null;
        for (final Player player : sortedPlayers){
            if (ExternalCompatibilityManager.isMobOfCitizens(player))
                continue;

            closestPlayer = player;
            break;
        }

        if (closestPlayer == null)
            return;

        if (doesMobNeedRelevelling(mob, closestPlayer)) {
            lmEntity.pendingPlayerIdToSet = closestPlayer.getUniqueId().toString();
            lmEntity.setPlayerForLevelling(closestPlayer);
            lmEntity.reEvaluateLevel = true;
            main._mobsQueueManager.addToQueue(new QueueItem(lmEntity, null));
        }
    }

    private void checkLevelledEntity(@NotNull final LivingEntityWrapper lmEntity, @NotNull final Player player){
        if (lmEntity.getLivingEntity() == null || !lmEntity.getLivingEntity().isValid()) return;
        final double maxDistance = Math.pow(128, 2); // square the distance we are using Location#distanceSquared. This is because it is faster than Location#distance since it does not need to sqrt which is taxing on the CPU.
        final Location location = player.getLocation();

        if (lmEntity.getLivingEntity().getCustomName() != null && main.rulesManager.getRule_MobCustomNameStatus(lmEntity) == MobCustomNameStatus.NOT_NAMETAGGED) {
            // mob has a nametag but is levelled so we'll remove it
            main.levelInterface.removeLevel(lmEntity);
        } else if (lmEntity.isMobTamed() && main.rulesManager.getRule_MobTamedStatus(lmEntity) == MobTamedStatus.NOT_TAMED) {
            // mob is tamed with a level but the rules don't allow it, remove the level
            main.levelInterface.removeLevel(lmEntity);
        } else if (lmEntity.getLivingEntity().isValid() &&
                !main.helperSettings.getBoolean(main.settingsCfg, "use-customname-for-mob-nametags", false) &&
                location.getWorld() != null &&
                location.getWorld().equals(lmEntity.getWorld()) &&
                lmEntity.getLocation().distanceSquared(location) <= maxDistance) {
            //if within distance, update nametag.
            main.nametagQueueManager_.addToQueue(new QueueItem(lmEntity, main.levelManager.getNametag(lmEntity, false), Collections.singletonList(player)));
        }
    }

    private boolean doesMobNeedRelevelling(final @NotNull LivingEntity mob, final @NotNull Player player){
        if (main.playerLevellingMinRelevelTime > 0 && main.playerLevellingEntities.containsKey(mob)){
            final Instant lastCheck = main.playerLevellingEntities.get(mob);
            final Duration duration = Duration.between(lastCheck, Instant.now());

            if (duration.toMillis() < main.playerLevellingMinRelevelTime) return false;
        }

        String playerId;
        if (main.playerLevellingMinRelevelTime > 0)
            main.playerLevellingEntities.put(mob, Instant.now());

        synchronized (mob.getPersistentDataContainer()) {
            if (!mob.getPersistentDataContainer().has(main.namespaced_keys.playerLevelling_Id, PersistentDataType.STRING))
                return true;

            playerId = mob.getPersistentDataContainer().get(main.namespaced_keys.playerLevelling_Id, PersistentDataType.STRING);
        }

        if (playerId == null && main.playerLevellingMinRelevelTime <= 0) return true;
        else if (playerId == null || !player.getUniqueId().toString().equals(playerId))
            return true;

        return !player.getUniqueId().toString().equals(playerId);
    }

    public void stopNametagAutoUpdateTask() {
        if (!ExternalCompatibilityManager.hasProtocolLibInstalled()) return;

        if (nametagAutoUpdateTask != null && !nametagAutoUpdateTask.isCancelled()) {
            Utils.logger.info("&fTasks: &7Stopping async nametag auto update task...");
            nametagAutoUpdateTask.cancel();
        }

        if (nametagTimerTask != null && !nametagTimerTask.isCancelled()){
            nametagTimerTask.cancel();
        }
    }

    public void applyLevelledAttributes(@NotNull final LivingEntityWrapper lmEntity, @NotNull final Addition addition) {
        assert lmEntity.isLevelled();

        // This functionality should be added into the enum.
        Attribute attribute;
        switch (addition) {
            case ATTRIBUTE_MAX_HEALTH:                  attribute = Attribute.GENERIC_MAX_HEALTH; break;
            case ATTRIBUTE_ATTACK_DAMAGE:               attribute = Attribute.GENERIC_ATTACK_DAMAGE; break;
            case ATTRIBUTE_MOVEMENT_SPEED:              attribute = Attribute.GENERIC_MOVEMENT_SPEED; break;
            case ATTRIBUTE_HORSE_JUMP_STRENGTH:         attribute = Attribute.HORSE_JUMP_STRENGTH; break;
            case ATTRIBUTE_ARMOR_BONUS:                 attribute = Attribute.GENERIC_ARMOR; break;
            case ATTRIBUTE_ARMOR_TOUGHNESS:             attribute = Attribute.GENERIC_ARMOR_TOUGHNESS; break;
            case ATTRIBUTE_KNOCKBACK_RESISTANCE:        attribute = Attribute.GENERIC_KNOCKBACK_RESISTANCE; break;
            case ATTRIBUTE_FLYING_SPEED:                attribute = Attribute.GENERIC_FLYING_SPEED; break;
            case ATTRIBUTE_ATTACK_KNOCKBACK:            attribute = Attribute.GENERIC_ATTACK_KNOCKBACK; break;
            case ATTRIBUTE_ZOMBIE_SPAWN_REINFORCEMENTS: attribute = Attribute.ZOMBIE_SPAWN_REINFORCEMENTS; break;
            case ATTRIBUTE_FOLLOW_RANGE:                attribute = Attribute.GENERIC_FOLLOW_RANGE; break;

            default:
                throw new IllegalStateException("Addition must be an Attribute, if so, it has not been considered in this method");
        }

        // Attr instance for the mob
        final AttributeInstance attrInst = lmEntity.getLivingEntity().getAttribute(attribute);

        // Don't try to apply an addition to their attribute if they don't have it
        if (attrInst == null) return;

        // Apply additions
        main.mobDataManager.setAdditionsForLevel(lmEntity, attribute, addition);
    }

    public void applyCreeperBlastRadius(final @NotNull LivingEntityWrapper lmEntity, int level) {
        final Creeper creeper = (Creeper) lmEntity.getLivingEntity();

        final FineTuningAttributes tuning = main.rulesManager.getFineTuningAttributes(lmEntity);
        if (tuning == null) {
            // make sure creeper explosion is at vanilla defaults incase of a relevel, etc
            if (creeper.getExplosionRadius() != 3)
                creeper.setExplosionRadius(3);
            Utils.debugLog(main, DebugType.CREEPER_BLAST_RADIUS, String.format("lvl: %s, mulp: null, result: 3",
                    lmEntity.getMobLevel()));
            return;
        }

        final int maxRadius = main.rulesManager.getRule_CreeperMaxBlastRadius(lmEntity);
        double damage = main.mobDataManager.getAdditionsForLevel(lmEntity, Addition.CREEPER_BLAST_DAMAGE, 3);
        int blastRadius = 3 + (int) Math.floor(damage);

        if (blastRadius > maxRadius) blastRadius = maxRadius;
        else if (blastRadius < 0) blastRadius = 0;

        Utils.debugLog(main, DebugType.CREEPER_BLAST_RADIUS, String.format("lvl: %s, mulp: %s, max: %s, result: %s",
                lmEntity.getMobLevel(), Utils.round(damage, 3), maxRadius, blastRadius));

        if (blastRadius < 0) blastRadius = 0;

        creeper.setExplosionRadius(blastRadius);
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
        if (level < 1) return;

        // Custom Drops must be enabled.
        if (!main.rulesManager.getRule_UseCustomDropsForMob(lmEntity).useDrops) return;

        final List<ItemStack> items = new LinkedList<>();
        main.customDropsHandler.getCustomItemDrops(lmEntity, items, true);
        if (items.isEmpty()) return;

        final EntityEquipment equipment = lmEntity.getLivingEntity().getEquipment();
        if (equipment == null) return;

        boolean hadMainItem = false;
        boolean hadPlayerHead = false;


        for (final ItemStack itemStack : items) {
            final Material material = itemStack.getType();
            if (EnchantmentTarget.ARMOR_FEET.includes(material)) {
                equipment.setBoots(itemStack, true);
                equipment.setBootsDropChance(0);
            } else if (EnchantmentTarget.ARMOR_LEGS.includes(material)) {
                equipment.setLeggings(itemStack, true);
                equipment.setLeggingsDropChance(0);
            } else if (EnchantmentTarget.ARMOR_TORSO.includes(material)) {
                equipment.setChestplate(itemStack, true);
                equipment.setChestplateDropChance(0);
            } else if (EnchantmentTarget.ARMOR_HEAD.includes(material) || material.name().endsWith("_HEAD") && !hadPlayerHead) {
                equipment.setHelmet(itemStack, true);
                equipment.setHelmetDropChance(0);
                if (material == Material.PLAYER_HEAD) hadPlayerHead = true;
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

    public double getMobAttributeValue(@NotNull final LivingEntityWrapper lmEntity, final Attribute attribute){
        double result = 0.0;
        synchronized (main.attributeSyncObject){
            final AttributeInstance attrib = lmEntity.getLivingEntity().getAttribute(attribute);
            if (attrib != null)
                result = attrib.getValue();
        }

        return result;
    }

    public double getMobHealth(@NotNull final LivingEntityWrapper lmEntity){
        double result;
        synchronized (main.attributeSyncObject){
            result = lmEntity.getLivingEntity().getHealth();
        }

        return result;
    }

    @NotNull
    public LevellableState getLevellableState(@NotNull final LivingEntityInterface lmInterface) {
        /*
        Certain entity types are force-blocked, regardless of what the user has configured.
        This is also ran in getLevellableState(EntityType), however it is important that this is ensured
        before all other checks are made.
         */
        if (FORCED_BLOCKED_ENTITY_TYPES.contains(lmInterface.getTypeName()))
            return LevellableState.DENIED_FORCE_BLOCKED_ENTITY_TYPE;

        if (lmInterface.getApplicableRules().isEmpty())
            return LevellableState.DENIED_NO_APPLICABLE_RULES;

        if (!main.rulesManager.getRule_IsMobAllowedInEntityOverride(lmInterface))
            return LevellableState.DENIED_CONFIGURATION_BLOCKED_ENTITY_TYPE;

        if (main.rulesManager.getRule_MobMaxLevel(lmInterface) < 1)
            return LevellableState.DENIED_LEVEL_0;

        if (!(lmInterface instanceof LivingEntityWrapper))
            return LevellableState.ALLOWED;

        LivingEntityWrapper lmEntity = (LivingEntityWrapper) lmInterface;

        final LevellableState externalCompatResult = ExternalCompatibilityManager.checkAllExternalCompats(lmEntity, main);
        if (externalCompatResult != LevellableState.ALLOWED)
            return externalCompatResult;

        if (lmEntity.isMobOfExternalType()) {
            lmEntity.invalidateCache();

            if (!main.rulesManager.getRule_IsMobAllowedInEntityOverride(lmInterface))
                return LevellableState.DENIED_CONFIGURATION_BLOCKED_ENTITY_TYPE;
        }

        /*
        Check 'No Level Conditions'
         */
        // Nametagged mobs.
        if (lmEntity.getLivingEntity().getCustomName() != null &&
                main.rulesManager.getRule_MobCustomNameStatus(lmEntity) == MobCustomNameStatus.NOT_NAMETAGGED)
            return LevellableState.DENIED_CONFIGURATION_CONDITION_NAMETAGGED;

        // Tamed mobs.
        if (lmEntity.isMobTamed() &&
                main.rulesManager.getRule_MobTamedStatus(lmEntity) == MobTamedStatus.NOT_TAMED)
            return LevellableState.DENIED_CONFIGURATION_CONDITION_TAMED;

        return LevellableState.ALLOWED;
    }

    /**
     * This method applies a level to the target mob.
     * <p>
     * You can run this method on a mob regardless if
     * they are already levelled or not.
     * <p>
     * This method DOES NOT check if it is LEVELLABLE. It is
     * assumed that plugins make sure this is the case (unless
     * they intend otherwise).
     * <p>
     * It is highly recommended to leave bypassLimits = false,
     * unless the desired behaviour is to override the
     * user-configured limits.
     * <p>
     * Thread-safety intended, but not tested.
     *
     * @param lmEntity target mob
     * @param level        the level the mob should have
     * @param isSummoned   if the mob was spawned by LevelledMobs, not by the server
     * @param bypassLimits whether LM should disregard max level, etc.
     * @param additionalLevelInformation used to determine the source event
     */
    public void applyLevelToMob(@NotNull final LivingEntityWrapper lmEntity, int level, final boolean isSummoned, final boolean bypassLimits, @NotNull final HashSet<AdditionalLevelInformation> additionalLevelInformation) {
        // this thread runs in async.  if adding any functions make sure they can be run in this fashion

        if (level <= 0)
            level = generateLevel(lmEntity);

        assert bypassLimits || isSummoned || getLevellableState(lmEntity) == LevellableState.ALLOWED;
        boolean skipLM_Nametag = false;

        if (lmEntity.getLivingEntity().isInsideVehicle() && main.rulesManager.getRule_PassengerMatchLevel(lmEntity)
                && lmEntity.getLivingEntity().getVehicle() instanceof LivingEntity){
            // entity is a passenger. grab the level from the "vehicle" entity
            final LivingEntityWrapper vehicle = LivingEntityWrapper.getInstance((LivingEntity) lmEntity.getLivingEntity().getVehicle(), main);
            if (vehicle.isLevelled())
                level = vehicle.getMobLevel();

            vehicle.free();
        }

        if (isSummoned) {
            SummonedMobPreLevelEvent summonedMobPreLevelEvent = new SummonedMobPreLevelEvent(lmEntity.getLivingEntity(), level);
            Bukkit.getPluginManager().callEvent(summonedMobPreLevelEvent);

            if (summonedMobPreLevelEvent.isCancelled()) return;
        } else {
            MobPreLevelEvent mobPreLevelEvent = new MobPreLevelEvent(lmEntity.getLivingEntity(), level, MobPreLevelEvent.LevelCause.NORMAL, additionalLevelInformation);

            Bukkit.getPluginManager().callEvent(mobPreLevelEvent);
            if (mobPreLevelEvent.isCancelled()) return;

            level = mobPreLevelEvent.getLevel();
            if (!mobPreLevelEvent.getShowLM_Nametag()) {
                skipLM_Nametag = true;
                lmEntity.setShouldShowLM_Nametag(false);
            }
        }

        boolean hasNoLevelKey;
        synchronized (lmEntity.getLivingEntity().getPersistentDataContainer()) {
            hasNoLevelKey = lmEntity.getPDC().has(main.namespaced_keys.noLevelKey, PersistentDataType.STRING);
        }

        if (hasNoLevelKey) {
            Utils.debugLog(main, DebugType.APPLY_LEVEL_FAIL, "Entity &b" + lmEntity.getTypeName() + "&7 had &bnoLevelKey&7 attached");
            return;
        }

        synchronized (lmEntity.getLivingEntity().getPersistentDataContainer()) {
            lmEntity.getPDC().set(main.namespaced_keys.levelKey, PersistentDataType.INTEGER, level);
        }
        lmEntity.invalidateCache();

        final List<String> nbtDatas = lmEntity.nbtData != null && !lmEntity.nbtData.isEmpty() ?
                lmEntity.nbtData : main.rulesManager.getRule_NBT_Data(lmEntity);

        if (!nbtDatas.isEmpty() && !ExternalCompatibilityManager.hasNBTAPI_Installed()){
            if (!hasMentionedNBTAPI_Missing) {
                Utils.logger.warning("NBT Data has been specified in customdrops.yml but required plugin NBTAPI is not installed!");
                hasMentionedNBTAPI_Missing = true;
            }
            nbtDatas.clear();
        }
        final int creeperLevel = level;

        // setting attributes should be only done in the main thread.
        final BukkitRunnable applyAttribs = new BukkitRunnable() {
            @Override
            public void run() {
                synchronized (main.attributeSyncObject) {
                    main.levelManager.applyLevelledAttributes(lmEntity, Addition.ATTRIBUTE_ATTACK_DAMAGE);
                    main.levelManager.applyLevelledAttributes(lmEntity, Addition.ATTRIBUTE_MAX_HEALTH);
                    main.levelManager.applyLevelledAttributes(lmEntity, Addition.ATTRIBUTE_MOVEMENT_SPEED);
                    main.levelManager.applyLevelledAttributes(lmEntity, Addition.ATTRIBUTE_ARMOR_BONUS);
                    main.levelManager.applyLevelledAttributes(lmEntity, Addition.ATTRIBUTE_ARMOR_TOUGHNESS);
                    main.levelManager.applyLevelledAttributes(lmEntity, Addition.ATTRIBUTE_ATTACK_KNOCKBACK);
                    main.levelManager.applyLevelledAttributes(lmEntity, Addition.ATTRIBUTE_FLYING_SPEED);
                    main.levelManager.applyLevelledAttributes(lmEntity, Addition.ATTRIBUTE_KNOCKBACK_RESISTANCE);

                    if (lmEntity.getLivingEntity() instanceof Zombie)
                        main.levelManager.applyLevelledAttributes(lmEntity, Addition.ATTRIBUTE_ZOMBIE_SPAWN_REINFORCEMENTS);
                    else if (lmEntity.getLivingEntity() instanceof Horse)
                        main.levelManager.applyLevelledAttributes(lmEntity, Addition.ATTRIBUTE_HORSE_JUMP_STRENGTH);
                }

                if (!nbtDatas.isEmpty()) {
                    boolean hadSuccess = false;
                    final List<NBTApplyResult> allResults = new LinkedList<>();

                    for (final String nbtData : nbtDatas) {
                        NBTApplyResult result = NBTManager.applyNBT_Data_Mob(lmEntity, nbtData);
                        if (result.hadException()) {
                            if (lmEntity.summonedSender == null) {
                                Utils.logger.warning(String.format(
                                        "Error applying NBT data '%s' to %s. Exception message: %s",
                                        nbtData, lmEntity.getNameIfBaby(), result.exceptionMessage));
                            }
                            else
                                lmEntity.summonedSender.sendMessage("Error applying NBT data to " + lmEntity.getNameIfBaby() + ". Exception message: " + result.exceptionMessage);
                        } else {
                            hadSuccess = true;
                            allResults.add(result);
                        }
                    }

                    if (hadSuccess && lmEntity.getMainInstance().helperSettings.getStringSet(lmEntity.getMainInstance().settingsCfg, "debug-misc").contains("NBT_APPLY_SUCCESS")) {
                        final String changes = getNBT_DebugMessage(allResults);

                        Utils.debugLog(main, DebugType.NBT_APPLY_SUCCESS, "Applied NBT data to '" + lmEntity.getNameIfBaby() + "'. " + changes);
                    }
                }

                if (lmEntity.getLivingEntity() instanceof Creeper)
                    main.levelManager.applyCreeperBlastRadius(lmEntity, creeperLevel);

                lmEntity.free();
            }
        };

        lmEntity.inUseCount.getAndIncrement();
        applyAttribs.runTask(main);

        if (!skipLM_Nametag)
            main.levelManager.updateNametag_WithDelay(lmEntity);
        main.levelManager.applyLevelledEquipment(lmEntity, lmEntity.getMobLevel());

        MobPostLevelEvent.LevelCause levelCause = isSummoned ? MobPostLevelEvent.LevelCause.SUMMONED : MobPostLevelEvent.LevelCause.NORMAL;
        Bukkit.getPluginManager().callEvent(new MobPostLevelEvent(lmEntity, levelCause, additionalLevelInformation));

        final StringBuilder sb = new StringBuilder();
        sb.append("entity: ");
        sb.append(lmEntity.getLivingEntity().getName());
        sb.append(", world: ");
        sb.append(lmEntity.getWorldName());
        sb.append(", level: ");
        sb.append(level);
        if (isSummoned) sb.append(" (summoned)");
        if (bypassLimits) sb.append(" (limit bypass)");
        if (lmEntity.isBabyMob()) sb.append(" (baby)");

        Utils.debugLog(main, DebugType.APPLY_LEVEL_SUCCESS, sb.toString());
    }

    @NotNull
    private String getNBT_DebugMessage(final @NotNull List<NBTApplyResult> results){
        final StringBuilder sb = new StringBuilder();

        for (final NBTApplyResult result : results) {
            if (result.objectsAdded == null) continue;

            for (int i = 0; i < result.objectsAdded.size(); i++) {
                if (i > 0) sb.append(", ");
                else sb.append("added: ");

                sb.append(result.objectsAdded.get(i));
            }
        }

        for (final NBTApplyResult result : results) {
            if (result.objectsUpdated == null) continue;

            for (int i = 0; i < result.objectsUpdated.size(); i++) {
                if (i > 0 || sb.length() > 0) sb.append(", ");
                if (i == 0) sb.append("updated: ");

                sb.append(result.objectsUpdated.get(i));
            }
        }

        for (final NBTApplyResult result : results) {
            if (result.objectsRemoved == null) continue;

            for (int i = 0; i < result.objectsRemoved.size(); i++) {
                if (i > 0 || sb.length() > 0) sb.append(", ");
                if (i == 0) sb.append("removed: ");

                sb.append(result.objectsRemoved.get(i));
            }
        }

        if (sb.length() > 0)
            return sb.toString();
        else
            return "";
    }

    private void getPlayersNearMob(final @NotNull LivingEntityWrapper lmEntity){
        final int checkDistance = main.helperSettings.getInt(main.settingsCfg, "async-task-max-blocks-from-player", 100);
        final List<Player> players = EntitySpawnListener.getPlayersNearMob(lmEntity.getLivingEntity(), checkDistance);

        for (final Player player : players){
            if (lmEntity.getLivingEntity().hasLineOfSight(player)){
                if (lmEntity.playersNeedingNametagCooldownUpdate == null)
                    lmEntity.playersNeedingNametagCooldownUpdate = new HashSet<>();
                lmEntity.playersNeedingNametagCooldownUpdate.add(player);
            }
        }
    }

    /**
     * Check if a LivingEntity is a levelled mob or not.
     * This is determined *after* MobPreLevelEvent.
     * <p>
     * Thread-safety intended, but not tested.
     *
     * @param livingEntity living entity to check
     * @return if the mob is levelled or not
     */
    public boolean isLevelled(@NotNull final LivingEntity livingEntity) {
        synchronized (livingEntity.getPersistentDataContainer()) {
            return livingEntity.getPersistentDataContainer().has(main.namespaced_keys.levelKey, PersistentDataType.INTEGER);
        }
    }

    /**
     * Retrieve the level of a levelled mob.
     *
     * Thread-safety intended, but not tested.
     *
     * @param livingEntity the levelled mob to get the level of
     * @return the mob's level
     */
    public int getLevelOfMob(@NotNull final LivingEntity livingEntity) {
        synchronized (livingEntity.getPersistentDataContainer()) {
            if (!livingEntity.getPersistentDataContainer().has(main.namespaced_keys.levelKey, PersistentDataType.INTEGER)) return -1;
            return Objects.requireNonNull(livingEntity.getPersistentDataContainer().get(main.namespaced_keys.levelKey, PersistentDataType.INTEGER), "levelKey was null");
        }
    }

    /**
     * Un-level a mob.
     *
     * @param lmEntity levelled mob to un-level
     */
    public void removeLevel(@NotNull final LivingEntityWrapper lmEntity) {
        assert lmEntity.isLevelled();

        // remove PDC value
        synchronized (lmEntity.getLivingEntity().getPersistentDataContainer()) {
            if (lmEntity.getPDC().has(main.namespaced_keys.levelKey, PersistentDataType.INTEGER))
                lmEntity.getPDC().remove(main.namespaced_keys.levelKey);
            if (lmEntity.getPDC().has(main.namespaced_keys.overridenEntityNameKey, PersistentDataType.STRING))
                lmEntity.getPDC().remove(main.namespaced_keys.overridenEntityNameKey);
        }

        // reset attributes
        synchronized (main.attributeSyncObject) {
            for (Attribute attribute : Attribute.values()) {
                final AttributeInstance attInst = lmEntity.getLivingEntity().getAttribute(attribute);

                if (attInst == null) continue;

                attInst.getModifiers().clear();
            }
        }

        if (lmEntity.getLivingEntity() instanceof Creeper)
            ((Creeper) lmEntity.getLivingEntity()).setExplosionRadius(3);

        lmEntity.invalidateCache();

        // update nametag
        main.levelManager.updateNametag(lmEntity, lmEntity.getLivingEntity().getCustomName());
    }
}
