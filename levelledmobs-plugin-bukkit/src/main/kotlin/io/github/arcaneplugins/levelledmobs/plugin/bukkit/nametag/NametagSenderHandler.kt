package io.github.arcaneplugins.levelledmobs.plugin.bukkit.nametag

import io.github.arcaneplugins.levelledmobs.plugin.bukkit.LevelledMobs
import io.github.arcaneplugins.levelledmobs.plugin.bukkit.misc.Log

class NametagSenderHandler {
    private var sender: NmsNametagSender? = null

    fun load(){
        sender = NmsNametagSender()

        Log.info(
            String.format(
                "Using NMS version %s for nametag support",
                LevelledMobs.lmInstance.verInfo.nmsVersion
            ))
    }
}