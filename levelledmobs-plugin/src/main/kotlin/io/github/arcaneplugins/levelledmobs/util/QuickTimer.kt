package io.github.arcaneplugins.levelledmobs.util

/**
 * This is a small class useful for timing simple things such as the time required to start-up a plugin or run a command.
 * <p>
 * Mark the starting point of the timer with `QuickTimer timer = new QuickTimer()`, then get the time (in milliseconds)
 * since it started using `QuickTimer#getTimer()`.
 *
 * @author lokka30
 * @see System#currentTimeMillis()
 * @since unknown
 */
class QuickTimer {
    private var startTime = 0L

    init {
        start()
    }

    /**
     * Re/start the timer.
     */
    fun start() {
        startTime = System.currentTimeMillis()
    }

    /**
     * @return time (millis) since start time
     */
    val timer: Long
        get() = System.currentTimeMillis() - startTime
}