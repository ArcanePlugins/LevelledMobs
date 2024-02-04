package io.github.arcaneplugins.levelledmobs.wrappers

import java.util.concurrent.atomic.AtomicInteger
import io.github.arcaneplugins.levelledmobs.LevelledMobs
import org.bukkit.Location
import org.bukkit.World

/**
 * Baseclass for LivingEntityWrapper and LivingEntityPlaceholder to hold various information about
 * mobs
 *
 * @author stumper66
 * @since 3.1.0
 */
abstract class LivingEntityWrapperBase {
    private var _world: World? = null
    private var _location: Location? = null
    private var _distanceFromSpawn: Double? = null
    val main = LevelledMobs.instance
    var summonedLevel: Int? = null
    var isPopulated = false
    val inUseCount = AtomicInteger()

    protected fun populateData(
        world: World,
        location: Location
    ) {
        this._world = world
        this._location = location
        this.isPopulated = true
    }

    protected open fun clearEntityData() {
        this._world = null
        this._location = null
        this._distanceFromSpawn = null
        inUseCount.set(0)
        this.isPopulated = false
        this.summonedLevel = null
    }

    val distanceFromSpawn: Double
        get() {
            if (_distanceFromSpawn == null) {
                _distanceFromSpawn = world.spawnLocation.distance(location)
            }

            return _distanceFromSpawn!!
        }

    val world: World
        get() = this._world!!

    val location: Location
        get() = this._location!!

    val worldName: String
        get() = this._world!!.name
}