package io.github.arcaneplugins.levelledmobs.bukkit.logic.label;

import io.github.arcaneplugins.entitylabellib.bukkit.PacketInterceptor.LabelResponse;
import io.github.arcaneplugins.levelledmobs.bukkit.data.InternalEntityDataUtil;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.Context;
import javax.annotation.Nonnull;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public abstract class LabelHandler {

    private final String id;

    public LabelHandler(
        @Nonnull String id
    ) {
        this.id = id;
    }

    @Nonnull
    public String getId() {
        return id;
    }

    @Nonnull
    public String getFormula(
        final @Nonnull LivingEntity lent
    ) {
        return InternalEntityDataUtil.getLabelHandlerFormulaMap(lent, false)
            .getOrDefault(getId(), "");
    }

    // note: player can be null or non-null, the implementation should annotate whichever it wants.
    @NotNull
    public abstract LabelResponse generateLabelResponse(
        final @NotNull LivingEntity lent,
        final Player player,
        final @NotNull Context context
    );

    public abstract void update(
        final @Nonnull LivingEntity lent,
        final @Nonnull Context context
    );

    public abstract void update(
        final @Nonnull LivingEntity lent,
        final @Nonnull Player player,
        final @Nonnull Context context
    );

    protected void deferEntityUpdate(
        final @Nonnull LivingEntity entity,
        final @Nonnull Context context
    ) {
        //TODO customisable range. may want to use a more efficient nearby entities method too
        entity
            .getNearbyEntities(50, 50, 50)
            .stream()
            .filter(otherEntity -> otherEntity instanceof Player)
            .map(player -> (Player) player)
            .forEach(player -> update(entity, player, context));
    }

}
