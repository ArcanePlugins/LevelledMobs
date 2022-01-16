package me.lokka30.levelledmobs.rules.option.type;

import me.lokka30.levelledmobs.rules.Rule;
import me.lokka30.levelledmobs.rules.option.RuleOption;
import me.lokka30.levelledmobs.rules.option.RuleOptionType;
import org.jetbrains.annotations.NotNull;

public record TemporaryDoNotUseOption(
        @NotNull Rule parentRule
) implements RuleOption {

    @Override @NotNull
    public RuleOptionType type() {
        return RuleOptionType.TEMPORARY_DO_NOT_USE;
    }

    @Override @NotNull
    public RuleOption merge(@NotNull RuleOption other) {
        return other;
    }
}