/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.managers;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.WeakHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import me.lokka30.levelledmobs.LevelInterface;
import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.LivingEntityInterface;
import me.lokka30.levelledmobs.compatibility.Compat1_17;
import me.lokka30.levelledmobs.customdrops.CustomDropItem;
import me.lokka30.levelledmobs.customdrops.CustomDropResult;
import me.lokka30.levelledmobs.customdrops.EquippedItemsInfo;
import me.lokka30.levelledmobs.customdrops.GroupLimits;
import me.lokka30.levelledmobs.events.MobPostLevelEvent;
import me.lokka30.levelledmobs.events.MobPreLevelEvent;
import me.lokka30.levelledmobs.events.SummonedMobPreLevelEvent;
import me.lokka30.levelledmobs.listeners.EntitySpawnListener;
import me.lokka30.levelledmobs.misc.Addition;
import me.lokka30.levelledmobs.misc.AdditionalLevelInformation;
import me.lokka30.levelledmobs.misc.DebugType;
import me.lokka30.levelledmobs.misc.LevellableState;
import me.lokka30.levelledmobs.misc.MinAndMaxHolder;
import me.lokka30.levelledmobs.misc.PickedUpEquipment;
import me.lokka30.levelledmobs.misc.StringReplacer;
import me.lokka30.levelledmobs.wrappers.LivingEntityWrapper;
import me.lokka30.levelledmobs.misc.MythicMobsMobInfo;
import me.lokka30.levelledmobs.result.NametagResult;
import me.lokka30.levelledmobs.misc.QueueItem;
import me.lokka30.levelledmobs.result.NBTApplyResult;
import me.lokka30.levelledmobs.result.PlayerHomeCheckResult;
import me.lokka30.levelledmobs.result.PlayerLevelSourceResult;
import me.lokka30.levelledmobs.result.PlayerNetherOrWorldSpawnResult;
import me.lokka30.levelledmobs.rules.CustomDropsRuleSet;
import me.lokka30.levelledmobs.rules.FineTuningAttributes;
import me.lokka30.levelledmobs.rules.HealthIndicator;
import me.lokka30.levelledmobs.rules.LevelledMobSpawnReason;
import me.lokka30.levelledmobs.rules.MobCustomNameStatus;
import me.lokka30.levelledmobs.rules.MobTamedStatus;
import me.lokka30.levelledmobs.rules.NametagVisibilityEnum;
import me.lokka30.levelledmobs.rules.PlayerLevellingOptions;
import me.lokka30.levelledmobs.rules.strategies.LevellingStrategy;
import me.lokka30.levelledmobs.rules.strategies.RandomLevellingStrategy;
import me.lokka30.levelledmobs.rules.strategies.SpawnDistanceStrategy;
import me.lokka30.levelledmobs.rules.strategies.YDistanceStrategy;
import me.lokka30.levelledmobs.util.MythicMobUtils;
import me.lokka30.levelledmobs.util.Utils;
import me.lokka30.levelledmobs.wrappers.SchedulerResult;
import me.lokka30.levelledmobs.wrappers.SchedulerWrapper;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.EnchantmentTarget;
import org.bukkit.entity.ChestedHorse;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Horse;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.AbstractHorseInventory;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Generates levels and manages other functions related to levelling mobs
 *
 * @author lokka30, stumper66, CoolBoy, Esophose, 7smile7, Shevchik, Hugo5551, limzikiki
 * @since 2.4.0
 */
@SuppressWarnings("deprecation")
public class LevelManager implements LevelInterface {

    public LevelManager(final @NotNull LevelledMobs main) {
        this.main = main;
        this.randomLevellingCache = new TreeMap<>();
        this.summonedOrSpawnEggs = new WeakHashMap<>();

        this.vehicleNoMultiplierItems = List.of(
            Material.SADDLE,
            Material.LEATHER_HORSE_ARMOR,
            Material.IRON_HORSE_ARMOR,
            Material.GOLDEN_HORSE_ARMOR,
            Material.DIAMOND_HORSE_ARMOR
        );

        this.FORCED_BLOCKED_ENTITY_TYPES = new HashSet<>(List.of(
            EntityType.AREA_EFFECT_CLOUD, EntityType.ARMOR_STAND, EntityType.ARROW, EntityType.BOAT,
            EntityType.DRAGON_FIREBALL, EntityType.DROPPED_ITEM, EntityType.EGG,
            EntityType.ENDER_CRYSTAL,
            EntityType.ENDER_PEARL, EntityType.ENDER_SIGNAL, EntityType.EXPERIENCE_ORB,
            EntityType.FALLING_BLOCK,
            EntityType.FIREBALL, EntityType.FIREWORK, EntityType.FISHING_HOOK,
            EntityType.ITEM_FRAME, EntityType.LEASH_HITCH, EntityType.LIGHTNING,
            EntityType.LLAMA_SPIT,
            EntityType.MINECART, EntityType.MINECART_CHEST, EntityType.MINECART_COMMAND,
            EntityType.MINECART_FURNACE,
            EntityType.MINECART_HOPPER, EntityType.MINECART_MOB_SPAWNER, EntityType.MINECART_TNT,
            EntityType.PAINTING,
            EntityType.PRIMED_TNT, EntityType.SMALL_FIREBALL, EntityType.SNOWBALL,
            EntityType.SPECTRAL_ARROW,
            EntityType.SPLASH_POTION, EntityType.THROWN_EXP_BOTTLE, EntityType.TRIDENT,
            EntityType.UNKNOWN,
            EntityType.WITHER_SKULL, EntityType.SHULKER_BULLET, EntityType.PLAYER
        ));
        if (main.getVerInfo().getMinecraftVersion() >= 1.17) {
            this.FORCED_BLOCKED_ENTITY_TYPES.addAll(Compat1_17.getForceBlockedEntityType());
        }

    }

    private final LevelledMobs main;
    private final List<Material> vehicleNoMultiplierItems;
    public final Map<LivingEntity, Object> summonedOrSpawnEggs;
    public static final Object summonedOrSpawnEggs_Lock = new Object();
    private boolean hasMentionedNBTAPI_Missing;
    public boolean doCheckMobHash;
    private final Map<String, RandomLevellingStrategy> randomLevellingCache;
    private Instant lastLEWCacheClearing;
    public EntitySpawnListener entitySpawnListener;

    /**
     * The following entity types *MUST NOT* be levellable.
     */
    public final HashSet<EntityType> FORCED_BLOCKED_ENTITY_TYPES;

    public void clearRandomLevellingCache() {
        this.randomLevellingCache.clear();
    }

    /**
     * This method generates a level for the mob. It utilises the levelling mode specified by the
     * administrator through the settings.yml configuration.
     * <p>
     * Thread-safety intended, but not tested.
     *
     * @param lmEntity the entity to generate a level for
     * @return a level for the entity
     */
    public int generateLevel(final @NotNull LivingEntityWrapper lmEntity) {
        return generateLevel(lmEntity, -1, -1);
    }

    /**
     * This method generates a level for the mob. It utilises the levelling mode specified by the
     * administrator through the settings.yml configuration.
     * <p>
     * Thread-safety intended, but not tested.
     *
     * @param lmEntity     the entity to generate a level for
     * @param minLevel_Pre the minimum level to be used for the mob
     * @param maxLevel_Pre the maximum level to be used for the mob
     * @return a level for the entity
     */
    public int generateLevel(final @NotNull LivingEntityWrapper lmEntity, final int minLevel_Pre,
        final int maxLevel_Pre) {
        int minLevel = minLevel_Pre;
        int maxLevel = maxLevel_Pre;

        if (minLevel == -1 || maxLevel == -1) {
            final MinAndMaxHolder levels = getMinAndMaxLevels(lmEntity);
            if (minLevel == -1) {
                minLevel = levels.min;
            }
            if (maxLevel == -1) {
                maxLevel = levels.max;
            }
        }

        final LevellingStrategy levellingStrategy = main.rulesManager.getRuleLevellingStrategy(
            lmEntity);

        if (levellingStrategy instanceof YDistanceStrategy
            || levellingStrategy instanceof SpawnDistanceStrategy) {
            return levellingStrategy.generateLevel(lmEntity, minLevel, maxLevel);
        }

        // if no levelling strategy was selected then we just use a random number between min and max

        if (minLevel == maxLevel) {
            return minLevel;
        }

        final RandomLevellingStrategy randomLevelling =
            (levellingStrategy instanceof RandomLevellingStrategy) ?
                (RandomLevellingStrategy) levellingStrategy : null;

        return generateRandomLevel(randomLevelling, minLevel, maxLevel);
    }

    private int generateRandomLevel(RandomLevellingStrategy randomLevelling, final int minLevel,
        final int maxLevel) {
        if (randomLevelling == null) {
            // used the caches defaults if it exists, otherwise add it to the cache
            if (this.randomLevellingCache.containsKey("default")) {
                randomLevelling = this.randomLevellingCache.get("default");
            } else {
                randomLevelling = new RandomLevellingStrategy();
                this.randomLevellingCache.put("default", randomLevelling);
            }
        } else {
            // used the caches one if it exists, otherwise add it to the cache
            final String checkName = String.format("%s-%s: %s", minLevel, maxLevel,
                randomLevelling);

            if (this.randomLevellingCache.containsKey(checkName)) {
                randomLevelling = this.randomLevellingCache.get(checkName);
            } else {
                randomLevelling.populateWeightedRandom(minLevel, maxLevel);
                this.randomLevellingCache.put(checkName, randomLevelling);
            }
        }

        return randomLevelling.generateLevel(minLevel, maxLevel);
    }

