package io.github.lokka30.levelledmobs;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import io.github.lokka30.levelledmobs.listeners.CreatureSpawnListener;
import io.github.lokka30.levelledmobs.utils.ModalList;
import io.github.lokka30.levelledmobs.utils.Utils;
import me.lokka30.microlib.MicroUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
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
import java.util.concurrent.ThreadLocalRandom;

public class LevelManager {

    private final LevelledMobs instance;

    public LevelManager(LevelledMobs instance) {
        this.instance = instance;

        levelKey = new NamespacedKey(instance, "level");
        isLevelledKey = new NamespacedKey(instance, "isLevelled");
    }

    public final NamespacedKey levelKey; // This stores the mob's level.
    public final NamespacedKey isLevelledKey; //This is stored on levelled mobs to tell plugins that it is a levelled mob.

    public final HashSet<String> forcedTypes = new HashSet<>(Arrays.asList("GHAST", "MAGMA_CUBE", "HOGLIN", "SHULKER", "PHANTOM", "ENDER_DRAGON", "SLIME", "MAGMA_CUBE", "ZOMBIFIED_PIGLIN"));

    public final static int maxCreeperBlastRadius = 100;
    //public final Pattern slimeRegex = Pattern.compile("Level.*?(\\d{1,2})", Pattern.CASE_INSENSITIVE);
    public CreatureSpawnListener creatureSpawnListener;

    public boolean isLevellable(final EntityType entityType) {
        // Don't level these
        if (entityType == EntityType.PLAYER || entityType == EntityType.UNKNOWN) return false;

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

        //Players shouldn't be levelled! Keep this at the top to ensure they don't return true
        if (livingEntity.getType() == EntityType.PLAYER || livingEntity.getType() == EntityType.UNKNOWN || livingEntity.hasMetadata("NPC")) {
            return false;
        }

        // Check WorldGuard flag.
        if (instance.hasWorldGuardInstalled && !instance.worldGuardManager.regionAllowsLevelling(livingEntity))
            return false;

        // Check for overrides
        if (instance.settingsCfg.getStringList("overriden-entities").contains(livingEntity.getType().toString()))
            return true;

        //Check allowed entities for normal entity types
        if (!ModalList.isEnabledInList(instance.settingsCfg, "allowed-entities-list", livingEntity.getType().toString()))
            return false;

        // Specific allowed entities check for BABY_ZOMBIE
        if (livingEntity instanceof Zombie && Utils.isZombieBaby((Zombie) livingEntity)) {
            if (!ModalList.isEnabledInList(instance.settingsCfg, "allowed-entities-list", "BABY_ZOMBIE"))
                return false;
        }

        return isLevellable(livingEntity.getType());
    }

    public void updateNametagWithDelay(LivingEntity livingEntity, String nametag, List<Player> players, long delay) {
        new BukkitRunnable() {
            public void run() {
                if (livingEntity == null) return; // may have died/removed after the timer.
                updateNametag(livingEntity, nametag, players);
            }
        }.runTaskLater(instance, delay);
    }

    public void updateNametagWithDelay(LivingEntity livingEntity, List<Player> players, long delay) {
        new BukkitRunnable() {
            public void run() {
                if (livingEntity == null) return; // may have died/removed after the timer.
                updateNametag(livingEntity, getNametag(livingEntity), players);
            }
        }.runTaskLater(instance, delay);
    }

