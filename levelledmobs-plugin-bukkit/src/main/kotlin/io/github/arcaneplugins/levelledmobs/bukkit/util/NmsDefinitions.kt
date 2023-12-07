package io.github.arcaneplugins.levelledmobs.bukkit.util

import io.github.arcaneplugins.levelledmobs.bukkit.util.ServerInfoInfo
import net.kyori.adventure.text.Component
import org.bukkit.entity.LivingEntity
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.util.Optional
import io.github.arcaneplugins.levelledmobs.bukkit.util.ServerInfoInfo as ver

object NmsDefinitions {
    private var hasMiniMessage = false
    var mm: net.kyori.adventure.text.minimessage.MiniMessage? = null
        private set

    fun load() {
        build()
        if (hasMiniMessage) mm = net.kyori.adventure.text.minimessage.MiniMessage.miniMessage()
    }

    var hasKiori = false
        private set
    var isOneNinteenThreeOrNewer = false
        private set
    // TODO: make this setting configurable
    var useTranslationComponents = true
     private set
    // TODO: make this setting configurable
    var useLegacySerializer = true

    // classes:
    var clazz_IChatMutableComponent: Class<*>? = null
        private set
    var clazz_IChatBaseComponent: Class<*>? = null
        private set
    var clazz_TranslatableComponent: Class<*>? = null
        private set
    var clazz_CraftLivingEntity: Class<*>? = null
        private set
    var clazz_CraftEntity: Class<*>? = null
        private set
    var clazz_Entity: Class<*>? = null
        private set
    var clazz_DataWatcher: Class<*>? = null
        private set
    var clazz_DataWatcher_Item: Class<*>? = null
        private set
    var clazz_DataWatcherRegistry: Class<*>? = null
        private set
    var clazz_DataWatcherObject: Class<*>? = null
        private set
    var clazz_DataWatcherSerializer: Class<*>? = null
        private set
    var clazz_ClientboundSetEntityDataPacket: Class<*>? = null
        private set
    var clazz_CraftPlayer: Class<*>? = null
        private set
    var clazz_Packet: Class<*>? = null
        private set
    var clazz_PlayerConnection: Class<*>? = null
        private set
    var clazz_ServerPlayerConnection: Class<*>? = null
        private set
    var clazz_NetworkManager: Class<*>? = null
        private set
    var clazz_EntityPlayer: Class<*>? = null
        private set
    var clazz_PaperAdventure: Class<*>? = null
        private set
    var clazz_EntityTypes: Class<*>? = null
        private set

    // methods:
    var method_ComponentAppend: Method? = null
        private set
    var method_EmptyComponent: Method? = null
        private set
    var method_TextComponent: Method? = null
        private set
    var method_TranslatableWithArgs: Method? = null
        private set
    var method_getHandle: Method? = null
        private set
    var method_getEntityData: Method? = null
        private set
    var method_set: Method? = null
        private set
    var method_getId: Method? = null
        private set
    var method_PlayergetHandle: Method? = null
        private set
    var method_Send: Method? = null
        private set
    var method_getAll: Method? = null
        private set
    var method_define: Method? = null
        private set
    var method_getAccessor: Method? = null
        private set
    var method_getValue: Method? = null
        private set
    var method_AsVanilla: Method? = null
        private set
    var method_EntityTypeByString: Method? = null
        private set
    var method_GetDescriptionId: Method? = null
        private set
    var method_getNonDefaultValues: Method? = null
        private set
    var method_SynchedEntityData_Define: Method? = null
        private set
    var method_DataWatcher_GetItem: Method? = null
        private set
    var method_DataWatcherItem_Value: Method? = null
        private set

    // fields
    var field_OPTIONAL_COMPONENT: Field? = null
        private set
    var field_BOOLEAN: Field? = null
        private set
    var field_Connection: Field? = null
        private set
    var field_Int2ObjectMap: Field? = null
        private set

    // Constructors
    var ctor_EntityDataAccessor: Constructor<*>? = null
    var ctor_SynchedEntityData: Constructor<*>? = null
    var ctor_Packet: Constructor<*>? = null

    private fun build(){
        isOneNinteenThreeOrNewer = ver.minecraftVersion == 1.19 && ver.minecraftVersion >= 3.0 ||
                ver.minecraftVersion >= 1.20

        try {
            buildClasses()

            // build methods
            getMethodComponentAppend()
            getMethodTextComponents()
            getMethodTranslatable()
            buildSimpleMethods()
            buildFields()
            buildConstructors()
        } catch (ex: Exception) {
            ExceptionUtil.printExceptionNicely(
                ex = ex,
                context = "An error has occurred while building the NMS mappings for Minecraft"
            )
        }
    }