    public @NotNull PlayerLevelSourceResult getPlayerLevelSourceNumber(final @Nullable Player player,
        final @NotNull LivingEntityWrapper lmEntity ,final @NotNull String variableToUse) {
        if (player == null) {
            return new PlayerLevelSourceResult(1);
        }

        double origLevelSource;
        String homeNameUsed = "spawn";

        if ("%level%".equalsIgnoreCase(variableToUse)) {
            origLevelSource = player.getLevel();
        } else if ("%exp%".equalsIgnoreCase(variableToUse)) {
            origLevelSource = player.getExp();
        } else if ("%exp-to-level%".equalsIgnoreCase(variableToUse)) {
            origLevelSource = player.getExpToLevel();
        } else if ("%total-exp%".equalsIgnoreCase(variableToUse)) {
            origLevelSource = player.getTotalExperience();
        } else if ("%world_time_ticks%".equalsIgnoreCase(variableToUse)) {
            origLevelSource = player.getWorld().getTime();
        } else if ("%home_distance%".equalsIgnoreCase(variableToUse)
            || "%home_distance_with_bed%".equalsIgnoreCase(variableToUse)) {
            final boolean allowBed = "%home_distance_with_bed%".equalsIgnoreCase(variableToUse);
            PlayerNetherOrWorldSpawnResult netherOrWorldSpawnResult;
            final PlayerHomeCheckResult result = ExternalCompatibilityManager.getPlayerHomeLocation(
                player, allowBed);
            if (result.homeNameUsed != null) {
                homeNameUsed = result.homeNameUsed;
            }

            Location useLocation = result.location;
            if (useLocation == null || useLocation.getWorld() != player.getWorld()) {
                netherOrWorldSpawnResult = Utils.getPortalOrWorldSpawn(main, player);
                useLocation = netherOrWorldSpawnResult.location();
                if (netherOrWorldSpawnResult.isWorldPortalLocation()) {
                    homeNameUsed = "world_portal";
                } else if (netherOrWorldSpawnResult.isNetherPortalLocation()) {
                    homeNameUsed = "nether_portal";
                } else {
                    homeNameUsed= "spawn";
                }
            }

            if (result.resultMessage != null) {
                DebugManager.log(DebugType.PLAYER_LEVELLING, lmEntity, () -> result.resultMessage);
            }

            origLevelSource = useLocation.distance(player.getLocation());
        } else if ("%bed_distance%".equalsIgnoreCase(variableToUse)) {
            Location useLocation = player.getBedSpawnLocation();
            homeNameUsed = "bed";

            if (useLocation == null || useLocation.getWorld() != player.getWorld()) {
                final PlayerNetherOrWorldSpawnResult result = Utils.getPortalOrWorldSpawn(main,
                    player);
                useLocation = result.location();
                if (result.isWorldPortalLocation()) {
                    homeNameUsed = "world_portal";
                } else if (result.isNetherPortalLocation()) {
                    homeNameUsed = "nether_portal";
                } else {
                    homeNameUsed = "spawn";
                }
            }

            origLevelSource = useLocation.distance(player.getLocation());
        } else {
            boolean usePlayerLevel = false;
            String PAPIResult = null;

            if (ExternalCompatibilityManager.hasPapiInstalled()) {
                PAPIResult = ExternalCompatibilityManager.getPapiPlaceholder(player, variableToUse);
                if (Utils.isNullOrEmpty(PAPIResult)) {
                    final Location l = player.getLocation();
                    DebugManager.log(DebugType.PLAYER_LEVELLING, lmEntity, () -> String.format(
                        "Got blank result for '%s' from PAPI. Player %s at %s,%s,%s in %s",
                        variableToUse, player.getName(), l.getBlockX(), l.getBlockY(),
                        l.getBlockZ(), player.getWorld().getName()));
                    usePlayerLevel = true;
                }
            } else {
                Utils.logger.warning(
                    "PlaceHolderAPI is not installed, unable to get variable " + variableToUse);
                usePlayerLevel = true;
            }

            if (usePlayerLevel) {
                origLevelSource = player.getLevel();
            } else {
                final Location l = player.getLocation();
                if (Utils.isNullOrEmpty(PAPIResult)) {
                    origLevelSource = player.getLevel();
                    DebugManager.log(DebugType.PLAYER_LEVELLING, lmEntity, () -> String.format(
                        "Got blank result for '%s' from PAPI. Player %s at %s,%s,%s in %s",
                        variableToUse, player.getName(), l.getBlockX(), l.getBlockY(),
                        l.getBlockZ(), player.getWorld().getName()));
                } else {
                    if (Utils.isDouble(PAPIResult)){
                        try {
                            origLevelSource = Double.parseDouble(PAPIResult);
                        } catch (Exception ignored) {
                            origLevelSource = player.getLevel();
                        }
                    }
                    else{
                        final PlayerLevelSourceResult result = new PlayerLevelSourceResult(PAPIResult);
                        result.homeNameUsed = homeNameUsed;
                        return result;
                    }
                }
            }
        }

        final PlayerLevelSourceResult sourceResult = new PlayerLevelSourceResult((int) Math.round(origLevelSource));
        final Integer maxRandomVariance = main.rulesManager.getRuleMaxRandomVariance(lmEntity);

        if (maxRandomVariance != null){
            sourceResult.randomVarianceResult = ThreadLocalRandom.current().nextInt(0, maxRandomVariance + 1);
            if (ThreadLocalRandom.current().nextBoolean()){
                sourceResult.randomVarianceResult *= -1;
            }
        }

        sourceResult.homeNameUsed = homeNameUsed;
        return sourceResult;
    }

    public MinAndMaxHolder getMinAndMaxLevels(final @NotNull LivingEntityInterface lmInterface) {
        // final EntityType entityType, final boolean isAdultEntity, final String worldName
        // if called from summon command then lmEntity is null

        int minLevel = main.rulesManager.getRuleMobMinLevel(lmInterface);
        int maxLevel = main.rulesManager.getRuleMobMaxLevel(lmInterface);

        if (main.configUtils.playerLevellingEnabled && lmInterface instanceof final LivingEntityWrapper lmEntity &&
            ((LivingEntityWrapper) lmInterface).getPlayerForLevelling() != null) {
            final PlayerLevellingOptions options = main.rulesManager.getRulePlayerLevellingOptions(
                    lmEntity);

            MinAndMaxHolder playerLevellingResults = null;
            if (options != null && options.getEnabled()){
                playerLevellingResults = options.getPlayerLevels(lmEntity);
            }

            if (playerLevellingResults != null) {
                // this will only be false if no tiers were met and there was a cap specified
                if (playerLevellingResults.useMin) minLevel = playerLevellingResults.min;
                maxLevel = playerLevellingResults.max;
            }
        }

        // this will prevent an unhandled exception:
        minLevel = Math.max(minLevel, 1);
        maxLevel = Math.max(maxLevel, 1);
        minLevel = Math.min(minLevel, maxLevel);

        return new MinAndMaxHolder(minLevel, maxLevel);
    }

    // This sets the levelled currentDrops on a levelled mob that just died.
    public void setLevelledItemDrops(final LivingEntityWrapper lmEntity,
        final @NotNull List<ItemStack> currentDrops, final boolean disableItemBoost) {

        final int vanillaDrops = currentDrops.size();
        // this accomodates chested animals, saddles and armor on ridable creatures
        final List<ItemStack> dropsToMultiply = getDropsToMultiply(lmEntity, currentDrops);
        final List<ItemStack> customDrops = new LinkedList<>();
        currentDrops.clear();

        final boolean doNotMultiplyDrops = disableItemBoost ||
                main.rulesManager.getRuleCheckIfNoDropMultiplierEntitiy(lmEntity);
        boolean hasOverride = false;

        if (lmEntity.lockedCustomDrops != null || main.rulesManager.getRuleUseCustomDropsForMob(lmEntity).useDrops) {
            // custom drops also get multiplied in the custom drops handler
            final CustomDropResult dropResult = main.customDropsHandler.getCustomItemDrops(lmEntity,
                customDrops, false);

            final MythicMobsMobInfo mmInfo = MythicMobUtils.getMythicMobInfo(lmEntity);
            if (mmInfo != null && mmInfo.preventOtherDrops) {
                hasOverride = true;
            }

            if (dropResult.hasOverride()) {
                hasOverride = true;
            }

            if (hasOverride) {
                removeVanillaDrops(lmEntity, dropsToMultiply);
            }
        }

        int additionUsed = 0;

        if (!doNotMultiplyDrops && !dropsToMultiply.isEmpty()) {
            // Get currentDrops added per level valu
            final float additionValue = main.mobDataManager.getAdditionsForLevel(lmEntity,
                Addition.CUSTOM_ITEM_DROP, 2.0f);
            if (additionValue == Float.MIN_VALUE) {
                DebugManager.log(DebugType.SET_LEVELLED_ITEM_DROPS, lmEntity, () -> String.format(
                    "&7Mob: &b%s&7, mob-lvl: &b%s&7, removing any drops present",
                    lmEntity.getNameIfBaby(), lmEntity.getMobLevel()));
                currentDrops.clear();
                return;
            }

            final int addition = BigDecimal.valueOf(additionValue)
                .setScale(0, RoundingMode.HALF_DOWN).intValueExact(); // truncate double to int
            additionUsed = addition;

            // Modify current drops
            for (final ItemStack currentDrop : dropsToMultiply) {
                multiplyDrop(lmEntity, currentDrop, addition, false);
            }
        }

        if (!customDrops.isEmpty()) {
            currentDrops.addAll(customDrops);
        }
        if (!dropsToMultiply.isEmpty()) {
            currentDrops.addAll(dropsToMultiply);
        }
        final String nameWithOverride = hasOverride ?
            lmEntity.getNameIfBaby() + " (override)" : lmEntity.getNameIfBaby();
        final int additionUsedFinal = additionUsed;
        DebugManager.log(DebugType.SET_LEVELLED_ITEM_DROPS, lmEntity, () -> String.format(
            "&7Mob: &b%s&7, mob-lvl: &b%s&7, vanilla drops: &b%s&7, all drops: &b%s&7, addition: &b%s&7.",
            nameWithOverride, lmEntity.getMobLevel(), vanillaDrops, currentDrops.size(),
            additionUsedFinal));
    }

