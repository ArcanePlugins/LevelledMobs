package io.github.arcaneplugins.levelledmobs.bukkit.logic.label;

import de.themoep.minedown.adventure.MineDown;
import io.github.arcaneplugins.levelledmobs.bukkit.api.data.EntityDataUtil;
import io.github.arcaneplugins.levelledmobs.bukkit.data.InternalEntityDataUtil;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.LogicHandler;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.Context;
import io.github.arcaneplugins.levelledmobs.bukkit.util.Log;
import javax.annotation.Nonnull;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.minimessage.MiniMessage;
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
    public Component generateLabelComponents(
        final @Nonnull LivingEntity lent,
        final @Nonnull Context context
    ) {
        final Component label = MineDown.parse(
            LogicHandler.replacePapiAndContextPlaceholders(getFormula(lent), context)
        );

        Component replacement;
        if(context.getEntity() != null) {
            replacement = Component.translatable(context.getEntity().getType().translationKey());
        } else if(context.getEntityType() != null) {
            replacement = Component.translatable(context.getEntityType().translationKey());
        } else {
            // TODO error
            Log.war("Unable to replace entity name placeholder in message '" +
                    getFormula(lent) + "': "
                    + "no entity/entity-type context", true);
            replacement = Component.empty();
        }

        return label.replaceText(TextReplacementConfig.builder()
                .matchLiteral("%entity-name%").replacement(replacement).build());
    }

    //TODO use?
    @Nonnull
    public String generateLabelLegacy(
        final @Nonnull LivingEntity lent,
        final @Nonnull Context context
    ) {
        return MiniMessage.miniMessage().serialize(
                generateLabelComponents(lent, context));
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
