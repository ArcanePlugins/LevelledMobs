package io.github.arcaneplugins.levelledmobs.customdrops

/**
 * Holds any custom commands as parsed from customdrops.yml
 *
 * @author stumper66
 * @since 3.0.0
 */
class CustomCommand(
    defaults: CustomDropsDefaults
) : CustomDropBase(defaults), Cloneable {
    var commandName: String? = null
    val commands = mutableListOf<String>()
    val rangedEntries = mutableMapOf<String, String>()
    var runOnSpawn = false
    var runOnDeath = true
    var mobScale: Double? = null
    var delay = 0

    fun cloneItem(): CustomCommand? {
        var copy: CustomCommand? = null
        try {
            copy = super.clone() as CustomCommand?
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return copy
    }
}