    public void multiplyDrop(final LivingEntityWrapper lmEntity,
        final @NotNull ItemStack currentDrop, final double addition, final boolean isCustomDrop) {
        final int oldAmount = currentDrop.getAmount();

        if (isCustomDrop || main.mobDataManager.isLevelledDropManaged(
            lmEntity.getLivingEntity().getType(), currentDrop.getType())) {
            int useAmount = (int)Math.round(currentDrop.getAmount() + ((double)currentDrop.getAmount() * addition));
            if (useAmount > currentDrop.getMaxStackSize()) {
                useAmount = currentDrop.getMaxStackSize();
            }
            currentDrop.setAmount(useAmount);
            DebugManager.log(DebugType.SET_LEVELLED_ITEM_DROPS, lmEntity, () -> String.format(
                "&7Drop: &b%s&7, old amount: &b%s&7, addition value: &b%s&7, new amount: &b%s&7.",
                currentDrop.getType(), oldAmount, addition, currentDrop.getAmount()));
        } else {
            DebugManager.log(DebugType.SET_LEVELLED_ITEM_DROPS, lmEntity, () -> "&7Item was unmanaged.");
        }
    }

    private @NotNull List<ItemStack> getDropsToMultiply(@NotNull final LivingEntityWrapper lmEntity,
        final @NotNull List<ItemStack> drops) {
        final List<ItemStack> results = new ArrayList<>(drops.size());
        results.addAll(drops);

        // we only need to check for chested animals and 'vehicles' since they can have saddles and armor
        // those items shouldn't get multiplied

        if (lmEntity.getLivingEntity() instanceof ChestedHorse
            && ((ChestedHorse) lmEntity.getLivingEntity()).isCarryingChest()) {
            final AbstractHorseInventory inv = ((ChestedHorse) lmEntity.getLivingEntity()).getInventory();
            final ItemStack[] chestItems = inv.getContents();
            // look thru the animal's inventory for leather. That is the only item that will get duplicated
            for (final ItemStack item : chestItems) {
                if (item.getType() == Material.LEATHER) {
                    return List.of(item);
                }
            }

            // if we made it here it didn't drop leather so don't return anything
            results.clear();
            return results;
        }

        if (!(lmEntity.getLivingEntity() instanceof Vehicle)) {
            return results;
        }

        for (int i = results.size() - 1; i >= 0; i--) {
            // remove horse armor or saddles
            final ItemStack item = results.get(i);
            if (this.vehicleNoMultiplierItems.contains(item.getType())) // saddle or horse armor
            {
                results.remove(i);
            }
        }

        return results;
    }

    public void removeVanillaDrops(final @NotNull LivingEntityWrapper lmEntity,
        final List<ItemStack> drops) {
        boolean hadSaddle = false;
        final List<ItemStack> itemsToKeep = new LinkedList<>();

        if (lmEntity.getLivingEntity() instanceof ChestedHorse
            && ((ChestedHorse) lmEntity.getLivingEntity()).isCarryingChest()) {
            final AbstractHorseInventory inv = ((ChestedHorse) lmEntity.getLivingEntity()).getInventory();
            Collections.addAll(itemsToKeep, inv.getContents());
            itemsToKeep.add(new ItemStack(Material.CHEST));
        } else if (lmEntity.getLivingEntity() instanceof Vehicle) {
            for (final ItemStack itemStack : drops) {
                if (itemStack.getType() == Material.SADDLE) {
                    hadSaddle = true;
                    break;
                }
            }
        }

        if (main.getVerInfo().getIsRunningPaper()){
            final List<ItemStack> pickedUpItems = new PickedUpEquipment(lmEntity).getMobPickedUpItems();

            for (final ItemStack mobItem : drops){
                for (final ItemStack foundItem : pickedUpItems){
                    if (mobItem.isSimilar(foundItem)){
                        itemsToKeep.add(mobItem);
                        break;
                    }
                }
            }
        }

        drops.clear();
        drops.addAll(itemsToKeep);
        if (hadSaddle) {
            drops.add(new ItemStack(Material.SADDLE));
        }
    }

    //Calculates the XP dropped when a levellable creature dies.
    public int getLevelledExpDrops(final @NotNull LivingEntityWrapper lmEntity, final double xp) {
        if (lmEntity.isLevelled()) {
            final float dropAddition = main.mobDataManager.getAdditionsForLevel(lmEntity,
                Addition.CUSTOM_XP_DROP, 3.0f);
            double newXp = 0;

            if (dropAddition == Float.MIN_VALUE){
                DebugManager.log(DebugType.SET_LEVELLED_XP_DROPS, lmEntity, () -> String.format(
                        "&7Mob: &b%s&7: lvl: &b%s&7, xp-vanilla: &b%s&7, new-xp: &b0&7",
                                lmEntity.getNameIfBaby(), lmEntity.getMobLevel(), xp));
                return 0;
            }

            if (dropAddition > -1) {
                newXp = Math.round(xp + (xp * dropAddition));
            }

            final int newXpFinal = (int) newXp;
            DebugManager.log(DebugType.SET_LEVELLED_XP_DROPS, lmEntity, () -> String.format(
                    "&7Mob: &b%s&7: lvl: &b%s&7, xp-vanilla: &b%s&7, new-xp: &b%s&7",
                    lmEntity.getNameIfBaby(), lmEntity.getMobLevel(), xp, newXpFinal));
            return (int)newXp;
        } else {
            return (int)xp;
        }
    }

    public @NotNull NametagResult getNametag(final @NotNull LivingEntityWrapper lmEntity, final boolean isDeathNametag) {
        return getNametag(lmEntity, isDeathNametag, false);
    }

    public @NotNull NametagResult getNametag(final @NotNull LivingEntityWrapper lmEntity, final boolean isDeathNametag, boolean preserveMobName) {
        StringReplacer nametag;
        String customDeathMessage = null;
        if (isDeathNametag) {
            nametag = new StringReplacer(main.rulesManager.getRuleNametagCreatureDeath(lmEntity));
        } else {
            checkLockedNametag(lmEntity);

            final String nametagText = lmEntity.lockedNametag == null || lmEntity.lockedNametag.isEmpty() ?
                main.rulesManager.getRuleNametag(lmEntity) :
                lmEntity.lockedNametag;
            nametag = new StringReplacer(nametagText);
        }

        if ("disabled".equalsIgnoreCase(nametag.text) || "none".equalsIgnoreCase(nametag.text)) {
            return new NametagResult(null);
        }

        if (isDeathNametag){
            final String deathMessage = main.rulesManager.getDeathMessage(lmEntity);
            if (deathMessage != null && !deathMessage.isEmpty()){
                nametag = new StringReplacer(deathMessage.replace("%death_nametag%", nametag.text));
                final Player player = lmEntity.associatedPlayer;
                nametag.replace("%player%", player != null ? player.getName() + "&r" : "");
                nametag.text = replaceStringPlaceholders(nametag.text, lmEntity, true, player, preserveMobName);
                preserveMobName = true;

                customDeathMessage = nametag.contains("{DisplayName}") ?
                        main.rulesManager.getRuleNametagCreatureDeath(lmEntity) :
                        nametag.text;
            }
        }

        // ignore if 'disabled'
        if (nametag.isEmpty()) {
            final boolean useCustomNameForNametags = main.helperSettings.getBoolean(main.settingsCfg,
                    "use-customname-for-mob-nametags");
            if (useCustomNameForNametags) {
                return new NametagResult(lmEntity.getTypeName());
            } else {
                return new NametagResult(lmEntity.getLivingEntity().getCustomName()); // CustomName can be null, that is meant to be the case.
            }
        }
        if (!lmEntity.isLevelled()) {
            nametag.text = "";
        }

        return updateNametag(lmEntity, nametag, preserveMobName, customDeathMessage);
    }

    public @NotNull NametagResult updateNametag(final @NotNull LivingEntityWrapper lmEntity, @NotNull StringReplacer nametag,
                                                final boolean preserveMobName,
                                                final String customDeathMessage) {
        if (nametag.isEmpty()) {
            final NametagResult result = new NametagResult(nametag.text);
            result.setDeathMessage(customDeathMessage);
            return result;
        }

        checkLockedNametag(lmEntity);
        final String overridenName = lmEntity.lockedOverrideName == null ?
            main.rulesManager.getRuleEntityOverriddenName(lmEntity, false) :
            lmEntity.lockedOverrideName;

        replaceStringPlaceholders(nametag, lmEntity, false, null, preserveMobName);

        String indicatorStr = "";
        String colorOnly = "";

        if (nametag.text.contains("%health-indicator%") ||
                nametag.text.contains("%health-indicator-color%")) {
            final HealthIndicator indicator = lmEntity.getMainInstance().rulesManager.getRuleNametagIndicator(lmEntity);

            if (indicator != null){
                final HealthIndicator.HealthIndicatorResult result = indicator.formatHealthIndicator(lmEntity);
                indicatorStr = result.formattedString + "&r";
                colorOnly = result.colorOnly;
            }
        }

        nametag.replace("%health-indicator%", indicatorStr);
        nametag.replace("%health-indicator-color%", colorOnly);

        if (nametag.text.contains("%") && ExternalCompatibilityManager.hasPapiInstalled()) {
            nametag.text = ExternalCompatibilityManager.getPapiPlaceholder(null, nametag.text);
        }

        final NametagResult result = new NametagResult(nametag.text);
        // this field is only used for sending nametags to client
        result.overriddenName = overridenName;
        result.setDeathMessage(customDeathMessage);
        result.killerMob =lmEntity.getLivingEntity();

        return result;
    }

    private void checkLockedNametag(final @NotNull LivingEntityWrapper lmEntity) {
        synchronized (lmEntity.getPDC()) {
            Integer doLockSettings;
            if (lmEntity.getPDC()
                    .has(main.namespacedKeys.lockSettings, PersistentDataType.INTEGER)) {
                doLockSettings = lmEntity.getPDC()
                        .get(main.namespacedKeys.lockSettings, PersistentDataType.INTEGER);
                if (doLockSettings == null || doLockSettings != 1) {
                    return;
                }
            } else {
                return;
            }

            if (lmEntity.getPDC()
                    .has(main.namespacedKeys.lockedNametag, PersistentDataType.STRING)) {
                lmEntity.lockedNametag = lmEntity.getPDC()
                        .get(main.namespacedKeys.lockedNametag, PersistentDataType.STRING);
            }
            if (lmEntity.getPDC()
                    .has(main.namespacedKeys.lockedNameOverride, PersistentDataType.STRING)) {
                lmEntity.lockedOverrideName = lmEntity.getPDC()
                        .get(main.namespacedKeys.lockedNameOverride, PersistentDataType.STRING);
            }
        }
    }

