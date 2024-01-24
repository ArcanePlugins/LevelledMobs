package io.github.arcaneplugins.levelledmobs.nametag

import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.util.Optional
import java.util.UUID
import io.github.arcaneplugins.levelledmobs.LevelledMobs
import io.github.arcaneplugins.levelledmobs.nametag.ServerVersionInfo.MinecraftMajorVersion
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.bukkit.entity.LivingEntity

/**
 * Provides all NMS classes, fields and methods that are used for
 * nametags and some Mythic Mobs stuff
 *
 * @author stumper66
 * @since 3.9.2
 */
class Definitions {
    internal var hasKiori = false
    internal var isOneNinteenThreeOrNewer = false
    private var hasMiniMessage = false
    var useTranslationComponents = false
    private var useLegacySerializer = false
    var mm: MiniMessage? = null

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

    // mythic mobs:
    var clazz_MM_MobExecutor: Class<*>? = null
        private set
    var clazz_MM_ActiveMob: Class<*>? = null
        private set
    var clazz_MM_MobType: Class<*>? = null
        private set

    // methods:
    var method_ComponentAppend: Method? = null
        private set
    var method_EmptyComponent: Method? = null
        private set
    var method_TextComponent: Method? = null
        private set
    var method_Translatable: Method? = null
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

    // mythic mobs:
    var method_MM_getActiveMob: Method? = null
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

    // mythic mobs:
    var field_MM_mobManager: Field? = null
        private set
    var field_MM_type: Field? = null
        private set
    var field_MM_preventOtherDrops: Field? = null
        private set
    var field_MM_preventRandomEquipment: Field? = null
        private set
    var field_MM_internalName: Field? = null
        private set

    // Constructors
    var ctor_EntityDataAccessor: Constructor<*>? = null
        private set
    var ctor_SynchedEntityData: Constructor<*>? = null
        private set
    var ctor_Packet: Constructor<*>? = null
        private set

    fun load(){
        build()
        if (hasMiniMessage) mm = MiniMessage.miniMessage()
    }

