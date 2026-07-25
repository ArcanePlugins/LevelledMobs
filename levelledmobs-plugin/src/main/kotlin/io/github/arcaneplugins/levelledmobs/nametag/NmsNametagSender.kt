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

    // The custom-name (id 2) and custom-name-visible (id 3) EntityDataAccessors are
    // version-constant and player-independent, so they are built once and reused instead of
    // being reconstructed via reflection (two field reads + two ctor invocations) on every
    // single packet send.
    @Volatile
    private var accessorsInitialized = false
    private var customNameAccessor: Any? = null
    private var customNameVisibleAccessor: Any? = null

    override fun sendNametag(
        livingEntity: LivingEntity,
        nametag: NametagResult,
        player: Player,
        alwaysVisible: Boolean
    ) {
        if (!player.isOnline || !player.isValid) return

        if (LevelledMobs.instance.ver.isRunningFolia)
            sendNametagNonAsync(livingEntity, nametag, player, alwaysVisible)
        else{
            val scheduler = SchedulerWrapper(livingEntity) {
                sendNametagNonAsync(livingEntity, nametag, player, alwaysVisible)
            }
            scheduler.run()
        }
    }

    fun refresh() {
        synchronized(this) {
            this.def = LevelledMobs.instance.definitions
            this.accessorsInitialized = false
            this.customNameAccessor = null
            this.customNameVisibleAccessor = null
        }
    }

    // Builds the two constant EntityDataAccessors once. Double-checked locking keeps it safe
    // even on Folia where sendNametagNonAsync can run from multiple region threads.
    private fun ensureAccessors() {
        if (accessorsInitialized) return

        synchronized(this) {
            if (accessorsInitialized) return

            // net.minecraft.network.syncher.EntityDataSerializers.OPTIONAL_COMPONENT
            val optionalComponent = def.fieldOPTIONALCOMPONENT!![def.clazzDataWatcherRegistry]
            // new EntityDataAccessor<>(2, EntityDataSerializers.OPTIONAL_COMPONENT)
            customNameAccessor = def.ctorEntityDataAccessor!!.newInstance(2, optionalComponent)

            // net.minecraft.network.syncher.EntityDataSerializers.BOOLEAN
            val objBoolean = def.fieldBOOLEAN!![def.clazzDataWatcherRegistry]
            // new EntityDataAccessor<>(3, EntityDataSerializers.BOOLEAN)
            customNameVisibleAccessor = def.ctorEntityDataAccessor!!.newInstance(3, objBoolean)

            accessorsInitialized = true
        }
    }

    private fun sendNametagNonAsync(
        livingEntity: LivingEntity,
        nametag: NametagResult,
        player: Player,
        doAlwaysVisible: Boolean
    ) {
        try {
            // https://wiki.vg/Entity_metadata#Entity_Metadata_Format
            // final EntityDataAccessor<Optional<Component>> customNameAccessor = new EntityDataAccessor<>(2, ...);
            // final EntityDataAccessor<Boolean> customNameVisibleAccessor = new EntityDataAccessor<>(3, ...);
            ensureAccessors()

            // livingEntity.getHandle()
            val internalLivingEntity = def.methodGetHandle!!.invoke(livingEntity)
            // internalLivingEntity.getEntityData()
            val entityDataPreClone = def.methodGetEntityData!!.invoke(internalLivingEntity)
            val entityData: Any = cloneEntityData(entityDataPreClone, internalLivingEntity) ?: return

            // The custom name component doesn't depend on the viewing player, so it is built
            // once per nametag update and cached on the NametagResult for the other players.
            val customName: Optional<Any> = buildNametagComponent(livingEntity, nametag)

            //final Optional<Object> customName = entityData.set(customNameAccessor, customName);
            def.methodSet!!.invoke(entityData, customNameAccessor!!, customName)

            // entityData.set(customNameVisibleAccessor, !nametag.isNullOrEmpty() && doAlwaysVisible);
            def.methodSet!!.invoke(entityData, customNameVisibleAccessor!!, doAlwaysVisible)

            val livingEntityId = def.methodGetId!!.invoke(internalLivingEntity) as Int

            // List<DataWatcher.b<?>>
            // java.util.List getAllNonDefaultValues() -> c
            val getAllNonDefaultValues: List<*> = getNametagFields(entityData)
            val packet = def.ctorPacket!!
                .newInstance(livingEntityId, getAllNonDefaultValues)

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
        // constructor:
        // public a(SyncedDataHolder synceddataholder)
        // SynchedEntityData.Builder builder = new SynchedEntityData.Builder(internalLivingEntity);
        val entityDataBuilder: Any = def.ctorSynchedEntityDataBuilder!!.newInstance(internalLivingEntity)

        try {
            // SynchedEntityData.DataItem<?>[]
            val itemsById = def.fieldInt2ObjectMap!!.get(entityDataPreClone) as Array<Any>
            if (itemsById.isEmpty()) return null

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

    private fun getNametagFields(
        entityData: Any
    ): List<Any> {
        // List<SynchedEntityData.DataValue<?>>
        val results: MutableList<Any> = ArrayList(2)

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
                if (objDataId !in 2..3) continue

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
        val results: MutableList<Any> = ArrayList(2)

        try {
            val itemsById =
                def.fieldInt2ObjectMap!![entityData] as Map<Int, Any>

            if (itemsById.isEmpty()) return results

            for (objDataId in itemsById.keys) {
                if (objDataId !in 2..3) continue

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
        // reuse the previously built component if this result was already rendered for
        // another player during the same update cycle
        nametag.cachedComponent?.let { return it }

        val built = buildNametagComponentInternal(livingEntity, nametag)
        nametag.cachedComponent = built
        return built
    }

    private fun buildNametagComponentInternal(
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

        val leftText = if (displayNameIndex > 0) resolveText(mobName.take(displayNameIndex)) else null

        val rightText =
            if (mobName.length > displayNameIndex + displayName.length) resolveText(mobName.substring(displayNameIndex + displayName.length)) else null
        val mobNameComponent = if (nametag.overriddenName == null) {
            if (def.useTranslationComponents) getTranslatableComponent(def.getTranslationKey(livingEntity)) else getTextComponent(
                livingEntity.name
            )
        }
        else
            getTextComponent(resolveText(nametag.overriddenName))

        // for whatever reason if you use an empty component,
        // the nametag will get duplicated with each call of this function
        val comp = getTextComponent("")!!

        if (leftText != null) {
            // comp.append(Component);
            appendComponents(comp, getTextComponent(leftText))
        }

        appendComponents(comp, mobNameComponent)

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