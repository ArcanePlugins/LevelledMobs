/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.misc;

/**
 * @author lokka30
 * @see System#currentTimeMillis()
 * @since v3.1.2
 * This class is used to put cooldowns on certain actions,
 * on certain in-game objects - e.g. clicking entities or blocks.
 * For example, this is used in Debug Entity Damage to stop spam from
 * clicking the same entity over and over. It is also used in the
 * Spawner Info for the same reason: blocks unnecessary chat spam.
 */
public record ActionCooldown(
        long    startingTime,
        String  identifier
) {

    /**
     * @param identifier ID to check if this cooldown has the same ID.
     * @return if the IDs match.
     * @author lokka30
     * @since v3.1.2
     * Checks if this cooldown belongs to something with a certain ID.
     */
    public boolean doesCooldownBelongToIdentifier(final String identifier) {
        return identifier.equals(this.identifier);
    }

    /**
     * @param requiredTimeInSeconds how many seconds should the cooldown have lasted?
     * @return if the cooldown has expired.
     * @author lokka30
     * @since v3.1.2
     * Check if the cooldown's required time (seconds!) has surpassed since
     * the starting point.
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean hasCooldownExpired(final long requiredTimeInSeconds) {
        return ((System.currentTimeMillis() - startingTime) >= (requiredTimeInSeconds * 1000));
    }
}