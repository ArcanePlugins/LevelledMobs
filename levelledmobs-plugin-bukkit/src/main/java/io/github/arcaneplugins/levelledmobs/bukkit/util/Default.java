package io.github.arcaneplugins.levelledmobs.bukkit.util;

import org.jetbrains.annotations.Nullable;

public class Default<T> {

    private final T desired;
    private final T fallback;

    public Default(
        @Nullable final T desired,
        @Nullable final T fallback
    ) {
        this.desired = desired;
        this.fallback = fallback;
    }

    public T get() {
        return desired == null ? fallback : desired;
    }

}
