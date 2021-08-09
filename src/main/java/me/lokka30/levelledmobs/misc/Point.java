/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.misc;

import org.bukkit.Location;

/**
 * A smaller version of the Location class only including
 * a world name, and three integers for the x, y and z.
 * Finds uses where the extra data and precision of the
 * Location class is completely unnecessary.
 *
 * @author lokka30
 * @see Location
 * @since 3.1.2
 */
public class Point {

    public String worldName;
    public int x, y, z;

    public Point(final String worldName, final int x, final int y, final int z) {
        this.worldName = worldName;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Integer[] getCoordinates() {
        return new Integer[]{x, y, z};
    }

    public String toString() {
        return String.format("%s, %s, %s, %s", worldName, x, y, z);
    }

    public Point(final String str) {
        final String[] split = str.split(",");
        this.worldName = split[0];
        this.x = Integer.parseInt(split[1]);
        this.y = Integer.parseInt(split[2]);
        this.z = Integer.parseInt(split[3]);
    }

    public Point(final Location location) {
        assert location.getWorld() != null;
        worldName = location.getWorld().getName();
        x = location.getBlockX();
        y = location.getBlockY();
        z = location.getBlockZ();
    }

    public static boolean matches(final Point point1, final Point point2) {
        return (point1.worldName.equals(point2.worldName) && point1.getCoordinates() == point2.getCoordinates());
    }
}
