package io.github.arcaneplugins.levelledmobs.bukkit.logic.customdrops;

import java.util.List;
import javax.annotation.Nonnull;
import org.bukkit.inventory.ItemStack;

//TODO instantiate
public class CustomDropResult {

    private final List<ItemStack> drops;
    private final boolean overridesVanillaDrops;
    private final boolean overridesNonVanillaDrops;

    public CustomDropResult(
        @Nonnull final List<ItemStack> drops,
        final boolean overridesVanillaDrops,
        final boolean overridesNonVanillaDrops
    ) {
        this.drops = drops;
        this.overridesVanillaDrops = overridesVanillaDrops;
        this.overridesNonVanillaDrops = overridesNonVanillaDrops;
    }

    public @Nonnull List<ItemStack> getDrops() {
        return drops;
    }

    public boolean overridesVanillaDrops() {
        return overridesVanillaDrops;
    }

    public boolean overridesNonVanillaDrops() {
        return overridesNonVanillaDrops;
    }
}
