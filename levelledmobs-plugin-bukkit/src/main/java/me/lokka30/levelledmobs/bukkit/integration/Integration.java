package me.lokka30.levelledmobs.bukkit.integration;

import java.util.Objects;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public abstract class Integration {

    /* vars */

    private final String description;
    private final boolean enabledByDefault;
    private final boolean internal;
    private final IntegrationPriority priority;
    private boolean enabled;

    /* constructors */

    public Integration(
        final @NotNull String description,
        final boolean enabledByDefault,
        final boolean internal,
        final IntegrationPriority priority
    ) {
        this.description = Objects.requireNonNull(description, "description");
        this.enabledByDefault = enabledByDefault;
        this.internal = internal;
        this.priority = priority;
        this.enabled = enabledByDefault;
    }

    /* methods */

    /* getters and setters */

    @NotNull
    public String getDescription() { return description; }

    public boolean isEnabledByDefault() { return enabledByDefault; }

    public boolean isInternal() { return internal; }

    @NotNull
    public IntegrationPriority getPriority() {
        return priority;
    }

    public boolean isEnabled() { return enabled; }

    public void setEnabled(final boolean state) { this.enabled = state; }

}
