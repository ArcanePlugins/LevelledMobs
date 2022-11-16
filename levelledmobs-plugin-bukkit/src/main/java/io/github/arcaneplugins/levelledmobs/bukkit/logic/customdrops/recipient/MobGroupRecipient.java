package io.github.arcaneplugins.levelledmobs.bukkit.logic.customdrops.recipient;

import io.github.arcaneplugins.levelledmobs.bukkit.logic.customdrops.type.ItemCustomDrop;
import java.util.Collection;
import javax.annotation.Nonnull;
import org.jetbrains.annotations.NotNull;

public class MobGroupRecipient extends CustomDropRecipient {

    private final String mobGroupId;

    public MobGroupRecipient(
        final @Nonnull Collection<ItemCustomDrop> drops,
        final int overallChance,
        final @Nonnull Collection<String> overallPermissions,
        final @Nonnull String mobGroupId
    ) {
        super(drops, overallChance, overallPermissions);
        this.mobGroupId = mobGroupId;
    }

    @NotNull
    @Override
    public CustomDropRecipientType getRecipientType() {
        return CustomDropRecipientType.MOB_GROUP;
    }

    public @Nonnull String getMobGroupId() {
        return mobGroupId;
    }

}
