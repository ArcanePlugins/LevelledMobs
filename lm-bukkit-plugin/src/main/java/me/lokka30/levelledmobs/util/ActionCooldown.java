/*
 * This file is Copyright (c) 2020-2022 lokka30.
 * This file is/was present in the LevelledMobs resource.
 * Repository: <https://github.com/lokka30/LevelledMobs>
 * Use of this source code is governed by the GNU GPL v3.0
 * license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.util;

/**
 * This class is used to put cooldowns on certain actions, on certain in-game objects -
 * e.g. clicking entities or blocks. For example, this is used in Debug Entity Damage to stop spam
 * from clicking the same entity over and over. It is also used in the Spawner Info for the same
 * reason: blocks unnecessary chat spam.
 *
 * @author lokka30
 * @since 3.1.2
 */
public record ActionCooldown(
    long startingTime,
    String identifier
) {

    /**
     * @param identifier ID to check if this cooldown has the same ID.
     * @return if the IDs match.
     * @author lokka30
     * @since 3.1.2 Checks if this cooldown belongs to something with a certain ID.
     */
    public boolean doesCooldownBelongToIdentifier(final String identifier) {
        return identifier.equals(this.identifier);
    }

    /**
     * @param requiredTimeInSeconds how many seconds should the cooldown have lasted?
     * @return if the cooldown has expired.
     * @author lokka30
     * @since 3.1.2 Check if the cooldown's required time (seconds!) has surpassed since the
     * starting point.
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean hasCooldownExpired(final long requiredTimeInSeconds) {
        return ((System.currentTimeMillis() - startingTime) >= (requiredTimeInSeconds * 1000));
    }
}