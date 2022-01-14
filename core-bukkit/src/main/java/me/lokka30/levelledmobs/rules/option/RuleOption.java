package me.lokka30.levelledmobs.rules.option;

import org.jetbrains.annotations.NotNull;

public interface RuleOption {

    @NotNull
    RuleOptionType getType();

    @NotNull
    RuleOption merge(final @NotNull RuleOption other);
}
