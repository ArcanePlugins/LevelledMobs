package me.lokka30.levelledmobs.bukkit.logic.label;

import de.themoep.minedown.MineDown;
import javax.annotation.Nonnull;
import me.lokka30.levelledmobs.bukkit.LevelledMobs;
import me.lokka30.levelledmobs.bukkit.data.InternalEntityDataUtil;
import me.lokka30.levelledmobs.bukkit.logic.context.Context;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.LivingEntity;

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

}
