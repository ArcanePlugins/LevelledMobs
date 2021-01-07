package io.github.lokka30.levelledmobs.utils;

import io.github.lokka30.levelledmobs.LevelledMobs;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.Random;

public class LevelManager {

    private LevelledMobs instance;

    public LevelManager(LevelledMobs instance) {
        this.instance = instance;
    }

    //Checks if an entity can be levelled.
    public boolean isLevellable(final LivingEntity entity) {

        //Players shouldn't be levelled! Keep this at the top to ensure they don't return true
        if (entity.getType() == EntityType.PLAYER || entity.hasMetadata("NPC")) {
            return false;
        }

        //Blacklist override, entities here will return true regardless if they are in blacklistedTypes or are passive
        List<String> blacklistOverrideTypes = instance.fileCache.SETTINGS_BLACKLIST_OVERRIDE_TYPES;
        if (blacklistOverrideTypes.contains(entity.getType().name())) {
            return true;
        } else {
            if (entity instanceof Zombie) {
                Zombie zombie = (Zombie) entity;
                if (zombie.isBaby() && blacklistOverrideTypes.contains("BABY_ZOMBIE")) {
                    return true;
                }
            }
        }

        //Set it to what's specified. If it's invalid, it'll just take a small predefiend list.
        List<String> blacklistedTypes = instance.fileCache.SETTINGS_BLACKLISTED_TYPES;
        if (blacklistedTypes.contains(entity.getType().name())) {
            return false;
        } else {
            if (entity instanceof Zombie) {
                Zombie zombie = (Zombie) entity;
                if (zombie.isBaby() && blacklistedTypes.contains("BABY_ZOMBIE")) {
                    return false;
                }
            }
        }
        
        boolean result = entity instanceof Monster || entity instanceof Boss || instance.fileCache.SETTINGS_LEVEL_PASSIVE;
        
        // there are a few special cases here since they aren't part of the 'Monster' interface
        if (!result && (entity instanceof Ghast || entity instanceof MagmaCube))
        	result = true;

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

        if (entity instanceof LivingEntity && instance.fileCache.SETTINGS_ENABLE_NAMETAG_CHANGES) { //if the settings allows nametag changes, go ahead.
            final LivingEntity livingEntity = (LivingEntity) entity;

            if (entity.getPersistentDataContainer().get(instance.levelKey, PersistentDataType.INTEGER) == null) { //if the entity doesn't contain a level, skip this.
                return;
            }

            if (isLevellable(livingEntity)) { // If the mob is levellable, go ahead.
            	
            	// changed from regex replace to case-insensitive replace
            	// in theory should use less resources and they can use capital letters in the variables now
            	String customName = instance.fileCache.SETTINGS_CREATURE_NAMETAG;
            	customName = Utils.replaceEx(customName, "%level%", String.valueOf(entity.getPersistentDataContainer().get(instance.levelKey, PersistentDataType.INTEGER)));
            	customName = Utils.replaceEx(customName, "%name%", instance.fileCache.getEntityName(entityType));
            	customName = Utils.replaceEx(customName, "%health%", String.valueOf(Utils.round(livingEntity.getHealth(), 1)));
            	AttributeInstance att = livingEntity.getAttribute(Attribute.GENERIC_MAX_HEALTH);
            	String health = att == null ? "" : String.valueOf(Utils.round((livingEntity.getAttribute(Attribute.GENERIC_MAX_HEALTH)).getBaseValue(), 1));
            	customName = Utils.replaceEx(customName, "%max_health%", health);
            	customName = Utils.replaceEx(customName, "%heart_symbol%", "❤");
            	
            	/*
                customName = instance.fileCache.SETTINGS_CREATURE_NAMETAG
                        .replaceAll("%level%", entity.getPersistentDataContainer().get(instance.levelKey, PersistentDataType.INTEGER) + "")
                        .replaceAll("%name%", instance.fileCache.getEntityName(entityType))
                        .replaceAll("%health%", Utils.round(livingEntity.getHealth(), 1) + "")
                        .replaceAll("%max_health%", Utils.round(Objects.requireNonNull(livingEntity.getAttribute(Attribute.GENERIC_MAX_HEALTH)).getBaseValue(), 1) + "")
                        .replaceAll("%heart_symbol%", "❤");
                */
                entity.setCustomName(instance.messageMethods.colorize(customName));
                entity.setCustomNameVisible(instance.fileCache.SETTINGS_FINE_TUNING_CUSTOM_NAME_VISIBLE);
            }
        }
    }

    //Clear their nametag on death.
    public void checkClearNametag(final LivingEntity ent) {
        if (isLevellable(ent) && instance.fileCache.SETTINGS_FINE_TUNING_REMOVE_NAMETAG_ON_DEATH) {
            ent.setCustomName(null);
        }
    }

    //Calculates the drops when a levellable creature dies.
    public void calculateDrops(final LivingEntity ent, List<ItemStack> drops) {

        if (isLevellable(ent)) {
            //If mob is levellable, but wasn't levelled, return.
            Integer level = ent.getPersistentDataContainer().get(instance.levelKey, PersistentDataType.INTEGER);
            if (level == null)
                return;

            //Read settings for drops.
            double dropMultiplier = instance.fileCache.SETTINGS_FINE_TUNING_MULTIPLIERS_ITEM_DROP;
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
            double xpMultiplier = instance.fileCache.SETTINGS_FINE_TUNING_MULTIPLIERS_XP_DROP;
            Integer level = ent.getPersistentDataContainer().get(instance.levelKey, PersistentDataType.INTEGER);

            if (level != null) {
                xp *= xpMultiplier * level + 1;
            }
        }
        return xp;
    }
}
