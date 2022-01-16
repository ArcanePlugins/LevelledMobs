package me.lokka30.levelledmobs.rules.condition.type;

import de.leonhard.storage.sections.FlatFileSection;
import me.lokka30.levelledmobs.rules.Rule;
import me.lokka30.levelledmobs.rules.condition.RuleCondition;
import me.lokka30.levelledmobs.rules.condition.RuleConditionType;
import me.lokka30.levelledmobs.util.ModalList;
import me.lokka30.levelledmobs.util.Utils;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;

public record EntityTypeCondition(
        @NotNull Rule                   parentRule,
        @NotNull ModalList<EntityType>  types,
        boolean                         inverse
) implements RuleCondition {

    @Override @NotNull
    public RuleConditionType type() {
        return RuleConditionType.ENTITY_TYPE;
    }

    @Override
    public boolean appliesTo(@NotNull LivingEntity livingEntity) {
        return inverse() != types.contains(livingEntity.getType());
    }

    @NotNull
    public static EntityTypeCondition of(final @NotNull Rule parentRule, final @NotNull FlatFileSection section) {
        final ModalList.ListMode listMode;
        final EnumSet<EntityType> contents;

        if(section.contains(".inclusive-list")) {
            listMode = ModalList.ListMode.INCLUSIVE;
            contents = EnumSet.noneOf(EntityType.class);
            for(String entityTypeStr : section.getStringList(".inclusive-list")) {
                final EntityType entityType;
                try {
                    entityType = EntityType.valueOf(entityTypeStr);
                } catch(IllegalArgumentException ex) {
                    Utils.LOGGER.error("Invalid EntityType '&b" + entityTypeStr + "&7' specified in" +
                            " condition located at '&b" + section.getPathPrefix() + "&7'! Fix this ASAP.");
                    continue;
                }
                contents.add(entityType);
            }
        } else if(section.contains(".exclusive-list")) {
            listMode = ModalList.ListMode.EXCLUSIVE;
            contents = EnumSet.noneOf(EntityType.class);
            for(String entityTypeStr : section.getStringList(".exclusive-list")) {
                final EntityType entityType;
                try {
                    entityType = EntityType.valueOf(entityTypeStr);
                } catch(IllegalArgumentException ex) {
                    Utils.LOGGER.error("Invalid EntityType '&b" + entityTypeStr + "&7' specified in" +
                            " condition located at '&b" + section.getPathPrefix() + "&7'! Fix this ASAP.");
                    continue;
                }
                contents.remove(entityType);
            }
        } else {
            throw new IllegalArgumentException("EntityType condition does not have a valid modal list present at path" +
                    " '&b" + section.getPathPrefix() + "&7'! Fix this ASAP.");
        }

        return new EntityTypeCondition(
                parentRule,
                new ModalList<>(listMode, contents),
                section.get(".inverse", false)
        );
    }
}
