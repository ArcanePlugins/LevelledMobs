package me.lokka30.levelledmobs.nametag;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import java.util.ConcurrentModificationException;
import java.util.Optional;
import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.misc.DebugType;
import me.lokka30.levelledmobs.result.NametagResult;
import me.lokka30.levelledmobs.util.MessageUtils;
import me.lokka30.levelledmobs.util.Utils;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Handles sending nametag packets to player via ProtocolLib
 *
 * @author PenalBuffalo (aka stumper66)
 * @since 3.6.0
 */
public class ProtocolLibNametagSender implements NametagSender {

    public ProtocolLibNametagSender(
        final @NotNull LevelledMobs main
    ) {
        this.main = main;
    }

    private final LevelledMobs main;

    public void sendNametag(
        final @NotNull LivingEntity livingEntity,
        final @NotNull NametagResult nametagInfo,
        final @NotNull Player player,
        final boolean alwaysVisible
    ) {
        if (!player.isOnline() || !player.isValid()) {
            return;
        }
        final WrappedDataWatcher dataWatcher;
        final WrappedDataWatcher.Serializer chatSerializer;

        try {
            dataWatcher = WrappedDataWatcher.getEntityWatcher(livingEntity).deepClone();
        } catch (final ConcurrentModificationException ex) {
            Utils.debugLog(main, DebugType.UPDATE_NAMETAG_FAIL,
                "&bConcurrentModificationException &7caught, skipping nametag update of &b"
                    + livingEntity.getName() + "&7.");
            return;
        }

        try {
            chatSerializer = WrappedDataWatcher.Registry.getChatComponentSerializer(true);
        } catch (final ConcurrentModificationException ex) {
            Utils.debugLog(main, DebugType.UPDATE_NAMETAG_FAIL,
                "&bConcurrentModificationException &7caught, "
                    + "skipping nametag update of &b"
                    + livingEntity.getName() + "&7.");
            return;
        } catch (final IllegalArgumentException ex) {
            Utils.debugLog(main, DebugType.UPDATE_NAMETAG_FAIL,
                "Registry is empty (&bIllegalArgumentException&7 caught), "
                    + "skipping nametag update of &b"
                    + livingEntity.getName() + "&7.");
            return;
        }

        final WrappedDataWatcher.WrappedDataWatcherObject watcherObject =
            new WrappedDataWatcher.WrappedDataWatcherObject(2, chatSerializer);
        final int objectIndex = 3;
        final int fieldIndex = 0;
        final Optional<Object> optional = nametagInfo.isNullOrEmpty() ?
            Optional.empty()
            : Optional.of(WrappedChatComponent.fromChatMessage(
                MessageUtils.colorizeAll(nametagInfo.getNametag()))[0].getHandle());

        dataWatcher.setObject(watcherObject, optional);

        if (nametagInfo.isNullOrEmpty()) {
            dataWatcher.setObject(objectIndex, false);
        } else {
            dataWatcher.setObject(objectIndex, alwaysVisible);
        }

        final PacketContainer packet = ProtocolLibrary.getProtocolManager()
            .createPacket(PacketType.Play.Server.ENTITY_METADATA);
        packet.getWatchableCollectionModifier()
            .write(fieldIndex, dataWatcher.getWatchableObjects());
        packet.getIntegers().write(fieldIndex, livingEntity.getEntityId());

        try {
            ProtocolLibrary.getProtocolManager().sendServerPacket(player, packet);
        } catch (final Exception ex) {
            Utils.logger.error(
                "Unable to update nametag packet for player &b" +
                    player.getName() +
                    "&7; stack trace:"
            );
            ex.printStackTrace();
        }
    }

    public String toString() {
        return "ProtocolLibHandler";
    }
}
