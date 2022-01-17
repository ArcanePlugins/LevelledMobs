package me.lokka30.levelledmobs.rules.action;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Optional;

public enum DefaultRuleActionType {

    EXECUTE("execute");

    private final String id;
    DefaultRuleActionType(String id) {
        this.id = id;
    }

    @NotNull
    public String id() { return id; }

    @NotNull
    public static Optional<DefaultRuleActionType> fromId(final @NotNull String id) {
        return Arrays.stream(values())
                .filter(type -> type.id().equals(id))
                .findFirst();
    }
}
