package io.github.arcaneplugins.levelledmobs.result

import java.time.Instant

/**
 * Records entity deaths for use in the chunk kill max feature
 *
 * @author stumper66
 * @since 3.4.0
 */
class ChunkKillInfo {
    // timestamp of death, max cooldown time
    val entityCounts = mutableMapOf<Instant, Int>()

    val entrySet: Set<Map.Entry<Instant, Int>>
        get() = entityCounts.entries

    val isEmpty: Boolean
        get() = entityCounts.isEmpty()

    val count: Int
        get() = entityCounts.size

    override fun toString(): String {
        return entityCounts.toString()
    }
}