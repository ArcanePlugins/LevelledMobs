package me.lokka30.levelledmobs.rules.option;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Optional;

public enum DefaultRuleOptionType {

    TEMPORARY_DO_NOT_USE("temporary-do-not-use");

    private final String id;
    DefaultRuleOptionType(String id) {
        this.id = id;
    }

    @NotNull
    public String getId() { return id; }

    @NotNull
    public static Optional<DefaultRuleOptionType> fromId(final @NotNull String id) {
        return Arrays.stream(values())
                .filter(type -> type.getId().equals(id))
                .findFirst();
    }
}
