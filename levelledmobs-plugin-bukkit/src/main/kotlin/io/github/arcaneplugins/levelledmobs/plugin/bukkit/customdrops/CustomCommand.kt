package io.github.arcaneplugins.levelledmobs.plugin.bukkit.customdrops

import java.util.LinkedList
import java.util.TreeMap

class CustomCommand() : CustomDropBase() {
    var commandName: String? = null
    val commands: List<String>
    private val rangedEntries: Map<String, String>
    var runOnSpawn = false
    var runOnDeath = true
    var delay = 0

    init {
        this.rangedEntries = TreeMap()
        this.commands = LinkedList()
    }

    fun cloneItem() : CustomCommand{
        return super.clone() as CustomCommand
    }
}