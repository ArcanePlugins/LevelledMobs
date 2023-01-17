package io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.action.impl.setpacketlabel;

import static io.github.arcaneplugins.levelledmobs.bukkit.debug.DebugCategory.PACKET_LABELS;

import io.github.arcaneplugins.entitylabellib.bukkit.PacketInterceptor;
import io.github.arcaneplugins.levelledmobs.bukkit.LevelledMobs;
import io.github.arcaneplugins.levelledmobs.bukkit.api.data.EntityDataUtil;
import io.github.arcaneplugins.levelledmobs.bukkit.data.InternalEntityDataUtil;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.Context;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.action.impl.setpacketlabel.SetPacketLabelAction.PacketLabelHandler;
import io.github.arcaneplugins.levelledmobs.bukkit.util.Log;
import java.util.concurrent.CompletableFuture;
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
        public CompletableFuture<LabelResponse> interceptEntityLabelPacket(
            final @NotNull Entity entity,
            final @NotNull Player player
        ) {
            Log.debug(PACKET_LABELS, () -> "intercept: begin intercepting packet");

            final CompletableFuture<LabelResponse> cf = new CompletableFuture<>();

            try {
                Bukkit.getScheduler().callSyncMethod(LevelledMobs.getInstance(), () -> {
                    if(!(entity instanceof final LivingEntity lentity) ||
                        entity.getType() == EntityType.PLAYER ||
                        !EntityDataUtil.isLevelled(lentity, true) ||
                        EntityDataUtil.getDeniesLabel(lentity, true) ||
                        !InternalEntityDataUtil
                            .getLabelHandlerFormulaMap(lentity, true)
                            .containsKey(SetPacketLabelAction.LABEL_ID)
                    ) {
                        cf.complete(null);
                        return cf;
                    };

                    Log.debug(PACKET_LABELS, () -> "intercept: done; returning label response");
                    cf.complete(
                        PacketLabelHandler.INSTANCE.generateLabelResponse(
                            lentity,
                            player,
                            new Context().withEntity(lentity).withPlayer(player)
                        )
                    );

                    return cf;
                });
            } catch(final Exception ex) {
                Log.debug(PACKET_LABELS, () -> "intercept: caught exception");
                cf.completeExceptionally(ex);
            }

            return cf;
        }
    }

}
