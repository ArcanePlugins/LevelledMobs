/*
 * This file is Copyright (c) 2020-2022 lokka30.
 * This file is/was present in the LevelledMobs resource.
 * Repository: <https://github.com/lokka30/LevelledMobs>
 * Use of this source code is governed by the GNU GPL v3.0
 * license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.util;

import java.util.Objects;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

/**
 * A smaller version of the Location class only including a world name, and three
 * integers for the x, y and z. Finds uses where the extra data and precision of the Location class
 * is unnecessary.
 *
 * @author lokka30
 * @see Location
 * @since 3.1.2
 */
public record Point(
    @NotNull String worldName,
    int x,
    int y,
    int z
) implements Serializable {

    /**
     * @return array of X, Y and Z coordinates
     * @author lokka30
     * @since 3.1.2
     */
    public Integer[] getCoordinates() {
        return new Integer[]{x, y, z};
    }

    /**
     * @return a String format of the Point but more human-readable.
     * @author lokka30
     * @since 4.0.0
     */
    public String toFormattedString() {
        return "{world='" + worldName + "',x='" + x + "',y='" + y + "',z='" + z + "'}";
    }

    public static Point fromLocation(final @NotNull Location location) {
        Objects.requireNonNull(location.getWorld(), "world");
        return new Point(
            location.getWorld().getName(),
            location.getBlockX(),
            location.getBlockY(),
            location.getBlockZ()
        );
    }
}