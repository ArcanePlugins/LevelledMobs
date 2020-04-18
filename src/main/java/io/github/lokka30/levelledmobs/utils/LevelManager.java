package io.github.lokka30.levelledmobs.utils;

import io.github.lokka30.levelledmobs.LevelledMobs;
import org.apache.commons.lang.StringUtils;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class LevelManager {

    private LevelledMobs instance;

    public LevelManager(LevelledMobs instance) {
        this.instance = instance;
    }

    //Checks if an entity can be levelled.
    public boolean isLevellable(final LivingEntity entity) {

        //Players shouldn't be levelled! Keep this at the top to ensure they don't return true
        if (entity.getType() == EntityType.PLAYER) {
            return false;
        }

        //Blacklist override, entities here will return true regardless if they are in blacklistedTypes or are passive
        List<String> blacklistOverrideTypes = instance.settings.get("blacklist-override-types", Collections.singletonList("SHULKER"));
        if (blacklistOverrideTypes.contains(entity.getType().name())) {
            return true;
        }

        //Set it to what's specified. If it's invalid, it'll just take a small predefiend list.
        List<String> blacklistedTypes = instance.settings.get("blacklisted-types", Arrays.asList("VILLAGER", "WANDERING_TRADER", "ENDER_DRAGON", "WITHER"));
        if (blacklistedTypes.contains(entity.getType().name())) {
            return false;
        }

        //Checks for the 'level-passive' option.
        if (instance.settings.get("level-passive", false)) {
            return true;
        }

        //If the entity is a monster, return true
        return entity instanceof Monster;
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
        if (entity instanceof LivingEntity && instance.settings.get("enable-nametag-changes", true)) { //if the settings allows nametag changes, go ahead.
            final LivingEntity livingEntity = (LivingEntity) entity;

            if (entity.getPersistentDataContainer().get(instance.levelKey, PersistentDataType.INTEGER) == null) { //if the entity doesn't contain a level, skip this.
                return;
            }

            if (isLevellable(livingEntity)) { // If the mob is levellable, go ahead.
                String customName = instance.settings.get("creature-nametag", "&8[&7Level %level%&8 | &f%name%&8 | &c%health%&8/&c%max_health% %heart_symbol%&8]")
                        .replaceAll("%level%", entity.getPersistentDataContainer().get(instance.levelKey, PersistentDataType.INTEGER) + "")
                        .replaceAll("%name%", StringUtils.capitalize(entity.getType().name().toLowerCase()))
                        .replaceAll("%health%", instance.utils.round(livingEntity.getHealth(), 1) + "")
                        .replaceAll("%max_health%", instance.utils.round(Objects.requireNonNull(livingEntity.getAttribute(Attribute.GENERIC_MAX_HEALTH)).getBaseValue(), 1) + "")
                        .replaceAll("%heart_symbol%", "❤");
                entity.setCustomName(instance.colorize(customName));

                // CustomNameVisible
                // If true, players can see it from afar and through walls and roofs and the surface of the world if under caves.
                // If false, players can only see it when looking directly at it and within 4 or so blocks.
                //
                // I can't change anything else here, as it's a Minecraft feature.
                // Unfortunately no hybrid between the two where you can't see it through caves and that. :(
                entity.setCustomNameVisible(instance.settings.get("fine-tuning.custom-name-visible", false));
            }
        }
    }

    //Clear their nametag on death.
    public void checkClearNametag(final LivingEntity ent) {
        if (instance.levelManager.isLevellable(ent) && instance.settings.get("fine-tuning.remove-nametag-on-death", false)) {
            ent.setCustomName(null);
        }
    }

    //Calculates the drops when a levellable creature dies.
    public void calculateDrops(final LivingEntity ent, List<ItemStack> drops) {

        if (instance.levelManager.isLevellable(ent)) {
            //If mob is levellable, but wasn't levelled, return.
            Integer level = ent.getPersistentDataContainer().get(instance.levelKey, PersistentDataType.INTEGER);
            if (level == null)
                return;

            //Read settings for drops.
            double dropMultiplier = instance.settings.get("fine-tuning.multipliers.item-drop", 0.16);
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
            double xpMultiplier = instance.settings.get("fine-tuning.multipliers.xp-drop", 0.1D);
            Integer level = ent.getPersistentDataContainer().get(instance.levelKey, PersistentDataType.INTEGER);

            if (level != null) {
                xp *= xpMultiplier * level + 1;
            }
        }
        return xp;
    }
}
