package me.lokka30.levelledmobs.misc;

import org.bukkit.inventory.ItemStack;

/**
 * @author stumper66
 */
public class NBT_ApplyResult {
    public ItemStack itemStack;
    public String exceptionMessage;

    public boolean hadException(){
        return this.exceptionMessage != null;
    }
}
