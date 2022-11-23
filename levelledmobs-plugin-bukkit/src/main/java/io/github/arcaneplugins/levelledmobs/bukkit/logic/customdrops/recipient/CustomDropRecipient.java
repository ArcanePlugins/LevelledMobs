package io.github.arcaneplugins.levelledmobs.bukkit.logic.customdrops.recipient;

import io.github.arcaneplugins.levelledmobs.bukkit.logic.customdrops.type.ItemCustomDrop;
import java.util.Collection;
import javax.annotation.Nonnull;

public abstract class CustomDropRecipient {

    private final Collection<ItemCustomDrop> drops;
    private final float overallChance;
    private final Collection<String> overallPermissions;

    public CustomDropRecipient(
        final @Nonnull Collection<ItemCustomDrop> drops,
        final float overallChance,
        final @Nonnull Collection<String> overallPermissions
    ) {
        this.drops = drops;
        this.overallChance = overallChance;
        this.overallPermissions = overallPermissions;
    }

    @Nonnull
    public abstract CustomDropRecipientType getRecipientType();

    @Nonnull
    public Collection<ItemCustomDrop> getDrops() {
        return drops;
    }

    public float getOverallChance() {
        return overallChance;
    }

    @Nonnull
    public Collection<String> getOverallPermissions() {
        return overallPermissions;
    }

}
