package me.lokka30.levelledmobs.bukkit.logic.preset;

import org.jetbrains.annotations.NotNull;

public class Preset {

    /* vars */

    private final String identifier;
    private final String description;

    /* constructors */

    public Preset(
        final @NotNull String identifier,
        final @NotNull String description
    ) {
        this.identifier = identifier;
        this.description = description;
    }

    /* getters and setters */

    @NotNull
    public String getIdentifier() { return identifier; }

    @NotNull
    public String getDescription() { return description; }

}
