package io.github.arcaneplugins.levelledmobs.bukkit.logic.label;

import de.themoep.minedown.adventure.MineDown;
import io.github.arcaneplugins.entitylabellib.bukkit.PacketInterceptor.LabelResponse;
import io.github.arcaneplugins.levelledmobs.bukkit.data.InternalEntityDataUtil;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.LogicHandler;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.Context;
import javax.annotation.Nonnull;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

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

    @Deprecated // deprecation: use generateLabelResponse instead.
    @Nonnull
    public Component generateLabelComponents(
        final @Nonnull LivingEntity lent,
        final @Nonnull Context context
    ) {
        return MineDown.parse(
            LogicHandler.replacePapiAndContextPlaceholders(
                getFormula(lent),
                context
            )
        );
    }

    @Nonnull
    public abstract LabelResponse generateLabelResponse(
        final @Nonnull LivingEntity lent,
        final @Nonnull Player player,
        final @Nonnull Context context
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
