package me.lokka30.levelledmobs.bukkit.logic.preset;

import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.CommentedConfigurationNode;

/**
 * <h3>Parsed Node vs Actual Node</h3>
 * When LM4 parses presets, it creates a clone, {@link Preset#getParsedNode()}, of the actual node,
 * {@link Preset#getActualNode()}. The parsed node is a clone/copy of the actual node which removes
 * the 'preset' key so that when presets are virtually pasted into the functions (see
 * {@link me.lokka30.levelledmobs.bukkit.logic.LogicHandler}'s 'walk' method), the preset key is
 * not included, which makes things cleaner in the function's node.
 * <p>
 * <b>WARNING:</b> The parsed node is not synced with the file, use the actual node to make changes
 * to the file.
 */
 public class Preset {

    /* vars */

    private final String identifier;
    private final CommentedConfigurationNode parsedNode;
    private final CommentedConfigurationNode actualNode;

    /* constructors */

    public Preset(
        final @NotNull String identifier,
        final @NotNull CommentedConfigurationNode parsedNode,
        final @NotNull CommentedConfigurationNode actualNode
    ) {
        this.identifier = identifier;
        this.parsedNode = parsedNode;
        this.actualNode = actualNode;
    }

    /* getters and setters */

    @NotNull
    public String getIdentifier() {
        return identifier;
    }

    /**
     * getParsedNode vs getActualNode is explained in the top level javadoc.
     *
     * @return parsed node (not file-synced).
     */
    @NotNull
    public CommentedConfigurationNode getParsedNode() {
        return parsedNode;
    }

    /**
     * getParsedNode vs getActualNode is explained in the top level javadoc.
     *
     * @return actual node (file-synced), non-parsed.
     */
    @NotNull
    public CommentedConfigurationNode getActualNode() {
        return actualNode;
    }

}
