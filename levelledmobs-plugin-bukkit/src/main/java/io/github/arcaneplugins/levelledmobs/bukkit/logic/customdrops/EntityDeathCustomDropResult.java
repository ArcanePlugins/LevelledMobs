package io.github.arcaneplugins.levelledmobs.bukkit.logic.customdrops;

import java.util.Collection;
import javax.annotation.Nonnull;
import org.bukkit.inventory.ItemStack;

//TODO instantiate
public class EntityDeathCustomDropResult {

    private final Collection<ItemStack> drops;
    private final boolean overridesVanillaDrops;
    private final boolean overridesNonVanillaDrops;

    public EntityDeathCustomDropResult(
        @Nonnull final Collection<ItemStack> drops,
        final boolean overridesVanillaDrops,
        final boolean overridesNonVanillaDrops
    ) {
        this.drops = drops;
        this.overridesVanillaDrops = overridesVanillaDrops;
        this.overridesNonVanillaDrops = overridesNonVanillaDrops;
    }

    public @Nonnull Collection<ItemStack> getDrops() {
        return drops;
    }

    public boolean overridesVanillaDrops() {
        return overridesVanillaDrops;
    }

    public boolean overridesNonVanillaDrops() {
        return overridesNonVanillaDrops;
    }
}
