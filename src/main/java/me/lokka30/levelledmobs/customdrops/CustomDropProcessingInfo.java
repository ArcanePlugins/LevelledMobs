package me.lokka30.levelledmobs.customdrops;

import me.lokka30.levelledmobs.misc.LivingEntityWrapper;
import me.lokka30.levelledmobs.rules.CustomDropsRuleSet;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.*;

/**
 * Used to store information when a custom drop item
 * is being requested either during mob spawn in for
 * equipped items or after mob death to get the items
 * the mob will potentially drop
 *
 * @author stumper66
 */
public class CustomDropProcessingInfo {
    public CustomDropProcessingInfo() {
        this.groupIDsDroppedAlready = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        this.allDropInstances = new LinkedList<>();
    }

    public LivingEntityWrapper lmEntity;
    public int addition;
    public boolean isSpawner;
    public boolean equippedOnly;
    public boolean deathByFire;
    public boolean wasKilledByPlayer;
    public boolean doNotMultiplyDrops;
    public boolean hasOverride;
    public boolean hasCustomDropId;
    public boolean madeOverallChance;
    public boolean hasEquippedItems;
    public String customDropId;
    public List<ItemStack> newDrops;
    @Nonnull
    final public Map<String, Integer> groupIDsDroppedAlready;
    public Map<Integer, List<CustomDropBase>> prioritizedDrops;
    @Nullable
    public CustomDropsRuleSet dropRules;
    @NotNull
    final public List<CustomDropInstance> allDropInstances;
}
