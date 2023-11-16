package io.github.arcaneplugins.levelledmobs.bukkit.logic.preset

import org.spongepowered.configurate.CommentedConfigurationNode

/**
 * <h3>Parsed Node vs Actual Node</h3>
 * When LM4 parses presets, it creates a clone, {@link Preset#getParsedNode()}, of the actual node,
 * {@link Preset#getActualNode()}. The parsed node is a clone/copy of the actual node which removes
 * the 'preset' key so that when presets are virtually pasted into the functions (see
 * {@link LogicHandler}'s 'walk' method), the preset key is
 * not included, which makes things cleaner in the function's node.
 * <p>
 * <b>WARNING:</b> The parsed node is not synced with the file, use the actual node to make changes
 * to the file.
 */
class Preset(
    val identifier: String,
    val parsedNode: CommentedConfigurationNode,
    val actualNode: CommentedConfigurationNode
) {

}