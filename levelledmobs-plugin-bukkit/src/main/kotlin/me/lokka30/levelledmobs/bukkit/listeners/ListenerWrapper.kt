package me.lokka30.levelledmobs.bukkit.listeners

import me.lokka30.levelledmobs.bukkit.LevelledMobs
import me.lokka30.levelledmobs.bukkit.utils.ClassUtils
import org.bukkit.Bukkit
import org.bukkit.event.Listener

/*
FIXME comment
 */
abstract class ListenerWrapper(
    val eventClasspath: String
) : Listener {

    /*
    FIXME comment
     */
    fun canRegister(): Boolean {
        return ClassUtils.classExists(eventClasspath)
    }

    /*
    FIXME comment
     */
    fun register() {
        if(!canRegister()) return
        Bukkit.getPluginManager().registerEvents(this, LevelledMobs.instance!!)
    }
}