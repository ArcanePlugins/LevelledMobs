package me.lokka30.levelledmobs.managers;

import me.lokka30.levelledmobs.LevelInterface;
import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.LivingEntityInterface;
import me.lokka30.levelledmobs.customdrops.CustomDropResult;
import me.lokka30.levelledmobs.listeners.EntitySpawnListener;
import me.lokka30.levelledmobs.misc.*;
import me.lokka30.levelledmobs.rules.MobCustomNameStatusEnum;
import me.lokka30.levelledmobs.rules.MobTamedStatusEnum;
import me.lokka30.levelledmobs.rules.strategies.LevellingStrategy;
import me.lokka30.levelledmobs.rules.strategies.SpawnDistanceStrategy;
import me.lokka30.levelledmobs.rules.strategies.YDistanceStrategy;
import me.lokka30.microlib.MessageUtils;
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

import javax.annotation.Nonnull;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Generates levels and manages other functions related to levelling mobs
 *
 * @author lokka30, CoolBoy, Esophose, 7smile7,
 * wShevchik, Hugo5551, limzikiki
 */
public class LevelManager {

    private final LevelledMobs main;
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
    public Location summonedLocation;
    public EntityType summonedEntityType;

    public final static int maxCreeperBlastRadius = 100;
    public EntitySpawnListener entitySpawnListener;

    public LevelManager(final LevelledMobs main) {
        this.main = main;

        levelKey = new NamespacedKey(main, "level");
        spawnReasonKey = new NamespacedKey(main, "spawnReason");
        noLevelKey = new NamespacedKey(main, "noLevel");
        wasBabyMobKey = new NamespacedKey(main, "wasBabyMob");
        overridenEntityNameKey = new NamespacedKey(main, "overridenEntityName");
        this.summonedEntityType = EntityType.UNKNOWN;

        this.vehicleNoMultiplierItems = Arrays.asList(
                Material.SADDLE,
                Material.LEATHER_HORSE_ARMOR,
                Material.IRON_HORSE_ARMOR,
                Material.GOLDEN_HORSE_ARMOR,
                Material.DIAMOND_HORSE_ARMOR
        );
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

        final LevellingStrategy levellingStrategy = main.rulesManager.getRule_LevellingStrategy(lmEntity);

        if (levellingStrategy instanceof YDistanceStrategy || levellingStrategy instanceof SpawnDistanceStrategy)
            return levellingStrategy.generateLevel(lmEntity, minLevel, maxLevel);

        // if no levelling strategy was selected then we just use a random number between min and max

        if (minLevel == maxLevel)
            return minLevel;

        final LevelNumbersWithBias levelNumbersWithBias = main.rulesManager.getRule_LowerMobLevelBiasFactor(lmEntity, minLevel, maxLevel);
        if (levelNumbersWithBias != null)
            return levelNumbersWithBias.getNumberWithinLimits();

        return ThreadLocalRandom.current().nextInt(minLevel, maxLevel + 1);
    }

    public int[] getMinAndMaxLevels(final @NotNull LivingEntityInterface lmInterface) {
        // final EntityType entityType, final boolean isAdultEntity, final String worldName
        // if called from summon command then lmEntity is null

        int minLevel = main.rulesManager.getRule_MobMinLevel(lmInterface);
        int maxLevel = main.rulesManager.getRule_MobMaxLevel(lmInterface);

        // world guard regions take precedence over any other min / max settings
        // livingEntity is null if passed from summon mobs command
        if (ExternalCompatibilityManager.hasWorldGuardInstalled() && main.worldGuardManager.checkRegionFlags(lmInterface)) {
            final int[] levels = generateWorldGuardRegionLevel(lmInterface);
            if (levels[0] > -1) minLevel = levels[0];
            if (levels[1] > -1) maxLevel = levels[1];
        }

        // this will prevent an unhandled exception:
        if (minLevel > maxLevel) minLevel = maxLevel;

        return new int[]{ minLevel, maxLevel };
    }

