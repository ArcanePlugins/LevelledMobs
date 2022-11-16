package io.github.arcaneplugins.levelledmobs.bukkit.logic.customdrops.recipient;

import io.github.arcaneplugins.levelledmobs.bukkit.logic.customdrops.type.ItemCustomDrop;
import io.github.arcaneplugins.levelledmobs.bukkit.util.modal.impl.ModalEntityTypeSet;
import java.util.Collection;
import javax.annotation.Nonnull;
import org.jetbrains.annotations.NotNull;

public class DropTableRecipient extends CustomDropRecipient {

    private final ModalEntityTypeSet applicableEntities;

    public DropTableRecipient(
        final @NotNull Collection<ItemCustomDrop> drops,
        final int overallChance,
        final @NotNull Collection<String> overallPermissions,
        final ModalEntityTypeSet applicableEntities
    ) {
        super(drops, overallChance, overallPermissions);
        this.applicableEntities = applicableEntities;
    }

    @NotNull
    @Override
    public CustomDropRecipientType getRecipientType() {
        return CustomDropRecipientType.DROP_TABLE;
    }

    @Nonnull
    public ModalEntityTypeSet getApplicableEntities() {
        return applicableEntities;
    }

}
