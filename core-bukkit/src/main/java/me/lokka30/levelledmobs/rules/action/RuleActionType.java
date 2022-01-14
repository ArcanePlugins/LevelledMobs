package me.lokka30.levelledmobs.rules.action;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Optional;

public enum RuleActionType {

    EXECUTE("execute");

    private final String id;
    RuleActionType(String id) {
        this.id = id;
    }

    @NotNull
    public String getId() { return id; }

    @NotNull
    public static Optional<RuleActionType> fromId(final @NotNull String id) {
        return Arrays.stream(values())
                .filter(type -> type.getId().equals(id))
                .findFirst();
    }
}
