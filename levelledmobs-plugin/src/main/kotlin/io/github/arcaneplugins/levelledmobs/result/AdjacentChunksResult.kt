package io.github.arcaneplugins.levelledmobs.result

/**
 * Used in conjunction with the chunk kill count feature
 *
 * @author stumper66
 * @since 3.4.0
 */
class AdjacentChunksResult {
    var entities: Int = 0
    val chunkKeys = mutableListOf<Long>()
}