package io.github.arcaneplugins.levelledmobs.plugin.bukkit.listener

import io.github.arcaneplugins.levelledmobs.plugin.bukkit.listener.entity.EntitySpawnListener

//todo doc
class ListenerManager {

    //todo doc
    val listeners: Set<ListenerWrapper> = setOf(
        EntitySpawnListener()
    )

    //todo doc
    fun load() {
        listeners.forEach(ListenerWrapper::register)
    }

}