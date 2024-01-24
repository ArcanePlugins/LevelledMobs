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

    constructor(
        worldName: String,
        x: Int,
        y: Int,
        z: Int
    ){
        this.worldName = worldName
        this.x = x
        this.y = y
        this.z = z
    }

    constructor(str: String) {
        val split = str.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        this.worldName = split[0]
        this.x = split[1].toInt()
        this.y = split[2].toInt()
        this.z = split[3].toInt()
    }

    constructor(location: Location) {
        this.worldName = location.world.name
        this.x = location.blockX
        this.y = location.blockY
        this.z = location.blockZ
    }

    private val coordinates: MutableList<Int>
        get() = mutableListOf(x, y, z)

    override fun toString(): String {
        return String.format("%s, %s, %s, %s", worldName, x, y, z)
    }

    fun matches(point1: Point, point2: Point): Boolean {
        return (point1.worldName == point2.worldName &&
                point1.toString() == point2.toString())
    }
}