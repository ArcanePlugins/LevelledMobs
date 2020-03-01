package io.github.lokka30.levelledmobs.utils;

import io.github.lokka30.levelledmobs.LevelledMobs;
import org.apache.commons.lang.StringUtils;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Random;

public class LevelManager {

    private LevelledMobs instance;

    public LevelManager(LevelledMobs instance) {
        this.instance = instance;
    }

    //Checks if an entity can be levelled.
    public boolean isLevellable(final LivingEntity entity) {
        //Checks for the 'blacklisted-types' option.
        List<String> blacklistedTypes;

        //Set it to what's specified. If it's invalid, it'll just take a small predefiend list.
        blacklistedTypes = instance.settings.get("blacklisted-types", Arrays.asList("VILLAGER", "WANDERING_TRADER", "ENDER_DRAGON", "WITHER"));
        for (String blacklistedType : blacklistedTypes) {
            if (entity.getType().toString().equalsIgnoreCase(blacklistedType)) {
                return false;
            }
        }

        //Checks for the 'level-passive' option.
        return entity instanceof Monster || instance.settings.get("level-passive", false);
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

            if (entity.getPersistentDataContainer().get(instance.key, PersistentDataType.INTEGER) == null) { //if the entity doesn't contain a level, skip this.
                return;
            }

            if (isLevellable(livingEntity)) { // If the mob is levellable, go ahead.
                String customName = instance.settings.get("creature-nametag", "&8[&7Level %level%&8 | &f%name%&8 | &c%health%&8/&c%max_health% %heart_symbol%&8]")
                        .replaceAll("%level%", entity.getPersistentDataContainer().get(instance.key, PersistentDataType.INTEGER) + "")
                        .replaceAll("%name%", StringUtils.capitalize(entity.getType().name().toLowerCase()))
                        .replaceAll("%health%", Utils.round(livingEntity.getHealth(), 1) + "")
                        .replaceAll("%max_health%", Utils.round(Objects.requireNonNull(livingEntity.getAttribute(Attribute.GENERIC_MAX_HEALTH)).getBaseValue(), 1) + "")
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
            Integer level = ent.getPersistentDataContainer().get(instance.key, PersistentDataType.INTEGER);
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
            final ItemStack helmet = ent.getEquipment().getHelmet();
            final ItemStack chestplate = ent.getEquipment().getChestplate();
            final ItemStack leggings = ent.getEquipment().getLeggings();
            final ItemStack boots = ent.getEquipment().getBoots();
            final ItemStack mainHand = ent.getEquipment().getItemInMainHand();
            final ItemStack offHand = ent.getEquipment().getItemInOffHand();

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
                if (itemStack.isSimilar(helmet)) {
                    amount = helmet.getAmount();
                }
                if (itemStack.isSimilar(chestplate)) {
                    amount = chestplate.getAmount();
                }
                if (itemStack.isSimilar(leggings)) {
                    amount = leggings.getAmount();
                }
                if (itemStack.isSimilar(boots)) {
                    amount = boots.getAmount();
                }
                if (itemStack.isSimilar(mainHand)) {
                    amount = mainHand.getAmount();
                }
                if (itemStack.isSimilar(offHand)) {
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
            Integer level = ent.getPersistentDataContainer().get(instance.key, PersistentDataType.INTEGER);

            if (level != null) {
                xp *= xpMultiplier * level + 1;
            }
        }
        return xp;
    }
}
