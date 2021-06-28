package me.lokka30.levelledmobs.misc;

import org.bukkit.inventory.ItemStack;

public class NBTParseResult {
    public ItemStack itemStack;
    public String exceptionMessage;

    public boolean hadException(){
        return this.exceptionMessage != null;
    }
}
