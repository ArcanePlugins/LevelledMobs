package me.lokka30.levelledmobs.managers;

import de.tr7zw.nbtapi.NBTContainer;
import de.tr7zw.nbtapi.NBTEntity;
import de.tr7zw.nbtapi.NBTItem;
import io.lumine.xikage.mythicmobs.utils.shadows.nbt.NBTTagCompound;
import me.lokka30.levelledmobs.customdrops.CustomDropItem;
import me.lokka30.levelledmobs.misc.LivingEntityWrapper;
import me.lokka30.levelledmobs.misc.NBT_ApplyResult;
import me.lokka30.levelledmobs.misc.Utils;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.Objects;

public class NBTManager {

    @NotNull
    public static NBT_ApplyResult applyNBT_Data_Item(@NotNull final CustomDropItem item, @NotNull final String nbtStuff){
        final NBT_ApplyResult result = new NBT_ApplyResult();
        final NBTItem nbtent = new NBTItem(item.getItemStack());

        try {
            nbtent.mergeCompound(new NBTContainer(nbtStuff));
            result.itemStack = nbtent.getItem();
        }
        catch (Exception e){
            result.exceptionMessage = e.getMessage();
        }

        return result;
    }

    public static NBT_ApplyResult applyNBT_Data_Mob(@NotNull final LivingEntityWrapper lmEntity, @NotNull final String nbtStuff){
        final NBT_ApplyResult result = new NBT_ApplyResult();

        try {
            final NBTEntity nbtent = new NBTEntity(lmEntity.getLivingEntity());
            final String jsonBefore = nbtent.toString();
            nbtent.mergeCompound(new NBTContainer(nbtStuff));
            final String jsonAfter = nbtent.toString();

            if (jsonBefore.equals(jsonAfter))
                result.exceptionMessage = "No NBT data changed.  Make sure you have used proper NBT strings";
        }
        catch (Exception e){
            result.exceptionMessage = e.getMessage();
        }

        return result;
    }
}
