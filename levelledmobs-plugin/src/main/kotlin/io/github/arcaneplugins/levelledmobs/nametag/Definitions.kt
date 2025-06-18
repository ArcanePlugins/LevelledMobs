package io.github.arcaneplugins.levelledmobs.nametag

import io.github.arcaneplugins.levelledmobs.LevelledMobs
import io.github.arcaneplugins.levelledmobs.nametag.ServerVersionInfo.MinecraftMajorVersion
import io.github.arcaneplugins.levelledmobs.util.Log
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.util.UUID
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.LivingEntity

/**
 * Provides all NMS classes, fields and methods that are used for
 * nametags and some Mythic Mobs stuff
 *
 * @author stumper66
 * @since 3.9.2
 */
class Definitions{
    internal var hasKiori = false
    internal var isOneNinteenThreeOrNewer = false
    internal var isOneTwentyFiveOrNewer = false
    private var hasMiniMessage = false
    var useTranslationComponents = false
    private var useLegacySerializer = false
    private var ver = ServerVersionInfo()
    var mm: MiniMessage? = null

    // classes:
    private var clazzIChatMutableComponent: Class<*>? = null
    private var clazzIChatBaseComponent: Class<*>? = null
    var clazzTranslatableComponent: Class<*>? = null
        private set
    private var clazzCraftLivingEntity: Class<*>? = null
    private var clazzCraftEntity: Class<*>? = null
    var clazzEntity: Class<*>? = null
        private set
    private var clazzDataWatcher: Class<*>? = null
    private var clazzDataWatcherBuilder: Class<*>? = null
    private var clazzDataWatcherItem: Class<*>? = null
    private var clazzDataWatcherValue: Class<*>? = null
    var clazzDataWatcherRegistry: Class<*>? = null
        private set
    private var clazzDataWatcherObject: Class<*>? = null
    private var clazzSyncedDataHolder: Class<*>? = null
    private var clazzDataWatcherSerializer: Class<*>? = null
    private var clazzClientboundSetEntityDataPacket: Class<*>? = null
    private var clazzCraftPlayer: Class<*>? = null
    private var clazzPacket: Class<*>? = null
    private var clazzServerPlayerConnection: Class<*>? = null
    private var clazzEntityPlayer: Class<*>? = null
    var clazzPaperAdventure: Class<*>? = null
        private set
    private var clazzEntityTypes: Class<*>? = null
    var clazzEquipmentSlotGroup: Class<*>? = null

    // mythic mobs:
    private var clazzMMmobExecutor: Class<*>? = null
    private var clazzMMactiveMob: Class<*>? = null
    private var clazzMMmobType: Class<*>? = null

    // methods:
    var methodDataWatcherBuilderBuild: Method? = null
        private set
    var methodDataWatcherBuilderDefine: Method? = null
        private set
    var methodDataWatcherGetId: Method? = null
        private set
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
    var methodDefine: Method? = null
        private set
    var methodGetAccessor: Method? = null
        private set
    var methodGetValue: Method? = null
        private set
    var methodAsVanilla: Method? = null
        private set
    private var methodEntityTypeByString: Method? = null
    private var methodGetDescriptionId: Method? = null
    private var methodGetNonDefaultValues: Method? = null
    private var methodSynchedEntityDataDefine: Method? = null
    var methodDataWatcherGetItem: Method? = null
        private set
    var methodDataWatcherItemValue: Method? = null
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
    var fieldInt2ObjectMap: Field? = null
        private set
    var fieldEquipmentSlotAny: Field? = null
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
    var ctorSynchedEntityDataBuilder: Constructor<*>? = null
        private set
    var ctorPacket: Constructor<*>? = null
        private set
    var ctorAttributeModifier: Constructor<*>? = null

    fun load(){
        ver = LevelledMobs.instance.ver

        Log.inf("Building reflection cache, use simple names: ${ver.useSimpleName}")
        build()
        if (hasMiniMessage) mm = MiniMessage.miniMessage()
    }

    private fun build() {
        this.isOneNinteenThreeOrNewer =
            ver.minorVersion == 19 && ver.revision >= 3 ||
                    ver.minorVersion >= 20

        this.isOneTwentyFiveOrNewer =
            ver.minorVersion == 20 && ver.revision >= 5 ||
                    ver.minorVersion >= 21

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

        // if running folia only use simple name if the version is 1.21+

        return if (ver.useSimpleName)
            "org.bukkit.craftbukkit.$classSuffix"
        else
            "org.bukkit.craftbukkit.${ver.nmsVersion}.$classSuffix"
    }

