package io.github.arcaneplugins.levelledmobs.bukkit.api.data;

import static org.bukkit.persistence.PersistentDataType.BYTE;

import io.github.arcaneplugins.levelledmobs.bukkit.api.data.keys.ItemKeyStore;
import io.github.arcaneplugins.levelledmobs.bukkit.api.util.TriState;
import javax.annotation.Nonnull;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;

public class ItemDataUtil {

    private ItemDataUtil() throws IllegalAccessException {
        throw new IllegalAccessException("Attempted instantiation of utility class");
    }

    public static @Nonnull TriState isItemCustom(
        final @Nonnull ItemStack stack
    ) {
        final ItemMeta im = stack.getItemMeta();
        if(im == null || !stack.hasItemMeta()) return TriState.UNKNOWN;
        final PersistentDataContainer pdc = im.getPersistentDataContainer();

        //noinspection ConstantConditions
        return TriState.of(
            pdc.has(ItemKeyStore.IS_CUSTOM_ITEM, BYTE) &&
                pdc.get(ItemKeyStore.IS_CUSTOM_ITEM, BYTE) == 1
        );
    }

    public static void setIsItemCustom(
        final @Nonnull ItemStack stack,
        final boolean isCustom
    ) {
        final ItemMeta im = stack.getItemMeta();
        if(im == null || !stack.hasItemMeta())
            throw new IllegalArgumentException("Unable to set PDC value to item: no metadata");
        final PersistentDataContainer pdc = im.getPersistentDataContainer();

        pdc.set(ItemKeyStore.IS_CUSTOM_ITEM, BYTE, (byte) (isCustom ? 1 : 0));
    }

}