    private fun buildClasses(){
        clazz_IChatMutableComponent = Class.forName(
            "net.minecraft.network.chat.IChatMutableComponent"
        )

        clazz_IChatBaseComponent = Class.forName(
            "net.minecraft.network.chat.IChatBaseComponent"
        )

        clazz_CraftEntity = Class.forName(
            "org.bukkit.craftbukkit.${ver.nmsVersion}.entity.CraftEntity"
        )

        clazz_CraftLivingEntity = Class.forName(
            "org.bukkit.craftbukkit.${ver.nmsVersion}.entity.CraftLivingEntity"
        )

        // net.minecraft.network.syncher.SynchedEntityData
        clazz_DataWatcher = Class.forName(
            "net.minecraft.network.syncher.DataWatcher"
        )

        clazz_DataWatcher_Item = Class.forName(
            "net.minecraft.network.syncher.DataWatcher\$Item"
        )

        clazz_DataWatcherRegistry = Class.forName(
            "net.minecraft.network.syncher.DataWatcherRegistry"
        )

        clazz_Entity = Class.forName(
            "net.minecraft.world.entity.Entity"
        )

        clazz_DataWatcherObject = Class.forName(
            "net.minecraft.network.syncher.DataWatcherObject"
        )

        clazz_DataWatcherSerializer = Class.forName(
            "net.minecraft.network.syncher.DataWatcherSerializer"
        )

        clazz_ClientboundSetEntityDataPacket = Class.forName(
            "net.minecraft.network.protocol.game.PacketPlayOutEntityMetadata"
        )

        clazz_CraftPlayer = Class.forName(
            "org.bukkit.craftbukkit.${ver.nmsVersion}.entity.CraftPlayer"
        )

        clazz_Packet = Class.forName(
            "net.minecraft.network.protocol.Packet"
        )

        clazz_NetworkManager = Class.forName(
            "net.minecraft.network.NetworkManager"
        )

        //net.minecraft.server.network.ServerPlayerConnection ->
        // void send(net.minecraft.network.protocol.Packet) ->

        //net.minecraft.server.network.ServerPlayerConnection ->
        // void send(net.minecraft.network.protocol.Packet) ->
        clazz_PlayerConnection = Class.forName(
            "net.minecraft.server.network.PlayerConnection"
        )

        clazz_ServerPlayerConnection = Class.forName(
            "net.minecraft.server.network.ServerPlayerConnection"
        )

        // net.minecraft.server.level.ServerPlayer ->

        // net.minecraft.server.level.ServerPlayer ->
        clazz_EntityPlayer = Class.forName(
            "net.minecraft.server.level.EntityPlayer"
        )

        try {
            clazz_PaperAdventure = Class.forName(
                "io.papermc.paper.adventure.PaperAdventure"
            )
            hasKiori = true
        } catch (ignored: ClassNotFoundException) {}

        clazz_EntityTypes = Class.forName(
            "net.minecraft.world.entity.EntityTypes"
        )

        if (hasKiori) {
            try {
                Class.forName("net.kyori.adventure.text.minimessage.MiniMessage")
                hasMiniMessage = true
            } catch (ignored: ClassNotFoundException) {}
        }
    }

    private fun getMethodComponentAppend(){
        // # {"fileName":"MutableComponent.java","id":"sourceFile"}
        // net.minecraft.network.chat.MutableComponent append(net.minecraft.network.chat.Component) ->
        // 1.19.0 = a, everything else  = b
        val methodName = if (ver.minecraftVersion == 1.19 && ver.revision == 0
        ) "a" else "b"

        method_ComponentAppend = clazz_IChatMutableComponent!!.getDeclaredMethod(
            methodName, clazz_IChatBaseComponent
        )
    }

    private fun getMethodTextComponents(){
        // # {"fileName":"Component.java","id":"sourceFile"}
        // net.minecraft.network.chat.Component ->
        //     net.minecraft.network.chat.MutableComponent empty()


        // # {"fileName":"Component.java","id":"sourceFile"}
        // net.minecraft.network.chat.Component ->
        //     net.minecraft.network.chat.MutableComponent empty()
        val methodName: String = if (ver.minecraftVersion >= 1.20) {
            if (ver.revision >= 3) // 1.20.3+ or 1.20.0 - 2
                "i" else "h"
        } else {
            // 1.19.0 = g, 1.19.1+ = h
            if (ver.revision == 0) "g" else "h"
        }

        // net.minecraft.network.chat.Component ->
        //     net.minecraft.network.chat.MutableComponent empty()
        method_EmptyComponent = clazz_IChatBaseComponent!!.getDeclaredMethod(methodName)
    }

