package me.lokka30.levelledmobs.bukkit.logic;

import java.util.LinkedHashSet;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.CommentedConfigurationNode;

public class Process {

    /* vars */

    private final String identifier;
    private final String description;
    private final Set<Preset> presets = new LinkedHashSet<>();
    private final CommentedConfigurationNode node;

    /* constructors */

    public Process(
        final @NotNull String identifier,
        final @NotNull String description,
        final @NotNull CommentedConfigurationNode node
    ) {
        this.identifier = identifier;
        this.description = description;
        this.node = node;
    }

    /* getters and setters */

    @NotNull
    public String getIdentifier() { return identifier; }

    @NotNull
    public String getDescription() { return description; }

    @NotNull
    public Set<Preset> getPresets() { return presets; }

    @NotNull
    public CommentedConfigurationNode getNode() { return node; }

}
