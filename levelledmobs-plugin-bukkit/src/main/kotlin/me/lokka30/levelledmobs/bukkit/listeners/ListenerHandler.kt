package me.lokka30.levelledmobs.bukkit.listeners

import me.lokka30.levelledmobs.bukkit.LevelledMobs.Companion.logInf

/*
FIXME comment
 */
class ListenerHandler {

    /*
    FIXME comment
     */
    val listeners = setOf(
        EntitySpawnListener(),
        PlayerJoinListener()
    )

    /*
    FIXME comment
     */
    fun load() {
        logInf("Registering listeners...")
        listeners.forEach(ListenerWrapper::register)
    }
}