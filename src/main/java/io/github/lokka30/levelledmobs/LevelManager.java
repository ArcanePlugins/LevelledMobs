package io.github.lokka30.levelledmobs;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import io.github.lokka30.levelledmobs.listeners.CreatureSpawnListener;
import io.github.lokka30.levelledmobs.utils.Utils;
import me.lokka30.microlib.MicroUtils;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.regex.Pattern;

public class LevelManager {

    private final LevelledMobs instance;

    public LevelManager(LevelledMobs instance) {
        this.instance = instance;

        levelKey = new NamespacedKey(instance, "level");
        isLevelledKey = new NamespacedKey(instance, "isLevelled");

        registerPacketListeners();
    }

    public final NamespacedKey levelKey; // This stores the mob's level.
    public final NamespacedKey isLevelledKey; //This is stored on levelled mobs to tell plugins that it is a levelled mob.

    public final static int maxCreeperBlastRadius = 100;
    public final Pattern slimeRegex = Pattern.compile("Level.*?(\\d{1,2})", Pattern.CASE_INSENSITIVE);
    public CreatureSpawnListener creatureSpawnListener;

    public boolean isLevellable(final EntityType entityType) {
        if (entityType == EntityType.PLAYER || entityType == EntityType.UNKNOWN) {
            return false;
        }

        //Blacklist override, entities here will return true regardless if they are in blacklistedTypes or are passive
        List<String> blacklistOverrideTypes = instance.settingsCfg.getStringList("blacklist-override-types");
        if (blacklistOverrideTypes.contains(entityType.toString())) {
            return true;
        }

        //Set it to what's specified. If it's invalid, it'll just take a small predefiend list.
        List<String> blacklistedTypes = instance.settingsCfg.getStringList("blacklisted-types");
        if (blacklistedTypes.contains(entityType.toString())) {
            return false;
        }

        Class<? extends Entity> entityClass = entityType.getEntityClass();
        if (entityClass == null) {
            return false;
        }

        // These entities don't implement Monster or Boss and thus must be forced to return true
        if (Arrays.asList(EntityType.GHAST, EntityType.MAGMA_CUBE, EntityType.HOGLIN, EntityType.SHULKER, EntityType.PHANTOM, EntityType.ENDER_DRAGON)
                .contains(entityType)) {
            return true;
        }

        return Monster.class.isAssignableFrom(entityClass)
                || Boss.class.isAssignableFrom(entityClass)
                || instance.settingsCfg.getBoolean("level-passive");
    }

    //Checks if an entity can be levelled.
    public boolean isLevellable(final LivingEntity entity) {

        //Players shouldn't be levelled! Keep this at the top to ensure they don't return true
        if (entity.getType() == EntityType.PLAYER || entity.getType() == EntityType.UNKNOWN || entity.hasMetadata("NPC")) {
            return false;
        }

        // Check WorldGuard flag.
        if (instance.hasWorldGuardInstalled) {
            if (!instance.worldGuardManager.regionAllowsLevelling(entity)) {
                return false;
            }
        }

        //Blacklist override, entities here will return true regardless if they are in blacklistedTypes or are passive
        List<String> blacklistOverrideTypes = instance.settingsCfg.getStringList("blacklist-override-types");
        if (blacklistOverrideTypes.contains(entity.getType().name())) {
            return true;
        } else {
            if (entity instanceof Zombie) {
                Zombie zombie = (Zombie) entity;
                if (!zombie.isAdult() && blacklistOverrideTypes.contains("BABY_ZOMBIE")) {
                    return true;
                }
            }
        }

        //Set it to what's specified. If it's invalid, it'll just take a small predefiend list.
        List<String> blacklistedTypes = instance.settingsCfg.getStringList("blacklisted-types");
        if (blacklistedTypes.contains(entity.getType().name())) {
            return false;
        } else {
            if (entity instanceof Zombie) {
                Zombie zombie = (Zombie) entity;
                if (!zombie.isAdult() && blacklistedTypes.contains("BABY_ZOMBIE")) {
                    return false;
                }
            }
        }

        boolean result = entity instanceof Monster || entity instanceof Boss || instance.settingsCfg.getBoolean("level-passive");

        // there are a few special cases here since they aren't part of the 'Monster' interface
        if (!result && (entity instanceof Ghast || entity instanceof MagmaCube || entity instanceof Hoglin || entity instanceof Shulker || entity instanceof Phantom)) {
            result = true;
        }

        return result;
    }