    private fun buildClasses() {
        if (!ver.isRunningPaper) {
            this.clazzIChatMutableComponent = Class.forName(
                "net.minecraft.network.chat.IChatMutableComponent"
            )

            this.clazzIChatBaseComponent = Class.forName(
                "net.minecraft.network.chat.IChatBaseComponent"
            )
        }

        this.clazzCraftEntity = Class.forName(
            getClassName("entity.CraftEntity")
        )

        this.clazzCraftLivingEntity = Class.forName(
            getClassName("entity.CraftLivingEntity")
        )

        // net.minecraft.network.syncher.SynchedEntityData
        this.clazzDataWatcher = Class.forName(
            NmsMappings.getMapping("clazzDataWatcher")
        )

        if (isOneTwentyFiveOrNewer){
            // net.minecraft.network.syncher.SynchedEntityData$Builder ->
            this.clazzDataWatcherBuilder = Class.forName(
                NmsMappings.getMapping("clazzDataWatcherBuilder")
            )

            this.clazzSyncedDataHolder = Class.forName(
                "net.minecraft.network.syncher.SyncedDataHolder"
            )

            // net.minecraft.network.syncher.SynchedEntityData$DataValue ->
            this.clazzDataWatcherValue = Class.forName(
                NmsMappings.getMapping("clazzDataWatcherValue")
            )
        }

        // net.minecraft.network.syncher.SynchedEntityData$DataItem ->
        this.clazzDataWatcherItem = Class.forName(
            NmsMappings.getMapping("clazzDataWatcherItem")
        )

        this.clazzDataWatcherRegistry = Class.forName(
            NmsMappings.getMapping("clazzDataWatcherRegistry")
        )

        this.clazzEntity = Class.forName(
            "net.minecraft.world.entity.Entity"
        )

        this.clazzDataWatcherObject = Class.forName(
            NmsMappings.getMapping("clazzDataWatcherObject")
        )

        this.clazzDataWatcherSerializer = Class.forName(
            NmsMappings.getMapping("clazzDataWatcherSerializer")
        )

        this.clazzClientboundSetEntityDataPacket = Class.forName(
            NmsMappings.getMapping("clazzClientboundSetEntityDataPacket")
        )

        this.clazzCraftPlayer = Class.forName(
            getClassName("entity.CraftPlayer")
        )

        this.clazzPacket = Class.forName(
            "net.minecraft.network.protocol.Packet"
        )

        //net.minecraft.server.network.ServerPlayerConnection ->
        // void send(net.minecraft.network.protocol.Packet) ->


        this.clazzServerPlayerConnection = Class.forName(
            "net.minecraft.server.network.ServerPlayerConnection"
        )

        // net.minecraft.server.level.ServerPlayer ->
        this.clazzEntityPlayer = Class.forName(
            NmsMappings.getMapping("clazzEntityPlayer")
        )

        try {
            this.clazzPaperAdventure = Class.forName(
                "io.papermc.paper.adventure.PaperAdventure"
            )
            this.hasKiori = true
        } catch (_: ClassNotFoundException) {}

        this.clazzEntityTypes = Class.forName(
            NmsMappings.getMapping("clazzEntityTypes")
        )

        if (!ver.useOldEnums){
            this.clazzEquipmentSlotGroup = Class.forName(
                "org.bukkit.inventory.EquipmentSlotGroup"
            )
        }

        if (hasKiori) {
            try {
                Class.forName("net.kyori.adventure.text.minimessage.MiniMessage")
                this.hasMiniMessage = true
            } catch (_: ClassNotFoundException) {}
        }
    }

    private fun getMethodComponentAppend() {
        if (ver.isRunningPaper) return // spigot only stuff here

        // # {"fileName":"MutableComponent.java","id":"sourceFile"}
        // net.minecraft.network.chat.MutableComponent append(net.minecraft.network.chat.Component) ->
        // 1.19.0 = a, everything else  = b

        val methodName = if (ver.minecraftVersion == 1.19 && ver.revision == 0
        ) "a" else "b"

        this.methodComponentAppend = clazzIChatMutableComponent!!.getDeclaredMethod(
            methodName, this.clazzIChatBaseComponent
        )
    }