    private fun build() {
        val ver = LevelledMobs.instance.ver
        this.isOneNinteenThreeOrNewer =
            ver.minecraftVersion == 1.19 && ver.revision >= 3.0 ||
                    ver.minecraftVersion >= 1.20

        // protocollib is used on 1.16
        if (ver.majorVersionEnum == MinecraftMajorVersion.V1_16) return

        try {
            buildClasses()

            // build methods
            getMethodComponentAppend()
            getMethodTextComponents()
            getMethodTranslatable()
            buildSimpleMethods()
            buildFields()
            buildConstructors()
            buildMythicMobs()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getClassName(classSuffix: String): String {
        // suffix ------------------------->
        // "org.bukkit.craftbukkit.v1_20_R3.entity.CraftEntity"
        return if (LevelledMobs.instance.ver.isRunningFabric) {
            "org.bukkit.craftbukkit.$classSuffix"
        } else {
            ("org.bukkit.craftbukkit." + LevelledMobs.instance.ver.nmsVersion) + "." + classSuffix
        }
    }

    private fun buildClasses() {
        this.clazz_IChatMutableComponent = Class.forName(
            "net.minecraft.network.chat.IChatMutableComponent"
        )

        this.clazz_IChatBaseComponent = Class.forName(
            "net.minecraft.network.chat.IChatBaseComponent"
        )

        this.clazz_CraftEntity = Class.forName(
            getClassName("entity.CraftEntity")
        )

        this.clazz_CraftLivingEntity = Class.forName(
            getClassName("entity.CraftLivingEntity")
        )

        // net.minecraft.network.syncher.SynchedEntityData
        this.clazz_DataWatcher = Class.forName(
            "net.minecraft.network.syncher.DataWatcher"
        )

        this.clazz_DataWatcher_Item = Class.forName(
            "net.minecraft.network.syncher.DataWatcher\$Item"
        )

        this.clazz_DataWatcherRegistry = Class.forName(
            "net.minecraft.network.syncher.DataWatcherRegistry"
        )

        this.clazz_Entity = Class.forName(
            "net.minecraft.world.entity.Entity"
        )

        this.clazz_DataWatcherObject = Class.forName(
            "net.minecraft.network.syncher.DataWatcherObject"
        )

        this.clazz_DataWatcherSerializer = Class.forName(
            "net.minecraft.network.syncher.DataWatcherSerializer"
        )

        this.clazz_ClientboundSetEntityDataPacket = Class.forName(
            "net.minecraft.network.protocol.game.PacketPlayOutEntityMetadata"
        )

        this.clazz_CraftPlayer = Class.forName(
            getClassName("entity.CraftPlayer")
        )

        this.clazz_Packet = Class.forName(
            "net.minecraft.network.protocol.Packet"
        )

        this.clazz_NetworkManager = Class.forName(
            "net.minecraft.network.NetworkManager"
        )

        //net.minecraft.server.network.ServerPlayerConnection ->
        // void send(net.minecraft.network.protocol.Packet) ->
        this.clazz_PlayerConnection = Class.forName(
            "net.minecraft.server.network.PlayerConnection"
        )

        this.clazz_ServerPlayerConnection = Class.forName(
            "net.minecraft.server.network.ServerPlayerConnection"
        )

        // net.minecraft.server.level.ServerPlayer ->
        this.clazz_EntityPlayer = Class.forName(
            "net.minecraft.server.level.EntityPlayer"
        )

        if (LevelledMobs.instance.ver.minecraftVersion < 1.19) {
            // this is basically TranslatableComponent
            this.clazz_TranslatableComponent = Class.forName(
                "net.minecraft.network.chat.ChatMessage"
            )
        }

        try {
            this.clazz_PaperAdventure = Class.forName(
                "io.papermc.paper.adventure.PaperAdventure"
            )
            this.hasKiori = true
        } catch (ignored: ClassNotFoundException) {
        }

        this.clazz_EntityTypes = Class.forName(
            "net.minecraft.world.entity.EntityTypes"
        )

        if (hasKiori && LevelledMobs.instance.ver.minecraftVersion >= 1.18) {
            try {
                Class.forName("net.kyori.adventure.text.minimessage.MiniMessage")
                this.hasMiniMessage = true
            } catch (ignored: ClassNotFoundException) {
            }
        }
    }

    @Throws(NoSuchMethodException::class)
    private fun getMethodComponentAppend() {
        // # {"fileName":"MutableComponent.java","id":"sourceFile"}
        // net.minecraft.network.chat.MutableComponent append(net.minecraft.network.chat.Component) ->
        // 1.19.0 = a, everything else  = b
        val ver = LevelledMobs.instance.ver
        var methodName = if (ver.minecraftVersion == 1.19 && ver.revision == 0 ||
            ver.minecraftVersion == 1.18
        ) "a" else "b"

        if (ver.minecraftVersion <= 1.17) {
            methodName = "addSibling"
        }

        this.method_ComponentAppend = clazz_IChatMutableComponent!!.getDeclaredMethod(
            methodName, this.clazz_IChatBaseComponent
        )
    }

    @Throws(NoSuchMethodException::class)
    private fun getMethodTextComponents() {
        // # {"fileName":"Component.java","id":"sourceFile"}
        // net.minecraft.network.chat.Component ->
        //     net.minecraft.network.chat.MutableComponent empty()

        val ver = LevelledMobs.instance.ver
        if (ver.minecraftVersion >= 1.19) {
            val methodName = if (ver.minecraftVersion >= 1.20) {
                if (ver.revision >= 3) // 1.20.3+ or 1.20.0 - 2
                    "i" else "h"
            } else {
                // 1.19.0 = g, 1.19.1+ = h
                if (ver.revision == 0) "g" else "h"
            }

            // net.minecraft.network.chat.Component ->
            //     net.minecraft.network.chat.MutableComponent empty()
            this.method_EmptyComponent = clazz_IChatBaseComponent!!.getDeclaredMethod(methodName)
        }

        // 1.18 doesn't have #empty(), instead use #nullToEmpty()
        // net.minecraft.network.chat.Component -> qk:
        //    net.minecraft.network.chat.Component nullToEmpty(java.lang.String) -> a
        this.method_TextComponent = clazz_IChatBaseComponent!!.getDeclaredMethod("a", String::class.java)
    }

    @Throws(NoSuchMethodException::class)
    private fun getMethodTranslatable() {
        if (LevelledMobs.instance.ver.minecraftVersion <= 1.18) {
            // 1.18 instantiates an object, so this method doesn't apply
            return
        }

        // # {"fileName":"Component.java","id":"sourceFile"}
        // net.minecraft.network.chat.Component ->
        // net.minecraft.network.chat.MutableComponent translatable(java.lang.String) ->
        this.method_Translatable = clazz_IChatBaseComponent!!.getDeclaredMethod(
            "c",
            String::class.java
        )

        // net.minecraft.network.chat.Component ->
        // net.minecraft.network.chat.MutableComponent translatable(java.lang.String,java.lang.Object[])
        this.method_TranslatableWithArgs = clazz_IChatBaseComponent!!.getDeclaredMethod(
            "a",
            String::class.java, Array<Any>::class.java
        )
    }

    fun getTranslationKey(livingEntity: LivingEntity): String {
        // only needed for spigot. paper has a built-in method

        // net.minecraft.world.entity.EntityType ->
        //   java.util.Optional byString(java.lang.String) -> a
        // public static Optional<EntityTypes<?>> byString(String s)

        val optionalResult: Optional<*>
        try {
            optionalResult = method_EntityTypeByString!!.invoke(
                null,
                livingEntity.type.getName()
            ) as Optional<*>

            if (optionalResult.isEmpty) {
                return ""
            }

            // net.minecraft.world.entity.EntityTypes<T extends Entity>
            return method_GetDescriptionId!!.invoke(optionalResult.get()) as String
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        } catch (e: InvocationTargetException) {
            e.printStackTrace()
        }

        return ""
    }

    @Throws(NoSuchMethodException::class)
    private fun buildSimpleMethods() {
        val ver = LevelledMobs.instance.ver
        this.method_getHandle = clazz_CraftLivingEntity!!.getDeclaredMethod("getHandle")

        // # {"fileName":"Entity.java","id":"sourceFile"}
        // net.minecraft.network.syncher.SynchedEntityData getEntityData() ->
        var methodName: String

        methodName = when (ver.majorVersionEnum) {
            MinecraftMajorVersion.V1_20 -> {
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

            MinecraftMajorVersion.V1_19 -> {
                if (ver.revision >= 4) {
                    "aj"
                } else if (ver.revision == 3) {
                    "al"
                } else {
                    "ai"
                }
            }

            MinecraftMajorVersion.V1_18 -> "ai"
            MinecraftMajorVersion.V1_17 -> "getDataWatcher"
            else -> throw RuntimeException("Unable to determine NMS method name for your Minecraft server version. Is your server version compatible?")
        }
        // net.minecraft.network.syncher.SynchedEntityData getEntityData() ->
        this.method_getEntityData = clazz_Entity!!.getMethod(methodName)

        methodName = if (ver.minecraftVersion >= 1.18) "b" else "set"

        // set(net.minecraft.network.syncher.EntityDataAccessor,java.lang.Object) ->
        this.method_set = clazz_DataWatcher!!.getMethod(
            methodName, clazz_DataWatcherObject,
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
            } else if (this.isOneNinteenThreeOrNewer) {
                // 1.19.3
                "ah"
            } else {
                // 1.18 - 1.19.2
                "ae"
            }
        } else if (ver.minecraftVersion <= 1.17) {
            methodName = "getId"
        }

        this.method_getId = clazz_Entity!!.getDeclaredMethod(methodName)

        this.method_PlayergetHandle = clazz_CraftPlayer!!.getDeclaredMethod("getHandle")

        // starting with 1.20.2 it is:
        // # {"fileName":"ServerCommonPacketListenerImpl.java","id":"sourceFile"}
        // net.minecraft.server.network.ServerCommonPacketListenerImpl ->

        // 1.20.1 and older:
        // net.minecraft.server.network.ServerGamePacketListenerImpl ->

        //    void send(net.minecraft.network.protocol.Packet) ->
        methodName = if (ver.minecraftVersion >= 1.18) {
            if (ver.majorVersionEnum == MinecraftMajorVersion.V1_20 && ver.revision >= 2) {
                "b"
            } else {
                "a"
            }
        } else {
            "sendPacket"
        }

        this.method_Send = clazz_ServerPlayerConnection!!.getDeclaredMethod(methodName, clazz_Packet)

        if (ver.minecraftVersion <= 1.18) {
            methodName = if (ver.majorVersionEnum == MinecraftMajorVersion.V1_18) "c" else "getAll"
            // java.util.List getAll() ->
            this.method_getAll = clazz_DataWatcher!!.getDeclaredMethod(methodName)
        }

        // # {"fileName":"SynchedEntityData.java","id":"sourceFile"}
        // net.minecraft.network.syncher.SynchedEntityData ->
        //    define(net.minecraft.network.syncher.EntityDataAccessor,java.lang.Object) ->
        methodName = if (ver.minecraftVersion >= 1.18) "a" else "register"

        this.method_define = clazz_DataWatcher!!.getDeclaredMethod(
            methodName,
            clazz_DataWatcherObject, Any::class.java
        )

        // net.minecraft.network.syncher.EntityDataAccessor getAccessor() ->
        this.method_getAccessor = clazz_DataWatcher_Item!!.getDeclaredMethod("a")
        // java.lang.Object getValue() ->
        this.method_getValue = clazz_DataWatcher_Item!!.getDeclaredMethod("b")

        // net.minecraft.network.Connection getConnection() ->
        //this.method_getConnection = clazz_CraftPlayer.getDeclaredMethod("networkManager");
        if (this.hasKiori) {
            this.method_AsVanilla = clazz_PaperAdventure!!.getDeclaredMethod(
                "asVanilla",
                Component::class.java
            )
        }

        // java.util.Optional byString(java.lang.String) ->
        this.method_EntityTypeByString = clazz_EntityTypes!!.getDeclaredMethod("a", String::class.java)

        // java.lang.String getDescriptionId() ->
        this.method_GetDescriptionId = clazz_EntityTypes!!.getDeclaredMethod("g")

        if (this.isOneNinteenThreeOrNewer) {
            // the methods here were added in 1.19.3

            // java.util.List getNonDefaultValues() ->

            this.method_getNonDefaultValues = clazz_DataWatcher!!.getDeclaredMethod("c")

            // define(net.minecraft.network.syncher.EntityDataAccessor,java.lang.Object) ->
            this.method_SynchedEntityData_Define = clazz_DataWatcher!!.getMethod(
                "a",
                clazz_DataWatcherObject, Any::class.java
            )

            // net.minecraft.network.syncher.SynchedEntityData$DataItem getItem(net.minecraft.network.syncher.EntityDataAccessor) ->
            // private <T> DataWatcher.Item<T> getItem(DataWatcherObject<T> datawatcherobject)
            methodName = if (ver.minecraftVersion >= 1.20) "c" else "b"
            this.method_DataWatcher_GetItem = clazz_DataWatcher!!.getDeclaredMethod(
                methodName,
                clazz_DataWatcherObject
            )
            method_DataWatcher_GetItem!!.setAccessible(true)

            // net.minecraft.network.syncher.SynchedEntityData$DataItem ->
            //       net.minecraft.network.syncher.SynchedEntityData$DataValue value() ->
            this.method_DataWatcherItem_Value = clazz_DataWatcher_Item!!.getDeclaredMethod("e")
        }
    }

    @Throws(NoSuchFieldException::class)
    private fun buildFields() {
        // net.minecraft.network.syncher.EntityDataSerializer OPTIONAL_COMPONENT
        this.field_OPTIONAL_COMPONENT = clazz_DataWatcherRegistry!!.getDeclaredField("f")

        // net.minecraft.network.syncher.EntityDataSerializer BOOLEAN
        this.field_BOOLEAN = clazz_DataWatcherRegistry!!.getDeclaredField("i")

        // # {"fileName":"ServerPlayer.java","id":"sourceFile"}
        // net.minecraft.server.level.ServerPlayer ->
        //    net.minecraft.server.network.ServerGamePacketListenerImpl connection ->
        val fieldName = if (LevelledMobs.instance.ver.minecraftVersion >= 1.20) "c" else "b"
        this.field_Connection = clazz_EntityPlayer!!.getDeclaredField(fieldName)

        if (LevelledMobs.instance.ver.minorVersion >= 19) {
            // net.minecraft.network.syncher.SynchedEntityData ->
            //   it.unimi.dsi.fastutil.ints.Int2ObjectMap itemsById ->
            // (decompiled) private final Int2ObjectMap<DataWatcher.Item<?>> itemsById

            val methodName = if (this.isOneNinteenThreeOrNewer) "e" else "f"

            this.field_Int2ObjectMap = clazz_DataWatcher!!.getDeclaredField(methodName)
            field_Int2ObjectMap!!.setAccessible(true)
        }
    }

    @Throws(NoSuchMethodException::class)
    private fun buildConstructors() {
        this.ctor_EntityDataAccessor = clazz_DataWatcherObject!!.getConstructor(
            Int::class.javaPrimitiveType, clazz_DataWatcherSerializer
        )

        this.ctor_SynchedEntityData = clazz_DataWatcher!!.getConstructor(clazz_Entity)

        if (this.isOneNinteenThreeOrNewer) {
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

    @Throws(NoSuchMethodException::class, NoSuchFieldException::class, ClassNotFoundException::class)
    private fun buildMythicMobs() {
        // io.lumine.mythic.bukkit.MythicBukkit
        val mmMain = Bukkit.getPluginManager().getPlugin("MythicMobs")
        if (mmMain == null || !mmMain.isEnabled) {
            return
        }

        this.clazz_MM_ActiveMob = Class.forName("io.lumine.mythic.core.mobs.ActiveMob")
        this.clazz_MM_MobExecutor = Class.forName("io.lumine.mythic.core.mobs.MobExecutor")
        this.clazz_MM_MobType = Class.forName("io.lumine.mythic.core.mobs.MobType")

        this.method_MM_getActiveMob = clazz_MM_MobExecutor!!.getMethod(
            "getActiveMob",
            UUID::class.java
        )

        this.field_MM_mobManager = mmMain.javaClass.getDeclaredField("mobManager")
        field_MM_mobManager!!.setAccessible(true)
        this.field_MM_type = clazz_MM_ActiveMob!!.getDeclaredField("type")
        field_MM_type!!.setAccessible(true)
        this.field_MM_preventOtherDrops = clazz_MM_MobType!!.getDeclaredField(
            "preventOtherDrops"
        ) // boolean
        field_MM_preventOtherDrops!!.setAccessible(true)
        this.field_MM_preventRandomEquipment = clazz_MM_MobType!!.getDeclaredField(
            "preventRandomEquipment"
        ) // boolean
        field_MM_preventRandomEquipment!!.setAccessible(true)
        this.field_MM_internalName = clazz_MM_MobType!!.getDeclaredField(
            "internalName"
        ) // string
        field_MM_internalName!!.setAccessible(true)
    }

    fun setUseLegacySerializer(useLegacySerializer: Boolean) {
        this.useLegacySerializer = useLegacySerializer
    }

    fun getUseLegacySerializer(): Boolean {
        return !this.hasMiniMessage || this.useLegacySerializer
    }
}