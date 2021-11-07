/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.managers;

import de.tr7zw.nbtapi.NBTContainer;
import de.tr7zw.nbtapi.NBTEntity;
import de.tr7zw.nbtapi.NBTItem;
import me.lokka30.levelledmobs.customdrops.CustomDropItem;
import me.lokka30.levelledmobs.misc.LivingEntityWrapper;
import me.lokka30.levelledmobs.misc.NBTApplyResult;
import org.jetbrains.annotations.NotNull;

/**
 * @author stumper66
 * @since 3.1.0
 */
public class NBTManager {

    @NotNull
    public static NBTApplyResult applyNBT_Data_Item(@NotNull final CustomDropItem item, @NotNull final String nbtStuff) {
        final NBTApplyResult result = new NBTApplyResult();
        final NBTItem nbtent = new NBTItem(item.getItemStack());

        try {
            nbtent.mergeCompound(new NBTContainer(nbtStuff));
            result.itemStack = nbtent.getItem();
        } catch (Exception e) {
            result.exceptionMessage = e.getMessage();
        }

        return result;
    }

    public static @NotNull NBTApplyResult applyNBT_Data_Mob(@NotNull final LivingEntityWrapper lmEntity, @NotNull final String nbtStuff) {
        final NBTApplyResult result = new NBTApplyResult();

        try {
            final NBTEntity nbtent = new NBTEntity(lmEntity.getLivingEntity());
            final String jsonBefore = nbtent.toString();
            nbtent.mergeCompound(new NBTContainer(nbtStuff));
            final String jsonAfter = nbtent.toString();

            if (jsonBefore.equals(jsonAfter))
                result.exceptionMessage = "No NBT data changed.  Make sure you have used proper NBT strings";
        } catch (Exception e) {
            result.exceptionMessage = e.getMessage();
        }

        return result;
    }
}
