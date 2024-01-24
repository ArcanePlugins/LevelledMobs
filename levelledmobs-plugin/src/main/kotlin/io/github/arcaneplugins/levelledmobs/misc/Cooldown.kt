package io.github.arcaneplugins.levelledmobs.misc

/**
 * This class is used to put cooldowns on certain actions, on certain in-game objects - e.g.
 * clicking entities or blocks.
 * <p>
 * For example, this is used in Debug Entity Damage to stop spam from clicking the same entity over
 * and over. It is also used in the Spawner Info for the same reason: blocks unnecessary chat spam.
 *
 * @author lokka30
 * @see System#currentTimeMillis()
 * @since 3.1.2
 */
class Cooldown(
    /**
     * The starting point of the cooldown - just run System#currentTimeMillis()
     */
    private val startingTime: Long,
    /**
     * What this cooldown should be identifiable by - e.g. entity ID, location
     */
    private val identifier: String
) {
    /**
     * Checks if this cooldown belongs to something with a certain ID.
     *
     * @param identifier ID to check if this cooldown has the same ID.
     * @return if the IDs match.
     */
    fun doesCooldownBelongToIdentifier(identifier: String): Boolean {
        return identifier == this.identifier
    }

    /**
     * Check if the cooldown's required time (seconds!) has surpassed since the starting point.
     *
     * @param requiredTimeInSeconds how many seconds should the cooldown have lasted?
     * @return if the cooldown has expired.
     */
    fun hasCooldownExpired(requiredTimeInSeconds: Long): Boolean {
        return ((System.currentTimeMillis() - startingTime) >= (requiredTimeInSeconds * 1000))
    }
}