    public @NotNull String replaceStringPlaceholders(final @NotNull String text,
        final @NotNull LivingEntityWrapper lmEntity, final boolean usePAPI,
        final @Nullable Player player, final boolean preserveMobName) {
        return replaceStringPlaceholders(
                new StringReplacer(text),
                lmEntity,
                usePAPI,
                player,
                preserveMobName
        );
    }

    private @NotNull String replaceStringPlaceholders(final @NotNull StringReplacer text,
        final @NotNull LivingEntityWrapper lmEntity, final boolean usePAPI,
        final @Nullable Player player, final boolean preserveMobName) {

        final double maxHealth = getMobAttributeValue(lmEntity);
        final double entityHealth = getMobHealth(lmEntity);
        final int entityHealthRounded = entityHealth < 1.0 && entityHealth > 0.0 ?
            1 : (int) Utils.round(entityHealth);
        final String roundedMaxHealth = String.valueOf(Utils.round(maxHealth));
        final String roundedMaxHealthInt = String.valueOf((int) Utils.round(maxHealth));
        final double percentHealthTemp = Math.round(entityHealth / maxHealth * 100.0);
        final int percentHealth = percentHealthTemp < 1.0 ? 1 : (int) percentHealthTemp;
        final String playerId = player != null ? player.getUniqueId().toString() : "";
        final String playerName = player != null ? player.getName() : "";

        String tieredPlaceholder = main.rulesManager.getRuleTieredPlaceholder(lmEntity);
        if (tieredPlaceholder == null) {
            tieredPlaceholder = "";
        }

        // replace them placeholders ;)
        text.replace("%mob-lvl%", lmEntity.getMobLevel());
        text.replace("%entity-name%",
            Utils.capitalize(lmEntity.getNameIfBaby().replace("_", " ")));
        text.replace("%entity-health%", Utils.round(entityHealth));
        text.replace("%entity-health-rounded%", entityHealthRounded);
        text.replace("%entity-max-health%", roundedMaxHealth);
        text.replace("%entity-max-health-rounded%", roundedMaxHealthInt);
        text.replace("%heart_symbol%", "❤");
        text.replace("%tiered%", tieredPlaceholder);
        text.replace("%wg_region%", lmEntity.getWGRegionName());
        text.replace("%world%", lmEntity.getWorldName());
        text.replaceIfExists("%location%", () -> String.format("%s %s %s",
                lmEntity.getLivingEntity().getLocation().getBlockX(),
                lmEntity.getLivingEntity().getLocation().getBlockY(),
                lmEntity.getLivingEntity().getLocation().getBlockZ()));
        text.replace("%health%-percent%", percentHealth);
        text.replace("%x%", lmEntity.getLivingEntity().getLocation().getBlockX());
        text.replace("%y%", lmEntity.getLivingEntity().getLocation().getBlockY());
        text.replace("%z%", lmEntity.getLivingEntity().getLocation().getBlockZ());
        text.replace("%player-uuid%", playerId);
        text.replace("%player%", playerName);
        text.replaceIfExists("%displayname%", () -> {
            final boolean useCustomNameForNametags = main.helperSettings.getBoolean(
                    main.settingsCfg, "use-customname-for-mob-nametags");
            final String overridenName = lmEntity.lockedOverrideName == null ?
                    main.rulesManager.getRuleEntityOverriddenName(lmEntity, false) :
                    lmEntity.lockedOverrideName;

            boolean hasOverridenName = (overridenName != null && !overridenName.isEmpty());
            String useDisplayname = overridenName;

            if (preserveMobName)
                useDisplayname =  "{DisplayName}";
            else if (!hasOverridenName)
                useDisplayname = Utils.capitalize(lmEntity.getTypeName().replaceAll("_", " "));

            if (lmEntity.getLivingEntity().getCustomName() != null && !useCustomNameForNametags) {
                useDisplayname = lmEntity.getLivingEntity().getCustomName();
            }
            return useDisplayname;
        });

        if (usePAPI && text.contains("%") && ExternalCompatibilityManager.hasPapiInstalled()) {
            text.text = ExternalCompatibilityManager.getPapiPlaceholder(player, text.text);
        }

        return text.text;
    }

    public void updateNametagWithDelay(final @NotNull LivingEntityWrapper lmEntity) {
        final SchedulerWrapper scheduler = new SchedulerWrapper(lmEntity.getLivingEntity(), () -> {
            updateNametag(lmEntity);
            lmEntity.free();
        });

        lmEntity.inUseCount.getAndIncrement();
        scheduler.runDelayed(1L);
    }

    public void updateNametag(final LivingEntityWrapper lmEntity) {
        final boolean preserveMobName = !main.nametagQueueManager.nametagSenderHandler.isUsingProtocolLib;
        final NametagResult nametag = getNametag(lmEntity, false, preserveMobName);

        final QueueItem queueItem = new QueueItem(
            lmEntity,
            nametag,
            lmEntity.getLivingEntity().getWorld().getPlayers()
        );

        main.nametagQueueManager.addToQueue(queueItem);
    }