    private fun getMethodTranslatable(){
        // # {"fileName":"Component.java","id":"sourceFile"}
        // net.minecraft.network.chat.Component ->
        // net.minecraft.network.chat.MutableComponent translatable(java.lang.String,java.lang.Object[])

        method_TranslatableWithArgs = clazz_IChatBaseComponent!!.getDeclaredMethod(
            "a",
            String::class.java, Array<Any>::class.java
        )
    }

    private fun buildSimpleMethods(){
        method_getHandle = clazz_CraftLivingEntity!!.getDeclaredMethod("getHandle")

        // # {"fileName":"Entity.java","id":"sourceFile"}
        // net.minecraft.network.syncher.SynchedEntityData getEntityData() ->
        var methodName: String

        methodName = when (ver.majorVersionEnum) {
            ServerInfoInfo.MinecraftMajorVersion.V1_20 -> {
                if (ver.revision >= 3) {
                    // 1.20.3+
                    "an"
                } else if (ver.revision == 2) {
                    // 1.20.2
                    "al"
                } else {
                    // 1.20 - 1.20.1
                    "aj"
                }
            }

            ServerInfoInfo.MinecraftMajorVersion.V1_19 -> {
                if (ver.revision >= 4) {
                    "aj"
                } else if (ver.revision == 3) {
                    "al"
                } else {
                    "ai"
                }
            }

            ServerInfoInfo.MinecraftMajorVersion.V1_18 -> "ai"
            ServerInfoInfo.MinecraftMajorVersion.V1_17 -> "getDataWatcher"
            else -> throw RuntimeException("Unable to determine NMS method name for your Minecraft server version. Is your server version compatible?")
        }

        // net.minecraft.network.syncher.SynchedEntityData getEntityData() ->
        method_getEntityData = clazz_Entity!!.getMethod(methodName)

        // set(net.minecraft.network.syncher.EntityDataAccessor,java.lang.Object) ->
        method_set = clazz_DataWatcher!!.getMethod(
            "b", clazz_DataWatcherObject,
            Any::class.java
        )

        // # {"fileName":"ChunkStatusUpdateListener.java","id":"sourceFile"}
        // net.minecraft.world.level.entity.EntityAccess ->
        //   int getId() ->
        if (ver.minecraftVersion >= 1.20) {
            methodName = if (ver.revision >= 3) {
                // 1.20.3+
                "aj"
            } else {
                if (ver.revision >= 2) "ah" else "af"
            }
        } else if (ver.minecraftVersion >= 1.18) {
            methodName = if (ver.revision >= 4) {
                "af"
            } else if (isOneNinteenThreeOrNewer) {
                // 1.19.3
                "ah"
            } else {
                // 1.18 - 1.19.2
                "ae"
            }
        } else if (ver.minecraftVersion <= 1.17) {
            methodName = "getId"
        }

        method_getId = clazz_Entity!!.getDeclaredMethod(methodName)
        method_PlayergetHandle = clazz_CraftPlayer!!.getDeclaredMethod("getHandle")

        // starting with 1.20.2 it is:
        // # {"fileName":"ServerCommonPacketListenerImpl.java","id":"sourceFile"}
        // net.minecraft.server.network.ServerCommonPacketListenerImpl ->

        // 1.20.1 and older:
        // net.minecraft.server.network.ServerGamePacketListenerImpl ->

        //    void send(net.minecraft.network.protocol.Packet) ->

        methodName = if (ver.majorVersionEnum == ServerInfoInfo.MinecraftMajorVersion.V1_20 && ver.revision >= 2) {
            "b"
        } else {
            "a"
        }

        method_Send = clazz_ServerPlayerConnection!!.getDeclaredMethod(methodName, clazz_Packet)

        // # {"fileName":"SynchedEntityData.java","id":"sourceFile"}
        // net.minecraft.network.syncher.SynchedEntityData ->
        //    define(net.minecraft.network.syncher.EntityDataAccessor,java.lang.Object) ->
        method_define = clazz_DataWatcher!!.getDeclaredMethod(
            "a",
            clazz_DataWatcherObject, Any::class.java
        )

        // net.minecraft.network.syncher.EntityDataAccessor getAccessor() ->
        method_getAccessor = clazz_DataWatcher_Item!!.getDeclaredMethod("a")
        // java.lang.Object getValue() ->
        method_getValue = clazz_DataWatcher_Item!!.getDeclaredMethod("b")

        // net.minecraft.network.Connection getConnection() ->
        //this.method_getConnection = clazz_CraftPlayer.getDeclaredMethod("networkManager");
        if (hasKiori) {
            method_AsVanilla = clazz_PaperAdventure!!.getDeclaredMethod(
                "asVanilla",
                Component::class.java
            )
        }

        // java.util.Optional byString(java.lang.String) ->
        method_EntityTypeByString = clazz_EntityTypes!!.getDeclaredMethod("a", String::class.java)

        // java.lang.String getDescriptionId() ->
        method_GetDescriptionId = clazz_EntityTypes!!.getDeclaredMethod("g")

        if (this.isOneNinteenThreeOrNewer) {
            // the methods here were added in 1.19.3

            // java.util.List getNonDefaultValues() ->
            method_getNonDefaultValues = clazz_DataWatcher!!.getDeclaredMethod("c")

            // define(net.minecraft.network.syncher.EntityDataAccessor,java.lang.Object) ->
            method_SynchedEntityData_Define = clazz_DataWatcher!!.getMethod(
                "a",
                clazz_DataWatcherObject, Any::class.java
            )

            // net.minecraft.network.syncher.SynchedEntityData$DataItem getItem(net.minecraft.network.syncher.EntityDataAccessor) ->
            // private <T> DataWatcher.Item<T> getItem(DataWatcherObject<T> datawatcherobject)
            methodName = if (ver.minecraftVersion >= 1.20) "c" else "b"
            method_DataWatcher_GetItem = clazz_DataWatcher!!.getDeclaredMethod(
                methodName,
                clazz_DataWatcherObject
            )
            method_DataWatcher_GetItem!!.setAccessible(true)

            // net.minecraft.network.syncher.SynchedEntityData$DataItem ->
            //       net.minecraft.network.syncher.SynchedEntityData$DataValue value() ->
            method_DataWatcherItem_Value = clazz_DataWatcher_Item!!.getDeclaredMethod("e")
        }
    }

