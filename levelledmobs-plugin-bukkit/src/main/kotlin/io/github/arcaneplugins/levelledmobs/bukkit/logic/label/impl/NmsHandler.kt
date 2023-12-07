package io.github.arcaneplugins.levelledmobs.bukkit.logic.label.impl

import io.github.arcaneplugins.levelledmobs.bukkit.logic.LogicHandler
import io.github.arcaneplugins.levelledmobs.bukkit.logic.SchedulerWrapper
import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.Context
import io.github.arcaneplugins.levelledmobs.bukkit.util.ComponentUtils
import io.github.arcaneplugins.levelledmobs.bukkit.util.Log
import io.github.arcaneplugins.levelledmobs.bukkit.util.MessageUtils
import java.util.Optional
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import io.github.arcaneplugins.levelledmobs.bukkit.util.NmsDefinitions as def

object NmsHandler{
    fun update(
        context: Context,
        formula: String
    ) {
        var nametag = formula.replace("%entity-name%", "{Entity-Name}")
        for (contextHandler in LogicHandler.CONTEXT_PLACEHOLDER_HANDLER.contextPlaceholders){
            nametag = contextHandler.replace(nametag, context)
        }

        if (context.player != null){
            sendNametag(
                context.livingEntity!!,
                nametag,
                context.player!!,
                true
            )
        }
    }

    private fun sendNametag(
        livingEntity: LivingEntity,
        nametag: String,
        player: Player,
        alwaysVisible: Boolean
    ){
        if (!player.isOnline || !player.isValid) {
            Log.inf("sendNametag: player was not online or invalid")
            return
        }

        val scheduler = SchedulerWrapper(livingEntity) {
            sendNametagNonAsync(
                livingEntity,
                nametag,
                player,
                alwaysVisible
            )
        }

        scheduler.run()
    }

    private fun sendNametagNonAsync(
        livingEntity: LivingEntity,
        nametag: String,
        player: Player,
        doAlwaysVisible: Boolean
    ){
        try {
            // livingEntity.getHandle()
            val internalLivingEntity: Any = def.method_getHandle!!.invoke(livingEntity)
            // internalLivingEntity.getEntityData()
            val entityDataPreClone: Any = def.method_getEntityData!!.invoke(internalLivingEntity)
            val entityData: Any = cloneEntityData(entityDataPreClone, internalLivingEntity) ?: return

            //final Object entityData = entityDataPreClone;
            val optionalComponent = def.field_OPTIONAL_COMPONENT!![def.clazz_DataWatcherRegistry]

            // final EntityDataAccessor<Optional<Component>> customNameAccessor =
            //     //new EntityDataAccessor<>(2, EntityDataSerializers.OPTIONAL_COMPONENT);
            val customNameAccessor = def.ctor_EntityDataAccessor!!.newInstance(2, optionalComponent)
            val customName: Optional<Any> = buildNametagComponent(livingEntity, nametag)

            //final Optional<Object> customName = entityData.set(customNameAccessor, customName);
            def.method_set!!.invoke(entityData, customNameAccessor, customName)

            val BOOLEAN = def.field_BOOLEAN!![def.clazz_DataWatcherRegistry]
            val customNameVisibleAccessor = def.ctor_EntityDataAccessor!!.newInstance(3, BOOLEAN)

            // entityData.set(customNameVisibleAccessor, !nametag.isNullOrEmpty() && doAlwaysVisible);
            def.method_set!!.invoke(entityData, customNameVisibleAccessor, doAlwaysVisible)

            val livingEntityId = def.method_getId!!.invoke(internalLivingEntity) as Int

            val packet: Any = if (def.isOneNinteenThreeOrNewer) {
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
        catch (ex: Exception){
            ex.printStackTrace()
        }
    }

    // returns SynchedEntityData (DataWatcher)
    // args: SynchedEntityData, LivingEntity (nms)
    private fun cloneEntityData(
        entityDataPreClone: Any,
        internalLivingEntity: Any
    ): Any?{
        // constructor:
        // public net.minecraft.network.syncher.DataWatcher(net.minecraft.world.entity.Entity)
        val entityData = def.ctor_SynchedEntityData!!.newInstance(internalLivingEntity)

        try {
            val itemsById = def.field_Int2ObjectMap!![entityDataPreClone] as Map<*, *>
            if (itemsById.isEmpty()) {
                return null
            }
            for (objDataItem in itemsById.values) {
                val accessor = def.method_getAccessor!!.invoke(objDataItem)
                val value = def.method_getValue!!.invoke(objDataItem)
                def.method_define!!.invoke(entityData, accessor, value)
            }
            return entityData
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }

        return entityData
    }

    @Suppress("UNCHECKED_CAST")
    private fun getNametagFields(
        entityData: Any
    ): MutableList<Any>{
        val results = mutableListOf<Any>()

        try {
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
                //results.add(objDataItem);
            }
        } catch (ex: java.lang.Exception) {
            ex.printStackTrace()
        }

        return results
    }

    private fun buildNametagComponent(
        livingEntity: LivingEntity,
        nametag: String
    ): Optional<Any>{
        if (nametag.isEmpty()) {
            return Optional.of(ComponentUtils.getEmptyComponent())
        }

        if (def.hasKiori) {
            // paper servers go here:
            return Optional.of(KyoriNametags.generateComponent(livingEntity, nametag))
        }

        // the rest of this method will only be used on spigot servers


        // the rest of this method will only be used on spigot servers
        val mobName: String = nametag
        val displayName = "{DisplayName}"
        val displayNameIndex = mobName.indexOf(displayName)

        if (displayNameIndex < 0) {
            val comp = ComponentUtils.getTextComponent(MessageUtils.colorizeAll(nametag))
            return if (comp == null) Optional.empty() else Optional.of(comp)
        }

        val leftText = if (displayNameIndex > 0) resolveText(mobName.substring(0, displayNameIndex)) else null

        val rightText =
            if (mobName.length > displayNameIndex + displayName.length) resolveText(mobName.substring(displayNameIndex + displayName.length)) else null

        val mobNameComponent =
            if (def.useTranslationComponents)
                ComponentUtils.getTranslatableComponent(def.getTranslationKey(livingEntity))
            else
                ComponentUtils.getTextComponent(livingEntity.name)

        // for whatever reason if you use an empty component,
        // the nametag will get duplicated with each call of this function

        // for whatever reason if you use an empty component,
        // the nametag will get duplicated with each call of this function
        val comp = ComponentUtils.getEmptyComponent()

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

    private fun resolveText(
        text: String?
    ): String?{
        if (text.isNullOrEmpty()) return null

        return MessageUtils.colorizeAll(text)
    }
}