    private fun getMethodTextComponents() {
        if (ver.isRunningPaper) return // spigot only stuff here

        // # {"fileName":"Component.java","id":"sourceFile"}
        // net.minecraft.network.chat.Component ->
        //     net.minecraft.network.chat.MutableComponent empty()

        val methodName = if (ver.minecraftVersion >= 1.20) {
            if (ver.revision >= 3 || ver.minorVersion >= 21 && ver.revision >= 2) // 1.20.3+, 1.20.0 - 2 or 1.21.2+
                "i" else "h"
        } else {
            // 1.19.0 = g, 1.19.1+ = h
            if (ver.revision == 0) "g" else "h"
        }

        // net.minecraft.network.chat.Component ->
        //     net.minecraft.network.chat.MutableComponent empty()
        this.methodEmptyComponent = clazzIChatBaseComponent!!.getDeclaredMethod(methodName)

        // net.minecraft.network.chat.Component ->
        //    net.minecraft.network.chat.Component nullToEmpty(java.lang.String) -> a
        this.methodTextComponent = clazzIChatBaseComponent!!.getDeclaredMethod("a", String::class.java)
    }

    private fun getMethodTranslatable() {
        if (ver.isRunningPaper) return // spigot only stuff here

        // # {"fileName":"Component.java","id":"sourceFile"}
        // net.minecraft.network.chat.Component ->
        // net.minecraft.network.chat.MutableComponent translatable(java.lang.String) ->
        this.methodTranslatable = clazzIChatBaseComponent!!.getDeclaredMethod(
            "c",
            String::class.java
        )

        // net.minecraft.network.chat.Component ->
        // net.minecraft.network.chat.MutableComponent translatable(java.lang.String,java.lang.Object[]) ->
        this.methodTranslatableWithArgs = clazzIChatBaseComponent!!.getDeclaredMethod(
            "a",
            String::class.java, Array<Any>::class.java
        )
    }

    @Suppress("DEPRECATION")
    fun getTranslationKey(livingEntity: LivingEntity): String {
        // only needed for spigot. paper has a built-in method

        return Bukkit.getUnsafe().getTranslationKey(livingEntity.type)
    }

