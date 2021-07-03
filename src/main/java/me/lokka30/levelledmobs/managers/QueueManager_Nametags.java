package me.lokka30.levelledmobs.managers;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.misc.*;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Queues up mob nametag updates so they can be applied in a background thread
 *
 * @author stumper66
 */
public class QueueManager_Nametags {

    public QueueManager_Nametags(final LevelledMobs main){
        this.main = main;
        this.queue = new LinkedBlockingQueue<>();
    }

    private final LevelledMobs main;
    private boolean isRunning;
    private boolean doThread;
    private final LinkedBlockingQueue<QueueItem> queue;

    public void start(){
        if (isRunning) return;
        doThread = true;
        isRunning = true;

        final BukkitRunnable bgThread = new BukkitRunnable(){
            @Override
            public void run() {
                try {
                    main();
                } catch (InterruptedException ignored) {
                    isRunning = false;
                }
                Utils.logger.info("Nametag update queue Manager has exited");
            }
        };

        bgThread.runTaskAsynchronously(main);
    }

    public void stop(){
        doThread = false;
    }

    public void addToQueue(final QueueItem item){
        queue.offer(item);
    }

    private void main() throws InterruptedException{
        while (doThread) {

            final QueueItem item = queue.poll(200, TimeUnit.MILLISECONDS);
            if (item == null) continue;

            updateNametag(item.lmEntity, item.nametag, item.players);
        }

        isRunning = false;
    }

    private void updateNametag(final @NotNull LivingEntityWrapper lmEntity, final String nametag, final List<Player> players) {

        if (main.settingsCfg.getBoolean(YmlParsingHelper.getKeyNameFromConfig(main.settingsCfg, "use-customname-for-mob-nametags"))){
            updateNametag_CustomName(lmEntity, nametag);
            return;
        }

        if (!ExternalCompatibilityManager.hasProtocolLibInstalled()) return;
        if (main.settingsCfg.getBoolean(YmlParsingHelper.getKeyNameFromConfig(main.settingsCfg, "assert-entity-validity-with-nametag-packets")) && !lmEntity.getLivingEntity().isValid())
            return;

        final WrappedDataWatcher dataWatcher;
        final WrappedDataWatcher.Serializer chatSerializer;

        try {
            dataWatcher = WrappedDataWatcher.getEntityWatcher(lmEntity.getLivingEntity()).deepClone();
        } catch (ConcurrentModificationException ex) {
            Utils.debugLog(main, DebugType.UPDATE_NAMETAG_FAIL, "&bConcurrentModificationException &7caught, skipping nametag update of &b" + lmEntity.getLivingEntity().getName() + "&7.");
            return;
        }

        try {
            chatSerializer = WrappedDataWatcher.Registry.getChatComponentSerializer(true);
        } catch (ConcurrentModificationException ex) {
            Utils.debugLog(main, DebugType.UPDATE_NAMETAG_FAIL, "&bConcurrentModificationException &7caught, skipping nametag update of &b" + lmEntity.getLivingEntity().getName() + "&7.");
            return;
        } catch (IllegalArgumentException ex) {
            Utils.debugLog(main, DebugType.UPDATE_NAMETAG_FAIL, "Registry is empty (&bIllegalArgumentException&7 caught), skipping nametag update of &b" + lmEntity.getLivingEntity().getName() + "&7.");
            return;
        }

        final WrappedDataWatcher.WrappedDataWatcherObject watcherObject = new WrappedDataWatcher.WrappedDataWatcherObject(2, chatSerializer);

        Optional<Object> optional;
        if (Utils.isNullOrEmpty(nametag))
            optional = Optional.empty();
        else
            optional = Optional.of(WrappedChatComponent.fromChatMessage(nametag)[0].getHandle());

        dataWatcher.setObject(watcherObject, optional);
        if (nametag == null)
            dataWatcher.setObject(3, false);
        else
            dataWatcher.setObject(3, !"".equals(nametag) && lmEntity.getLivingEntity().isCustomNameVisible() ||
                    main.rulesManager.getRule_CreatureNametagAlwaysVisible(lmEntity));

        final PacketContainer packet = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.ENTITY_METADATA);
        packet.getWatchableCollectionModifier().write(0, dataWatcher.getWatchableObjects());
        packet.getIntegers().write(0, lmEntity.getLivingEntity().getEntityId());

        for (final Player player : players) {
            if (!player.isOnline()) continue;
            if (!lmEntity.getLivingEntity().isValid()) return;

            try {
                Utils.debugLog(main, DebugType.UPDATE_NAMETAG_SUCCESS, "Nametag packet sent for &b" + lmEntity.getLivingEntity().getName() + "&7 to &b" + player.getName() + "&7.");
                ProtocolLibrary.getProtocolManager().sendServerPacket(player, packet);
            } catch (IllegalArgumentException ex) {
                Utils.debugLog(main, DebugType.UPDATE_NAMETAG_FAIL, "&bIllegalArgumentException&7 caught whilst trying to sendServerPacket");
            } catch (InvocationTargetException ex) {
                Utils.logger.error("Unable to update nametag packet for player &b" + player.getName() + "&7; Stack trace:");
                ex.printStackTrace();
            }
        }
    }

    private void updateNametag_CustomName(final @NotNull LivingEntityWrapper lmEntity, final String nametag){
        final boolean hadCustomName = lmEntity.getLivingEntity().getCustomName() != null;

        lmEntity.getLivingEntity().setCustomName(nametag);
        lmEntity.getLivingEntity().setCustomNameVisible(true);

        if (!hadCustomName)
            lmEntity.getLivingEntity().setRemoveWhenFarAway(true);
    }
}
