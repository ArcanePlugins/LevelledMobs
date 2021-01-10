package io.github.lokka30.levelledmobs.utils;

import io.github.lokka30.levelledmobs.LevelledMobs;
import io.github.lokka30.levelledmobs.listeners.CreatureSpawnListener;
import me.lokka30.microlib.MicroUtils;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.regex.Pattern;

public class LevelManager {

    private final LevelledMobs instance;

    public LevelManager(LevelledMobs instance) {
        this.instance = instance;

        levelKey = new NamespacedKey(instance, "level");
        isLevelledKey = new NamespacedKey(instance, "isLevelled");
    }

    public final NamespacedKey levelKey; // This stores the mob's level.
    public final NamespacedKey isLevelledKey; //This is stored on levelled mobs to tell plugins that it is a levelled mob.

    public final static int maxCreeperBlastRadius = 100;
    public final Pattern slimeRegex = Pattern.compile("Level.*?(\\d{1,2})", Pattern.CASE_INSENSITIVE);
    public CreatureSpawnListener creatureSpawnListener;

    public boolean isLevellable(final EntityType entityType) {
        if (entityType == EntityType.PLAYER) {
            return false;
        }

        //TODO check config lists. skipping for the moment

        Class<? extends Entity> entityClass = entityType.getEntityClass();
        if (entityClass == null) {
            return false;
        }

        return entityClass.isAssignableFrom(Monster.class)

                // there are a few special cases here since they aren't part of the 'Monster' interface
                || entityClass.isAssignableFrom(Boss.class)
                || entityClass.isAssignableFrom(Ghast.class)
                || entityClass.isAssignableFrom(MagmaCube.class)
                || entityClass.isAssignableFrom(Hoglin.class)
                || entityClass.isAssignableFrom(Shulker.class)
                || entityClass.isAssignableFrom(Phantom.class)

                // Allow passive mobs?
                || instance.settingsCfg.getBoolean("level-passive");
    }

    //Checks if an entity can be levelled.
    public boolean isLevellable(final LivingEntity entity) {

        //Players shouldn't be levelled! Keep this at the top to ensure they don't return true
        if (entity.getType() == EntityType.PLAYER || entity.hasMetadata("NPC")) {
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

    //Update an entity's tag. it is called twice as when a mob gets damaged their health is updated after to the health after they got damaged.
    public void updateTag(Entity ent) {
        new BukkitRunnable() {
            public void run() {
                setTag(ent);
            }
        }.runTaskLater(instance, 1L);
    }

    //Updates the nametag of a creature. Gets called by certain listeners.
    public void setTag(final Entity entity) {

        final EntityType entityType = entity.getType();

        if (entity instanceof LivingEntity && instance.settingsCfg.getBoolean("enable-nametag-changes")) { //if the settings allows nametag changes, go ahead.
            final LivingEntity livingEntity = (LivingEntity) entity;

            if (entity.getPersistentDataContainer().get(instance.levelManager.levelKey, PersistentDataType.INTEGER) == null) { //if the entity doesn't contain a level, skip this.
                return;
            }

            if (isLevellable(livingEntity)) { // If the mob is levellable, go ahead.

                // case-insensitive replace
                String customName = instance.settingsCfg.getString("creature-nametag");
                customName = Utils.replaceEx(customName, "%level%", String.valueOf(entity.getPersistentDataContainer().get(instance.levelManager.levelKey, PersistentDataType.INTEGER)));
                customName = Utils.replaceEx(customName, "%name%", instance.configUtils.getEntityName(entityType));
                customName = Utils.replaceEx(customName, "%health%", String.valueOf(Utils.round(livingEntity.getHealth())));
                AttributeInstance att = livingEntity.getAttribute(Attribute.GENERIC_MAX_HEALTH);
                String health = att == null ? "" : String.valueOf(Utils.round((Objects.requireNonNull(livingEntity.getAttribute(Attribute.GENERIC_MAX_HEALTH))).getBaseValue()));
                customName = Utils.replaceEx(customName, "%max_health%", health);
                customName = Utils.replaceEx(customName, "%heart_symbol%", "❤");

                assert customName != null;
                entity.setCustomName(MicroUtils.colorize(customName));
                entity.setCustomNameVisible(instance.settingsCfg.getBoolean("fine-tuning.custom-name-visible"));
            }
        }
    }

    //Clear their nametag on death.
    public void checkClearNametag(final LivingEntity ent) {
        if (isLevellable(ent) && instance.settingsCfg.getBoolean("fine-tuning.remove-nametag-on-death")) {
            ent.setCustomName(null);
        }
    }

    //Calculates the drops when a levellable creature dies.
    public void calculateDrops(final LivingEntity ent, List<ItemStack> drops) {

        if (isLevellable(ent)) {
            //If mob is levellable, but wasn't levelled, return.
            Integer level = ent.getPersistentDataContainer().get(instance.levelManager.levelKey, PersistentDataType.INTEGER);
            if (level == null)
                return;

            //Read settings for drops.
            double dropMultiplier = instance.settingsCfg.getDouble("fine-tuning.multipliers.item-drop");
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
    public int calculateXp(final LivingEntity ent, int xp) {
        if (instance.levelManager.isLevellable(ent)) {
            double xpMultiplier = instance.settingsCfg.getDouble("fine-tuning.multipliers.xp-drop");
            Integer level = ent.getPersistentDataContainer().get(instance.levelManager.levelKey, PersistentDataType.INTEGER);

            if (level != null) {
                xp *= xpMultiplier * level + 1;
            }
        }
        return xp;
    }
}