    private fun buildSimpleMethods() {
        this.methodGetHandle = clazzCraftLivingEntity!!.getDeclaredMethod("getHandle")

        // # {"fileName":"Entity.java","id":"sourceFile"}
        // net.minecraft.network.syncher.SynchedEntityData getEntityData() ->
        var methodName: String

        if (ver.useMojangMappings)
            methodName = "getEntityData"
        else {
            methodName = when (ver.majorVersionEnum) {
                MinecraftMajorVersion.V1_21 -> {
                    if (ver.revision >= 6)
                        "au"
                    else
                        "ar"
                }

                MinecraftMajorVersion.V1_20 -> {
                    if (ver.revision >= 5) {
                        // 1.20.5+
                        "ap"
                    } else if (ver.revision >= 3) {
                        // 1.20.3 - .4
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
                    if (ver.revision >= 4)
                        "aj"
                    else if (ver.revision == 3)
                        "al"
                    else
                        "ai"
                }

                else -> throw RuntimeException("Unable to determine NMS method name for your Minecraft server version. Is your server version compatible?")
            }
        }

        // net.minecraft.network.syncher.SynchedEntityData getEntityData() ->
        this.methodGetEntityData = clazzEntity!!.getMethod(methodName)

        // set(net.minecraft.network.syncher.EntityDataAccessor,java.lang.Object) ->
        methodName =
            if (ver.useMojangMappings) "set"
            else if (isOneTwentyFiveOrNewer) "a"
            else "b"

        this.methodSet = clazzDataWatcher!!.getMethod(
            methodName, clazzDataWatcherObject,
            Any::class.java
        )

        // # {"fileName":"ChunkStatusUpdateListener.java","id":"sourceFile"}
        // net.minecraft.world.level.entity.EntityAccess ->
        //   int getId() ->
        when (ver.majorVersionEnum) {
            MinecraftMajorVersion.V1_21 -> {
                methodName =
                    if (ver.useMojangMappings)
                        "getId"
                    else if (ver.revision == 5)
                        "ao" // 1.21.5 only
                    else if (ver.revision >= 2)
                        "ar" // 1.21.2 - 1.21.4, 1.21.6+
                    else
                        "an"
            }
            MinecraftMajorVersion.V1_20 -> {
                methodName =
                    if (ver.revision >= 5) {
                        // 1.20.5+
                        "al"
                    }
                    else if (ver.revision >= 3) {
                        // 1.20.3 - .4
                        "aj"
                    } else {
                        if (ver.revision >= 2) "ah" else "af"
                    }
            }
            else -> {
                methodName = if (ver.revision >= 4) {
                    "af"
                } else if (this.isOneNinteenThreeOrNewer) {
                    // 1.19.3
                    "ah"
                } else {
                    // 1.18 - 1.19.2
                    "ae"
                }
            }
        }

        this.methodGetId = clazzEntity!!.getDeclaredMethod(methodName)

        if (isOneTwentyFiveOrNewer){
            // net.minecraft.network.syncher.SynchedEntityData$Builder ->
            //     net.minecraft.network.syncher.SynchedEntityData$Builder define(net.minecraft.network.syncher.EntityDataAccessor,java.lang.Object) ->
            methodDataWatcherBuilderDefine =
                clazzDataWatcherBuilder!!.getDeclaredMethod(
                    NmsMappings.getMapping("methodDataWatcherBuilderDefine"),
                    clazzDataWatcherObject, Any::class.java)

            // net.minecraft.network.syncher.SynchedEntityData build() ->
            methodDataWatcherBuilderBuild = clazzDataWatcherBuilder!!.getDeclaredMethod(
                NmsMappings.getMapping("methodDataWatcherBuilderBuild")
            )

            // int id() ->
            methodDataWatcherGetId = clazzDataWatcherValue!!.getDeclaredMethod(
                NmsMappings.getMapping("methodDataWatcherGetId")
            )
        }

        this.methodPlayergetHandle = clazzCraftPlayer!!.getDeclaredMethod("getHandle")

        // starting with 1.20.2 it is:
        // # {"fileName":"ServerCommonPacketListenerImpl.java","id":"sourceFile"}
        // net.minecraft.server.network.ServerCommonPacketListenerImpl ->

        // 1.20.1 and older:
        // net.minecraft.server.network.ServerGamePacketListenerImpl ->

        //    void send(net.minecraft.network.protocol.Packet) ->
        methodName =
            if (ver.useMojangMappings)
                "send"
            else if (ver.majorVersionEnum == MinecraftMajorVersion.V1_20 && ver.revision >= 2 ||
                ver.minorVersion >= 21)
                "b"
            else
                "a"

        this.methodSend = clazzServerPlayerConnection!!.getDeclaredMethod(methodName, clazzPacket)

        // # {"fileName":"SynchedEntityData.java","id":"sourceFile"}
        // net.minecraft.network.syncher.SynchedEntityData ->
        //    set(net.minecraft.network.syncher.EntityDataAccessor,java.lang.Object) ->
        methodName = if (ver.useMojangMappings) "set" else "a"

        this.methodDefine = clazzDataWatcher!!.getDeclaredMethod(
            methodName,
            clazzDataWatcherObject, Any::class.java
        )

        // net.minecraft.network.syncher.EntityDataAccessor getAccessor() ->
        methodName = if (ver.useMojangMappings) "getAccessor" else "a"
        this.methodGetAccessor = clazzDataWatcherItem!!.getDeclaredMethod(methodName)
        // java.lang.Object getValue() ->
        methodName = if (ver.useMojangMappings) "getValue" else "b"
        this.methodGetValue = clazzDataWatcherItem!!.getDeclaredMethod(methodName)

        // net.minecraft.network.Connection getConnection() ->
        //this.method_getConnection = clazz_CraftPlayer.getDeclaredMethod("networkManager");
        if (this.hasKiori) {
            this.methodAsVanilla = clazzPaperAdventure!!.getDeclaredMethod(
                "asVanilla",
                Component::class.java
            )
        }

        // # {"fileName":"EntityType.java","id":"sourceFile"}
        // java.util.Optional byString(java.lang.String) ->
        this.methodEntityTypeByString = clazzEntityTypes!!.getDeclaredMethod(
            NmsMappings.getMapping("methodEntityTypeByString"),
            String::class.java
        )

        // java.lang.String getDescriptionId() ->
        this.methodGetDescriptionId = clazzEntityTypes!!.getDeclaredMethod(
            NmsMappings.getMapping("methodGetDescriptionId")
        )

        if (this.isOneNinteenThreeOrNewer) {
            // the methods here were added in 1.19.3

            // java.util.List getNonDefaultValues() ->

            this.methodGetNonDefaultValues = clazzDataWatcher!!.getDeclaredMethod(
                NmsMappings.getMapping("methodGetNonDefaultValues")
            )

            // set(net.minecraft.network.syncher.EntityDataAccessor,java.lang.Object) ->
            this.methodSynchedEntityDataDefine = clazzDataWatcher!!.getMethod(
                NmsMappings.getMapping("methodSynchedEntityDataDefine"),
                clazzDataWatcherObject, Any::class.java
            )

            // net.minecraft.network.syncher.SynchedEntityData$DataItem getItem(net.minecraft.network.syncher.EntityDataAccessor) ->
            // private <T> DataWatcher.Item<T> getItem(DataWatcherObject<T> datawatcherobject)
            methodName = if (ver.minorVersion == 20 && ver.revision <= 4)
                "c"
            else
                NmsMappings.getMapping("methodDataWatcherGetItem")
            // 1.19, 1.20.5 = b, 1.20 - 1.20.4+ = c

            this.methodDataWatcherGetItem = clazzDataWatcher!!.getDeclaredMethod(
                methodName,
                clazzDataWatcherObject
            )
            methodDataWatcherGetItem!!.setAccessible(true)

            // net.minecraft.network.syncher.SynchedEntityData$DataItem ->
            //       net.minecraft.network.syncher.SynchedEntityData$DataValue value() ->
            this.methodDataWatcherItemValue = clazzDataWatcherItem!!.getDeclaredMethod(
                NmsMappings.getMapping("methodDataWatcherItemValue")
            )
        }
    }

    @Throws(NoSuchFieldException::class)
    private fun buildFields() {
        // net.minecraft.network.syncher.EntityDataSerializer OPTIONAL_COMPONENT
        this.fieldOPTIONALCOMPONENT = clazzDataWatcherRegistry!!.getDeclaredField(
            NmsMappings.getMapping("fieldOPTIONALCOMPONENT")
        )

        // net.minecraft.network.syncher.EntityDataSerializer BOOLEAN
        this.fieldBOOLEAN = clazzDataWatcherRegistry!!.getDeclaredField(
            NmsMappings.getMapping("fieldBOOLEAN")
        )

        // # {"fileName":"ServerPlayer.java","id":"sourceFile"}
        // net.minecraft.server.level.ServerPlayer ->
        //    net.minecraft.server.network.ServerGamePacketListenerImpl connection ->
        val fieldName = when (ver.majorVersionEnum) {
            MinecraftMajorVersion.V1_21 -> {
                if (ver.useMojangMappings)
                    "connection"
                else if (ver.revision >= 6)
                    "g"
                else if (ver.revision >= 2)
                    "f"
                else
                    "c"
            }
            MinecraftMajorVersion.V1_20 -> "c"
            else -> /* 1.19 */ "b"
        }

        this.fieldConnection = clazzEntityPlayer!!.getDeclaredField(fieldName)

        if (ver.minorVersion >= 19) {
            // net.minecraft.network.syncher.SynchedEntityData ->
            // pre 1.20.5:
            //   it.unimi.dsi.fastutil.ints.Int2ObjectMap itemsById ->
            // 1.20.5+:
            //    net.minecraft.network.syncher.SynchedEntityData$DataItem[] itemsById ->
            // (decompiled) private final Int2ObjectMap<DataWatcher.Item<?>> itemsById

            val methodName = if (this.isOneNinteenThreeOrNewer)
                NmsMappings.getMapping("fieldInt2ObjectMap")
            else
                "f"

            this.fieldInt2ObjectMap = clazzDataWatcher!!.getDeclaredField(methodName)
            fieldInt2ObjectMap!!.setAccessible(true)
        }

        if (!ver.useOldEnums)
            fieldEquipmentSlotAny = clazzEquipmentSlotGroup!!.getDeclaredField("ANY")
    }

    @Throws(NoSuchMethodException::class)
    private fun buildConstructors() {
        this.ctorEntityDataAccessor = clazzDataWatcherObject!!.getConstructor(
            Int::class.javaPrimitiveType, clazzDataWatcherSerializer
        )

        if (isOneTwentyFiveOrNewer)
            this.ctorSynchedEntityDataBuilder = clazzDataWatcherBuilder!!.getConstructor(clazzSyncedDataHolder)
        else
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

        if (!ver.useOldEnums){
            // AttributeModifier(attribute.key, additionValue.toDouble(), modifierOperation, EquipmentSlotGroup.ANY)
            this.ctorAttributeModifier = AttributeModifier::class.java.getConstructor(
                NamespacedKey::class.java, Double::class.java, AttributeModifier.Operation::class.java, clazzEquipmentSlotGroup
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