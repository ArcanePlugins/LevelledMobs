package io.github.arcaneplugins.levelledmobs.bukkit.logic.customdrops.recipient;

import io.github.arcaneplugins.levelledmobs.bukkit.logic.customdrops.CustomDrop;
import io.github.arcaneplugins.levelledmobs.bukkit.util.modal.impl.ModalEntityTypeSet;
import java.util.Collection;
import javax.annotation.Nonnull;
import org.jetbrains.annotations.NotNull;

public class DropTableRecipient extends CustomDropRecipient {

    private final String id;
    private final ModalEntityTypeSet applicableEntities;

    public DropTableRecipient(
        final @Nonnull String id,
        final @NotNull Collection<CustomDrop> drops,
        final float overallChance,
        final @NotNull Collection<String> overallPermissions,
        final ModalEntityTypeSet applicableEntities
    ) {
        super(drops, overallChance, overallPermissions);
        this.applicableEntities = applicableEntities;
        this.id = id;
    }

    @NotNull
    @Override
    public CustomDropRecipientType getRecipientType() {
        return CustomDropRecipientType.DROP_TABLE;
    }

    @Nonnull
    public ModalEntityTypeSet getApplicableEntityTypes() {
        return applicableEntities;
    }

    @Nonnull
    public String getId() {
        return id;
    }

}
