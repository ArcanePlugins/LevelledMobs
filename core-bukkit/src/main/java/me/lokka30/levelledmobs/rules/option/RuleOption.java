package me.lokka30.levelledmobs.rules.option;

import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public record RuleOption(
        @NotNull Optional<Boolean> onceProcessedStopProcessing
) {}
