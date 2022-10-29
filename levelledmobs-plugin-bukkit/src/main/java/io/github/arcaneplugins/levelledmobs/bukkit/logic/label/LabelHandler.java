package io.github.arcaneplugins.levelledmobs.bukkit.logic.label;

import de.themoep.minedown.MineDown;
import io.github.arcaneplugins.levelledmobs.bukkit.LevelledMobs;
import io.github.arcaneplugins.levelledmobs.bukkit.api.data.EntityDataUtil;
import io.github.arcaneplugins.levelledmobs.bukkit.data.InternalEntityDataUtil;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.Context;
import javax.annotation.Nonnull;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
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

    @Nonnull
    public BaseComponent[] generateLabelComponents(
        final @Nonnull LivingEntity lent,
        final @Nonnull Context context
    ) {
        return MineDown.parse(
            LevelledMobs.getInstance().getLogicHandler().getContextPlaceholderHandler().replace(
                getFormula(lent), context
            )
        );
    }

    @Nonnull
    public String generateLabelLegacy(
        final @Nonnull LivingEntity lent,
        final @Nonnull Context context
    ) {
        return TextComponent.toLegacyText(generateLabelComponents(lent, context));
    }

    public abstract void update(
        final @Nonnull LivingEntity lent,
        final @Nonnull Context context
    );

    public abstract void update(
        final @Nonnull Player player,
        final @Nonnull Context context
    );

    public abstract void update(
        final @Nonnull LivingEntity lent,
        final @Nonnull Player player,
        final @Nonnull Context context
    );

    protected void deferPlayerUpdate(
        final @Nonnull Player player,
        final @Nonnull Context context
    ) {
        //TODO customisable range. may want to use a more efficient nearby entities method too
        player
            .getNearbyEntities(50, 50, 50)
            .stream()
            .filter(entity -> entity instanceof LivingEntity)
            .map(entity -> (LivingEntity) entity)
            .filter(entity -> EntityDataUtil.isLevelled(entity, false))
            .forEach(entity -> update(entity, player, context));
    }

    protected void deferEntityUpdate(
        final @Nonnull LivingEntity lent,
        final @Nonnull Context context
    ) {
        //TODO customisable range. may want to use a more efficient nearby entities method too
        lent
            .getNearbyEntities(50, 50, 50)
            .stream()
            .filter(otherEntity -> otherEntity instanceof Player)
            .map(player -> (Player) player)
            .forEach(player -> update(lent, player, context));
    }

}
