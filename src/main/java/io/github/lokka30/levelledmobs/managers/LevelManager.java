package io.github.lokka30.levelledmobs.managers;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import io.github.lokka30.levelledmobs.LevelledMobs;
import io.github.lokka30.levelledmobs.customdrops.CustomDropResult;
import io.github.lokka30.levelledmobs.listeners.CreatureSpawnListener;
import io.github.lokka30.levelledmobs.misc.Addition;
import io.github.lokka30.levelledmobs.misc.ModalList;
import io.github.lokka30.levelledmobs.misc.Utils;
import me.lokka30.microlib.MessageUtils;
import org.apache.commons.lang.WordUtils;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

public class LevelManager {

    private final LevelledMobs instance;

    public LevelManager(final LevelledMobs instance) {
        this.instance = instance;

        levelKey = new NamespacedKey(instance, "level");
        isLevelledKey = new NamespacedKey(instance, "isLevelled");
        isSpawnerKey = new NamespacedKey(instance, "isSpawner");
    }

    public final NamespacedKey levelKey; // This stores the mob's level.
    public final NamespacedKey isLevelledKey; //This is stored on levelled mobs to tell plugins that it is a levelled mob.
    public final NamespacedKey isSpawnerKey; //This is stored on levelled mobs to tell plugins that a mob was created from a spawner

    public final HashSet<String> forcedTypes = new HashSet<>(Arrays.asList("GHAST", "MAGMA_CUBE", "HOGLIN", "SHULKER", "PHANTOM", "ENDER_DRAGON", "SLIME", "MAGMA_CUBE", "ZOMBIFIED_PIGLIN"));

    public final static int maxCreeperBlastRadius = 100;
    //public final Pattern slimeRegex = Pattern.compile("Level.*?(\\d{1,2})", Pattern.CASE_INSENSITIVE);
    public CreatureSpawnListener creatureSpawnListener;

    public boolean isLevellable(final EntityType entityType) {
        // Don't level these
        if (entityType == EntityType.PLAYER || entityType == EntityType.UNKNOWN || entityType == EntityType.ARMOR_STAND)
            return false;

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
        if (Utils.isEntityBaby(livingEntity)) {
            if (!ModalList.isEnabledInList(instance.settingsCfg, "allowed-entities-list", "BABY_" + livingEntity.getName().toUpperCase()))
                return false;
        }

        return isLevellable(livingEntity.getType());
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
        boolean isBabyEntity = Utils.isEntityBaby(livingEntity);
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
        if (nametag == null) return;
        if (!ExternalCompatibilityManager.hasProtocolLibInstalled()) return;

        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {

                for (Player player : players) {
                    // async task, so make sure the player & entity are valid
                    if (!player.isOnline() || entity == null) return;

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
                    final Optional<Object> optional = Optional.of(WrappedChatComponent.fromChatMessage(nametag)[0].getHandle());
                    dataWatcher.setObject(watcherObject, optional);
                    dataWatcher.setObject(3, entity.isCustomNameVisible() || instance.settingsCfg.getBoolean("creature-nametag-always-visible"));

                    final PacketContainer packet = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.ENTITY_METADATA);
                    packet.getWatchableCollectionModifier().write(0, dataWatcher.getWatchableObjects());
                    packet.getIntegers().write(0, entity.getEntityId());

                    try {
                        ProtocolLibrary.getProtocolManager().sendServerPacket(player, packet);
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
