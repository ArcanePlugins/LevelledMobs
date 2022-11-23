package io.github.arcaneplugins.levelledmobs.bukkit.logic.customdrops.recipient;

import io.github.arcaneplugins.levelledmobs.bukkit.logic.customdrops.type.ItemCustomDrop;
import java.util.Collection;
import javax.annotation.Nonnull;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;

public class EntityTypeRecipient extends CustomDropRecipient {

    private final EntityType entityType;

    public EntityTypeRecipient(
        final @Nonnull Collection<ItemCustomDrop> drops,
        final float overallChance,
        final @Nonnull Collection<String> overallPermissions,
        final EntityType entityType
    ) {
        super(drops, overallChance, overallPermissions);
        this.entityType = entityType;
    }

    @NotNull
    @Override
    public CustomDropRecipientType getRecipientType() {
        return CustomDropRecipientType.ENTITY_TYPE;
    }

    public @Nonnull EntityType getEntityType() {
        return entityType;
    }

}
