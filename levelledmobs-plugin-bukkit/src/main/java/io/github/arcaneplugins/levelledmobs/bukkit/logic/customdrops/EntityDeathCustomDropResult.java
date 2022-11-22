package io.github.arcaneplugins.levelledmobs.bukkit.logic.customdrops;

import java.util.Collection;
import javax.annotation.Nonnull;

//TODO instantiate
public class EntityDeathCustomDropResult {

    private final Collection<CustomDrop> drops;
    private final boolean overridesVanillaDrops;
    private final boolean overridesNonVanillaDrops;

    public EntityDeathCustomDropResult(
        @Nonnull final Collection<CustomDrop> drops,
        final boolean overridesVanillaDrops,
        final boolean overridesNonVanillaDrops
    ) {
        this.drops = drops;
        this.overridesVanillaDrops = overridesVanillaDrops;
        this.overridesNonVanillaDrops = overridesNonVanillaDrops;
    }

    public @Nonnull Collection<CustomDrop> getDrops() {
        return drops;
    }

    public boolean overridesVanillaDrops() {
        return overridesVanillaDrops;
    }

    public boolean overridesNonVanillaDrops() {
        return overridesNonVanillaDrops;
    }
}
