package io.github.arcaneplugins.levelledmobs.nametag

import java.lang.String
import io.github.arcaneplugins.levelledmobs.LevelledMobs
import io.github.arcaneplugins.levelledmobs.managers.ExternalCompatibilityManager
import io.github.arcaneplugins.levelledmobs.util.Utils

/**
 * Manages the ideal nametag sender implementation for the server's version
 *
 * @author PenalBuffalo (aka stumper66)
 * @since 3.6.0
 */
class NametagSenderHandler {
    private var currentUtil: NametagSender? = null
    var isUsingProtocolLib: Boolean = false

    fun getCurrentUtil(): NametagSender? {
        if (this.currentUtil != null) {
            return this.currentUtil
        }

        // supported is spigot >= 1.17
        // otherwise protocollib is used
        if (LevelledMobs.instance.ver.minecraftVersion >= 1.17) {
            // 1.18 and newer we support with direct nms (Paper)
            // or 1.19 spigot and newer
            this.currentUtil = NmsNametagSender()

            Utils.logger.info(
                String.format(
                    "Using NMS version %s for nametag support",
                    LevelledMobs.instance.ver.nmsVersion
                )
            )
        } else if (ExternalCompatibilityManager.hasProtocolLibInstalled()) {
            // we don't directly support this version, use ProtocolLib
            Utils.logger.info(
                "We don't have NMS support for this version of Minecraft, using ProtocolLib"
            )

            this.currentUtil = ProtocolLibNametagSender()
            this.isUsingProtocolLib = true
        } else {
            Utils.logger.warning("ProtocolLib is not installed. No nametags will be visible")
        }

        return this.currentUtil
    }

    fun refresh() {
        if (currentUtil is NmsNametagSender) {
            (currentUtil as NmsNametagSender).refresh()
        }
    }
}