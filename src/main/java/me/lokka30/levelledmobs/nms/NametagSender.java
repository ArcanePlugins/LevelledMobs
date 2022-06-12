package me.lokka30.levelledmobs.nms;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Sends NMS verison specific nametag packets to players
 *
 * @author stumper66
 * @since 3.6.0
 */
public class NametagSender implements NMSUtil {

    public NametagSender(final String nmsVersion) {
        this.nmsVersion = nmsVersion;
    }

    private final String nmsVersion;

    public void sendNametag(final @NotNull LivingEntity livingEntity, @Nullable String nametag,
        @NotNull Player player, final boolean doAlwaysVisible) {
        // org.bukkit.craftbukkit.v1_18_R1.entity.CraftLivingEntity

        try {
            final Class<?> clazz_CraftLivingEntity = Class.forName(
                "org.bukkit.craftbukkit." + nmsVersion + ".entity.CraftLivingEntity");
            final Method method_getHandle = clazz_CraftLivingEntity.getDeclaredMethod("getHandle");
            final net.minecraft.world.entity.LivingEntity internalLivingEntity = (net.minecraft.world.entity.LivingEntity) method_getHandle.invoke(
                livingEntity);

            final SynchedEntityData entityData = cloneEntityData(
                internalLivingEntity.getEntityData(), internalLivingEntity);
            final EntityDataAccessor<Optional<Component>> customNameAccessor =
                new EntityDataAccessor<>(2, EntityDataSerializers.OPTIONAL_COMPONENT);

            // org.bukkit.craftbukkit.v1_18_R1.util.CraftChatMessage
            final Class<?> clazz_CraftChatMessage = Class.forName(
                "org.bukkit.craftbukkit." + nmsVersion + ".util.CraftChatMessage");
            final Method method_fromString = clazz_CraftChatMessage.getDeclaredMethod("fromString",
                String.class);
            // components = org.bukkit.craftbukkit.v1_18_R1.util.CraftChatMessage.fromString(nametag);
            final Component[] components = (Component[]) method_fromString.invoke(
                clazz_CraftChatMessage, nametag);

            final Optional<Component> customName = nametag == null || nametag.isEmpty()
                ? Optional.empty()
                : Optional.ofNullable(components[0]);

            entityData.set(customNameAccessor, customName);

            final EntityDataAccessor<Boolean> customNameVisibleAccessor =
                new EntityDataAccessor<>(3, EntityDataSerializers.BOOLEAN);

            entityData.set(customNameVisibleAccessor, nametag != null && doAlwaysVisible);

            final ClientboundSetEntityDataPacket packet = new ClientboundSetEntityDataPacket(
                internalLivingEntity.getId(), entityData, true
            );

            final Class<?> clazz_CraftPlayer = Class.forName(
                "org.bukkit.craftbukkit." + nmsVersion + ".entity.CraftPlayer");
            final Method method_PlayergetHandle = clazz_CraftPlayer.getDeclaredMethod("getHandle");
            final ServerPlayer serverPlayer = (ServerPlayer) method_PlayergetHandle.invoke(player);
            serverPlayer.connection.send(packet);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException |
                 ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    @NotNull
    private static SynchedEntityData cloneEntityData(@NotNull final SynchedEntityData other,
        final Entity nmsEntity) {
        final SynchedEntityData entityData = new SynchedEntityData(nmsEntity);
        if (other.getAll() == null) {
            return entityData;
        }

        //noinspection rawtypes
        for (SynchedEntityData.DataItem dataItem : other.getAll())
        {
            //noinspection unchecked
            entityData.define(dataItem.getAccessor(), dataItem.getValue());
        }

        return entityData;
    }

    public String toString() {
        return "Nametags_18_R1";
    }
}
