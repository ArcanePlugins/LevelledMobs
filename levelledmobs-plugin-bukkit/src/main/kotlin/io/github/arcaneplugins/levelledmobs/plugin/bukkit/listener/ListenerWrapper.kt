package io.github.arcaneplugins.levelledmobs.plugin.bukkit.listener

import io.github.arcaneplugins.levelledmobs.plugin.bukkit.LevelledMobs.Companion.lmInstance
import org.bukkit.Bukkit
import org.bukkit.event.Listener

//todo doc
abstract class ListenerWrapper(
    val imperative: Boolean,
) : Listener {

    //todo doc
    fun register() {
        Bukkit.getPluginManager().registerEvents(this, lmInstance)
    }

}