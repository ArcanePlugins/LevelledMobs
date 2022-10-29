package io.github.arcaneplugins.levelledmobs.bukkit.integration.impl;

import de.tr7zw.nbtapi.NBTContainer;
import de.tr7zw.nbtapi.NBTEntity;
import de.tr7zw.nbtapi.NBTItem;
import io.github.arcaneplugins.levelledmobs.bukkit.integration.type.nbt.NbtModificationResult;
import io.github.arcaneplugins.levelledmobs.bukkit.integration.type.nbt.NbtProvider;
import io.github.arcaneplugins.levelledmobs.bukkit.integration.Integration;
import io.github.arcaneplugins.levelledmobs.bukkit.integration.IntegrationPriority;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class NbtApiIntegration extends Integration implements NbtProvider {

    public NbtApiIntegration() {
        super(
            "NBTAPI",
            "Allows LevelledMobs to modify the NBT data of items and entities.",
            true,
            true,
            IntegrationPriority.NORMAL
        );

        setEnabled(Bukkit.getPluginManager().isPluginEnabled("NBTAPI"));
    }

    @Override
    public @NotNull NbtModificationResult addNbtTag(ItemStack itemStack, String tag) {
        final var result = new NbtModificationResult(itemStack);

        try {
            final var nbtItem = new NBTItem(itemStack);

            final var jsonBefore = nbtItem.toString();
            nbtItem.mergeCompound(new NBTContainer(tag));
            final var jsonAfter = nbtItem.toString();

            if(jsonBefore.equals(jsonAfter)) {
                result.withException(new RuntimeException(
                    "No NBT modification detected: ensure you are using a correct tag."
                ));
            }
        } catch(Exception ex) {
            result.withException(ex);
        }

        return result;
    }

    @Override
    public @NotNull NbtModificationResult addNbtTag(LivingEntity entity, String tag) {
        final var result = new NbtModificationResult(entity);

        try {
            final var nbtEntity = new NBTEntity(entity);

            final var jsonBefore = nbtEntity.toString();
            nbtEntity.mergeCompound(new NBTContainer(tag));
            final var jsonAfter = nbtEntity.toString();

            if(jsonBefore.equals(jsonAfter)) {
                result.withException(new RuntimeException(
                    "No NBT modification detected: ensure you are using a correct tag."
                ));
            }
        } catch(Exception ex) {
            result.withException(ex);
        }

        return result;
    }

    @Override
    public @NotNull String getNbtDump(ItemStack itemStack) {
        return new NBTItem(itemStack).toString();
    }

    @Override
    public @NotNull String getNbtDump(LivingEntity entity) {
        return new NBTEntity(entity).toString();
    }
}
