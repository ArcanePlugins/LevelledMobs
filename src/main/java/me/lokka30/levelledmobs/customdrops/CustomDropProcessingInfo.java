package me.lokka30.levelledmobs.customdrops;

import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nonnull;
import java.util.TreeMap;
import java.util.List;
import java.util.Map;

public class CustomDropProcessingInfo {
    public CustomDropProcessingInfo(){
        this.groupIDsDroppedAlready = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    }

    public LivingEntity livingEntity;
    public int level;
    public List<ItemStack> newDrops;
    public boolean isSpawner;
    public boolean equippedOnly;
    public boolean deathByFire;
    public int addition;
    public boolean doNotMultiplyDrops;
    public boolean hasOverride;
    @Nonnull
    final public Map<String, Integer> groupIDsDroppedAlready;
    //public List<CustomDropItem> combinedDrops;
    public Map<Integer, List<CustomDropItem>> prioritizedDrops;
}
