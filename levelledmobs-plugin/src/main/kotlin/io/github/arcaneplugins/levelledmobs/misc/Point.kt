package io.github.arcaneplugins.levelledmobs.misc

import org.bukkit.Location

/**
 * A smaller version of the Location class only including a world name, and three integers for the
 * x, y and z. Finds uses where the extra data and precision of the Location class is completely
 * unnecessary.
 *
 * @author lokka30
 * @see Location
 * @since 3.1.2
 */
class Point {
    private var worldName: String? = null
    private var x = 0
    private var y = 0
    private var z = 0

    constructor(location: Location) {
        this.worldName = location.world.name
        this.x = location.blockX
        this.y = location.blockY
        this.z = location.blockZ
    }

    override fun toString(): String {
        return "$worldName, $x, $y, $z"
    }
}