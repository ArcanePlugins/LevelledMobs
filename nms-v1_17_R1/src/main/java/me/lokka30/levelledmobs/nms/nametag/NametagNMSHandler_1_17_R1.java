package me.lokka30.levelledmobs.nms.nametag;

import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_17_R1.util.CraftChatMessage;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class NametagNMSHandler_1_17_R1 implements NametagNMSHandler {

    public void sendNametag(final @NotNull LivingEntity livingEntity, @Nullable String nametag,
        @NotNull Player player, final boolean alwaysVisible) {
        final CraftLivingEntity cle = (CraftLivingEntity) livingEntity;
        final net.minecraft.world.entity.LivingEntity internalLivingEntity = cle.getHandle();

        final SynchedEntityData entityData = cloneEntityData(internalLivingEntity.getEntityData(),
            internalLivingEntity);
        final EntityDataAccessor<Optional<Component>> customNameAccessor =
            new EntityDataAccessor<>(2, EntityDataSerializers.OPTIONAL_COMPONENT);

        final Optional<Component> customName = nametag == null || nametag.isEmpty()
            ? Optional.empty()
            : Optional.ofNullable(CraftChatMessage.fromString(nametag)[0]);

        entityData.set(customNameAccessor, customName);

        final EntityDataAccessor<Boolean> customNameVisibleAccessor =
            new EntityDataAccessor<>(3, EntityDataSerializers.BOOLEAN);

        entityData.set(customNameVisibleAccessor, nametag != null && alwaysVisible);

        final ClientboundSetEntityDataPacket packet = new ClientboundSetEntityDataPacket(
            internalLivingEntity.getId(), entityData, true
        );

        ((CraftPlayer) player).getHandle().connection.send(packet);
    }

    @NotNull
    private static SynchedEntityData cloneEntityData(@NotNull final SynchedEntityData other,
        final Entity nmsEntity) {
        final SynchedEntityData entityData = new SynchedEntityData(nmsEntity);
        if (other.getAll() == null) {
            return entityData;
        }

        //noinspection rawtypes
        for (SynchedEntityData.DataItem dataItem : other.getAll()) {
            //noinspection unchecked
            entityData.define(dataItem.getAccessor(), dataItem.getValue());
        }

        return entityData;
    }
}
