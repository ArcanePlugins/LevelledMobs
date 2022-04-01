package me.lokka30.levelledmobs.plugin.bukkit.rule.condition;

import java.util.Arrays;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;

public enum DefaultRuleConditionType {
    // Retain alphabetical order please!
    ENTITY_TYPE("entity-type"),
    IS_LEVELLED("is-levelled"),
    LIGHT_LEVEL_FROM_BLOCK("light-level-from-blocks"),
    LIGHT_LEVEL_FROM_SKY("light-level-from-sky");

    private final String id;

    DefaultRuleConditionType(final @NotNull String id) {
        this.id = id;
    }

    @NotNull
    public String id() {
        return id;
    }

    @NotNull
    public static Optional<DefaultRuleConditionType> fromId(final @NotNull String id) {
        return Arrays.stream(values())
            .filter(type -> type.id().equals(id))
            .findFirst();
    }
}
