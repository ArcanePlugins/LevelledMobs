package io.github.arcaneplugins.levelledmobs.nametag

import io.github.arcaneplugins.levelledmobs.LevelledMobs
import io.github.arcaneplugins.levelledmobs.nametag.ComponentUtils.appendComponents
import io.github.arcaneplugins.levelledmobs.nametag.ComponentUtils.getTextComponent
import io.github.arcaneplugins.levelledmobs.nametag.ComponentUtils.getTranslatableComponent
import io.github.arcaneplugins.levelledmobs.nametag.KyoriNametags.generateComponent
import io.github.arcaneplugins.levelledmobs.result.NametagResult
import io.github.arcaneplugins.levelledmobs.util.MessageUtils.colorizeAll
import io.github.arcaneplugins.levelledmobs.wrappers.SchedulerWrapper
import java.lang.reflect.InvocationTargetException
import java.util.LinkedList
import java.util.Objects
import java.util.Optional
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player

/**
 * Sends NMS verison specific nametag packets to players
 *
 * @author PenalBuffalo (aka stumper66)
 * @since 3.6.0
 */
@Suppress("UNCHECKED_CAST")
class NmsNametagSender : NametagSender {
    private var def = LevelledMobs.instance.definitions

    override fun sendNametag(
        livingEntity: LivingEntity,
        nametag: NametagResult,
        player: Player,
        alwaysVisible: Boolean
    ) {
        if (!player.isOnline || !player.isValid) {
            return
        }

        val scheduler = SchedulerWrapper(
            livingEntity
        ) { sendNametagNonAsync(livingEntity, nametag, player, alwaysVisible) }
        scheduler.run()
    }

    fun refresh() {
        this.def = LevelledMobs.instance.definitions
    }

    private fun sendNametagNonAsync(
        livingEntity: LivingEntity,
        nametag: NametagResult,
        player: Player,
        doAlwaysVisible: Boolean
    ) {
        try {
            // livingEntity.getHandle()
            val internalLivingEntity = def.methodGetHandle!!.invoke(livingEntity)
            // internalLivingEntity.getEntityData()
            val entityDataPreClone = def.methodGetEntityData!!.invoke(internalLivingEntity)
            val entityData: Any = cloneEntityData(entityDataPreClone, internalLivingEntity) ?: return

            //final Object entityData = entityDataPreClone;
            val optionalComponent =
                def.fieldOPTIONALCOMPONENT!![def.clazzDataWatcherRegistry]
            // https://wiki.vg/Entity_metadata#Entity_Metadata_Format

            // final EntityDataAccessor<Optional<Component>> customNameAccessor =
            //     //new EntityDataAccessor<>(2, EntityDataSerializers.OPTIONAL_COMPONENT);
            val customNameAccessor =
                def.ctorEntityDataAccessor!!.newInstance(2, optionalComponent)
            val customName: Optional<Any> = buildNametagComponent(livingEntity, nametag)

            //final Optional<Object> customName = entityData.set(customNameAccessor, customName);
            def.methodSet!!.invoke(entityData, customNameAccessor, customName)

            val objBoolean = def.fieldBOOLEAN!![def.clazzDataWatcherRegistry]
            val customNameVisibleAccessor =
                def.ctorEntityDataAccessor!!.newInstance(3, objBoolean)

            // entityData.set(customNameVisibleAccessor, !nametag.isNullOrEmpty() && doAlwaysVisible);
            def.methodSet!!.invoke(entityData, customNameVisibleAccessor, doAlwaysVisible)

            val livingEntityId = def.methodGetId!!.invoke(internalLivingEntity) as Int

            val packet: Any
            if (def.isOneNinteenThreeOrNewer) {
                // List<DataWatcher.b<?>>
                // java.util.List getAllNonDefaultValues() -> c
                val getAllNonDefaultValues: List<*> = getNametagFields(entityData)
                packet = def.ctorPacket!!
                    .newInstance(livingEntityId, getAllNonDefaultValues)
            } else {
                packet = def.ctorPacket!!
                    .newInstance(livingEntityId, entityData, true)
            }

            val serverPlayer = def.methodPlayergetHandle!!.invoke(player)
            val connection = def.fieldConnection!![serverPlayer]

            // serverPlayer.connection.send(packet);
            def.methodSend!!.invoke(connection, packet)
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        } catch (e: InvocationTargetException) {
            e.printStackTrace()
        } catch (e: InstantiationException) {
            e.printStackTrace()
        }
    }

    // returns SynchedEntityData (DataWatcher)
    // args: SynchedEntityData, LivingEntity (nms)
    @Throws(InvocationTargetException::class, InstantiationException::class, IllegalAccessException::class)
    private fun cloneEntityData(
        entityDataPreClone: Any,
        internalLivingEntity: Any
    ): Any? {
        // // 1.19 - 1.20.4 uses this method:
        if (!def.isOneNinteenThreeOrNewer)
            return cloneEntityDataLegacy(entityDataPreClone, internalLivingEntity)

        // constructor:
        // public a(SyncedDataHolder synceddataholder)
        // SynchedEntityData.Builder builder = new SynchedEntityData.Builder(internalLivingEntity);
        val entityDataBuilder: Any = def.ctorSynchedEntityDataBuilder!!.newInstance(internalLivingEntity)

        try {
            // SynchedEntityData.DataItem<?>[]
            val itemsById = def.fieldInt2ObjectMap!!.get(entityDataPreClone) as Array<Any>
            if (itemsById.isEmpty())
                return null

            for (objDataItem in itemsById) {
                // .getAccessor()
                val accessor: Any = def.methodGetAccessor!!.invoke(objDataItem)
                // .getValue()
                val value: Any = def.methodGetValue!!.invoke(objDataItem)

                // builder.define(dataItem.getAccessor(), dataItem.getValue());
                def.methodDataWatcherBuilderDefine!!.invoke(entityDataBuilder, accessor, value)
            }

            // builder.build();
            return def.methodDataWatcherBuilderBuild!!.invoke(entityDataBuilder)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }

        return def.methodDataWatcherBuilderBuild!!.invoke(entityDataBuilder)
    }

