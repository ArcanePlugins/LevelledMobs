package me.lokka30.levelledmobs.bukkit.integration.nbthandler;

import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public interface NbtProvider {

    @NotNull
    NbtModificationResult addNbtTag(final ItemStack itemStack, final String tag);

    @NotNull
    NbtModificationResult addNbtTag(final LivingEntity entity, final String tag);

    @NotNull
    String getNbtDump(final ItemStack itemStack);

    @NotNull
    String getNbtDump(final LivingEntity entity);

}
