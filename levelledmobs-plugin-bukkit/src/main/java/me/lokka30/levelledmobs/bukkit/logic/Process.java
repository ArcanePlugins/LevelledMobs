package me.lokka30.levelledmobs.bukkit.logic;

import java.util.LinkedHashSet;
import java.util.Set;
import org.jetbrains.annotations.NotNull;

public class Process {

    /* vars */

    private final String identifier;
    private final String description;
    private final LinkedHashSet<Preset> presets = new LinkedHashSet<>();

    /* constructors */

    public Process(
        final @NotNull String identifier,
        final @NotNull String description,
        final @NotNull Set<Preset> presets
    ) {
        this.identifier = identifier;
        this.description = description;
        this.presets.addAll(presets);
    }

    /* getters and setters */

    @NotNull
    public String getIdentifier() { return identifier; }

    @NotNull
    public String getDescription() { return description; }

    @NotNull
    public LinkedHashSet<Preset> getPresets() { return presets; }

}
