package me.lokka30.levelledmobs.rules.condition;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Optional;

public enum RuleConditionType {
    // Retain alphabetical order please!
    ENTITY_TYPE("entity-type"),
    IS_LEVELLED("is-levelled"),
    LIGHT_LEVEL_FROM_BLOCK("light-level-from-blocks"),
    LIGHT_LEVEL_FROM_SKY("light-level-from-sky");

    private final String conditionId;
    RuleConditionType(String conditionId) {
        this.conditionId = conditionId;
    }

    @NotNull
    public String getConditionId() { return conditionId; }

    @NotNull
    public static Optional<RuleConditionType> fromId(final @NotNull String conditionId) {
        return Arrays.stream(values())
                .filter(type -> type.getConditionId().equals(conditionId))
                .findFirst();
    }
}
