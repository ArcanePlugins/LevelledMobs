package me.lokka30.levelledmobs.customdrops;

import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nonnull;
import java.util.LinkedList;
import java.util.List;

public class CustomDropProcessingInfo {
    public CustomDropProcessingInfo(){
        this.groupIDsProcessed = new LinkedList<>();
    }

    public LivingEntity livingEntity;
    public int level;
    public CustomDropInstance dropInstance;
    public List<ItemStack> newDrops;
    public boolean isSpawner;
    public boolean equippedOnly;
    public boolean deathByFire;
    public int addition;
    public boolean doNotMultiplyDrops;
    @Nonnull
    public List<String> groupIDsProcessed;
}