    //Updates the entity's nametag after a 1 tick delay. Without the delay, it would
    //display the entity's previous health rather than their new health.
    //Used on EntityDamageEvent and EntityRegainHealthEvent.
    public void updateNametagWithDelay(LivingEntity entity) {
        new BukkitRunnable() {
            public void run() {
                updateNametag(entity);
            }
        }.runTaskLater(instance, 1L);
    }

    //Calculates the drops when a levellable creature dies.
    public void setLevelledDrops(final LivingEntity ent, List<ItemStack> drops) {

        if (isLevellable(ent)) {
            //If mob is levellable, but wasn't levelled, return.
            Integer level = ent.getPersistentDataContainer().get(instance.levelManager.levelKey, PersistentDataType.INTEGER);
            if (level == null)
                return;

            //Read settings for drops.
            double dropMultiplier = instance.settingsCfg.getDouble("fine-tuning.additions.custom.item-drop");
            int finalMultiplier = 1;

            //If multiplier * level gurantees an extra drop set 'finalMultiplier' to the amount of safe multiples.
            dropMultiplier *= level;
            finalMultiplier += (int) dropMultiplier;
            dropMultiplier -= (int) dropMultiplier;

            //Calculate if the remaining extra drop chance triggers.
            double random = new Random().nextDouble();
            if (random < dropMultiplier) {
                finalMultiplier++;
            }

            //Remove the hand item from the mob's drops so it doesn't get multiplied
            ItemStack helmet = null;
            ItemStack chestplate = null;
            ItemStack leggings = null;
            ItemStack boots = null;
            ItemStack mainHand = null;
            ItemStack offHand = null;
            if (ent.getEquipment() != null) {
                helmet = ent.getEquipment().getHelmet();
                chestplate = ent.getEquipment().getChestplate();
                leggings = ent.getEquipment().getLeggings();
                boots = ent.getEquipment().getBoots();
                mainHand = ent.getEquipment().getItemInMainHand();
                offHand = ent.getEquipment().getItemInOffHand();
            }

            //Edit the ItemStacks to drop the calculated multiple items.
            for (int i = 0; i < drops.size(); i++) {
                ItemStack itemStack = drops.get(i);

                int amount = itemStack.getAmount() * finalMultiplier;

                //Don't let the drops go over the max stack size.
                int maxStackSize = itemStack.getMaxStackSize();
                if (amount > maxStackSize) {
                    amount = maxStackSize;
                }

                //Don't let the plugin multiply items which match their equipment. stops bows and that from multiplying
                if (helmet != null && itemStack.isSimilar(helmet)) {
                    amount = helmet.getAmount();
                }
                if (chestplate != null && itemStack.isSimilar(chestplate)) {
                    amount = chestplate.getAmount();
                }
                if (leggings != null && itemStack.isSimilar(leggings)) {
                    amount = leggings.getAmount();
                }
                if (boots != null && itemStack.isSimilar(boots)) {
                    amount = boots.getAmount();
                }
                if (mainHand != null && itemStack.isSimilar(mainHand)) {
                    amount = mainHand.getAmount();
                }
                if (offHand != null && itemStack.isSimilar(offHand)) {
                    amount = offHand.getAmount();
                }

                itemStack.setAmount(amount);
                drops.set(i, itemStack);
            }
        }
    }

