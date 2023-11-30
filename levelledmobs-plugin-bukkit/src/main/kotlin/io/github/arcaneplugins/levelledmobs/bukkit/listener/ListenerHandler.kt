package io.github.arcaneplugins.levelledmobs.bukkit.listener

import io.github.arcaneplugins.levelledmobs.bukkit.listener.impl.*
import io.github.arcaneplugins.levelledmobs.bukkit.util.Log.inf

object ListenerHandler {
    private val primaryListeners = mutableSetOf(
        ActionParseListener(),
        ConditionParseListener(),
        LevellingStrategyRequestListener(),
        TestListener()
    )

    private val secondaryListeners = mutableSetOf(
        EntityBreedListener(),
        EntityDamageByEntityListener(),
        EntityDamageListener(),
        EntityDeathListener(),
        EntityExplodeListener(),
        EntityRegainHealthListener(),
        EntitySpawnListener(),
        EntityTransformListener(),
        PlayerDeathListener(),
        PlayerJoinListener(),
        PlayerQuitListener(),
        PlayerTeleportListener()
    )

    fun loadPrimary(){
        inf("Registering primary listeners")
        primaryListeners.forEach(ListenerWrapper::register)
    }

    fun loadSecondary(){
        inf("Registering secondary listeners")
        secondaryListeners.forEach(ListenerWrapper::register)
    }
}