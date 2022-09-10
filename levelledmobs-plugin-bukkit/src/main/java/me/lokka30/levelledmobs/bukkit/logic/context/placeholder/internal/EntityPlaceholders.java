package me.lokka30.levelledmobs.bukkit.logic.context.placeholder.internal;

import static me.lokka30.levelledmobs.bukkit.util.StringUtils.replaceIfExists;

import me.lokka30.levelledmobs.bukkit.LevelledMobs;
import me.lokka30.levelledmobs.bukkit.config.translations.TranslationHandler;
import me.lokka30.levelledmobs.bukkit.logic.context.Context;
import me.lokka30.levelledmobs.bukkit.logic.context.placeholder.ContextPlaceholder;
import me.lokka30.levelledmobs.bukkit.util.EnumUtils;
import me.lokka30.levelledmobs.bukkit.util.MathUtils;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class EntityPlaceholders implements ContextPlaceholder {

    @SuppressWarnings("ConstantConditions")
    @Override
    public @NotNull String replace(String str, Context context) {
        final EntityType entityType = context.getEntityType();
        final Entity entity = context.getEntity();

        if(entityType == null && entity == null)
            return str;

        if(entityType != null) {
            str = replaceIfExists(str, "%entity-type%", entityType::name);

            str = replaceIfExists(str, "%entity-type-formatted%", () -> EnumUtils
                .formatEnumConstant(entityType));
        }

        if(entity != null) {
            str = replaceIfExists(str, "%entity-name%", () ->
                getEntityName(entityType, entity));

            if(entity instanceof LivingEntity lent) {
                str = replaceIfExists(str, "%entity-health%", () ->
                    Double.toString(lent.getHealth()));

                str = replaceIfExists(str, "%entity-health-rounded%", () ->
                    Double.toString(MathUtils.round2dp(lent.getHealth())));

                str = replaceIfExists(str, "%entity-max-health%", () ->
                    Double.toString(lent.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue()));

                str = replaceIfExists(str, "%entity-max-health-rounded%", () ->
                    Double.toString(MathUtils.round2dp(
                        lent.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue()
                    )));
            }
        }

        return str;
    }

    @NotNull
    private String getEntityName(
        final @Nullable EntityType entityType,
        final @Nullable Entity entity
    ) {
        final TranslationHandler translationHandler = LevelledMobs.getInstance()
            .getConfigHandler()
            .getTranslationHandler();

        if(entityType != null) {
            return translationHandler.getEntityName(entityType);
        } else {
            assert entity != null;
            return translationHandler.getEntityName(entity);
        }
    }

}
