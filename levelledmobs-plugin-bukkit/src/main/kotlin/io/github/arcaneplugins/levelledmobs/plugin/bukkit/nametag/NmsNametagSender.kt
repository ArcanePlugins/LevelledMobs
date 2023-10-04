package io.github.arcaneplugins.levelledmobs.plugin.bukkit.nametag

import io.github.arcaneplugins.levelledmobs.plugin.bukkit.LevelledMobs
import io.github.arcaneplugins.levelledmobs.plugin.bukkit.misc.MessageUtils
import io.github.arcaneplugins.levelledmobs.plugin.bukkit.misc.NametagResult
import io.github.arcaneplugins.levelledmobs.plugin.bukkit.misc.SchedulerWrapper
import java.util.LinkedList
import java.util.Objects
import java.util.Optional
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player

@Suppress("UNCHECKED_CAST")
class NmsNametagSender {
    private val def: Definitions = LevelledMobs.lmInstance.definitions

    fun sendNametag(
        livingEntity: LivingEntity,
        nametag: NametagResult,
        player: Player,
        alwaysVisible: Boolean
    ){
        val scheduler = SchedulerWrapper(livingEntity, Runnable { sendNametagNonAsync(livingEntity, nametag, player, alwaysVisible) })
        scheduler.run()
    }

    private fun sendNametagNonAsync(
        livingEntity: LivingEntity,
        nametag: NametagResult,
        player: Player,
        doAlwaysVisible: Boolean
    ){
        // livingEntity.getHandle()
        val internalLivingEntity = def.method_getHandle!!.invoke(livingEntity)
        // internalLivingEntity.getEntityData()
        val entityDataPreClone = def.method_getEntityData!!.invoke(internalLivingEntity)
        val entityData: Any = cloneEntityData(entityDataPreClone, internalLivingEntity) ?: return

        //final Object entityData = entityDataPreClone;
        val optionalComponent = def.field_OPTIONAL_COMPONENT!![def.clazz_DataWatcherRegistry]

        // final EntityDataAccessor<Optional<Component>> customNameAccessor =
        //     //new EntityDataAccessor<>(2, EntityDataSerializers.OPTIONAL_COMPONENT);
        val customNameAccessor = def.ctor_EntityDataAccessor!!.newInstance(2, optionalComponent)
        val customName: Optional<Any> = buildNametagComponent(livingEntity, nametag)

        //final Optional<Object> customName = entityData.set(customNameAccessor, customName);
        def.method_set!!.invoke(entityData, customNameAccessor, customName)

        val bool = def.field_BOOLEAN!![def.clazz_DataWatcherRegistry]
        val customNameVisibleAccessor = def.ctor_EntityDataAccessor!!.newInstance(3, bool)

        // entityData.set(customNameVisibleAccessor, !nametag.isNullOrEmpty() && doAlwaysVisible);
        def.method_set!!.invoke(entityData, customNameVisibleAccessor, doAlwaysVisible)

        val livingEntityId = def.method_getId!!.invoke(internalLivingEntity) as Int

        val packet: Any = if (def.isOneNineteenThreeOrNewer) {
            // List<DataWatcher.b<?>>
            // java.util.List getAllNonDefaultValues() -> c
            val getAllNonDefaultValues: List<*> = getNametagFields(entityData)
            def.ctor_Packet!!
                .newInstance(livingEntityId, getAllNonDefaultValues)
        } else {
            def.ctor_Packet!!
                .newInstance(livingEntityId, entityData, true)
        }

        val serverPlayer = def.method_PlayergetHandle!!.invoke(player)
        val connection = def.field_Connection!![serverPlayer]

        // serverPlayer.connection.send(packet);
        def.method_Send!!.invoke(connection, packet)
    }

    private fun cloneEntityData(
        entityDataPreClone: Any,
        internalLivingEntity: Any
    ) : Any?{
        // constructor:
        // public net.minecraft.network.syncher.DataWatcher(net.minecraft.world.entity.Entity)
        val entityData = def.ctor_SynchedEntityData!!.newInstance(internalLivingEntity)
        val itemsById = def.field_Int2ObjectMap!![entityDataPreClone] as Map<Int, Any>

        if (itemsById.isEmpty()) {
            return null
        }

        for (objDataItem in itemsById.values) {
            val accessor = def.method_getAccessor!!.invoke(objDataItem)
            val value = def.method_getValue!!.invoke(objDataItem)
            def.method_define!!.invoke(entityData, accessor, value)
        }

        return entityData
    }

    private fun getNametagFields(
        entityData: Any
    ) : MutableList<Any>{
        val results: MutableList<Any> = LinkedList()
        val itemsById = def.field_Int2ObjectMap!![entityData] as Map<Int, Any>

        if (itemsById.isEmpty()) return results

        for (objDataId in itemsById.keys) {
            if (objDataId < 2 || objDataId > 3) continue
            val objDataItem = itemsById[objDataId]
            val accessor = def.method_getAccessor!!.invoke(objDataItem)

            // DataWatcher.Item
            val dataWatcherItem = def.method_DataWatcher_GetItem!!
                .invoke(entityData, accessor)
            results.add(def.method_DataWatcherItem_Value!!.invoke(dataWatcherItem))
        }

        return results
    }

    private fun buildNametagComponent(
        livingEntity: LivingEntity,
        nametag: NametagResult
    ): Optional<Any>{
        if (nametag.isNullOrEmpty) {
            return Optional.of(ComponentUtils.getEmptyComponent())
        }

        if (def.hasKiori) {
            // paper servers go here:
            return Optional.of(KyoriNametags.generateComponent(livingEntity, nametag))
        }

        // the rest of this method will only be used on spigot servers
        val mobName: String = nametag.nametagNonNull
        val displayName = "{DisplayName}"
        val displayNameIndex = mobName.indexOf(displayName)

        if (displayNameIndex < 0) {
            val comp = ComponentUtils.getTextComponent(MessageUtils.colorizeAll(nametag.nametagNonNull))
            return if (comp == null) Optional.empty() else Optional.of(comp)
        }

        val leftText: String? = if (displayNameIndex > 0) resolveText(mobName.substring(0, displayNameIndex)) else null

        val rightText: String? =
            if (mobName.length > displayNameIndex + displayName.length) resolveText(mobName.substring(displayNameIndex + displayName.length)) else null

        val mobNameComponent: Any?
        mobNameComponent = if (nametag.overriddenName == null) {
            if (def.useTranslationComponents) ComponentUtils.getTranslatableComponent(def.getTranslationKey(livingEntity)) else ComponentUtils.getTextComponent(
                livingEntity.name
            )
        } else {
            ComponentUtils.getTextComponent(resolveText(nametag.overriddenName))
        }

        // for whatever reason if you use an empty component,
        // the nametag will get duplicated with each call of this function
        val comp = Objects.requireNonNull(ComponentUtils.getTextComponent(""))!!

        if (leftText != null) {
            // comp.append(Component);
            ComponentUtils.appendComponents(comp, ComponentUtils.getTextComponent(leftText))
        }

        ComponentUtils.appendComponents(comp, mobNameComponent)

        if (rightText != null) {
            // comp.append(Component);
            ComponentUtils.appendComponents(comp, ComponentUtils.getTextComponent(rightText))
        }

        return Optional.of(comp)
    }

    private fun resolveText(text: String?) : String?{
        if (text.isNullOrEmpty()) return null

        return MessageUtils.colorizeAll(text)
    }
}