package io.github.arcaneplugins.levelledmobs.nametag

import io.github.arcaneplugins.levelledmobs.LevelledMobs
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
import org.bukkit.entity.Entity
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
    private var hasMiniMessage = false
    var useTranslationComponents = false
    private var useLegacySerializer = false
    private var ver = ServerVersionInfo()
    var mm: MiniMessage? = null

    // classes:
    var clazzCompoundTag: Class<*>? = null
        private set
    private var clazzProblemReport: Class<*>? = null
    private var clazzValueOutput: Class<*>? = null
    var clazzTagValueOutput: Class<*>? = null
        private set
    private var clazzIChatMutableComponent: Class<*>? = null
    private var clazzComponent: Class<*>? = null
    private var clazzMutableComponent: Class<*>? = null
    private var clazzCommonComponents: Class<*>? = null
    private var clazzIChatBaseComponent: Class<*>? = null
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
        private set
    var clazzNbtCompount: Class<*>? = null
        private set
    var clazzByteArrayTag: Class<*>? = null
        private set
    var clazzNBTTagType: Class<*>? = null
        private set
    var clazzNBTBase: Class<*>? = null
        private set

    // mythic mobs:
    private var clazzMMmobExecutor: Class<*>? = null
    private var clazzMMactiveMob: Class<*>? = null
    private var clazzMMmobType: Class<*>? = null

    // methods:
    var methodSaveWithoutId: Method? = null
        private set
    var methodWithoutContext: Method? = null
        private set
    var methodBuildResult: Method? = null
        private set
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
    var methodGetName: Method? = null
        private set
    var methodGetType: Method? = null
        private set
    var methodByteArrayTagSize: Method? = null
        private set

    // mythic mobs:
    var methodMMgetActiveMob: Method? = null
        private set

    // fields
    var fieldDISCARDING: Field? = null
        private set
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
    var fieldTags: Field? = null
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

    // Lib's Disguises
    var clazzDisguise: Class<*>? = null
        private set
    var clazzDisguiseAPI: Class<*>? = null
        private set
    var clazzLDFlagWatcher: Class<*>? = null
        private set
    var methodGetDisguise: Method? = null
        private set
    var methodIsDisguiseInUse: Method? = null
        private set
    var methodLDGetWather: Method? = null
        private set
    var methodLDSetCustomName: Method? = null
        private set

    fun load(){
        ver = LevelledMobs.instance.ver

        Log.inf("Building reflection cache, use simple names: ${ver.useSimpleName}")
        build()
        if (hasMiniMessage) mm = MiniMessage.miniMessage()
    }

    private fun build() {
        try {
            buildClasses()

            // build methods
            getMethodComponentAppend()
            getMethodTextComponents()
            getMethodTranslatable()
            buildSimpleMethods()
            buildFields()
            buildConstructors()
            buildNbtDumpRelatedStuff()
            buildMythicMobs()
            buildLibsDisguises()
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
            if (ver.minecraftVersion.isGreaterThanOrEqual("26.1")){
                this.clazzComponent = Class.forName(
                    "net.minecraft.network.chat.Component"
                )
            }
            else{
                this.clazzIChatMutableComponent = Class.forName(
                    "net.minecraft.network.chat.IChatMutableComponent"
                )

                this.clazzIChatBaseComponent = Class.forName(
                    "net.minecraft.network.chat.IChatBaseComponent"
                )
            }
        }

        if (ver.minecraftVersion.isGreaterThanOrEqual("26.1")){
            this.clazzMutableComponent = Class.forName(
                "net.minecraft.network.chat.MutableComponent"
            )

            this.clazzCommonComponents = Class.forName(
                "net.minecraft.network.chat.CommonComponents"
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

        if (ver.minecraftVersion.isGreaterThanOrEqual("1.21.5")){
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

    private fun buildNbtDumpRelatedStuff(){
        // these reflection items go here because they are optional
        // if they fail then only nbt-dump command will not be available

        // 1.21.5 and older:
        //    net.minecraft.nbt.CompoundTag saveWithoutId(net.minecraft.nbt.CompoundTag) -> h
        // 1.21.6+:
        //    void saveWithoutId(net.minecraft.world.level.storage.ValueOutput) -> d
        if (!ver.isRunningPaper) return // paper only command
        val isTwentySixOneOrNewer = ver.minecraftVersion.isGreaterThanOrEqual("26.1")

        try {
            var className = if (isTwentySixOneOrNewer)
                "CompoundTag" else "NBTTagCompound"
            this.clazzNbtCompount = Class.forName("net.minecraft.nbt.$className")
            this.clazzByteArrayTag = Class.forName("net.minecraft.nbt.ByteArrayTag")
            className = if (isTwentySixOneOrNewer)
                "TagType" else "NBTTagType"
            this.clazzNBTTagType = Class.forName("net.minecraft.nbt.$className")
            className = if (isTwentySixOneOrNewer)
                "Tag" else "NBTBase"
            this.clazzNBTBase = Class.forName("net.minecraft.nbt.$className")

            this.methodGetName = clazzNBTTagType!!.getDeclaredMethod("getName")
            this.methodGetType = clazzNBTBase!!.getDeclaredMethod("getType")
            this.methodByteArrayTagSize = clazzByteArrayTag!!.getMethod("size")
            this.fieldTags = clazzNbtCompount!!.getDeclaredField("tags")
            fieldTags!!.trySetAccessible()

            if (ver.minecraftVersion.isGreaterThanOrEqual("1.21.6")) {
                /*
                    net.minecraft.util.ProblemReporter p = net.minecraft.util.ProblemReporter.DISCARDING;
                    net.minecraft.world.level.storage.TagValueOutput tvo = net.minecraft.world.level.storage.TagValueOutput.createWithoutContext(p);
                    entity.saveWithoutId(tvo);
                    net.minecraft.nbt.CompoundTag tag = tvo.buildResult();
                */
                this.clazzProblemReport = Class.forName("net.minecraft.util.ProblemReporter")
                this.fieldDISCARDING = clazzProblemReport!!.getDeclaredField("DISCARDING")
                this.clazzValueOutput = Class.forName("net.minecraft.world.level.storage.ValueOutput")
                this.clazzTagValueOutput = Class.forName("net.minecraft.world.level.storage.TagValueOutput")
                this.methodWithoutContext =
                    clazzTagValueOutput!!.getDeclaredMethod("createWithoutContext", clazzProblemReport)
                this.methodSaveWithoutId = clazzEntity!!.getDeclaredMethod("saveWithoutId", clazzValueOutput)
                this.methodBuildResult = this.clazzTagValueOutput!!.getDeclaredMethod("buildResult")
            }
            else {
                // pre 1.21.6:
                this.clazzCompoundTag = Class.forName(
                    "net.minecraft.nbt.NBTTagCompound"
                )
                this.methodSaveWithoutId = clazzEntity!!.getDeclaredMethod("f", clazzCompoundTag)
            }
        }
        catch (e: Exception){
            Log.war("Error getting reflection methods for nbt-dump operations: ${e.message}")
        }
    }

    private fun getMethodComponentAppend() {
        if (ver.isRunningPaper) return // spigot only stuff here

        // # {"fileName":"MutableComponent.java","id":"sourceFile"}
        // net.minecraft.network.chat.MutableComponent append(net.minecraft.network.chat.Component) ->
        // 1.19.0 = a, everything else  = b

        /*
        * val methodName = if (ver.minecraftVersion == 1.19 && ver.revision == 0
        ) "a" else "b"

        this.methodComponentAppend = clazzIChatMutableComponent!!.getDeclaredMethod(
            methodName, this.clazzIChatBaseComponent
        )
        * */

        if (ver.minecraftVersion.isGreaterThanOrEqual("26.1")){
            this.methodComponentAppend = clazzMutableComponent!!.getDeclaredMethod(
                "append", this.clazzComponent
            )
        }
        else{
            this.methodComponentAppend = clazzIChatMutableComponent!!.getDeclaredMethod(
                "b", this.clazzIChatBaseComponent
            )
        }
    }

    private fun getMethodTextComponents() {
        if (ver.isRunningPaper) return // spigot only stuff here

        // # {"fileName":"Component.java","id":"sourceFile"}
        // net.minecraft.network.chat.Component ->
        //     net.minecraft.network.chat.MutableComponent empty()

        if (ver.minecraftVersion.isGreaterThanOrEqual("26.1")) {
            this.methodEmptyComponent = clazzComponent!!.getDeclaredMethod("empty")
            this.methodTextComponent = clazzComponent!!.getDeclaredMethod(
                "nullToEmpty", String::class.java)
        }
        else {
            this.methodEmptyComponent = clazzIChatBaseComponent!!.getDeclaredMethod("h")
            // net.minecraft.network.chat.Component ->
            //    net.minecraft.network.chat.Component nullToEmpty(java.lang.String) -> a
            this.methodTextComponent = clazzIChatBaseComponent!!.getDeclaredMethod(
                "a", String::class.java)
        }
    }

    private fun getMethodTranslatable() {
        if (ver.isRunningPaper) return // spigot only stuff here

        // # {"fileName":"Component.java","id":"sourceFile"}
        // net.minecraft.network.chat.Component ->
        // net.minecraft.network.chat.MutableComponent translatable(java.lang.String) ->

        if (ver.minecraftVersion.isGreaterThanOrEqual("26.1")) {
            this.methodTranslatable = clazzComponent!!.getDeclaredMethod(
                "translatable", String::class.java
            )
            // net.minecraft.network.chat.Component ->
            // net.minecraft.network.chat.MutableComponent translatable(java.lang.String,java.lang.Object[]) ->
            this.methodTranslatableWithArgs = clazzComponent!!.getDeclaredMethod(
                "translatable",
                String::class.java, Array<Any>::class.java
            )
        }
        else{
            this.methodTranslatable = clazzIChatBaseComponent!!.getDeclaredMethod(
                "c",
                String::class.java
            )
            this.methodTranslatableWithArgs = clazzIChatBaseComponent!!.getDeclaredMethod(
                "a",
                String::class.java, Array<Any>::class.java
            )
        }
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
        var methodName = if (ver.useMojangMappings)
            "getEntityData"
        else {
            if (ver.revision >= 11)
                "aD"
            else if (ver.revision >= 9)
                "aC"
            else if (ver.revision >= 2 && ver.revision != 5)
                "au"
            else
                "ar"
        }

        // net.minecraft.network.syncher.SynchedEntityData getEntityData() ->
        this.methodGetEntityData = clazzEntity!!.getMethod(methodName)

        // set(net.minecraft.network.syncher.EntityDataAccessor,java.lang.Object) ->
        methodName =
            if (ver.useMojangMappings) "set"
            else if (ver.minecraftVersion.isGreaterThanOrEqual("1.21.5")) "a"
            else "b"

        this.methodSet = clazzDataWatcher!!.getMethod(
            methodName, clazzDataWatcherObject,
            Any::class.java
        )

        // # {"fileName":"ChunkStatusUpdateListener.java","id":"sourceFile"}
        // net.minecraft.world.level.entity.EntityAccess ->
        //   int getId() ->
        methodName =
            if (ver.useMojangMappings)
                "getId"
            else if (ver.revision >= 11) // 1.21.x
                "aA"
            else if (ver.revision >= 9)
                "az"
            else if (ver.revision == 5)
                "ao" // 1.21.5 only
            else if (ver.revision >= 2)
                "ar" // 1.21.2 - 1.21.4, 1.21.6+
            else
                "an"

        this.methodGetId = clazzEntity!!.getDeclaredMethod(methodName)

        if (ver.minecraftVersion.isGreaterThanOrEqual("1.21.5")){
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
            else
                "b"

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
        methodName = NmsMappings.getMapping("methodDataWatcherGetItem")

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
        val fieldName =
            if (ver.useMojangMappings)
                "connection"
            else if (ver.revision >= 6)
                "g"
            else if (ver.revision >= 2)
                "f"
            else
                "c"

        this.fieldConnection = clazzEntityPlayer!!.getDeclaredField(fieldName)

        // net.minecraft.network.syncher.SynchedEntityData ->
        // pre 1.20.5:
        //   it.unimi.dsi.fastutil.ints.Int2ObjectMap itemsById ->
        // 1.20.5+:
        //    net.minecraft.network.syncher.SynchedEntityData$DataItem[] itemsById ->
        // (decompiled) private final Int2ObjectMap<DataWatcher.Item<?>> itemsById

        val methodName = NmsMappings.getMapping("fieldInt2ObjectMap")
        this.fieldInt2ObjectMap = clazzDataWatcher!!.getDeclaredField(methodName)
        fieldInt2ObjectMap!!.setAccessible(true)

        if (!ver.useOldEnums)
            fieldEquipmentSlotAny = clazzEquipmentSlotGroup!!.getDeclaredField("ANY")
    }

    @Throws(NoSuchMethodException::class)
    private fun buildConstructors() {
        this.ctorEntityDataAccessor = clazzDataWatcherObject!!.getConstructor(
            Int::class.javaPrimitiveType, clazzDataWatcherSerializer
        )

        if (ver.minecraftVersion.isGreaterThanOrEqual("1.21.5"))
            this.ctorSynchedEntityDataBuilder = clazzDataWatcherBuilder!!.getConstructor(clazzSyncedDataHolder)
        else
            this.ctorSynchedEntityData = clazzDataWatcher!!.getConstructor(clazzEntity)

        // starting with 1.19.3 use this one:
        // public net.minecraft.network.protocol.game.PacketPlayOutEntityMetadata(int,java.util.List<DataWatcher.b<?>>)

        this.ctorPacket = clazzClientboundSetEntityDataPacket!!.getConstructor(
            Int::class.javaPrimitiveType, MutableList::class.java
        )

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
        if (mmMain == null || !mmMain.isEnabled) return

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

    private fun buildLibsDisguises(){
        val ldMain = Bukkit.getPluginManager().getPlugin("LibsDisguises")
        if (ldMain == null || !ldMain.isEnabled) return

        clazzDisguise = Class.forName("me.libraryaddict.disguise.disguisetypes.Disguise")
        clazzDisguiseAPI = Class.forName("me.libraryaddict.disguise.DisguiseAPI")
        clazzLDFlagWatcher = Class.forName("me.libraryaddict.disguise.disguisetypes.FlagWatcher")
        methodGetDisguise = clazzDisguiseAPI!!.getMethod("getDisguise", Entity::class.java)
        methodIsDisguiseInUse = clazzDisguise!!.getMethod("isDisguiseInUse")
        methodLDGetWather = clazzDisguise!!.getMethod("getWatcher")
        methodLDSetCustomName = clazzLDFlagWatcher!!.getMethod("setCustomName", String::class.java)
    }

    fun setUseLegacySerializer(useLegacySerializer: Boolean) {
        this.useLegacySerializer = useLegacySerializer
    }

    fun getUseLegacySerializer(): Boolean {
        return !this.hasMiniMessage || this.useLegacySerializer
    }
}