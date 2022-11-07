package io.github.arcaneplugins.levelledmobs.bukkit.logic.context.placeholder.impl;

import io.github.arcaneplugins.levelledmobs.bukkit.LevelledMobs;
import io.github.arcaneplugins.levelledmobs.bukkit.config.translations.TranslationHandler;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.Context;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.placeholder.ContextPlaceholder;
import io.github.arcaneplugins.levelledmobs.bukkit.util.StringUtils;
import io.github.arcaneplugins.levelledmobs.bukkit.util.math.MathUtils;
import io.github.arcaneplugins.levelledmobs.bukkit.api.data.EntityDataUtil;
import io.github.arcaneplugins.levelledmobs.bukkit.util.EnumUtils;
import java.util.Objects;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class StandardPlaceholders implements ContextPlaceholder {

    @SuppressWarnings("ConstantConditions")
    @Override
    public @NotNull String replace(String str, Context context) {

        final Player player = context.getPlayer();
        if(player != null) {
            str = StringUtils.replaceIfExists(str, "%player-name%", player::getName);
            //noinspection deprecation
            str = StringUtils.replaceIfExists(str, "%player-displayname%", player::getDisplayName);
        }

        final EntityType entityType = context.getEntityType();
        if(entityType != null) {
            str = StringUtils.replaceIfExists(str, "%entity-type%", entityType::name);
            str = StringUtils.replaceIfExists(str, "%entity-type-formatted%", () -> EnumUtils.formatEnumConstant(entityType));
        }

        final Entity entity = context.getEntity();
        if(entity != null) {
            if(entity instanceof LivingEntity lent) {
                final LivingEntity father = EntityDataUtil.getFather(lent, false);
                final LivingEntity mother = EntityDataUtil.getMother(lent, false);

                //noinspection deprecation
                str = StringUtils.replaceIfExists(str, "%entity-name%", () -> Objects.requireNonNullElse(lent.getCustomName(), "%entity-name%"));
                //noinspection deprecation
                str = StringUtils.replaceIfExists(str, "%father-name%", () -> Objects.requireNonNullElse(father.getCustomName(), "%father-name%"));
                //noinspection deprecation
                str = StringUtils.replaceIfExists(str, "%mother-name%", () -> Objects.requireNonNullElse(mother.getCustomName(), "%mother-name%"));

                str = StringUtils.replaceIfExists(str, "%entity-name%", () -> Objects.requireNonNullElse(EntityDataUtil.getOverriddenName(lent, false), "%entity-name%"));
                str = StringUtils.replaceIfExists(str, "%father-name%", () -> Objects.requireNonNullElse(EntityDataUtil.getOverriddenName(father, false), "%father-name%"));
                str = StringUtils.replaceIfExists(str, "%mother-name%", () -> Objects.requireNonNullElse(EntityDataUtil.getOverriddenName(mother, false), "%mother-name%"));

                //TODO use adventure to fetch translated entity name instead of formatting the type
                str = StringUtils.replaceIfExists(str, "%entity-name%", () -> EnumUtils.formatEnumConstant(entity.getType()));
                str = StringUtils.replaceIfExists(str, "%father-name%", () -> EnumUtils.formatEnumConstant(father.getType()));
                str = StringUtils.replaceIfExists(str, "%mother-name%", () -> EnumUtils.formatEnumConstant(mother.getType()));

                str = StringUtils.replaceIfExists(str, "%father-type%", () -> father.getType().name());
                str = StringUtils.replaceIfExists(str, "%mother-type%", () -> mother.getType().name());

                str = StringUtils.replaceIfExists(str, "%father-type-formatted%", () -> EnumUtils.formatEnumConstant(father.getType()));
                str = StringUtils.replaceIfExists(str, "%mother-type-formatted%", () -> EnumUtils.formatEnumConstant(mother.getType()));

                str = StringUtils.replaceIfExists(str, "%entity-health%", () -> Double.toString(lent.getHealth()));
                str = StringUtils.replaceIfExists(str, "%father-health%", () -> Double.toString(father.getHealth()));
                str = StringUtils.replaceIfExists(str, "%mother-health%", () -> Double.toString(mother.getHealth()));

                str = StringUtils.replaceIfExists(str, "%entity-health-rounded%", () -> Double.toString(MathUtils.round2dp(lent.getHealth())));
                str = StringUtils.replaceIfExists(str, "%father-health-rounded%", () -> Double.toString(MathUtils.round2dp(father.getHealth())));
                str = StringUtils.replaceIfExists(str, "%mother-health-rounded%", () -> Double.toString(MathUtils.round2dp(mother.getHealth())));

                str = StringUtils.replaceIfExists(str, "%entity-max-health%", () -> Double.toString(lent.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue()));
                str = StringUtils.replaceIfExists(str, "%father-max-health%", () -> Double.toString(father.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue()));
                str = StringUtils.replaceIfExists(str, "%mother-max-health%", () -> Double.toString(mother.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue()));

                str = StringUtils.replaceIfExists(str, "%entity-max-health-rounded%", () -> Double.toString(MathUtils.round2dp(lent.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue())));
                str = StringUtils.replaceIfExists(str, "%father-max-health-rounded%", () -> Double.toString(MathUtils.round2dp(father.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue())));
                str = StringUtils.replaceIfExists(str, "%mother-max-health-rounded%", () -> Double.toString(MathUtils.round2dp(mother.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue())));

                str = StringUtils.replaceIfExists(str, "%entity-level%", () -> Integer.toString(EntityDataUtil.getLevel(lent, false)));
                str = StringUtils.replaceIfExists(str, "%father-level%", () -> Integer.toString(EntityDataUtil.getLevel(father, false)));
                str = StringUtils.replaceIfExists(str, "%mother-level%", () -> Integer.toString(EntityDataUtil.getLevel(mother, false)));

                str = StringUtils.replaceIfExists(str, "%entity-min-level%", () -> Integer.toString(EntityDataUtil.getMinLevel(lent, false)));
                str = StringUtils.replaceIfExists(str, "%father-min-level%", () -> Integer.toString(EntityDataUtil.getMinLevel(father, false)));
                str = StringUtils.replaceIfExists(str, "%mother-min-level%", () -> Integer.toString(EntityDataUtil.getMinLevel(mother, false)));

                str = StringUtils.replaceIfExists(str, "%entity-max-level%", () -> Integer.toString(EntityDataUtil.getMaxLevel(lent, false)));
                str = StringUtils.replaceIfExists(str, "%father-max-level%", () -> Integer.toString(EntityDataUtil.getMaxLevel(father, false)));
                str = StringUtils.replaceIfExists(str, "%mother-max-level%", () -> Integer.toString(EntityDataUtil.getMaxLevel(mother, false)));

                str = StringUtils.replaceIfExists(str, "%entity-level-ratio%", () -> Float.toString(EntityDataUtil.getLevelRatio(lent, false)));
                str = StringUtils.replaceIfExists(str, "%father-level-ratio%", () -> Float.toString(EntityDataUtil.getLevelRatio(father, false)));
                str = StringUtils.replaceIfExists(str, "%mother-level-ratio%", () -> Float.toString(EntityDataUtil.getLevelRatio(mother, false)));

                str = StringUtils.replaceIfExists(str, "%entity-prefix%", () -> ""); //TODO continue implementing; band-aid
                str = StringUtils.replaceIfExists(str, "%father-prefix%", () -> ""); //TODO continue implementing; band-aid
                str = StringUtils.replaceIfExists(str, "%mother-prefix%", () -> ""); //TODO continue implementing; band-aid

                str = StringUtils.replaceIfExists(str, "%entity-suffix%", () -> ""); //TODO continue implementing; band-aid
                str = StringUtils.replaceIfExists(str, "%father-suffix%", () -> ""); //TODO continue implementing; band-aid
                str = StringUtils.replaceIfExists(str, "%mother-suffix%", () -> ""); //TODO continue implementing; band-aid
            }
        }

        return str;
    }

    //TODO unsure where to use this now
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