    private fun cloneEntityDataLegacy(
        entityDataPreClone: Any,
        internalLivingEntity: Any
    ): Any? {
        // constructor:
        // public net.minecraft.network.syncher.DataWatcher(net.minecraft.world.entity.Entity)
        val entityData = def.ctorSynchedEntityData!!.newInstance(internalLivingEntity)

        try {
            val itemsById = def.fieldInt2ObjectMap!![entityDataPreClone] as Map<Int, Any>
            if (itemsById.isEmpty()) {
                return null
            }

            for (objDataItem in itemsById.values) {
                val accessor = def.methodGetAccessor!!.invoke(objDataItem)
                val value = def.methodGetValue!!.invoke(objDataItem)
                def.methodDefine!!.invoke(entityData, accessor, value)
            }
            return entityData
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return entityData
    }

    private fun getNametagFields(
        entityData: Any
    ): List<Any> {
        // 1.19.3 - 1.20.4 use the legacy method
        if (!def.isOneTwentyFiveOrNewer)
            return getNametagFieldsLegacy(entityData)


        // List<SynchedEntityData.DataValue<?>>
        val results: MutableList<Any> = LinkedList()

        try {
            // SynchedEntityData.DataItem<?>[]
            val itemsById =
                def.fieldInt2ObjectMap!!.get(entityData) as Array<Any>

            if (itemsById.isEmpty()) return results

            for (objItem in itemsById) {
                // objItem.value()
                val objData: Any = def.methodDataWatcherItemValue!!.invoke(objItem)

                // .id()
                val objDataId = def.methodDataWatcherGetId!!.invoke(objData) as Int
                if (objDataId < 2 || objDataId > 3) continue

                results.add(objData)
            }
        } catch (ex: java.lang.Exception) {
            ex.printStackTrace()
        }

        return results
    }

    private fun getNametagFieldsLegacy(
        entityData: Any
    ): List<Any> {
        val results: MutableList<Any> = LinkedList()

        try {
            val itemsById =
                def.fieldInt2ObjectMap!![entityData] as Map<Int, Any>

            if (itemsById.isEmpty()) return results

            for (objDataId in itemsById.keys) {
                if (objDataId < 2 || objDataId > 3) continue

                val objDataItem = itemsById[objDataId]
                val accessor = def.methodGetAccessor!!.invoke(objDataItem)

                // DataWatcher.Item
                val dataWatcherItem = def.methodDataWatcherGetItem!!
                    .invoke(entityData, accessor)

                results.add(def.methodDataWatcherItemValue!!.invoke(dataWatcherItem))
                //results.add(objDataItem);
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }

        return results
    }

    private fun buildNametagComponent(
        livingEntity: LivingEntity,
        nametag: NametagResult
    ): Optional<Any> {
        if (nametag.isNullOrEmpty) {
            return Optional.empty()
        }

        if (def.hasKiori) {
            // paper servers go here:
            return Optional.of(generateComponent(livingEntity, nametag))
        }

        // the rest of this method will only be used on spigot servers
        val mobName = nametag.nametagNonNull
        val displayName = "{DisplayName}"
        val displayNameIndex = mobName.indexOf(displayName)

        if (displayNameIndex < 0) {
            val comp = getTextComponent(colorizeAll(nametag.nametagNonNull))
            return if (comp == null) Optional.empty() else Optional.of(comp)
        }

        val leftText = if (displayNameIndex > 0) resolveText(mobName.substring(0, displayNameIndex)) else null

        val rightText =
            if (mobName.length > displayNameIndex + displayName.length) resolveText(mobName.substring(displayNameIndex + displayName.length)) else null
        val mobNameComponent = if (nametag.overriddenName == null) {
            if (def.useTranslationComponents) getTranslatableComponent(def.getTranslationKey(livingEntity)) else getTextComponent(
                livingEntity.name
            )
        } else {
            getTextComponent(resolveText(nametag.overriddenName))
        }

        // for whatever reason if you use an empty component,
        // the nametag will get duplicated with each call of this function
        val comp = Objects.requireNonNull(getTextComponent(""))

        if (leftText != null) {
            // comp.append(Component);
            appendComponents(comp!!, getTextComponent(leftText))
        }

        appendComponents(comp!!, mobNameComponent)

        if (rightText != null) {
            // comp.append(Component);
            appendComponents(comp, getTextComponent(rightText))
        }

        return Optional.of(comp)
    }

    private fun resolveText(text: String?): String? {
        if (text.isNullOrEmpty()) return null

        return colorizeAll(text)
    }

    override fun toString(): String {
        return "Nametags_NMS"
    }
}