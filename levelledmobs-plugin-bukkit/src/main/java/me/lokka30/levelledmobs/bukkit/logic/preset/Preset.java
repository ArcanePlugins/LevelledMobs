package me.lokka30.levelledmobs.bukkit.logic.preset;

import org.jetbrains.annotations.NotNull;

public record Preset(String identifier, String description) {

    /* vars */

    /* constructors */

    public Preset(
            final @NotNull String identifier,
            final @NotNull String description
    ) {
        this.identifier = identifier;
        this.description = description;
    }

    /* getters and setters */

    @Override
    @NotNull
    public String identifier() {
        return identifier;
    }

    @Override
    @NotNull
    public String description() {
        return description;
    }

}