    public int[] generateWorldGuardRegionLevel(final LivingEntityInterface lmInterface) {
        return main.worldGuardManager.getRegionLevel(lmInterface);
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

    @Nonnull
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
    public int getLevelledExpDrops(@NotNull final LivingEntityWrapper lmEntity, final int xp) {
        if (lmEntity.isLevelled()) {
            final int newXp = (int) Math.round(xp + (xp * main.mobDataManager.getAdditionsForLevel(lmEntity, Addition.CUSTOM_XP_DROP, 3.0)));
            Utils.debugLog(main, DebugType.SET_LEVELLED_XP_DROPS, lmEntity.getTypeName() );
            return newXp;
        }
        else
            return xp;
    }

    // When the persistent data container levelled key has not been set on the entity yet (i.e. for use in EntitySpawnListener)
    @Nullable
    public String getNametag(final LivingEntityWrapper lmEntity, final boolean isDeathNametag) {

        String nametag = isDeathNametag ? main.rulesManager.getRule_Nametag_CreatureDeath(lmEntity) : main.rulesManager.getRule_Nametag(lmEntity);
        if ("disabled".equalsIgnoreCase(nametag) || "none".equalsIgnoreCase(nametag)) return null;

        // ignore if 'disabled'
        if (nametag.isEmpty())
            return lmEntity.getLivingEntity().getCustomName(); // CustomName can be null, that is meant to be the case.

        final String overridenName = main.rulesManager.getRule_EntityOverriddenName(lmEntity);

        String displayName = overridenName == null ?
                Utils.capitalize(lmEntity.getTypeName().replaceAll("_", " ")) :
                MessageUtils.colorizeAll(overridenName);

        if (lmEntity.getLivingEntity().getCustomName() != null)
            displayName = lmEntity.getLivingEntity().getCustomName();

        nametag = replaceStringPlaceholders(nametag, lmEntity, displayName);

        // This is after colorize so that color codes in nametags dont get translated
        nametag = nametag.replace("%displayname%", displayName);

        return nametag;
    }

    public String replaceStringPlaceholders(final String nametag, @NotNull final LivingEntityWrapper lmEntity, final String displayName){
        String result = nametag;

        final double maxHealth = getMobAttributeValue(lmEntity, Attribute.GENERIC_MAX_HEALTH);
        final double entityHealth = getMobHealth(lmEntity);
        final int entityHealthRounded = entityHealth < 1.0 && entityHealth > 0.0 ?
                1 : (int) Utils.round(entityHealth);
        final String roundedMaxHealth = Utils.round(maxHealth) + "";
        final String roundedMaxHealthInt = (int) Utils.round(maxHealth) + "";

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
        result = MessageUtils.colorizeAll(result);

        return result;
    }

    public void updateNametag_WithDelay(final LivingEntityWrapper lmEntity){
        final BukkitRunnable runnable = new BukkitRunnable() {
            @Override
            public void run() {
                updateNametag(
                        lmEntity,
                        getNametag(lmEntity, false)
                );
            }
        };

        runnable.runTaskLater(main, 1L);
    }

    public void updateNametag(final LivingEntityWrapper lmEntity){
        updateNametag(
                lmEntity,
                getNametag(lmEntity, false)
        );
    }

    public void updateNametag(final @NotNull LivingEntityWrapper lmEntity, final String nametag) {
        updateNametag(lmEntity, nametag, lmEntity.getLivingEntity().getWorld().getPlayers());
    }

    public void updateNametag(final @NotNull LivingEntityWrapper lmEntity, final String nametag, final List<Player> players) {
        main.queueManager_nametags.addToQueue(new QueueItem(lmEntity, nametag, players));
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

    public void startNametagAutoUpdateTask() {
        Utils.logger.info("&fTasks: &7Starting async nametag auto update task...");

        final long period = main.settingsCfg.getInt("nametag-auto-update-task-period"); // run every ? seconds.

        nametagAutoUpdateTask = new BukkitRunnable() {
            @Override
            public void run() {
                final Map<Player, List<Entity>> entitiesPerPlayer = new LinkedHashMap<>();

                for (final Player player : Bukkit.getOnlinePlayers()) {
                    final List<Entity> entities = player.getNearbyEntities(50, 50, 50);
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
        // .runTaskTimerAsynchronously(main, 0, 20 * period);
    }

    private void runNametagCheck_aSync(final Map<Player,List<Entity>> entitiesPerPlayer){
        for (final Player player : entitiesPerPlayer.keySet()) {
            for (final Entity entity : entitiesPerPlayer.get(player)) {

                if (!entity.isValid()) continue; // async task, entity can despawn whilst it is running

                // Mob must be a livingentity that is ...living.
                if (!(entity instanceof LivingEntity)) continue;
                LivingEntityWrapper lmEntity = new LivingEntityWrapper((LivingEntity) entity, main);

                if (lmEntity.isLevelled())
                    checkLevelledEntity(lmEntity, player);
                else {
                    boolean wasBabyMob;
                    synchronized (lmEntity.getLivingEntity().getPersistentDataContainer()){
                        wasBabyMob = lmEntity.getPDC().has(main.levelManager.wasBabyMobKey, PersistentDataType.INTEGER);
                    }
                    if (
                            !lmEntity.isBabyMob() &&
                                    wasBabyMob &&
                                    main.levelInterface.getLevellableState(lmEntity) == LevelInterface.LevellableState.ALLOWED) {
                        // if the mob was a baby at some point, aged and now is eligable for levelling, we'll apply a level to it now
                        Utils.debugLog(main, DebugType.ENTITY_MISC, "&b" + lmEntity.getTypeName() + " &7was a baby and is now an adult, applying levelling rules");

                        main.queueManager_mobs.addToQueue(new QueueItem(lmEntity, null));
                    }
                }
            }
        }
    }

    private void checkLevelledEntity(@NotNull final LivingEntityWrapper lmEntity, @NotNull final Player player){
        final double maxDistance = Math.pow(128, 2); // square the distance we are using Location#distanceSquared. This is because it is faster than Location#distance since it does not need to sqrt which is taxing on the CPU.
        final Location location = player.getLocation();

        if (lmEntity.getLivingEntity().getCustomName() != null && main.rulesManager.getRule_MobCustomNameStatus(lmEntity) == MobCustomNameStatusEnum.NOT_NAMETAGGED){
            // mob has a nametag but is levelled so we'll remove it
            main.levelInterface.removeLevel(lmEntity);
        }
        else if (lmEntity.isMobTamed() && main.rulesManager.getRule_MobTamedStatus(lmEntity) == MobTamedStatusEnum.NOT_TAMED){
            // mob is tamed with a level but the rules don't allow it, remove the level
            main.levelInterface.removeLevel(lmEntity);
        }
        else if (
                location.getWorld() != null &&
                        location.getWorld().getName().equals(lmEntity.getWorld().getName()) &&
                        lmEntity.getLocation().distanceSquared(location) <= maxDistance) {
            //if within distance, update nametag.
            main.queueManager_nametags.addToQueue(new QueueItem(lmEntity, main.levelManager.getNametag(lmEntity, false), Collections.singletonList(player)));
        }
    }

    public void stopNametagAutoUpdateTask() {
        if (!ExternalCompatibilityManager.hasProtocolLibInstalled()) return;

        if (nametagAutoUpdateTask != null && !nametagAutoUpdateTask.isCancelled()) {
            Utils.logger.info("&fTasks: &7Stopping async nametag auto update task...");
            nametagAutoUpdateTask.cancel();
        }
    }

    public void applyLevelledAttributes(@NotNull final LivingEntityWrapper lmEntity, @NotNull final Addition addition) {
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

        // Apply additions
        main.mobDataManager.setAdditionsForLevel(lmEntity, attribute, addition);
    }

    public void applyCreeperBlastRadius(final LivingEntityWrapper lmEntity, int level) {
        final int creeperMaxDamageRadius = main.rulesManager.getRule_CreeperMaxBlastRadius(lmEntity);
        final Creeper creeper = (Creeper) lmEntity.getLivingEntity();

        if (creeperMaxDamageRadius == 3) return;

        final int maxLevel = main.rulesManager.getRule_MobMaxLevel(lmEntity);
        if (maxLevel == 0) return;

        final int minMobLevel = main.rulesManager.getRule_MobMinLevel(lmEntity);
        final double levelDiff = maxLevel - minMobLevel;
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
}
