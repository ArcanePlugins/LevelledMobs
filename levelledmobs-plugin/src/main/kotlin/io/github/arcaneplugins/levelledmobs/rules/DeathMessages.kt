package io.github.arcaneplugins.levelledmobs.rules

import java.util.concurrent.ThreadLocalRandom
import kotlin.math.max

/**
 * Holds settings relating to the custom
 * death messages feature
 *
 * @author stumper66
 * @since 3.7.0
 */
class DeathMessages {
    private val messages = mutableListOf<String>()
    val isEnabled: Boolean = false

    fun addEntry(weight: Int, message: String) {
        val number = max(1.0, weight.toDouble()).toInt()
        repeat(
            number,
            action = { messages.add(message) }
        )
    }

    fun getDeathMessage(): String? {
        if (messages.isEmpty()) return null

        val useArray = ThreadLocalRandom.current().nextInt(messages.size)
        return messages[useArray]
    }

    val isEmpty: Boolean
        get() = messages.isEmpty()

    override fun toString(): String {
        if (!this.isEnabled) return "DeathMessages (disabled)"
        if (this.isEmpty) return "DeathMessages"

        return "DeathMessages (${messages.size} defined)"
    }
}