    // This sets the levelled currentDrops on a levelled mob that just died.
    public List<ItemStack> getLevelledItemDrops(final LivingEntity livingEntity, List<ItemStack> currentDrops) {

        Utils.debugLog(instance, "LevelManager#getLevelledItemDrops", "1: Method called. " + currentDrops.size() + " drops will be analysed.");

        // Must be a levelled mob
        if (!livingEntity.getPersistentDataContainer().has(isLevelledKey, PersistentDataType.STRING))
            return currentDrops;

        Utils.debugLog(instance, "LevelManager#getLevelledItemDrops", "2: LivingEntity is a levelled mob.");

        // Get their level
        int level = Objects.requireNonNull(livingEntity.getPersistentDataContainer().get(levelKey, PersistentDataType.INTEGER));
        Utils.debugLog(instance, "LevelManager#getLevelledItemDrops", "3: Entity level is " + level + ".");

        // Get currentDrops added per level value
        int addition = new BigDecimal(instance.settingsCfg.getDouble("fine-tuning.additions.custom.item-drop") * level) // get value from config
                .setScale(0, RoundingMode.HALF_DOWN).intValueExact(); // truncate double to int
        Utils.debugLog(instance, "LevelManager#getLevelledItemDrops", "4: Item drop addition is +" + addition + ".");

        // Modify current drops
        Utils.debugLog(instance, "LevelManager#getLevelledItemDrops", "5: Scanning " + currentDrops.size() + " items...");
        for (ItemStack currentDrop : currentDrops) {
            Utils.debugLog(instance, "LevelManager#getLevelledItemDrops", "6: Scanning drop " + currentDrop.getType().toString() + " with current amount " + currentDrop.getAmount() + "...");

            if (instance.mobDataManager.isLevelledDropManaged(livingEntity.getType(), currentDrop.getType())) {
                currentDrop.setAmount(currentDrop.getAmount() + (currentDrop.getAmount() * addition));
                Utils.debugLog(instance, "LevelManager#getLevelledItemDrops", "7: Item was managed. New amount: " + currentDrop.getAmount() + ".");
            } else {
                Utils.debugLog(instance, "LevelManager#getLevelledItemDrops", "7: Item was unmanaged.");
            }
        }

        if (instance.settingsCfg.getBoolean("use-custom-item-drops-for-mobs") && instance.customDropsitems.containsKey(livingEntity.getType())){
            List<CustomItemDrop> drops = instance.customDropsitems.get(livingEntity.getType());

            for (CustomItemDrop drop : drops){
                if (drop.maxLevel > -1 && level > drop.maxLevel) continue;
                if (drop.minLevel > -1 && level < drop.minLevel) continue;
                if (drop.dropChance < 1.0){
                    double effectiveDropChance = drop.dropChance;
                    if (!drop.noMultiplier){
                        // TODO: factor in mob level here

                    }

                    double chanceRole = (double) ThreadLocalRandom.current().nextInt(0, 100001) * 0.00001;
                    if (instance.settingsCfg.getStringList("debug-misc").contains("custom-drops")) {
                        Utils.logger.info(String.format("mob: %s, item %s, origChance: %s, effectiveChance: %s, chanceRole: %s, dropped: %s",
                                livingEntity.getName(), drop.getMaterial().name(), drop.dropChance, effectiveDropChance, chanceRole, !(1.0 - chanceRole >= effectiveDropChance)
                                ));
                    }
                    if (1.0 - chanceRole >= effectiveDropChance) continue;
                }
                // if we made it this far then the item will be dropped
                ItemStack newItem = drop.getItemStack();
                if (drop.amount > 1){
                    newItem = new ItemStack(drop.getMaterial(), drop.amount);
                }

                currentDrops.add(newItem);
            }
        }

        return currentDrops;
    }

    //Calculates the XP dropped when a levellable creature dies.
    public int getLevelledExpDrops(final LivingEntity ent, int xp) {
        if (ent.getPersistentDataContainer().has(isLevelledKey, PersistentDataType.STRING)) {
            double xpPerLevel = instance.settingsCfg.getDouble("fine-tuning.additions.custom.xp-drop");
            int level = Objects.requireNonNull(ent.getPersistentDataContainer().get(instance.levelManager.levelKey, PersistentDataType.INTEGER));

            xp = (int) Math.round(xp + (xpPerLevel * level));
        }
        return xp;
    }

