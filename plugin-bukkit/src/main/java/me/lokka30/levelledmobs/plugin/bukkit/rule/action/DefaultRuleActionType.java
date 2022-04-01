package me.lokka30.levelledmobs.plugin.bukkit.rule.action;

import java.util.Arrays;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;

public enum DefaultRuleActionType {

    EXECUTE("execute");

    private final String id;

    DefaultRuleActionType(String id) {
        this.id = id;
    }

    @NotNull
    public String id() {
        return id;
    }

    @NotNull
    public static Optional<DefaultRuleActionType> fromId(final @NotNull String id) {
        return Arrays.stream(values())
            .filter(type -> type.id().equals(id))
            .findFirst();
    }
}