    public void updateNametag(final @NotNull LivingEntityWrapper lmEntity, final NametagResult nametag,
        final List<Player> players) {
        main.nametagQueueManager.addToQueue(new QueueItem(lmEntity, nametag, players));
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

    public SchedulerResult nametagAutoUpdateTask;
    private SchedulerResult nametagTimerTask;

    public void startNametagAutoUpdateTask() {
        Utils.logger.info("&fTasks: &7Starting async nametag auto update task...");

        final long period = main.helperSettings.getInt(main.settingsCfg, "async-task-update-period",
            6); // run every ? seconds.
        this.doCheckMobHash = main.helperSettings.getBoolean(main.settingsCfg, "check-mob-hash", true);

        if (main.getVerInfo().getIsRunningFolia()){
           final Consumer<ScheduledTask> bgThread = scheduledTask -> {
               if (Bukkit.getOnlinePlayers().isEmpty()) return;
               Player firstPlayer = null;
               for (final Player player : Bukkit.getOnlinePlayers()){
                   firstPlayer = player;
                   break;
               }
               if (firstPlayer == null) return;

               final Consumer<ScheduledTask> task = scheduledTask1 -> {
                   checkLEWCache();
                   final Map<Player, List<Entity>> entitiesPerPlayer = enumerateNearbyEntities();
                   if (entitiesPerPlayer != null) {
                       runNametagCheck_aSync(entitiesPerPlayer);
                   }
               };

               firstPlayer.getScheduler().run(main, task, null);
            };

            nametagTimerTask = new SchedulerResult(org.bukkit.Bukkit.getAsyncScheduler().runAtFixedRate(main, bgThread, 0, period, TimeUnit.SECONDS));
        }
        else{
            final Runnable runnable = () -> {
                final Map<Player, List<Entity>> entitiesPerPlayer = enumerateNearbyEntities();

                if (entitiesPerPlayer != null) {
                    final Runnable runnable2 = () -> {
                        checkLEWCache();
                        runNametagCheck_aSync(entitiesPerPlayer);
                    };
                    Bukkit.getScheduler().runTaskAsynchronously(main, runnable2);
                }
            };
            nametagTimerTask = new SchedulerResult(Bukkit.getScheduler().runTaskTimer(main, runnable, 0, 20 * period));
        }
    }

    private void checkLEWCache(){
        if (lastLEWCacheClearing == null){
            lastLEWCacheClearing = Instant.now();
            return;
        }

        final long duration = lastLEWCacheClearing.until(Instant.now(), ChronoUnit.MILLIS);
        final long configDuration = main.helperSettings.getIntTimeUnitMS(
                main.settingsCfg, "lew-cache-clear-period", 180000L);

        if (duration >= configDuration){
            final DecimalFormat formatter = new DecimalFormat("#,###");

            DebugManager.log(DebugType.DEVELOPER_LEW_CACHE, () ->
                    "Reached %s ms, clearing LEW cache, %s".formatted(
                            formatter.format(configDuration), LivingEntityWrapper.getLEWDebug()));

            lastLEWCacheClearing = Instant.now();
            LivingEntityWrapper.clearCache();
        }
    }

    private @Nullable Map<Player, List<Entity>> enumerateNearbyEntities(){
        final Map<Player, List<Entity>> entitiesPerPlayer = new LinkedHashMap<>();
        final int checkDistance = main.helperSettings.getInt(main.settingsCfg,
                "async-task-max-blocks-from-player", 100);

        for (final Player player : Bukkit.getOnlinePlayers()) {
            final List<Entity> entities = player.getNearbyEntities(checkDistance,
                    checkDistance, checkDistance);
            entitiesPerPlayer.put(player, entities);
        }
        if (entitiesPerPlayer.isEmpty()) {
            return null;
        }
        else{
            return entitiesPerPlayer;
        }
    }

    public void startNametagTimer() {
        final SchedulerWrapper scheduler = new SchedulerWrapper(() ->
                main.nametagTimerChecker.checkNametags());
        scheduler.runTaskTimerAsynchronously(0, 1000);
    }

    private void runNametagCheck_aSync(final @NotNull Map<Player, List<Entity>> entitiesPerPlayer) {
        final Map<LivingEntityWrapper, List<Player>> entityToPlayer = new LinkedHashMap<>();

        final SchedulerWrapper scheduler = new SchedulerWrapper(() -> {
            for (final Player player : entitiesPerPlayer.keySet()) {
                for (final Entity entity : entitiesPerPlayer.get(player)) {
                    checkEntity(entity, player, entityToPlayer);
                }
            }
        });

        if (main.getVerInfo().getIsRunningFolia()){
            Entity firstEntity = null;
            for (final Player player : entitiesPerPlayer.keySet()) {
                for (final Entity entity : entitiesPerPlayer.get(player)) {
                    firstEntity = entity;
                    break;
                }
                if (firstEntity != null) break;
            }
            scheduler.entity = firstEntity;
        }

        scheduler.runDirectlyInBukkit = true;
        scheduler.run();

        for (final Map.Entry<LivingEntityWrapper, List<Player>> entry : entityToPlayer.entrySet()) {
            final LivingEntityWrapper lmEntity = entry.getKey();
            if (entityToPlayer.containsKey(lmEntity)) {
                checkEntityForPlayerLevelling(lmEntity, entry.getValue());
            }

            lmEntity.free();
        }
    }

    private void checkEntity(final @NotNull Entity entity, final @NotNull Player player,
                             final @NotNull Map<LivingEntityWrapper, List<Player>> entityToPlayer){
        if (!entity.isValid()) {
            return; // async task, entity can despawn whilst it is running
        }

        // Mob must be a livingentity that is ...living.
        if (!(entity instanceof LivingEntity) || entity instanceof Player
                || !entity.isValid()) {
            return;
        }
        // this is mostly so for spawner mobs and spawner egg mobs as they have a 20 tick delay in before proessing
        if (entity.getTicksLived() < 30) {
            return;
        }

        boolean wrapperHasReference = false;
        final LivingEntityWrapper lmEntity = LivingEntityWrapper.getInstance(
                (LivingEntity) entity, main);
        lmEntity.associatedPlayer = player;
        if (doCheckMobHash && Utils.checkIfMobHashChanged(lmEntity)) {
            lmEntity.reEvaluateLevel = true;
            lmEntity.isRulesForceAll = true;
            lmEntity.wasPreviouslyLevelled = lmEntity.isLevelled();
        }

        if (lmEntity.isLevelled()) {
            boolean skipLevelling = (
                    lmEntity.getSpawnReason() == LevelledMobSpawnReason.LM_SPAWNER ||
                            lmEntity.getSpawnReason() == LevelledMobSpawnReason.LM_SUMMON
            );
            if (main.configUtils.playerLevellingEnabled && !lmEntity.isRulesForceAll && !checkIfReadyForRelevelling(lmEntity)){
                skipLevelling = true;
            }
            if (main.configUtils.playerLevellingEnabled && !skipLevelling) {
                final boolean hasKey = entityToPlayer.containsKey(lmEntity);
                final List<Player> players = hasKey ?
                        entityToPlayer.get(lmEntity) : new LinkedList<>();
                players.add(player);
                if (!hasKey) {
                    entityToPlayer.put(lmEntity, players);
                }
                wrapperHasReference = true;
            }

            if (lmEntity.getLivingEntity() == null) {
                return;
            }
            final List<NametagVisibilityEnum> nametagVisibilityEnums = main.rulesManager.getRuleCreatureNametagVisbility(
                    lmEntity);
            final long nametagVisibleTime = lmEntity.getNametagCooldownTime();
            if (nametagVisibleTime > 0L &&
                    nametagVisibilityEnums.contains(NametagVisibilityEnum.TARGETED) &&
                    lmEntity.getLivingEntity().hasLineOfSight(player)) {

                if (lmEntity.playersNeedingNametagCooldownUpdate == null) {
                    lmEntity.playersNeedingNametagCooldownUpdate = new HashSet<>();
                }
                lmEntity.playersNeedingNametagCooldownUpdate.add(player);
            }

            checkLevelledEntity(lmEntity, player);
        } else {
            final boolean wasBabyMob;
            synchronized (lmEntity.getLivingEntity().getPersistentDataContainer()) {
                wasBabyMob = lmEntity.getPDC()
                        .has(main.namespacedKeys.wasBabyMobKey, PersistentDataType.INTEGER);
            }
            if (lmEntity.getLivingEntity()
                    != null) { // a hack to prevent a null exception that was reported
                final LevellableState levellableState = main.levelInterface.getLevellableState(
                        lmEntity);
                if (!lmEntity.isBabyMob() &&
                        wasBabyMob &&
                        levellableState == LevellableState.ALLOWED) {
                    // if the mob was a baby at some point, aged and now is eligable for levelling, we'll apply a level to it now
                    DebugManager.log(DebugType.ENTITY_MISC, lmEntity, () ->
                            "&b" + lmEntity.getTypeName()
                                    + " &7was a baby and is now an adult, applying levelling rules");

                    main.mobsQueueManager.addToQueue(new QueueItem(lmEntity, null));
                } else if (levellableState == LevellableState.ALLOWED) {
                    main.mobsQueueManager.addToQueue(new QueueItem(lmEntity, null));
                }
            }
        }

        if (!wrapperHasReference) {
            lmEntity.free();
        }
    }

    private boolean checkIfReadyForRelevelling(final @NotNull LivingEntityWrapper lmEntity){
        final PlayerLevellingOptions opts = main.rulesManager.getRulePlayerLevellingOptions(lmEntity);
        if (opts == null || opts.preserveEntityTime == null) {
            return true;
        }

        if (!lmEntity.getPDC().has(main.namespacedKeys.lastDamageTime, PersistentDataType.LONG)){
            return true;
        }

        final Long lastLevelledTime = lmEntity.getPDC().get(main.namespacedKeys.lastDamageTime, PersistentDataType.LONG);
        if (lastLevelledTime == null) {
            return true;
        }

        final Instant levelledTime = Instant.ofEpochMilli(lastLevelledTime);
        return Utils.getMillisecondsFromInstant(levelledTime) > opts.preserveEntityTime;
    }

    private void checkEntityForPlayerLevelling(final @NotNull LivingEntityWrapper lmEntity,
        final @NotNull List<Player> players) {
        final LivingEntity mob = lmEntity.getLivingEntity();
        final List<Player> sortedPlayers = players.stream()
                .filter(p -> mob.getWorld().equals(p.getWorld()))
                .filter(p -> p.getGameMode() != GameMode.SPECTATOR)
                .map(p -> Map.entry(mob.getLocation().distanceSquared(p.getLocation()), p))
                .sorted(Comparator.comparingDouble(Map.Entry::getKey))
                .map(Map.Entry::getValue).toList();

        Player closestPlayer = null;
        for (final Player player : sortedPlayers) {
            if (ExternalCompatibilityManager.isMobOfCitizens(player)) {
                continue;
            }

            closestPlayer = player;
            break;
        }

        if (closestPlayer == null) {
            return;
        }

        // if player has been logged in for less than 5 seconds then ignore
        final Instant logonTime = main.companion.getRecentlyJoinedPlayerLogonTime(closestPlayer);
        if (logonTime != null) {
            if (Utils.getMillisecondsFromInstant(logonTime) < 5000L) {
                return;
            }
            main.companion.removeRecentlyJoinedPlayer(closestPlayer);
        }

        if (doesMobNeedRelevelling(lmEntity, closestPlayer)) {
            lmEntity.pendingPlayerIdToSet = closestPlayer.getUniqueId().toString();
            lmEntity.setPlayerForLevelling(closestPlayer);
            lmEntity.reEvaluateLevel = true;
            main.mobsQueueManager.addToQueue(new QueueItem(lmEntity, null));
        }
    }

    private void checkLevelledEntity(final @NotNull LivingEntityWrapper lmEntity,
        final @NotNull Player player) {
        if (lmEntity.getLivingEntity() == null || !lmEntity.getLivingEntity().isValid()) {
            return;
        }
        final double maxDistance = Math.pow(128,
            2); // square the distance we are using Location#distanceSquared. This is because it is faster than Location#distance since it does not need to sqrt which is taxing on the CPU.
        final Location location = player.getLocation();

        if (lmEntity.isRulesForceAll){
            main.mobsQueueManager.addToQueue(new QueueItem(lmEntity, null));
        }
        else if (lmEntity.getLivingEntity().getCustomName() != null
            && main.rulesManager.getRuleMobCustomNameStatus(lmEntity)
            == MobCustomNameStatus.NOT_NAMETAGGED) {
            // mob has a nametag but is levelled so we'll remove it
            main.levelInterface.removeLevel(lmEntity);
        } else if (lmEntity.isMobTamed()
            && main.rulesManager.getRuleMobTamedStatus(lmEntity) == MobTamedStatus.NOT_TAMED) {
            // mob is tamed with a level but the rules don't allow it, remove the level
            main.levelInterface.removeLevel(lmEntity);
        } else if (lmEntity.getLivingEntity().isValid() &&
            !main.helperSettings.getBoolean(main.settingsCfg, "use-customname-for-mob-nametags",
                false) &&
            location.getWorld() != null &&
            location.getWorld().equals(lmEntity.getWorld()) &&
            lmEntity.getLocation().distanceSquared(location) <= maxDistance) {
            //if within distance, update nametag.
            final boolean preserveMobName = !main.nametagQueueManager.nametagSenderHandler.isUsingProtocolLib;
            final NametagResult nametag = main.levelManager.getNametag(lmEntity, false, preserveMobName);
            main.nametagQueueManager.addToQueue(
                new QueueItem(lmEntity, nametag, List.of(player)));
        }
    }

    private boolean doesMobNeedRelevelling(final @NotNull LivingEntityWrapper lmEntity,
        final @NotNull Player player) {
        final LivingEntity mob = lmEntity.getLivingEntity();

        if (main.playerLevellingMinRelevelTime > 0L && main.playerLevellingEntities.containsKey(
            mob)) {
            final Instant lastCheck = main.playerLevellingEntities.get(mob);
            final Duration duration = Duration.between(lastCheck, Instant.now());

            if (duration.toMillis() < main.playerLevellingMinRelevelTime) {
                return false;
            }
        }

        final String playerId;
        if (main.playerLevellingMinRelevelTime > 0L) {
            main.playerLevellingEntities.put(mob, Instant.now());
        }

        synchronized (mob.getPersistentDataContainer()) {
            if (!mob.getPersistentDataContainer()
                .has(main.namespacedKeys.playerLevellingId, PersistentDataType.STRING)) {
                return true;
            }

            playerId = mob.getPersistentDataContainer()
                .get(main.namespacedKeys.playerLevellingId, PersistentDataType.STRING);
        }

        if (playerId == null && main.playerLevellingMinRelevelTime <= 0L) {
            return true;
        } else if (playerId == null || !player.getUniqueId().toString().equals(playerId)) {
            return true;
        }

        final PlayerLevellingOptions opts = main.rulesManager.getRulePlayerLevellingOptions(lmEntity);
        if (player.getUniqueId().toString().equals(playerId) && opts != null && opts.getRecheckPlayers()){
            final String previousResult = lmEntity.getPDC().get(main.namespacedKeys.playerLevellingSourceNumber, PersistentDataType.STRING);
            if (previousResult == null) {
                return true;
            }
            final String variableToUse =
                    Utils.isNullOrEmpty(opts.variable) ? "%level%" : opts.variable;
            PlayerLevelSourceResult result = getPlayerLevelSourceNumber(player, lmEntity, variableToUse);
            final String sourceNumberStr = result.isNumericResult ?
                    String.valueOf(result.numericResult) : result.stringResult;

            return !previousResult.equals(sourceNumberStr);
        }

        return !player.getUniqueId().toString().equals(playerId);
    }

    public void stopNametagAutoUpdateTask() {
        if (!main.nametagQueueManager.hasNametagSupport()) {
            return;
        }

        if (nametagAutoUpdateTask != null && !nametagAutoUpdateTask.isCancelled()) {
            Utils.logger.info("&fTasks: &7Stopping async nametag auto update task...");
            nametagAutoUpdateTask.cancelTask();
        }

        if (nametagTimerTask != null && !nametagTimerTask.isCancelled()) {
            nametagTimerTask.cancelTask();
        }
    }

    private void applyLevelledAttributes(final @NotNull LivingEntityWrapper lmEntity,
        final @NotNull Addition addition) {
        assert lmEntity.isLevelled();

        // This functionality should be added into the enum.
        final Attribute attribute;
        switch (addition) {
            case ATTRIBUTE_MAX_HEALTH -> attribute = Attribute.GENERIC_MAX_HEALTH;
            case ATTRIBUTE_ATTACK_DAMAGE -> attribute = Attribute.GENERIC_ATTACK_DAMAGE;
            case ATTRIBUTE_MOVEMENT_SPEED -> attribute = Attribute.GENERIC_MOVEMENT_SPEED;
            case ATTRIBUTE_HORSE_JUMP_STRENGTH -> attribute = Attribute.HORSE_JUMP_STRENGTH;
            case ATTRIBUTE_ARMOR_BONUS -> attribute = Attribute.GENERIC_ARMOR;
            case ATTRIBUTE_ARMOR_TOUGHNESS -> attribute = Attribute.GENERIC_ARMOR_TOUGHNESS;
            case ATTRIBUTE_KNOCKBACK_RESISTANCE -> attribute = Attribute.GENERIC_KNOCKBACK_RESISTANCE;
            case ATTRIBUTE_FLYING_SPEED -> attribute = Attribute.GENERIC_FLYING_SPEED;
            case ATTRIBUTE_ATTACK_KNOCKBACK -> attribute = Attribute.GENERIC_ATTACK_KNOCKBACK;
            case ATTRIBUTE_FOLLOW_RANGE -> attribute = Attribute.GENERIC_FOLLOW_RANGE;
            case ATTRIBUTE_ZOMBIE_SPAWN_REINFORCEMENTS -> {
                if (lmEntity.getSpawnReason() == LevelledMobSpawnReason.REINFORCEMENTS) {
                    return;
                }
                attribute = Attribute.ZOMBIE_SPAWN_REINFORCEMENTS;
            }
            default -> throw new IllegalStateException(
                    "Addition must be an Attribute, if so, it has not been considered in this method");
        }

        // Attr instance for the mob
        final AttributeInstance attrInst = lmEntity.getLivingEntity().getAttribute(attribute);

        // Don't try to apply an addition to their attribute if they don't have it
        if (attrInst == null) {
            return;
        }

        // Apply additions
        main.mobDataManager.setAdditionsForLevel(lmEntity, attribute, addition);
    }

    private void applyCreeperBlastRadius(final @NotNull LivingEntityWrapper lmEntity) {
        final Creeper creeper = (Creeper) lmEntity.getLivingEntity();

        final FineTuningAttributes tuning = main.rulesManager.getFineTuningAttributes(lmEntity);
        if (tuning == null) {
            // make sure creeper explosion is at vanilla defaults incase of a relevel, etc
            if (creeper.getExplosionRadius() != 3) {
                creeper.setExplosionRadius(3);
            }
            DebugManager.log(DebugType.CREEPER_BLAST_RADIUS, lmEntity,
                    () -> String.format("lvl: %s, mulp: null, result: 3",
                    lmEntity.getMobLevel()));
            return;
        }

        final int maxRadius = main.rulesManager.getRuleCreeperMaxBlastRadius(lmEntity);
        final double damage = main.mobDataManager.getAdditionsForLevel(lmEntity,
            Addition.CREEPER_BLAST_DAMAGE, 3);
        if (damage == 0.0){
            return;
        }

        int blastRadius = 3 + (int) Math.floor(damage);

        if (blastRadius > maxRadius) {
            blastRadius = maxRadius;
        } else if (blastRadius < 0) {
            blastRadius = 0;
        }

        final int blastRadiusFinal = blastRadius;
        DebugManager.log(DebugType.CREEPER_BLAST_RADIUS, lmEntity, () -> String.format(
                "lvl: %s, mulp: %s, max: %s, result: %s",
                lmEntity.getMobLevel(), Utils.round(damage, 3), maxRadius, blastRadiusFinal));

        if (blastRadius < 0) {
            blastRadius = 0;
        }

        creeper.setExplosionRadius(blastRadius);
    }

    /**
     * Add configured equipment to the levelled mob LivingEntity MUST be a levelled mob
     * <p>
     * Thread-safety unknown.
     *
     * @param lmEntity a levelled mob to apply levelled equipment to
     * @param level    the level of the levelled mob
     */
    private void applyLevelledEquipment(final @NotNull LivingEntityWrapper lmEntity,
        final int level) {
        if (!lmEntity.isLevelled()) {
            // if you summon a mob and it isn't levelled due to a config rule (baby zombies exempt for example)
            // then we'll be here with a non-levelled entity
            return;
        }
        if (level < 1) {
            return;
        }

        // Custom Drops must be enabled.
        final CustomDropsRuleSet customDropsRuleSet = main.rulesManager.getRuleUseCustomDropsForMob(lmEntity);
        if (!customDropsRuleSet.useDrops) {
            return;
        }

        final SchedulerWrapper scheduler = new SchedulerWrapper(() -> {
            applyLevelledEquipment_NonAsync(lmEntity, customDropsRuleSet);
            lmEntity.free();
        });

        scheduler.entity = lmEntity.getLivingEntity();
        lmEntity.inUseCount.getAndIncrement();
        scheduler.run();
    }

    private void applyLevelledEquipment_NonAsync(final @NotNull LivingEntityWrapper lmEntity, final CustomDropsRuleSet customDropsRuleSet) {
        final MythicMobsMobInfo mmInfo = MythicMobUtils.getMythicMobInfo(lmEntity);
        if (mmInfo != null && mmInfo.preventRandomEquipment) {
            return;
        }

        final List<ItemStack> items = new LinkedList<>();
        final CustomDropResult dropResult = main.customDropsHandler.getCustomItemDrops(lmEntity,
            items, true);
        if (items.isEmpty()) {
            return;
        }

        final EntityEquipment equipment = lmEntity.getLivingEntity().getEquipment();
        if (equipment == null) {
            return;
        }

        if (lmEntity.lockEntitySettings && !customDropsRuleSet.useDropTableIds.isEmpty()){
            final String customDrops = String.join(";", customDropsRuleSet.useDropTableIds);
            lmEntity.getPDC().set(main.namespacedKeys.lockedDropRules, PersistentDataType.STRING, customDrops);
            if (customDropsRuleSet.chunkKillOptions.getDisableVanillaDrops())
                lmEntity.getPDC().set(main.namespacedKeys.lockedDropRulesOverride, PersistentDataType.INTEGER, 1);
        }

        boolean hadMainItem = false;
        boolean hadPlayerHead = false;
        final EquippedItemsInfo equippedItemsInfo = new EquippedItemsInfo();
        final Map<String, Integer> equippedCountPerGroup = new TreeMap<>();
        int equippedSoFar = 0;

        Collections.shuffle(dropResult.stackToItem());

        for (final Map.Entry<ItemStack, CustomDropItem> pair : dropResult.stackToItem()) {
            final ItemStack itemStack = pair.getKey();
            final Material material = itemStack.getType();
            final CustomDropItem item = pair.getValue();
            final GroupLimits groupLimits = main.customDropsHandler.getGroupLimits(item);
            final boolean hasEquipLimits = item.hasGroupId() && groupLimits != null && groupLimits.hasCapEquipped();

            if (hasEquipLimits){
                if (equippedCountPerGroup.containsKey(item.groupId)) {
                    equippedSoFar = equippedCountPerGroup.get(item.groupId);
                }

                if (groupLimits.hasReachedCapEquipped(equippedSoFar)){
                    DebugManager.log(DebugType.GROUP_LIMITS, lmEntity, () -> String.format(
                            "Reached equip limit of %s, mob: %s, item: %s, group: %s",
                            groupLimits.capEquipped, lmEntity.getNameIfBaby(), material, item.groupId));
                    continue;
                }
            }

            if (EnchantmentTarget.ARMOR_FEET.includes(material)) {
                equipment.setBoots(itemStack, true);
                equipment.setBootsDropChance(0);
                equippedItemsInfo.boots = item;
            } else if (EnchantmentTarget.ARMOR_LEGS.includes(material)) {
                equipment.setLeggings(itemStack, true);
                equipment.setLeggingsDropChance(0);
                equippedItemsInfo.leggings = item;
            } else if (EnchantmentTarget.ARMOR_TORSO.includes(material)) {
                equipment.setChestplate(itemStack, true);
                equipment.setChestplateDropChance(0);
                equippedItemsInfo.chestplate = item;
            } else if (EnchantmentTarget.ARMOR_HEAD.includes(material)
                || material.name().endsWith("_HEAD") || item.equipOnHelmet
                && !hadPlayerHead) {
                equipment.setHelmet(itemStack, true);
                equipment.setHelmetDropChance(0);
                equippedItemsInfo.helmet = item;
                if (material == Material.PLAYER_HEAD) {
                    hadPlayerHead = true;
                }
            } else {
                if (!hadMainItem) {
                    equipment.setItemInMainHand(itemStack);
                    equipment.setItemInMainHandDropChance(0);
                    equippedItemsInfo.mainHand = item;
                    hadMainItem = true;
                } else if (item.equipOffhand) {
                    equipment.setItemInOffHand(itemStack);
                    equipment.setItemInOffHandDropChance(0);
                    equippedItemsInfo.offhand = item;
                }
            }

            equippedSoFar++;

            if (hasEquipLimits){
                equippedCountPerGroup.put(item.groupId, equippedSoFar);
            }
        }

        main.customDropsHandler.addEntityEquippedItems(lmEntity.getLivingEntity(),
            equippedItemsInfo);
    }

    private double getMobAttributeValue(final @NotNull LivingEntityWrapper lmEntity) {
        double result = 0.0;
        synchronized (main.attributeSyncObject) {
            final AttributeInstance attrib = lmEntity.getLivingEntity()
                .getAttribute(Attribute.GENERIC_MAX_HEALTH);
            if (attrib != null) {
                result = attrib.getValue();
            }
        }

        return result;
    }

    private double getMobHealth(final @NotNull LivingEntityWrapper lmEntity) {
        final double result;
        synchronized (main.attributeSyncObject) {
            result = lmEntity.getLivingEntity().getHealth();
        }

        return result;
    }

    public @NotNull LevellableState getLevellableState(final @NotNull LivingEntityInterface lmInterface) {
        /*
        Certain entity types are force-blocked, regardless of what the user has configured.
        This is also ran in getLevellableState(EntityType), however it is important that this is ensured
        before all other checks are made.
         */
        if (FORCED_BLOCKED_ENTITY_TYPES.contains(lmInterface.getEntityType())) {
            return LevellableState.DENIED_FORCE_BLOCKED_ENTITY_TYPE;
        }

        if (lmInterface.getApplicableRules().isEmpty()) {
            return LevellableState.DENIED_NO_APPLICABLE_RULES;
        }

        if (!main.rulesManager.getRuleIsMobAllowedInEntityOverride(lmInterface)) {
            return LevellableState.DENIED_CONFIGURATION_BLOCKED_ENTITY_TYPE;
        }

        if (main.rulesManager.getRuleMobMaxLevel(lmInterface) < 1) {
            return LevellableState.DENIED_LEVEL_0;
        }

        if (!(lmInterface instanceof final LivingEntityWrapper lmEntity)) {
            return LevellableState.ALLOWED;
        }

        final LevellableState externalCompatResult = ExternalCompatibilityManager.checkAllExternalCompats(
            lmEntity);
        if (externalCompatResult != LevellableState.ALLOWED) {
            return externalCompatResult;
        }

        if (lmEntity.isMobOfExternalType()) {
            lmEntity.invalidateCache();

            if (!main.rulesManager.getRuleIsMobAllowedInEntityOverride(lmInterface)) {
                return LevellableState.DENIED_CONFIGURATION_BLOCKED_ENTITY_TYPE;
            }
        }

        /*
        Check 'No Level Conditions'
         */
        // Nametagged mobs.
        if (lmEntity.getLivingEntity().getCustomName() != null &&
            main.rulesManager.getRuleMobCustomNameStatus(lmEntity)
                == MobCustomNameStatus.NOT_NAMETAGGED) {
            return LevellableState.DENIED_CONFIGURATION_CONDITION_NAMETAGGED;
        }

        return LevellableState.ALLOWED;
    }

    /**
     * This method applies a level to the target mob.
     * <p>
     * You can run this method on a mob regardless if they are already levelled or not.
     * <p>
     * This method DOES NOT check if it is LEVELLABLE. It is assumed that plugins make sure this is
     * the case (unless they intend otherwise).
     * <p>
     * It is highly recommended to leave bypassLimits = false, unless the desired behaviour is to
     * override the user-configured limits.
     * <p>
     * Thread-safety intended, but not tested.
     *
     * @param lmEntity                   target mob
     * @param level                      the level the mob should have
     * @param isSummoned                 if the mob was spawned by LevelledMobs, not by the server
     * @param bypassLimits               whether LM should disregard max level, etc.
     * @param additionalLevelInformation used to determine the source event
     */
    public void applyLevelToMob(final @NotNull LivingEntityWrapper lmEntity, int level,
        final boolean isSummoned, final boolean bypassLimits,
        final @NotNull HashSet<AdditionalLevelInformation> additionalLevelInformation) {
        // this thread runs in async.  if adding any functions make sure they can be run in this fashion

        if (level <= 0) {
            level = generateLevel(lmEntity);
        }
        lmEntity.setMobPrelevel(level);

        assert
            bypassLimits || isSummoned || getLevellableState(lmEntity) == LevellableState.ALLOWED;
        boolean skipLMNametag = false;

        if (lmEntity.getLivingEntity() == null) {
            return;
        }

        if (lmEntity.getLivingEntity().isInsideVehicle()
            && main.rulesManager.getRulePassengerMatchLevel(lmEntity)
            && lmEntity.getLivingEntity().getVehicle() instanceof LivingEntity) {
            // entity is a passenger. grab the level from the "vehicle" entity
            final LivingEntityWrapper vehicle = LivingEntityWrapper.getInstance(
                (LivingEntity) lmEntity.getLivingEntity().getVehicle(), main);
            if (vehicle.isLevelled()) {
                level = vehicle.getMobLevel();
                lmEntity.setMobPrelevel(level);
            }

            vehicle.free();
        }

        if (isSummoned) {
            lmEntity.setSpawnReason(LevelledMobSpawnReason.LM_SUMMON, true);
            final SummonedMobPreLevelEvent summonedMobPreLevelEvent = new SummonedMobPreLevelEvent(
                lmEntity.getLivingEntity(), level);
            Bukkit.getPluginManager().callEvent(summonedMobPreLevelEvent);

            if (summonedMobPreLevelEvent.isCancelled()) {
                return;
            }
        } else {
            final MobPreLevelEvent mobPreLevelEvent = new MobPreLevelEvent(
                lmEntity.getLivingEntity(), level, MobPreLevelEvent.LevelCause.NORMAL,
                additionalLevelInformation);

            Bukkit.getPluginManager().callEvent(mobPreLevelEvent);
            if (mobPreLevelEvent.isCancelled()) {
                return;
            }

            level = mobPreLevelEvent.getLevel();
            lmEntity.setMobPrelevel(level);
            if (!mobPreLevelEvent.getShowLM_Nametag()) {
                skipLMNametag = true;
                lmEntity.setShouldShowLM_Nametag(false);
            }
        }

        boolean hasNoLevelKey = false;
        if (!isSummoned) {
            synchronized (lmEntity.getLivingEntity().getPersistentDataContainer()) {
                hasNoLevelKey = lmEntity.getPDC()
                    .has(main.namespacedKeys.noLevelKey, PersistentDataType.STRING);
            }
        }

        if (hasNoLevelKey) {
            DebugManager.log(DebugType.APPLY_LEVEL_RESULT, lmEntity, false, () ->
                    "Entity &b" + lmEntity.getTypeName() + "&7 had &bnoLevelKey&7 attached");
            return;
        }

        synchronized (lmEntity.getLivingEntity().getPersistentDataContainer()) {
            lmEntity.getPDC().set(main.namespacedKeys.levelKey, PersistentDataType.INTEGER, level);
            lmEntity.getPDC().set(main.namespacedKeys.mobHash, PersistentDataType.STRING, main.rulesManager.getCurrentRulesHash());
        }
        lmEntity.invalidateCache();

        final List<String> nbtDatas = lmEntity.nbtData != null && !lmEntity.nbtData.isEmpty() ?
            lmEntity.nbtData : main.rulesManager.getRuleNbtData(lmEntity);

        if (!nbtDatas.isEmpty() && !ExternalCompatibilityManager.hasNbtApiInstalled()) {
            if (!hasMentionedNBTAPI_Missing) {
                Utils.logger.warning(
                    "NBT Data has been specified in customdrops.yml but required plugin NBTAPI is not installed!");
                hasMentionedNBTAPI_Missing = true;
            }
            nbtDatas.clear();
        }

        lmEntity.lockEntitySettings = main.rulesManager.getRuleDoLockEntity(lmEntity);
        if (lmEntity.lockEntitySettings && lmEntity.isNewlySpawned) {
            lmEntity.lockedNametag = main.rulesManager.getRuleNametag(lmEntity);
            lmEntity.lockedOverrideName = main.rulesManager.getRuleEntityOverriddenName(lmEntity,
                true);
        }

        final boolean doSkipLMNametag = skipLMNametag;

        final SchedulerWrapper scheduler = new SchedulerWrapper(() -> {
            applyLevelToMob2(lmEntity, nbtDatas, doSkipLMNametag);
            lmEntity.free();
        });

        lmEntity.inUseCount.getAndIncrement();
        scheduler.entity = lmEntity.getLivingEntity();
        scheduler.run();

        final MobPostLevelEvent.LevelCause levelCause =
            isSummoned ? MobPostLevelEvent.LevelCause.SUMMONED
                : MobPostLevelEvent.LevelCause.NORMAL;
        Bukkit.getPluginManager()
            .callEvent(new MobPostLevelEvent(lmEntity, levelCause, additionalLevelInformation));

        final StringBuilder sb = new StringBuilder();
        sb.append("entity: ");
        sb.append(lmEntity.getLivingEntity().getName());
        if (lmEntity.isBabyMob()) {
            sb.append(" (baby)");
        }
        sb.append(", world: ");
        sb.append(lmEntity.getWorldName());
        sb.append(", level: ");
        sb.append(level);
        if (isSummoned) {
            sb.append(" (summoned)");
        }
        if (bypassLimits) {
            sb.append(" (limit bypass)");
        }

        DebugManager.log(DebugType.APPLY_LEVEL_RESULT, lmEntity, true, sb::toString);
    }

    private void applyLevelToMob2(final @NotNull LivingEntityWrapper lmEntity,
        final @NotNull List<String> nbtDatas, final boolean doSkipLMNametag){
        applyAttribs(lmEntity, nbtDatas);

        if (!doSkipLMNametag) {
            main.levelManager.updateNametagWithDelay(lmEntity);
        }
        main.levelManager.applyLevelledEquipment(lmEntity, lmEntity.getMobLevel());
    }

    private void applyAttribs(final @NotNull LivingEntityWrapper lmEntity, final @NotNull List<String> nbtDatas){
        synchronized (main.attributeSyncObject) {
            main.levelManager.applyLevelledAttributes(lmEntity,
                    Addition.ATTRIBUTE_ATTACK_DAMAGE);
            main.levelManager.applyLevelledAttributes(lmEntity,
                    Addition.ATTRIBUTE_MAX_HEALTH);
            main.levelManager.applyLevelledAttributes(lmEntity,
                    Addition.ATTRIBUTE_MOVEMENT_SPEED);
            main.levelManager.applyLevelledAttributes(lmEntity,
                    Addition.ATTRIBUTE_ARMOR_BONUS);
            main.levelManager.applyLevelledAttributes(lmEntity,
                    Addition.ATTRIBUTE_ARMOR_TOUGHNESS);
            main.levelManager.applyLevelledAttributes(lmEntity,
                    Addition.ATTRIBUTE_ATTACK_KNOCKBACK);
            main.levelManager.applyLevelledAttributes(lmEntity,
                    Addition.ATTRIBUTE_FLYING_SPEED);
            main.levelManager.applyLevelledAttributes(lmEntity,
                    Addition.ATTRIBUTE_KNOCKBACK_RESISTANCE);
            main.levelManager.applyLevelledAttributes(lmEntity,
                    Addition.ATTRIBUTE_FOLLOW_RANGE);

            if (lmEntity.getLivingEntity() instanceof Zombie) {
                main.levelManager.applyLevelledAttributes(lmEntity,
                        Addition.ATTRIBUTE_ZOMBIE_SPAWN_REINFORCEMENTS);
            } else if (lmEntity.getLivingEntity() instanceof Horse) {
                main.levelManager.applyLevelledAttributes(lmEntity,
                        Addition.ATTRIBUTE_HORSE_JUMP_STRENGTH);
            }
        }

        if (lmEntity.lockEntitySettings) {
            lmEntity.getPDC()
                    .set(main.namespacedKeys.lockSettings, PersistentDataType.INTEGER, 1);
            if (lmEntity.lockedNametag != null) {
                lmEntity.getPDC()
                        .set(main.namespacedKeys.lockedNametag, PersistentDataType.STRING,
                                lmEntity.lockedNametag);
            }
            if (lmEntity.lockedOverrideName != null) {
                lmEntity.getPDC()
                        .set(main.namespacedKeys.lockedNameOverride, PersistentDataType.STRING,
                                lmEntity.lockedOverrideName);
            }
        }

        if (!nbtDatas.isEmpty()) {
            boolean hadSuccess = false;
            final List<NBTApplyResult> allResults = new LinkedList<>();

            for (final String nbtData : nbtDatas) {
                final NBTApplyResult result = NBTManager.applyNBT_Data_Mob(lmEntity,
                        nbtData);
                if (result.hadException()) {
                    if (lmEntity.summonedSender == null) {
                        Utils.logger.warning(String.format(
                                "Error applying NBT data '%s' to %s. Exception message: %s",
                                nbtData, lmEntity.getNameIfBaby(), result.exceptionMessage));
                    } else {
                        lmEntity.summonedSender.sendMessage(
                                "Error applying NBT data to " + lmEntity.getNameIfBaby()
                                        + ". Exception message: " + result.exceptionMessage);
                    }
                } else {
                    hadSuccess = true;
                    allResults.add(result);
                }

            }
            if (hadSuccess) {
                DebugManager.log(DebugType.NBT_APPLICATION, lmEntity, true, () ->
                        "Applied NBT data to '" + lmEntity.getNameIfBaby() +
                                "'. " + getNBT_DebugMessage(allResults));
            }
        }

        if (lmEntity.getLivingEntity() instanceof Creeper) {
            main.levelManager.applyCreeperBlastRadius(lmEntity);
        }
    }

    private @NotNull String getNBT_DebugMessage(final @NotNull List<NBTApplyResult> results) {
        final StringBuilder sb = new StringBuilder();

        for (final NBTApplyResult result : results) {
            if (result.objectsAdded == null) {
                continue;
            }

            for (int i = 0; i < result.objectsAdded.size(); i++) {
                if (i > 0) {
                    sb.append(", ");
                } else {
                    sb.append("added: ");
                }

                sb.append(result.objectsAdded.get(i));
            }
        }

        for (final NBTApplyResult result : results) {
            if (result.objectsUpdated == null) {
                continue;
            }

            for (int i = 0; i < result.objectsUpdated.size(); i++) {
                if (i > 0 || !sb.isEmpty()) {
                    sb.append(", ");
                }
                if (i == 0) {
                    sb.append("updated: ");
                }

                sb.append(result.objectsUpdated.get(i));
            }
        }

        for (final NBTApplyResult result : results) {
            if (result.objectsRemoved == null) {
                continue;
            }

            for (int i = 0; i < result.objectsRemoved.size(); i++) {
                if (i > 0 || !sb.isEmpty()) {
                    sb.append(", ");
                }
                if (i == 0) {
                    sb.append("removed: ");
                }

                sb.append(result.objectsRemoved.get(i));
            }
        }

        return sb.isEmpty() ?
                "" : sb.toString();
    }

    /**
     * Check if a LivingEntity is a levelled mob or not. This is determined *after*
     * MobPreLevelEvent.
     * <p>
     * Thread-safety intended, but not tested.
     *
     * @param livingEntity living entity to check
     * @return if the mob is levelled or not
     */
    public boolean isLevelled(final @NotNull LivingEntity livingEntity) {
        boolean hadError = false;
        boolean succeeded = false;
        boolean isLevelled = false;

        for (int i = 0; i < 2; i++) {
            try {
                synchronized (livingEntity.getPersistentDataContainer()) {
                    isLevelled = livingEntity.getPersistentDataContainer()
                        .has(main.namespacedKeys.levelKey, PersistentDataType.INTEGER);
                }
                succeeded = true;
                break;
            } catch (ConcurrentModificationException ignored) {
                hadError = true;
                try {
                    Thread.sleep(10);
                } catch (InterruptedException ignored2) {
                    return false;
                }
            }
        }

        if (hadError) {
            if (succeeded) {
                Utils.logger.warning(
                    "Got ConcurrentModificationException in LevelManager checking entity isLevelled, succeeded on retry");
            } else {
                Utils.logger.warning(
                    "Got ConcurrentModificationException (2x) in LevelManager checking entity isLevelled");
            }
        }

        return isLevelled;
    }

    /**
     * Retrieve the level of a levelled mob.
     * <p>
     * Thread-safety intended, but not tested.
     *
     * @param livingEntity the levelled mob to get the level of
     * @return the mob's level
     */
    public int getLevelOfMob(final @NotNull LivingEntity livingEntity) {
        synchronized (livingEntity.getPersistentDataContainer()) {
            if (!livingEntity.getPersistentDataContainer()
                .has(main.namespacedKeys.levelKey, PersistentDataType.INTEGER)) {
                return -1;
            }
            return Objects.requireNonNull(livingEntity.getPersistentDataContainer()
                    .get(main.namespacedKeys.levelKey, PersistentDataType.INTEGER),
                "levelKey was null");
        }
    }

    /**
     * Un-level a mob.
     *
     * @param lmEntity levelled mob to un-level
     */
    public void removeLevel(final @NotNull LivingEntityWrapper lmEntity) {
        assert lmEntity.isLevelled();

        // remove PDC value
        synchronized (lmEntity.getLivingEntity().getPersistentDataContainer()) {
            if (lmEntity.getPDC().has(main.namespacedKeys.levelKey, PersistentDataType.INTEGER)) {
                lmEntity.getPDC().remove(main.namespacedKeys.levelKey);
            }
            if (lmEntity.getPDC()
                .has(main.namespacedKeys.overridenEntityNameKey, PersistentDataType.STRING)) {
                lmEntity.getPDC().remove(main.namespacedKeys.overridenEntityNameKey);
            }
        }

        // reset attributes
        synchronized (main.attributeSyncObject) {
            for (final Attribute attribute : Attribute.values()) {
                final AttributeInstance attInst = lmEntity.getLivingEntity()
                    .getAttribute(attribute);

                if (attInst == null) {
                    continue;
                }

                final Enumeration<AttributeModifier> existingMods = Collections.enumeration(attInst.getModifiers());
                while (existingMods.hasMoreElements()){
                    final AttributeModifier existingMod = existingMods.nextElement();

                    if (main.mobDataManager.vanillaMultiplierNames.containsKey(existingMod.getName())) continue;
                    attInst.removeModifier(existingMod);
                }
            }
        }

        if (lmEntity.getLivingEntity() instanceof Creeper) {
            ((Creeper) lmEntity.getLivingEntity()).setExplosionRadius(3);
        }

        lmEntity.invalidateCache();

        // update nametag
        main.levelManager.updateNametag(lmEntity);
    }
}
