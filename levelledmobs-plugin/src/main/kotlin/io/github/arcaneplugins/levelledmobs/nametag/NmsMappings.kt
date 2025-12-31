package io.github.arcaneplugins.levelledmobs.nametag

import io.github.arcaneplugins.levelledmobs.LevelledMobs

/**
 * Holds mappings for spigot and mojang to be used with
 * the field names in Definitions class
 *
 * @author stumper66
 * @since 4.3.2
 */
object NmsMappings {
    private val mappings = mutableMapOf<String, MappingInfo>()

    fun load(){
        mappings["clazzDataWatcher"] = MappingInfo(
            "net.minecraft.network.syncher.DataWatcher",
            "net.minecraft.network.syncher.SynchedEntityData"
        )
        mappings["clazzDataWatcherBuilder"] = MappingInfo(
             $$"net.minecraft.network.syncher.DataWatcher$a",
             $$"net.minecraft.network.syncher.SynchedEntityData$Builder"
        )
        mappings["clazzDataWatcherValue"] = MappingInfo(
            $$"net.minecraft.network.syncher.DataWatcher$c",
            $$"net.minecraft.network.syncher.SynchedEntityData$DataValue"
        )
        mappings["clazzDataWatcherItem"] = MappingInfo(
            $$"net.minecraft.network.syncher.DataWatcher$Item",
            $$"net.minecraft.network.syncher.SynchedEntityData$DataItem"
        )
        mappings["clazzDataWatcherRegistry"] = MappingInfo(
            "net.minecraft.network.syncher.DataWatcherRegistry",
            "net.minecraft.network.syncher.EntityDataSerializers"
        )
        mappings["clazzDataWatcherObject"] = MappingInfo(
            "net.minecraft.network.syncher.DataWatcherObject",
            "net.minecraft.network.syncher.EntityDataAccessor"
        )
        mappings["clazzDataWatcherSerializer"] = MappingInfo(
            "net.minecraft.network.syncher.DataWatcherSerializer",
            "net.minecraft.network.syncher.EntityDataSerializer"
        )
        mappings["clazzClientboundSetEntityDataPacket"] = MappingInfo(
            "net.minecraft.network.protocol.game.PacketPlayOutEntityMetadata",
            "net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket"
        )
        mappings["clazzNetworkManager"] = MappingInfo(
            "net.minecraft.network.NetworkManager",
            "net.minecraft.network.Connection"
        )
        mappings["clazzPlayerConnection"] = MappingInfo(
            "net.minecraft.network.protocol.game.PacketPlayOutEntityMetadata",
            "net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket"
        )
        mappings["clazzEntityPlayer"] = MappingInfo(
            "net.minecraft.server.level.EntityPlayer",
            "net.minecraft.server.level.ServerPlayer"
        )
        mappings["clazzEntityTypes"] = MappingInfo(
            "net.minecraft.world.entity.EntityTypes",
            "net.minecraft.world.entity.EntityType"
        )
        mappings["methodDataWatcherBuilderDefine"] = MappingInfo(
            "a",
            "define"
        )
        mappings["methodDataWatcherBuilderBuild"] = MappingInfo(
            "a",
            "build"
        )
        mappings["methodDataWatcherGetId"] = MappingInfo(
            "a",
            "id"
        )
        mappings["methodEntityTypeByString"] = MappingInfo(
            "a",
            "byString"
        )
        mappings["methodGetDescriptionId"] = MappingInfo(
            "g",
            "getDescriptionId"
        )
        mappings["methodGetNonDefaultValues"] = MappingInfo(
            "c",
            "getNonDefaultValues"
        )
        mappings["methodSynchedEntityDataDefine"] = MappingInfo(
            "a",
            "set"
        )
        mappings["methodDataWatcherGetItem"] = MappingInfo(
            "b",
            "getItem"
        )
        mappings["methodDataWatcherItemValue"] = MappingInfo(
            "e",
            "value"
        )
        mappings["fieldOPTIONALCOMPONENT"] = MappingInfo(
            "g",
            "OPTIONAL_COMPONENT"
        )
        mappings["fieldBOOLEAN"] = MappingInfo(
            "k",
            "BOOLEAN"
        )
        mappings["fieldInt2ObjectMap"] = MappingInfo(
            "e",
            "itemsById"
        )
    }

    fun getMapping(name: String): String {
        val mapping = mappings[name] ?: throw IllegalArgumentException("Invalid NmsMapping: $name")

        return if (LevelledMobs.instance.ver.useMojangMappings)
            mapping.mojangName
        else
            mapping.spigotName
    }

    private data class MappingInfo(
        val spigotName: String,
        val mojangName: String
    )
}