    // When the persistent data container levelled key has been set on the entity already (i.e. when they are damaged)
    public String getNametag(LivingEntity livingEntity) {
        return getNametag(livingEntity, Objects.requireNonNull(
                livingEntity.getPersistentDataContainer().get(levelKey, PersistentDataType.INTEGER)));
    }

    // When the persistent data container levelled key has not been set on the entity yet (i.e. for use in CreatureSpawnListener)
    public String getNametag(LivingEntity livingEntity, int level) {
        String entityName = instance.configUtils.getEntityName(livingEntity.getType());
        String displayName = livingEntity.getCustomName() == null ? entityName : livingEntity.getCustomName();

        AttributeInstance maxHealth = livingEntity.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        String health = maxHealth == null ? "?" : Utils.round(maxHealth.getBaseValue()) + "";

        String nametag = instance.settingsCfg.getString("creature-nametag");

        if (nametag == null || nametag.isEmpty()) return null;

        nametag = nametag.replace("%level%", level + "");
        nametag = nametag.replace("%displayname%", displayName);
        nametag = nametag.replace("%typename%", entityName);
        nametag = nametag.replace("%health%", Utils.round(livingEntity.getHealth()) + "");
        nametag = nametag.replace("%max_health%", health);
        nametag = nametag.replace("%heart_symbol%", "❤");
        nametag = MicroUtils.colorize(nametag);

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
    public void updateNametag(LivingEntity entity, String nametag, List<Player> players) {
        if (!instance.hasProtocolLibInstalled) return;

        for (Player player : players) {
            WrappedDataWatcher dataWatcher = WrappedDataWatcher.getEntityWatcher(entity).deepClone();
            WrappedDataWatcher.Serializer chatSerializer = WrappedDataWatcher.Registry.getChatComponentSerializer(true);
            WrappedDataWatcher.WrappedDataWatcherObject watcherObject = new WrappedDataWatcher.WrappedDataWatcherObject(2, chatSerializer);
            Optional<Object> optional = Optional.of(WrappedChatComponent.fromChatMessage(nametag)[0].getHandle());
            dataWatcher.setObject(watcherObject, optional);
            dataWatcher.setObject(3, instance.settingsCfg.getBoolean("creature-nametag-always-visible"));

            PacketContainer packet = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.ENTITY_METADATA);
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

    private BukkitTask nametagAutoUpdateTask;

    public void startNametagAutoUpdateTask() {
        Utils.logger.info("&fTasks: &7Starting async nametag auto update task...");

        double maxDistance = Math.pow(128, 2); // square the distance we are using Location#distanceSquared. This is because it is faster than Location#distance since it does not need to sqrt which is taxing on the CPU.
        long period = 6; // run every ? seconds.

        nametagAutoUpdateTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    final Location location = player.getLocation();

                    for (Entity entity : player.getWorld().getEntities()) {

                        // Mob must be a livingentity that is ...living.
                        if (!(entity instanceof LivingEntity)) continue;
                        final LivingEntity livingEntity = (LivingEntity) entity;

                        // Mob must be levelled
                        if (!livingEntity.getPersistentDataContainer().has(instance.levelManager.isLevelledKey, PersistentDataType.STRING))
                            continue;

                        //if within distance, update nametag.
                        if (livingEntity.getLocation().distanceSquared(location) <= maxDistance) {
                            instance.levelManager.updateNametag(livingEntity, instance.levelManager.getNametag(livingEntity), Collections.singletonList(player));
                        }
                    }
                }
            }
        }.runTaskTimerAsynchronously(instance, 0, 20 * period);
    }

    public void stopNametagAutoUpdateTask() {
        if (!instance.hasProtocolLibInstalled) return;

        Utils.logger.info("&fTasks: &7Stopping async nametag auto update task...");

        if (nametagAutoUpdateTask != null)
            nametagAutoUpdateTask.cancel();
    }
}