    //Calculates the XP dropped when a levellable creature dies.
    public int setLevelledXP(final LivingEntity ent, int xp) {
        if (instance.levelManager.isLevellable(ent)) {
            double xpMultiplier = instance.settingsCfg.getDouble("fine-tuning.additions.custom.xp-drop");
            Integer level = ent.getPersistentDataContainer().get(instance.levelManager.levelKey, PersistentDataType.INTEGER);

            if (level != null) {
                xp *= xpMultiplier * level + 1;
            }
        }
        return xp;
    }

    public String getNametag(LivingEntity livingEntity) {
        String entityName = livingEntity.getCustomName() == null ? instance.configUtils.getEntityName(livingEntity.getType()) : livingEntity.getCustomName();

        AttributeInstance maxHealth = livingEntity.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        String health = maxHealth == null ? "?" : maxHealth.getBaseValue() + "";

        String nametag = instance.settingsCfg.getString("creature-nametag");
        nametag = Utils.replaceEx(nametag, "%level%", String.valueOf(livingEntity.getPersistentDataContainer().get(instance.levelManager.levelKey, PersistentDataType.INTEGER)));
        nametag = Utils.replaceEx(nametag, "%name%", entityName);
        nametag = Utils.replaceEx(nametag, "%health%", Utils.round(livingEntity.getHealth()) + "");
        nametag = Utils.replaceEx(nametag, "%max_health%", health);
        nametag = Utils.replaceEx(nametag, "%heart_symbol%", "❤");
        assert nametag != null;
        nametag = MicroUtils.colorize(nametag);

        return nametag;
    }

    /**
     * Credit: https://www.spigotmc.org/threads/changing-an-entitys-name-using-protocollib.482855/#post-4051032 by SpigotMC user: https://www.spigotmc.org/members/CoolBoy.102500/
     */
    public void registerPacketListeners() {
        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(instance, ListenerPriority.NORMAL, PacketType.Play.Server.ENTITY_METADATA) {
            @Override
            public void onPacketSending(PacketEvent event) {
                if (event.getPacketType() != PacketType.Play.Server.ENTITY_METADATA) {
                    return;
                }

                PacketContainer packet = event.getPacket();

                Entity entity = packet.getEntityModifier(event).read(0);

                if (entity instanceof LivingEntity) {
                    LivingEntity livingEntity = (LivingEntity) entity;

                    if (isLevellable(livingEntity)) {
                        WrappedDataWatcher dataWatcher = WrappedDataWatcher.getEntityWatcher(entity);

                        WrappedDataWatcher.Serializer chatSerializer = WrappedDataWatcher.Registry.getChatComponentSerializer(true);

                        dataWatcher.setObject(new WrappedDataWatcher.WrappedDataWatcherObject(2, chatSerializer),
                                Optional.of(WrappedChatComponent.fromChatMessage(getNametag(livingEntity))[0].getHandle()));

                        packet.getWatchableCollectionModifier().write(0, dataWatcher.getWatchableObjects());

                        event.setPacket(packet);
                    }
                }
            }
        });
    }

    public void updateNametag(LivingEntity entity) {
        WrappedDataWatcher dataWatcher = WrappedDataWatcher.getEntityWatcher(entity);
        WrappedDataWatcher.Serializer chatSerializer = WrappedDataWatcher.Registry.getChatComponentSerializer(true);
        dataWatcher.setObject(new WrappedDataWatcher.WrappedDataWatcherObject(2, chatSerializer),
                Optional.of(WrappedChatComponent.fromChatMessage(getNametag(entity))[0].getHandle()));

        PacketContainer packet = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.ENTITY_METADATA);
        packet.getWatchableCollectionModifier().write(0, dataWatcher.getWatchableObjects());

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            try {
                ProtocolLibrary.getProtocolManager().sendServerPacket(onlinePlayer, packet);
            } catch (InvocationTargetException ex) {
                Utils.logger.error("Unable to update nametag packet for player &b" + onlinePlayer.getName() + "&7! Stack trace:");
                ex.printStackTrace();
                return;
            }
        }
    }
}