    private fun buildFields(){
        // net.minecraft.network.syncher.EntityDataSerializer OPTIONAL_COMPONENT
        field_OPTIONAL_COMPONENT = clazz_DataWatcherRegistry!!.getDeclaredField("f")

        // net.minecraft.network.syncher.EntityDataSerializer BOOLEAN
        field_BOOLEAN = clazz_DataWatcherRegistry!!.getDeclaredField("i")

        // # {"fileName":"ServerPlayer.java","id":"sourceFile"}
        // net.minecraft.server.level.ServerPlayer ->
        //    net.minecraft.server.network.ServerGamePacketListenerImpl connection ->
        val fieldName = if (ver.minecraftVersion >= 1.20) "c" else "b"
        field_Connection = clazz_EntityPlayer!!.getDeclaredField(fieldName)

        // net.minecraft.network.syncher.SynchedEntityData ->
        //   it.unimi.dsi.fastutil.ints.Int2ObjectMap itemsById ->
        // (decompiled) private final Int2ObjectMap<DataWatcher.Item<?>> itemsById
        val methodName = if (this.isOneNinteenThreeOrNewer) "e" else "f"
        field_Int2ObjectMap = clazz_DataWatcher!!.getDeclaredField(methodName)
        field_Int2ObjectMap!!.trySetAccessible()
    }

    private fun buildConstructors(){
        this.ctor_EntityDataAccessor = clazz_DataWatcherObject!!.getConstructor(
            Int::class.javaPrimitiveType, clazz_DataWatcherSerializer
        )

        this.ctor_SynchedEntityData = clazz_DataWatcher!!.getConstructor(clazz_Entity)

        if (isOneNinteenThreeOrNewer) {
            // starting with 1.19.3 use this one:
            // public net.minecraft.network.protocol.game.PacketPlayOutEntityMetadata(int,java.util.List<DataWatcher.b<?>>)
            this.ctor_Packet = clazz_ClientboundSetEntityDataPacket!!.getConstructor(
                Int::class.javaPrimitiveType, MutableList::class.java
            )
        } else {
            // up to 1.19.2 use this one:
            this.ctor_Packet = clazz_ClientboundSetEntityDataPacket!!.getConstructor(
                Int::class.javaPrimitiveType, clazz_DataWatcher, Boolean::class.javaPrimitiveType
            )
        }
    }

    fun getTranslationKey(
        livingEntity: LivingEntity
    ): String{
        // only needed for spigot. paper has a built-in method

        // net.minecraft.world.entity.EntityType ->
        //   java.util.Optional byString(java.lang.String) -> a
        // public static Optional<EntityTypes<?>> byString(String s)
        val optionalResult: Optional<*>
        try {
            optionalResult = method_EntityTypeByString!!.invoke(
                null,
                livingEntity.type.name
            ) as Optional<*>
            return if (optionalResult.isEmpty) {
                ""
            } else method_GetDescriptionId!!.invoke(optionalResult.get()) as String
            // net.minecraft.world.entity.EntityTypes<T extends Entity>
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        } catch (e: InvocationTargetException) {
            e.printStackTrace()
        }

        return ""
    }
}