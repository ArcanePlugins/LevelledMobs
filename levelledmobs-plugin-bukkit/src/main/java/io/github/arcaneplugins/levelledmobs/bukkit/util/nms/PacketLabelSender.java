package io.github.arcaneplugins.levelledmobs.bukkit.util.nms;

import io.github.arcaneplugins.levelledmobs.bukkit.LevelledMobs;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@SuppressWarnings("unchecked")
public class PacketLabelSender {

    public boolean load() {
        this.def = LevelledMobs.getInstance().getNmsDefinitions();
        return true;
    }

    private Definitions def;

    public void sendLabel(
            final @NotNull LivingEntity livingEntity,
            final @NotNull Player player,
            final @NotNull String label
    ) {
        sendLabel(livingEntity,
                player,
                MiniMessage.miniMessage().deserialize(label)
        );
    }

    // TODO: change label to final @NotNull NMSComponent component
    public void sendLabel(
        final @NotNull LivingEntity livingEntity,
        final @NotNull Player player,
        final @NotNull Component label
    ) {
        if (!player.isOnline() || !player.isValid()) return;

        try {
            // livingEntity.getHandle()
            final Object internalLivingEntity = def.method_getHandle.invoke(livingEntity);
            // internalLivingEntity.getEntityData()
            final Object entityDataPreClone = def.method_getEntityData.invoke(internalLivingEntity);
            final Object entityData = cloneEntityData(entityDataPreClone, internalLivingEntity);
            if (entityData == null){
                return;
            }

            final Object optionalComponent = def.field_OPTIONAL_COMPONENT.get(def.clazz_DataWatcherRegistry);

            // final EntityDataAccessor<Optional<Component>> customNameAccessor =
            //       new EntityDataAccessor<>(2, EntityDataSerializers.OPTIONAL_COMPONENT);
            final Object customNameAccessor = def.ctor_EntityDataAccessor.newInstance(2, optionalComponent);
            final Optional<Object> customName = Optional.of(
                    def.method_AsVanilla.invoke(def.clazz_PaperAdventure, label));
            //final Optional<Object> customName = buildLabelComponent(livingEntity, label);
            //final Optional<Object> customName =
            // entityData.set(customNameAccessor, customName);
            def.method_set.invoke(entityData, customNameAccessor, customName);

            final Object BOOLEAN = def.field_BOOLEAN.get(def.clazz_DataWatcherRegistry);
            final Object customNameVisibleAccessor = def.ctor_EntityDataAccessor.newInstance(3, BOOLEAN);

            // entityData.set(customNameVisibleAccessor, !label.isNullOrEmpty() && doAlwaysVisible);
            // TODO: true / false needs to be set depending on various triggers
            def.method_set.invoke(entityData, customNameVisibleAccessor, true);

            final int livingEntityId = (int)def.method_getId.invoke(internalLivingEntity);

            Object packet;
            if (def.getIsOneNinteenThreeOrNewer()){
                // List<DataWatcher.b<?>>
                // java.util.List getAllNonDefaultValues() -> c
                final List<?> getAllNonDefaultValues = getNametagFields(entityData);
                packet = def.ctor_Packet
                        .newInstance(livingEntityId, getAllNonDefaultValues);
            }
            else{
                packet = def.ctor_Packet
                        .newInstance(livingEntityId, entityData, true);
            }

            // final ServerPlayer serverPlayer = (ServerPlayer) method_PlayergetHandle.invoke(player);
            final Object serverPlayer = def.method_PlayergetHandle.invoke(player);
            final Object connection = def.field_Connection.get(serverPlayer);

            sendPacket(connection, packet);
        } catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
            e.printStackTrace();
        }
    }

    private void sendPacket(final @NotNull Object connection, final @NotNull Object packet){
        // must run the following code on the main thread otherwise nothing happens
        // TODO: queue these packets up then invoke at once every x ticks
        // so we aren't potentially creating thousands of bukkit runnables
        final Runnable runnable = () -> {
            try {
                // serverPlayer.connection.send(packet);
                def.method_Send.invoke(connection, packet);
            } catch (IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
        };

        Bukkit.getScheduler().runTask(LevelledMobs.getInstance(), runnable);
    }

    // returns SynchedEntityData (DataWatcher)
    // args: SynchedEntityData, LivingEntity (nms)
    private @Nullable Object cloneEntityData(
            final @NotNull Object entityDataPreClone,
            final @NotNull Object internalLivingEntity
    ) throws InvocationTargetException, InstantiationException, IllegalAccessException {

        if (!def.getIsOneNinteenThreeOrNewer()) {
            return cloneEntityDataLegacy(entityDataPreClone, internalLivingEntity);
        }

        // constructor:
        // public net.minecraft.network.syncher.DataWatcher(net.minecraft.world.entity.Entity)
        final Object entityData = def.ctor_SynchedEntityData.newInstance(internalLivingEntity);

        try{
            final Map<Integer, Object> itemsById = (Map<Integer, Object>)
                    def.field_Int2ObjectMap.get(entityDataPreClone);
            if (itemsById.isEmpty()) {
                return null;
            }

            for (final Object objDataItem : itemsById.values()){
                final Object accessor = def.method_getAccessor.invoke(objDataItem);
                final Object value = def.method_getValue.invoke(objDataItem);
                def.method_define.invoke(entityData, accessor, value);
            }
            return entityData;
        }
        catch (Exception e){
            e.printStackTrace();
        }

        return entityData;
    }

    private @NotNull Object cloneEntityDataLegacy(
            final @NotNull Object entityDataPreClone,
            final @NotNull Object internalLivingEntity
    ) throws InvocationTargetException, InstantiationException, IllegalAccessException {

        final Object entityData = def.ctor_SynchedEntityData.newInstance(internalLivingEntity);
        if (def.method_getAll.invoke(entityDataPreClone) == null){
            return entityData;
        }

        // SynchedEntityData.DataItem
        // List<DataItem<?>> getAll()
        for (final Object dataItem : (List<?>)def.method_getAll.invoke(entityDataPreClone)){
            // entityData.define(dataItem.getAccessor(), dataItem.getValue());
            final Object accessor = def.method_getAccessor.invoke(dataItem);
            final Object value = def.method_getValue.invoke(dataItem);
            def.method_define.invoke(entityData, accessor, value);
        }

        return entityData;
    }

    private @NotNull List<Object> getNametagFields(final @NotNull Object entityData){
        final List<Object> results = new LinkedList<>();
        try{
            final Map<Integer, Object> itemsById = (Map<Integer, Object>)
                    def.field_Int2ObjectMap.get(entityData);
            if (itemsById.isEmpty()) {
                return results;
            }

            for (final int objDataId : itemsById.keySet()){
                if (objDataId < 2 || objDataId > 3) continue;
                final Object objDataItem = itemsById.get(objDataId);
                final Object accessor = def.method_getAccessor.invoke(objDataItem);

                // DataWatcher.Item
                final Object dataWatcherItem = def.method_DataWatcher_GetItem.invoke(entityData, accessor);
                results.add(def.method_DataWatcherItem_Value.invoke(dataWatcherItem));
                //results.add(objDataItem);
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }

        return results;
    }

    private Optional<Object> buildLabelComponent(
        final @NotNull LivingEntity livingEntity,
        final @Nullable String label
    ) {
        if (label == null || label.isEmpty()) return Optional.empty();

        // TODO: add translation support
        Object result = ComponentUtils.getTextComponent(label);
        if (result == null)
            return Optional.empty();
        else
            return Optional.of(result);
    }
}
