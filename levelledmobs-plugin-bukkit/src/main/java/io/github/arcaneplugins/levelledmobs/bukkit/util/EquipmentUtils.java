package io.github.arcaneplugins.levelledmobs.bukkit.util;

import java.util.function.Predicate;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class EquipmentUtils {

    private EquipmentUtils() throws IllegalAccessException {
        throw new IllegalAccessException("Illegal instantiation of utility class");
    }

    public static @Nullable ItemStack findSimilarItemStackInEntity(
        final @NotNull LivingEntity entity,
        final @NotNull ItemStack itemStack,
        final @NotNull Predicate<ItemStack> predicate
    ) {
        final ItemStack equip = findSimilarItemStackInEntityEquipment(entity, itemStack, predicate);
        if(equip == null) {
            return findSimilarItemStackInEntityInventory(entity, itemStack, predicate);
        } else {
            return equip;
        }
    }

    private static @Nullable ItemStack findSimilarItemStackInEntityEquipment(
        final @NotNull LivingEntity entity,
        final @NotNull ItemStack itemStack,
        final @NotNull Predicate<ItemStack> predicate
    ) {
        final EntityEquipment equipment = entity.getEquipment();

        if(equipment == null) return null;

        for(final EquipmentSlot slot : EquipmentSlot.values()) {
            final ItemStack itemInSlot = equipment.getItem(slot);
            if(predicate.test(itemInSlot) && itemStack.isSimilar(itemInSlot)) return itemInSlot;
        }

        return null;
    }

    private static @Nullable ItemStack findSimilarItemStackInEntityInventory(
        final @NotNull LivingEntity entity,
        final @NotNull ItemStack itemStack,
        final @NotNull Predicate<ItemStack> predicate
    ) {
        if(!(entity instanceof final InventoryHolder ih)) return null;

        for(final ItemStack otherStack : ih.getInventory().getContents()) {
            if(otherStack == null)
                continue;

            if(predicate.test(itemStack) && itemStack.isSimilar(otherStack))
                return otherStack;
        }

        return null;
    }

}
