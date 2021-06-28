package me.lokka30.levelledmobs.managers;

import de.tr7zw.nbtapi.NBTContainer;
import de.tr7zw.nbtapi.NBTItem;
import me.lokka30.levelledmobs.customdrops.CustomDropItem;
import me.lokka30.levelledmobs.misc.NBTParseResult;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class NBTManager {

    @NotNull
    public static NBTParseResult parseNBT_Data(@NotNull final CustomDropItem item, @NotNull final String nbtStuff){
        final NBTParseResult result = new NBTParseResult();
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
}
