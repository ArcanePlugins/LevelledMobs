package io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.action.impl.setpacketlabel;

import static io.github.arcaneplugins.levelledmobs.bukkit.debug.DebugCategory.PACKET_LABELS;

import io.github.arcaneplugins.entitylabellib.bukkit.PacketInterceptor;
import io.github.arcaneplugins.levelledmobs.bukkit.LevelledMobs;
import io.github.arcaneplugins.levelledmobs.bukkit.api.data.EntityDataUtil;
import io.github.arcaneplugins.levelledmobs.bukkit.data.InternalEntityDataUtil;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.Context;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.action.impl.setpacketlabel.SetPacketLabelAction.PacketLabelHandler;
import io.github.arcaneplugins.levelledmobs.bukkit.util.Log;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class PacketInterceptorUtil {

    public static boolean registered = false;

    private static PacketInterceptor interceptor = null;

    private PacketInterceptorUtil() throws IllegalAccessException {
        throw new IllegalAccessException("Illegal instantiation of utility class");
    }

    public static void registerInterceptor() {
        Log.debug(PACKET_LABELS, () -> "PacketInterceptorUtil#registerInterceptor begin");
        if(registered) return;
        if(interceptor == null) interceptor = new PacketInterceptorImpl();
        LevelledMobs.getInstance().getLibLabelHandler().registerInterceptor(interceptor);
        registered = true;
        Log.debug(PACKET_LABELS, () -> "PacketInterceptorUtil#registerInterceptor done");
    }

    public static void unregisterInterceptor() {
        Log.debug(PACKET_LABELS, () -> "PacketInterceptorUtil#unregisterInterceptor begin");
        if(!registered) return;
        LevelledMobs.getInstance().getLibLabelHandler().unregisterInterceptor(interceptor);
        interceptor = null;
        registered = false;
        Log.debug(PACKET_LABELS, () -> "PacketInterceptorUtil#unregisterInterceptor done");
    }

    private static class PacketInterceptorImpl extends PacketInterceptor {

        public PacketInterceptorImpl() {
            super(LevelledMobs.getInstance().getLibLabelHandler());
            Log.debug(PACKET_LABELS, () -> "intercept: initialized object");
        }

        @NotNull
        @Override
        public LabelResponse interceptEntityLabelPacket(
            final @NotNull Entity entity,
            final @NotNull Player player
        ) {
            Log.debug(PACKET_LABELS, () -> "intercept: begin intercepting packet");
            try {
                return Bukkit.getScheduler().callSyncMethod(LevelledMobs.getInstance(), () -> {
                    if(!(entity instanceof final LivingEntity lentity) ||
                        entity.getType() == EntityType.PLAYER ||
                        !EntityDataUtil.isLevelled(lentity, true) ||
                        EntityDataUtil.getDeniesLabel(lentity, true) ||
                        !InternalEntityDataUtil
                            .getLabelHandlerFormulaMap(lentity, true)
                            .containsKey(SetPacketLabelAction.LABEL_ID)
                    ) return new LabelResponse(null, null);

                    Log.debug(PACKET_LABELS, () -> "intercept: done; returning label response");
                    return PacketLabelHandler.INSTANCE.generateLabelResponse(
                        lentity,
                        player,
                        new Context().withEntity(lentity).withPlayer(player)
                    );
                }).get();
            } catch(final Exception ex) {
                Log.debug(PACKET_LABELS, () -> "intercept: caught exception; rethrowing");
                throw new RuntimeException(ex);
            }
        }
    }

}
