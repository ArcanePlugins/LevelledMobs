/*
This program is/was a part of the LevelledMobs project's source code.
Copyright (C) 2023  Lachlan Adamson (aka lokka30)
Copyright (C) 2023  LevelledMobs Contributors

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package io.github.arcaneplugins.levelledmobs.api.bukkit;

import static org.bukkit.persistence.PersistentDataType.BYTE;

import io.github.arcaneplugins.levelledmobs.api.bukkit.keys.ItemKeyStore;
import io.github.arcaneplugins.levelledmobs.api.bukkit.misc.TriState;
import javax.annotation.Nonnull;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;

//todo doc
public class ItemDataUtil {

    //todo doc
    private ItemDataUtil() throws IllegalAccessException {
        throw new IllegalAccessException("Attempted instantiation of utility class");
    }

    //todo doc
    public static @Nonnull TriState isItemCustom(
        final @Nonnull ItemStack stack
    ) {
        final ItemMeta im = stack.getItemMeta();
        if(im == null || !stack.hasItemMeta()) return TriState.UNSPECIFIED;
        final PersistentDataContainer pdc = im.getPersistentDataContainer();

        //noinspection ConstantConditions
        return TriState.of(
            pdc.has(ItemKeyStore.IS_CUSTOM_ITEM, BYTE) &&
                pdc.get(ItemKeyStore.IS_CUSTOM_ITEM, BYTE) == 1
        );
    }

    //todo doc
    public static void setIsItemCustom(
        final @Nonnull ItemStack stack,
        final boolean isCustom
    ) {
        final ItemMeta im = stack.getItemMeta();
        if(im == null)
            throw new IllegalArgumentException("Unable to set PDC value to item: metadata is null");
        final PersistentDataContainer pdc = im.getPersistentDataContainer();

        pdc.set(ItemKeyStore.IS_CUSTOM_ITEM, BYTE, (byte) (isCustom ? 1 : 0));
    }

}
