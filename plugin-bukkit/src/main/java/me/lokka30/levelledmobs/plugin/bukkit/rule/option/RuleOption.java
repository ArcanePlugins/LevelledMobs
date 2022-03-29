package me.lokka30.levelledmobs.plugin.bukkit.rule.option;

import me.lokka30.levelledmobs.plugin.bukkit.rule.Rule;
import org.jetbrains.annotations.NotNull;

public interface RuleOption {

    @NotNull
    DefaultRuleOptionType type();

    @NotNull
    Rule parentRule();

    @NotNull
    RuleOption merge(final @NotNull RuleOption other);
}
