package io.github.arcaneplugins.levelledmobs.nametag

import com.comphenix.protocol.PacketType
import com.comphenix.protocol.ProtocolLibrary
import com.comphenix.protocol.wrappers.WrappedChatComponent
import com.comphenix.protocol.wrappers.WrappedDataWatcher
import com.comphenix.protocol.wrappers.WrappedDataWatcher.WrappedDataWatcherObject
import java.util.Optional
import io.github.arcaneplugins.levelledmobs.debug.DebugManager
import io.github.arcaneplugins.levelledmobs.debug.DebugType
import io.github.arcaneplugins.levelledmobs.result.NametagResult
import io.github.arcaneplugins.levelledmobs.util.MessageUtils.colorizeAll
import io.github.arcaneplugins.levelledmobs.util.Utils
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player

/**
 * Handles sending nametag packets to player via ProtocolLib
 *
 * @author PenalBuffalo (aka stumper66)
 * @since 3.6.0
 */
class ProtocolLibNametagSender : NametagSender {
    override fun sendNametag(
        livingEntity: LivingEntity,
        nametagInfo: NametagResult,
        player: Player,
        alwaysVisible: Boolean
    ) {
        if (!player.isOnline || !player.isValid) {
            return
        }
        val dataWatcher: WrappedDataWatcher
        val chatSerializer: WrappedDataWatcher.Serializer

        try {
            dataWatcher = WrappedDataWatcher.getEntityWatcher(livingEntity).deepClone()
        } catch (ex: ConcurrentModificationException) {
            DebugManager.log(DebugType.PL_UPDATE_NAMETAG, livingEntity, false) {
                ("&bConcurrentModificationException &7caught, skipping nametag update of &b"
                        + livingEntity.name + "&7.")
            }
            return
        }

        try {
            chatSerializer = WrappedDataWatcher.Registry.getChatComponentSerializer(true)
        } catch (ex: ConcurrentModificationException) {
            DebugManager.log(DebugType.PL_UPDATE_NAMETAG, livingEntity, false) {
                ("&bConcurrentModificationException &7caught, "
                        + "skipping nametag update of &b"
                        + livingEntity.name + "&7.")
            }
            return
        } catch (ex: IllegalArgumentException) {
            DebugManager.log(DebugType.PL_UPDATE_NAMETAG, livingEntity, false) {
                ("Registry is empty (&bIllegalArgumentException&7 caught), "
                        + "skipping nametag update of &b"
                        + livingEntity.name + "&7.")
            }
            return
        }

        DebugManager.log(DebugType.PL_UPDATE_NAMETAG, livingEntity, true) { "Nametag sent: " + nametagInfo.nametag }

        val watcherObject =
            WrappedDataWatcherObject(2, chatSerializer)
        val objectIndex = 3
        val fieldIndex = 0
        val optional = if (nametagInfo.isNullOrEmpty) Optional.empty()
        else Optional.of(
            WrappedChatComponent.fromChatMessage(
                colorizeAll(nametagInfo.nametag)
            )[0].handle
        )

        dataWatcher.setObject(watcherObject, optional)

        if (nametagInfo.isNullOrEmpty) {
            dataWatcher.setObject(objectIndex, false)
        } else {
            dataWatcher.setObject(objectIndex, alwaysVisible)
        }

        val packet = ProtocolLibrary.getProtocolManager()
            .createPacket(PacketType.Play.Server.ENTITY_METADATA)
        packet.watchableCollectionModifier
            .write(fieldIndex, dataWatcher.watchableObjects)
        packet.integers.write(fieldIndex, livingEntity.entityId)

        try {
            ProtocolLibrary.getProtocolManager().sendServerPacket(player, packet)
        } catch (ex: Exception) {
            Utils.logger.error(
                "Unable to update nametag packet for player &b" +
                        player.name +
                        "&7; stack trace:"
            )
            ex.printStackTrace()
        }
    }

    override fun toString(): String {
        return "ProtocolLibHandler"
    }
}