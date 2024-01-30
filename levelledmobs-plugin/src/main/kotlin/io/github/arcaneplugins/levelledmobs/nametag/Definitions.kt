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
    var clazzIChatMutableComponent: Class<*>? = null
    private set
    var clazzIChatBaseComponent: Class<*>? = null
        private set
    var clazzTranslatableComponent: Class<*>? = null
        private set
    var clazzCraftLivingEntity: Class<*>? = null
        private set
    var clazzCraftEntity: Class<*>? = null
        private set
    var clazzEntity: Class<*>? = null
        private set
    var clazzDataWatcher: Class<*>? = null
        private set
    var clazzDataWatcher_Item: Class<*>? = null
        private set
    var clazzDataWatcherRegistry: Class<*>? = null
        private set
    var clazzDataWatcherObject: Class<*>? = null
        private set
    var clazzDataWatcherSerializer: Class<*>? = null
        private set
    var clazzClientboundSetEntityDataPacket: Class<*>? = null
        private set
    var clazzCraftPlayer: Class<*>? = null
        private set
    var clazzPacket: Class<*>? = null
        private set
    var clazzPlayerConnection: Class<*>? = null
        private set
    var clazzServerPlayerConnection: Class<*>? = null
        private set
    var clazzNetworkManager: Class<*>? = null
        private set
    var clazzEntityPlayer: Class<*>? = null
        private set
    var clazzPaperAdventure: Class<*>? = null
        private set
    var clazzEntityTypes: Class<*>? = null
        private set

    // mythic mobs:
    var clazzMMmobExecutor: Class<*>? = null
        private set
    var clazzMMactiveMob: Class<*>? = null
        private set
    var clazzMMmobType: Class<*>? = null
        private set

    // methods:
    var methodComponentAppend: Method? = null
        private set
    var methodEmptyComponent: Method? = null
        private set
    var methodTextComponent: Method? = null
        private set
    var methodTranslatable: Method? = null
        private set
    var methodTranslatableWithArgs: Method? = null
        private set
    var methodGetHandle: Method? = null
        private set
    var methodGetEntityData: Method? = null
        private set
    var methodSet: Method? = null
        private set
    var methodGetId: Method? = null
        private set
    var methodPlayergetHandle: Method? = null
        private set
    var methodSend: Method? = null
        private set
    var methodGetAll: Method? = null
        private set
    var methodDefine: Method? = null
        private set
    var methodGetAccessor: Method? = null
        private set
    var methodGetValue: Method? = null
        private set
    var methodAsVanilla: Method? = null
        private set
    var methodEntityTypeByString: Method? = null
        private set
    var methodGetDescriptionId: Method? = null
        private set
    var methodGetNonDefaultValues: Method? = null
        private set
    var methodSynchedEntityData_Define: Method? = null
        private set
    var methodDataWatcher_GetItem: Method? = null
        private set
    var methodDataWatcherItem_Value: Method? = null
        private set

    // mythic mobs:
    var methodMMgetActiveMob: Method? = null
        private set

    // fields
    var fieldOPTIONALCOMPONENT: Field? = null
        private set
    var fieldBOOLEAN: Field? = null
        private set
    var fieldConnection: Field? = null
        private set
    var field_Int2ObjectMap: Field? = null
        private set

    // mythic mobs:
    var fieldMMmobManager: Field? = null
        private set
    var fieldMMtype: Field? = null
        private set
    var fieldMMpreventOtherDrops: Field? = null
        private set
    var fieldMMpreventRandomEquipment: Field? = null
        private set
    var fieldMMinternalName: Field? = null
        private set

    // Constructors
    var ctorEntityDataAccessor: Constructor<*>? = null
        private set
    var ctorSynchedEntityData: Constructor<*>? = null
        private set
    var ctorPacket: Constructor<*>? = null
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
        this.clazzIChatMutableComponent = Class.forName(
            "net.minecraft.network.chat.IChatMutableComponent"
        )

        this.clazzIChatBaseComponent = Class.forName(
            "net.minecraft.network.chat.IChatBaseComponent"
        )

        this.clazzCraftEntity = Class.forName(
            getClassName("entity.CraftEntity")
        )

        this.clazzCraftLivingEntity = Class.forName(
            getClassName("entity.CraftLivingEntity")
        )

        // net.minecraft.network.syncher.SynchedEntityData
        this.clazzDataWatcher = Class.forName(
            "net.minecraft.network.syncher.DataWatcher"
        )

        this.clazzDataWatcher_Item = Class.forName(
            "net.minecraft.network.syncher.DataWatcher\$Item"
        )

        this.clazzDataWatcherRegistry = Class.forName(
            "net.minecraft.network.syncher.DataWatcherRegistry"
        )

        this.clazzEntity = Class.forName(
            "net.minecraft.world.entity.Entity"
        )

        this.clazzDataWatcherObject = Class.forName(
            "net.minecraft.network.syncher.DataWatcherObject"
        )

        this.clazzDataWatcherSerializer = Class.forName(
            "net.minecraft.network.syncher.DataWatcherSerializer"
        )

        this.clazzClientboundSetEntityDataPacket = Class.forName(
            "net.minecraft.network.protocol.game.PacketPlayOutEntityMetadata"
        )

        this.clazzCraftPlayer = Class.forName(
            getClassName("entity.CraftPlayer")
        )

        this.clazzPacket = Class.forName(
            "net.minecraft.network.protocol.Packet"
        )

        this.clazzNetworkManager = Class.forName(
            "net.minecraft.network.NetworkManager"
        )

        //net.minecraft.server.network.ServerPlayerConnection ->
        // void send(net.minecraft.network.protocol.Packet) ->
        this.clazzPlayerConnection = Class.forName(
            "net.minecraft.server.network.PlayerConnection"
        )

        this.clazzServerPlayerConnection = Class.forName(
            "net.minecraft.server.network.ServerPlayerConnection"
        )

        // net.minecraft.server.level.ServerPlayer ->
        this.clazzEntityPlayer = Class.forName(
            "net.minecraft.server.level.EntityPlayer"
        )

        if (LevelledMobs.instance.ver.minecraftVersion < 1.19) {
            // this is basically TranslatableComponent
            this.clazzTranslatableComponent = Class.forName(
                "net.minecraft.network.chat.ChatMessage"
            )
        }

        try {
            this.clazzPaperAdventure = Class.forName(
                "io.papermc.paper.adventure.PaperAdventure"
            )
            this.hasKiori = true
        } catch (ignored: ClassNotFoundException) {
        }

        this.clazzEntityTypes = Class.forName(
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

        this.methodComponentAppend = clazzIChatMutableComponent!!.getDeclaredMethod(
            methodName, this.clazzIChatBaseComponent
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
            this.methodEmptyComponent = clazzIChatBaseComponent!!.getDeclaredMethod(methodName)
        }

        // 1.18 doesn't have #empty(), instead use #nullToEmpty()
        // net.minecraft.network.chat.Component -> qk:
        //    net.minecraft.network.chat.Component nullToEmpty(java.lang.String) -> a
        this.methodTextComponent = clazzIChatBaseComponent!!.getDeclaredMethod("a", String::class.java)
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
        this.methodTranslatable = clazzIChatBaseComponent!!.getDeclaredMethod(
            "c",
            String::class.java
        )

        // net.minecraft.network.chat.Component ->
        // net.minecraft.network.chat.MutableComponent translatable(java.lang.String,java.lang.Object[])
        this.methodTranslatableWithArgs = clazzIChatBaseComponent!!.getDeclaredMethod(
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
            optionalResult = methodEntityTypeByString!!.invoke(
                null,
                livingEntity.type.getName()
            ) as Optional<*>

            if (optionalResult.isEmpty) {
                return ""
            }

            // net.minecraft.world.entity.EntityTypes<T extends Entity>
            return methodGetDescriptionId!!.invoke(optionalResult.get()) as String
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
        this.methodGetHandle = clazzCraftLivingEntity!!.getDeclaredMethod("getHandle")

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
        this.methodGetEntityData = clazzEntity!!.getMethod(methodName)

        methodName = if (ver.minecraftVersion >= 1.18) "b" else "set"

        // set(net.minecraft.network.syncher.EntityDataAccessor,java.lang.Object) ->
        this.methodSet = clazzDataWatcher!!.getMethod(
            methodName, clazzDataWatcherObject,
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

        this.methodGetId = clazzEntity!!.getDeclaredMethod(methodName)

        this.methodPlayergetHandle = clazzCraftPlayer!!.getDeclaredMethod("getHandle")

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

        this.methodSend = clazzServerPlayerConnection!!.getDeclaredMethod(methodName, clazzPacket)

        if (ver.minecraftVersion <= 1.18) {
            methodName = if (ver.majorVersionEnum == MinecraftMajorVersion.V1_18) "c" else "getAll"
            // java.util.List getAll() ->
            this.methodGetAll = clazzDataWatcher!!.getDeclaredMethod(methodName)
        }

        // # {"fileName":"SynchedEntityData.java","id":"sourceFile"}
        // net.minecraft.network.syncher.SynchedEntityData ->
        //    define(net.minecraft.network.syncher.EntityDataAccessor,java.lang.Object) ->
        methodName = if (ver.minecraftVersion >= 1.18) "a" else "register"

        this.methodDefine = clazzDataWatcher!!.getDeclaredMethod(
            methodName,
            clazzDataWatcherObject, Any::class.java
        )

        // net.minecraft.network.syncher.EntityDataAccessor getAccessor() ->
        this.methodGetAccessor = clazzDataWatcher_Item!!.getDeclaredMethod("a")
        // java.lang.Object getValue() ->
        this.methodGetValue = clazzDataWatcher_Item!!.getDeclaredMethod("b")

        // net.minecraft.network.Connection getConnection() ->
        //this.method_getConnection = clazz_CraftPlayer.getDeclaredMethod("networkManager");
        if (this.hasKiori) {
            this.methodAsVanilla = clazzPaperAdventure!!.getDeclaredMethod(
                "asVanilla",
                Component::class.java
            )
        }

        // java.util.Optional byString(java.lang.String) ->
        this.methodEntityTypeByString = clazzEntityTypes!!.getDeclaredMethod("a", String::class.java)

        // java.lang.String getDescriptionId() ->
        this.methodGetDescriptionId = clazzEntityTypes!!.getDeclaredMethod("g")

        if (this.isOneNinteenThreeOrNewer) {
            // the methods here were added in 1.19.3

            // java.util.List getNonDefaultValues() ->

            this.methodGetNonDefaultValues = clazzDataWatcher!!.getDeclaredMethod("c")

            // define(net.minecraft.network.syncher.EntityDataAccessor,java.lang.Object) ->
            this.methodSynchedEntityData_Define = clazzDataWatcher!!.getMethod(
                "a",
                clazzDataWatcherObject, Any::class.java
            )

            // net.minecraft.network.syncher.SynchedEntityData$DataItem getItem(net.minecraft.network.syncher.EntityDataAccessor) ->
            // private <T> DataWatcher.Item<T> getItem(DataWatcherObject<T> datawatcherobject)
            methodName = if (ver.minecraftVersion >= 1.20) "c" else "b"
            this.methodDataWatcher_GetItem = clazzDataWatcher!!.getDeclaredMethod(
                methodName,
                clazzDataWatcherObject
            )
            methodDataWatcher_GetItem!!.setAccessible(true)

            // net.minecraft.network.syncher.SynchedEntityData$DataItem ->
            //       net.minecraft.network.syncher.SynchedEntityData$DataValue value() ->
            this.methodDataWatcherItem_Value = clazzDataWatcher_Item!!.getDeclaredMethod("e")
        }
    }

    @Throws(NoSuchFieldException::class)
    private fun buildFields() {
        // net.minecraft.network.syncher.EntityDataSerializer OPTIONAL_COMPONENT
        this.fieldOPTIONALCOMPONENT = clazzDataWatcherRegistry!!.getDeclaredField("f")

        // net.minecraft.network.syncher.EntityDataSerializer BOOLEAN
        this.fieldBOOLEAN = clazzDataWatcherRegistry!!.getDeclaredField("i")

        // # {"fileName":"ServerPlayer.java","id":"sourceFile"}
        // net.minecraft.server.level.ServerPlayer ->
        //    net.minecraft.server.network.ServerGamePacketListenerImpl connection ->
        val fieldName = if (LevelledMobs.instance.ver.minecraftVersion >= 1.20) "c" else "b"
        this.fieldConnection = clazzEntityPlayer!!.getDeclaredField(fieldName)

        if (LevelledMobs.instance.ver.minorVersion >= 19) {
            // net.minecraft.network.syncher.SynchedEntityData ->
            //   it.unimi.dsi.fastutil.ints.Int2ObjectMap itemsById ->
            // (decompiled) private final Int2ObjectMap<DataWatcher.Item<?>> itemsById

            val methodName = if (this.isOneNinteenThreeOrNewer) "e" else "f"

            this.field_Int2ObjectMap = clazzDataWatcher!!.getDeclaredField(methodName)
            field_Int2ObjectMap!!.setAccessible(true)
        }
    }

    @Throws(NoSuchMethodException::class)
    private fun buildConstructors() {
        this.ctorEntityDataAccessor = clazzDataWatcherObject!!.getConstructor(
            Int::class.javaPrimitiveType, clazzDataWatcherSerializer
        )

        this.ctorSynchedEntityData = clazzDataWatcher!!.getConstructor(clazzEntity)

        if (this.isOneNinteenThreeOrNewer) {
            // starting with 1.19.3 use this one:
            // public net.minecraft.network.protocol.game.PacketPlayOutEntityMetadata(int,java.util.List<DataWatcher.b<?>>)

            this.ctorPacket = clazzClientboundSetEntityDataPacket!!.getConstructor(
                Int::class.javaPrimitiveType, MutableList::class.java
            )
        } else {
            // up to 1.19.2 use this one:
            this.ctorPacket = clazzClientboundSetEntityDataPacket!!.getConstructor(
                Int::class.javaPrimitiveType, clazzDataWatcher, Boolean::class.javaPrimitiveType
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

        this.clazzMMactiveMob = Class.forName("io.lumine.mythic.core.mobs.ActiveMob")
        this.clazzMMmobExecutor = Class.forName("io.lumine.mythic.core.mobs.MobExecutor")
        this.clazzMMmobType = Class.forName("io.lumine.mythic.core.mobs.MobType")

        this.methodMMgetActiveMob = clazzMMmobExecutor!!.getMethod(
            "getActiveMob",
            UUID::class.java
        )

        this.fieldMMmobManager = mmMain.javaClass.getDeclaredField("mobManager")
        fieldMMmobManager!!.setAccessible(true)
        this.fieldMMtype = clazzMMactiveMob!!.getDeclaredField("type")
        fieldMMtype!!.setAccessible(true)
        this.fieldMMpreventOtherDrops = clazzMMmobType!!.getDeclaredField(
            "preventOtherDrops"
        ) // boolean
        fieldMMpreventOtherDrops!!.setAccessible(true)
        this.fieldMMpreventRandomEquipment = clazzMMmobType!!.getDeclaredField(
            "preventRandomEquipment"
        ) // boolean
        fieldMMpreventRandomEquipment!!.setAccessible(true)
        this.fieldMMinternalName = clazzMMmobType!!.getDeclaredField(
            "internalName"
        ) // string
        fieldMMinternalName!!.setAccessible(true)
    }

    fun setUseLegacySerializer(useLegacySerializer: Boolean) {
        this.useLegacySerializer = useLegacySerializer
    }

    fun getUseLegacySerializer(): Boolean {
        return !this.hasMiniMessage || this.useLegacySerializer
    }
}