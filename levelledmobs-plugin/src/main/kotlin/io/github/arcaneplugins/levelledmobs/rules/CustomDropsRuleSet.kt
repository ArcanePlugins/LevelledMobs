package io.github.arcaneplugins.levelledmobs.rules

/**
 * When in conjunction when a customdrops is being processed
 *
 * @author stumper66
 * @since 3.0.0
 */
class CustomDropsRuleSet {
    var useDrops = false
    var chunkKillOptions: ChunkKillOptions? = null
    val useDropTableIds = mutableListOf<String>()
}