package me.lokka30.levelledmobs.bukkit.logic.nms;

import me.lokka30.levelledmobs.bukkit.LevelledMobs;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Optional;

public class NametagSender {
    public NametagSender(final @NotNull LevelledMobs main) {
        this.main = main;
        this.def = main.getNmsDefinitions();
    }

    private final LevelledMobs main;
    private final Definitions def;

    // TODO: change nametag to final @NotNull NMSComponent component
    public void sendNametag(final @NotNull LivingEntity livingEntity,
                            final @NotNull Player player,
                            final @NotNull String nametag){

        if (!player.isOnline() || !player.isValid()) return;

        try {
            // livingEntity.getHandle()
            final Object internalLivingEntity = def.method_getHandle.invoke(livingEntity);
            // internalLivingEntity.getEntityData()
            final Object entityDataPreClone = def.method_getEntityData.invoke(internalLivingEntity);
            final Object entityData = cloneEntityData(entityDataPreClone, internalLivingEntity);
            final Object OPTIONAL_COMPONENT = def.field_OPTIONAL_COMPONENT.get(def.clazz_DataWatcherRegistry);

            // final EntityDataAccessor<Optional<Component>> customNameAccessor =
            //       new EntityDataAccessor<>(2, EntityDataSerializers.OPTIONAL_COMPONENT);
            final Object customNameAccessor = def.ctor_EntityDataAccessor.newInstance(2, OPTIONAL_COMPONENT);
            final Optional<Object> customName = buildNametagComponent(livingEntity, nametag);
            // entityData.set(customNameAccessor, customName);
            def.method_set.invoke(entityData, customNameAccessor, customName);

            final Object BOOLEAN = def.field_BOOLEAN.get(def.clazz_DataWatcherRegistry);
            final Object customNameVisibleAccessor = def.ctor_EntityDataAccessor.newInstance(3, BOOLEAN);

            // entityData.set(customNameVisibleAccessor, !nametag.isNullOrEmpty() && doAlwaysVisible);
            // TODO: true / false needs to be set depending on various triggers
            def.method_set.invoke(entityData, customNameVisibleAccessor, true);

            final int livingEntityId = (int)def.method_getId.invoke(internalLivingEntity);
            final Object packet = def.ctor_Packet
                    .newInstance(livingEntityId, entityData, true);


            // final ServerPlayer serverPlayer = (ServerPlayer) method_PlayergetHandle.invoke(player);
            final Object serverPlayer = def.method_PlayergetHandle.invoke(player);
            final Object connection = def.field_Connection.get(serverPlayer);

            // serverPlayer.connection.send(packet);
            def.method_Send.invoke(connection, packet);
        } catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
            e.printStackTrace();
        }
    }

    // returns SynchedEntityData (DataWatcher)
    // args: SynchedEntityData, LivingEntity (nms)
    private @NotNull Object cloneEntityData(@NotNull final Object other,
        final Object internalLivingEntity) throws
            InvocationTargetException, InstantiationException, IllegalAccessException {

        final Object entityData = def.ctor_SynchedEntityData.newInstance(internalLivingEntity);
        if (def.method_getAll.invoke(other) == null){
            return entityData;
        }

        // SynchedEntityData.DataItem
        // List<DataItem<?>> getAll()
        for (final Object dataItem : (List<?>)def.method_getAll.invoke(other)){
            // entityData.define(dataItem.getAccessor(), dataItem.getValue());
            final Object accessor = def.method_getAccessor.invoke(dataItem);
            final Object value = def.method_getValue.invoke(dataItem);
            def.method_define.invoke(entityData, accessor, value);
        }

        return entityData;
    }

    private Optional<Object> buildNametagComponent(final @NotNull LivingEntity livingEntity, final @Nullable String nametag){
        if (nametag == null || nametag.isEmpty())
            return Optional.empty();

        // TODO: add translation support
        Object result = ComponentUtils.getTextComponent(nametag, main.getNmsDefinitions());
        if (result == null)
            return Optional.empty();
        else
            return Optional.of(result);
    }

    public String toString() {
        return "Nametags_NMS";
    }
}
