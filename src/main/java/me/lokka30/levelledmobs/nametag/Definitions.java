package me.lokka30.levelledmobs.nametag;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import me.lokka30.levelledmobs.LevelledMobs;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * Provides all NMS classes, fields and methods that are used for
 * nametags and some Mythic Mobs stuff
 *
 * @author stumper66
 * @since 3.9.2
 */
public class Definitions {

    public Definitions() {
        this.ver = LevelledMobs.getInstance().getVerInfo();
        build();
        if (hasMiniMessage) mm = net.kyori.adventure.text.minimessage.MiniMessage.miniMessage();
    }

    private final ServerVersionInfo ver;
    private boolean hasKiori;
    private boolean isOneNinteenThreeOrNewer;
    private boolean hasMiniMessage;
    public boolean useTranslationComponents;
    private boolean useLegacySerializer;
    public net.kyori.adventure.text.minimessage.MiniMessage mm;

    // classes:
    Class<?> clazz_IChatMutableComponent;
    Class<?> clazz_IChatBaseComponent;
    Class<?> clazz_TranslatableComponent;
    Class<?> clazz_CraftLivingEntity;
    Class<?> clazz_CraftEntity;
    Class<?> clazz_Entity;
    Class<?> clazz_DataWatcher;
    Class<?> clazz_DataWatcher_Item;
    Class<?> clazz_DataWatcherRegistry;
    Class<?> clazz_DataWatcherObject;
    Class<?> clazz_DataWatcherSerializer;
    Class<?> clazz_ClientboundSetEntityDataPacket;
    Class<?> clazz_CraftPlayer;
    Class<?> clazz_Packet;
    Class<?> clazz_PlayerConnection;
    Class<?> clazz_ServerPlayerConnection;
    Class<?> clazz_NetworkManager;
    Class<?> clazz_EntityPlayer;
    Class<?> clazz_PaperAdventure;
    Class<?> clazz_EntityTypes;
    // mythic mobs:
    public Class<?> clazz_MM_MobExecutor;
    public Class<?> clazz_MM_ActiveMob;
    public Class<?> clazz_MM_MobType;

    // methods:
    Method method_ComponentAppend;
    Method method_EmptyComponent;
    Method method_TextComponent;
    Method method_Translatable;
    Method method_TranslatableWithArgs;
    Method method_getHandle;
    Method method_getEntityData;
    Method method_set;
    Method method_getId;
    Method method_PlayergetHandle;
    Method method_Send;
    Method method_getAll;
    Method method_define;
    Method method_getAccessor;
    Method method_getValue;
    Method method_AsVanilla;
    Method method_EntityTypeByString;
    Method method_GetDescriptionId;
    Method method_getNonDefaultValues;
    Method method_SynchedEntityData_Define;
    Method method_DataWatcher_GetItem;
    Method method_DataWatcherItem_Value;
    // mythic mobs:
    public Method method_MM_getActiveMob;

    // fields
    Field field_OPTIONAL_COMPONENT;
    Field field_BOOLEAN;
    Field field_Connection;
    Field field_Int2ObjectMap;
    // mythic mobs:
    public Field field_MM_mobManager;
    public Field field_MM_type;
    public Field field_MM_preventOtherDrops;
    public Field field_MM_preventRandomEquipment;
    public Field field_MM_internalName;

    // Constructors
    Constructor<?> ctor_EntityDataAccessor;
    Constructor<?> ctor_SynchedEntityData;
    Constructor<?> ctor_Packet;

    private void build() {
        this.isOneNinteenThreeOrNewer =
            ver.getMinecraftVersion() == 1.19d && ver.getRevision() >= 3d ||
                ver.getMinecraftVersion() >= 1.20d;

        // protocollib is used on 1.16
        if (ver.getMajorVersionEnum() == ServerVersionInfo.MinecraftMajorVersion.V1_16) return;

        try {
            buildClasses();

            // build methods
            getMethodComponentAppend();
            getMethodTextComponents();
            getMethodTranslatable();
            buildSimpleMethods();
            buildFields();
            buildConstructors();
            buildMythicMobs();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private @NotNull String getClassName(final @NotNull String classSuffix){
        // suffix ------------------------->
        // "org.bukkit.craftbukkit.v1_20_R3.entity.CraftEntity"
        if (ver.getIsRunningFabric()){
            return "org.bukkit.craftbukkit." + classSuffix;
        }
        else{
            return "org.bukkit.craftbukkit." + ver.getNMSVersion() + "." + classSuffix;
        }
    }

    private void buildClasses() throws ClassNotFoundException {
        this.clazz_IChatMutableComponent = Class.forName(
            "net.minecraft.network.chat.IChatMutableComponent");

        this.clazz_IChatBaseComponent = Class.forName(
            "net.minecraft.network.chat.IChatBaseComponent");

        this.clazz_CraftEntity = Class.forName(
            getClassName("entity.CraftEntity"));

        this.clazz_CraftLivingEntity = Class.forName(
            getClassName("entity.CraftLivingEntity"));

        // net.minecraft.network.syncher.SynchedEntityData
        this.clazz_DataWatcher = Class.forName(
            "net.minecraft.network.syncher.DataWatcher");

        this.clazz_DataWatcher_Item = Class.forName(
            "net.minecraft.network.syncher.DataWatcher$Item");

        this.clazz_DataWatcherRegistry = Class.forName(
            "net.minecraft.network.syncher.DataWatcherRegistry");

        this.clazz_Entity = Class.forName(
            "net.minecraft.world.entity.Entity");

        this.clazz_DataWatcherObject = Class.forName(
            "net.minecraft.network.syncher.DataWatcherObject");

        this.clazz_DataWatcherSerializer = Class.forName(
            "net.minecraft.network.syncher.DataWatcherSerializer");

        this.clazz_ClientboundSetEntityDataPacket = Class.forName(
            "net.minecraft.network.protocol.game.PacketPlayOutEntityMetadata");

        this.clazz_CraftPlayer = Class.forName(
            getClassName("entity.CraftPlayer"));

        this.clazz_Packet = Class.forName(
            "net.minecraft.network.protocol.Packet");

        this.clazz_NetworkManager = Class.forName(
            "net.minecraft.network.NetworkManager");

        //net.minecraft.server.network.ServerPlayerConnection ->
        // void send(net.minecraft.network.protocol.Packet) ->
        this.clazz_PlayerConnection = Class.forName(
            "net.minecraft.server.network.PlayerConnection");

        this.clazz_ServerPlayerConnection = Class.forName(
            "net.minecraft.server.network.ServerPlayerConnection");

        // net.minecraft.server.level.ServerPlayer ->
        this.clazz_EntityPlayer = Class.forName(
            "net.minecraft.server.level.EntityPlayer");

        if (ver.getMinecraftVersion() < 1.19) {
            // this is basically TranslatableComponent
            this.clazz_TranslatableComponent = Class.forName(
                "net.minecraft.network.chat.ChatMessage");
        }

        try {
            this.clazz_PaperAdventure = Class.forName(
                "io.papermc.paper.adventure.PaperAdventure");
            this.hasKiori = true;
        } catch (ClassNotFoundException ignored) {
        }

        this.clazz_EntityTypes = Class.forName(
            "net.minecraft.world.entity.EntityTypes");

        if (hasKiori && ver.getMinecraftVersion() >= 1.18){
            try{
                Class.forName("net.kyori.adventure.text.minimessage.MiniMessage");
                this.hasMiniMessage = true;
            }
            catch (ClassNotFoundException ignored) {}
        }
    }

    private void getMethodComponentAppend() throws NoSuchMethodException {
        // # {"fileName":"MutableComponent.java","id":"sourceFile"}
        // net.minecraft.network.chat.MutableComponent append(net.minecraft.network.chat.Component) ->
        // 1.19.0 = a, everything else  = b
        String methodName = ver.getMinecraftVersion() == 1.19 && ver.getRevision() == 0 ||
                ver.getMinecraftVersion() == 1.18
            ? "a" : "b";

        if (ver.getMinecraftVersion() <= 1.17) {
            methodName = "addSibling";
        }

        this.method_ComponentAppend = clazz_IChatMutableComponent.getDeclaredMethod(
            methodName, this.clazz_IChatBaseComponent);
    }

    private void getMethodTextComponents() throws NoSuchMethodException {
        // # {"fileName":"Component.java","id":"sourceFile"}
        // net.minecraft.network.chat.Component ->
        //     net.minecraft.network.chat.MutableComponent empty()

        if (ver.getMinecraftVersion() >= 1.19) {
            String methodName;
            if (ver.getMinecraftVersion() >= 1.20){
                methodName = ver.getRevision() >= 3 ?
                        // 1.20.3+ or 1.20.0 - 2
                        "i" : "h";
            }
            else {
                // 1.19.0 = g, 1.19.1+ = h
                methodName = ver.getRevision() == 0 ? "g" : "h";
            }

            // net.minecraft.network.chat.Component ->
            //     net.minecraft.network.chat.MutableComponent empty()
            this.method_EmptyComponent = clazz_IChatBaseComponent.getDeclaredMethod(methodName);
        }

        // 1.18 doesn't have #empty(), instead use #nullToEmpty()
        // net.minecraft.network.chat.Component -> qk:
        //    net.minecraft.network.chat.Component nullToEmpty(java.lang.String) -> a
        this.method_TextComponent = clazz_IChatBaseComponent.getDeclaredMethod("a", String.class);

    }

    private void getMethodTranslatable() throws NoSuchMethodException {
        if (ver.getMinecraftVersion() <= 1.18) {
            // 1.18 instantiates an object, so this method doesn't apply
            return;
        }

        // # {"fileName":"Component.java","id":"sourceFile"}
        // net.minecraft.network.chat.Component ->
        // net.minecraft.network.chat.MutableComponent translatable(java.lang.String) ->

        this.method_Translatable = clazz_IChatBaseComponent.getDeclaredMethod("c",
                String.class);

        // net.minecraft.network.chat.Component ->
        // net.minecraft.network.chat.MutableComponent translatable(java.lang.String,java.lang.Object[])

        this.method_TranslatableWithArgs = clazz_IChatBaseComponent.getDeclaredMethod("a",
            String.class, Object[].class);
    }

    @SuppressWarnings("deprecation")
    public @NotNull String getTranslationKey(final @NotNull LivingEntity livingEntity) {
        // only needed for spigot. paper has a built-in method

        // net.minecraft.world.entity.EntityType ->
        //   java.util.Optional byString(java.lang.String) -> a
        // public static Optional<EntityTypes<?>> byString(String s)

        Optional<?> optionalResult;
        try {
            optionalResult = (Optional<?>) this.method_EntityTypeByString.invoke(null,
                livingEntity.getType().getName());

            if (optionalResult.isEmpty()) {
                return "";
            }

            // net.minecraft.world.entity.EntityTypes<T extends Entity>
            return (String) method_GetDescriptionId.invoke(optionalResult.get());
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }

        return "";
    }

    private void buildSimpleMethods() throws NoSuchMethodException {
        this.method_getHandle = clazz_CraftLivingEntity.getDeclaredMethod("getHandle");

        // # {"fileName":"Entity.java","id":"sourceFile"}
        // net.minecraft.network.syncher.SynchedEntityData getEntityData() ->
        String methodName;

        switch (ver.getMajorVersionEnum()) {
            case V1_20 -> {
                if (ver.getRevision() >= 3){
                    // 1.20.3+
                    methodName = "an";
                }
                else if (ver.getRevision() == 2){
                    // 1.20.2
                    methodName = "al";
                }
                else{
                    // 1.20 - 1.20.1
                    methodName = "aj";
                }
            }
            case V1_19 -> {
                if (ver.getRevision() >= 4) {
                    methodName = "aj";
                } else if (ver.getRevision() == 3) {
                    methodName = "al";
                } else {
                    methodName = "ai";
                }
            }
            case V1_18 -> methodName = "ai";
            case V1_17 -> methodName = "getDataWatcher";
            default ->
                    throw new RuntimeException("Unable to determine NMS method name for your Minecraft server version. Is your server version compatible?");
        }

        // net.minecraft.network.syncher.SynchedEntityData getEntityData() ->
        this.method_getEntityData = clazz_Entity.getMethod(methodName);

        methodName = ver.getMinecraftVersion() >= 1.18 ?
            "b" : "set";

        // set(net.minecraft.network.syncher.EntityDataAccessor,java.lang.Object) ->
        this.method_set = clazz_DataWatcher.getMethod(methodName, clazz_DataWatcherObject,
            Object.class);

        // # {"fileName":"ChunkStatusUpdateListener.java","id":"sourceFile"}
        // net.minecraft.world.level.entity.EntityAccess ->
        //   int getId() ->
        if (ver.getMinecraftVersion() >= 1.20){
            if (ver.getRevision() >= 3){
                // 1.20.3+
                methodName = "aj";
            }
            else{
                methodName = ver.getRevision() >= 2 ?
                        "ah" : "af";
            }
        }
        else if (ver.getMinecraftVersion() >= 1.18){
            if (ver.getRevision() >= 4){
                methodName = "af";
            }
            else if (this.isOneNinteenThreeOrNewer){
                // 1.19.3
                methodName = "ah";
            }
            else{
                // 1.18 - 1.19.2
                methodName = "ae";
            }
        }
        else if (ver.getMinecraftVersion() <= 1.17) {
            methodName = "getId";
        }

        this.method_getId = clazz_Entity.getDeclaredMethod(methodName);

        this.method_PlayergetHandle = clazz_CraftPlayer.getDeclaredMethod("getHandle");

        // starting with 1.20.2 it is:
        // # {"fileName":"ServerCommonPacketListenerImpl.java","id":"sourceFile"}
        // net.minecraft.server.network.ServerCommonPacketListenerImpl ->

        // 1.20.1 and older:
        // net.minecraft.server.network.ServerGamePacketListenerImpl ->

        //    void send(net.minecraft.network.protocol.Packet) ->

        if (ver.getMinecraftVersion() >= 1.18){
            if (ver.getMajorVersionEnum() == ServerVersionInfo.MinecraftMajorVersion.V1_20 && ver.getRevision() >= 2){
                methodName = "b";
            }
            else{
                methodName = "a";
            }
        }
        else{
            methodName ="sendPacket";
        }

        this.method_Send = clazz_ServerPlayerConnection.getDeclaredMethod(methodName, clazz_Packet);

        if (ver.getMinecraftVersion() <= 1.18) {
            methodName = ver.getMajorVersionEnum() == ServerVersionInfo.MinecraftMajorVersion.V1_18 ?
                    "c" : "getAll";
            // java.util.List getAll() ->
            this.method_getAll = clazz_DataWatcher.getDeclaredMethod(methodName);
        }

        // # {"fileName":"SynchedEntityData.java","id":"sourceFile"}
        // net.minecraft.network.syncher.SynchedEntityData ->
        //    define(net.minecraft.network.syncher.EntityDataAccessor,java.lang.Object) ->
        methodName = ver.getMinecraftVersion() >= 1.18 ?
                "a" : "register";

        this.method_define = clazz_DataWatcher.getDeclaredMethod(methodName,
            clazz_DataWatcherObject, Object.class);

        // net.minecraft.network.syncher.EntityDataAccessor getAccessor() ->
        this.method_getAccessor = clazz_DataWatcher_Item.getDeclaredMethod("a");
        // java.lang.Object getValue() ->
        this.method_getValue = clazz_DataWatcher_Item.getDeclaredMethod("b");

        // net.minecraft.network.Connection getConnection() ->
        //this.method_getConnection = clazz_CraftPlayer.getDeclaredMethod("networkManager");

        if (this.hasKiori) {
            this.method_AsVanilla = clazz_PaperAdventure.getDeclaredMethod("asVanilla",
                Component.class);
        }

        // java.util.Optional byString(java.lang.String) ->
        this.method_EntityTypeByString = clazz_EntityTypes.getDeclaredMethod("a", String.class);

        // java.lang.String getDescriptionId() ->
        this.method_GetDescriptionId = clazz_EntityTypes.getDeclaredMethod("g");

        if (this.isOneNinteenThreeOrNewer()) {
            // the methods here were added in 1.19.3

            // java.util.List getNonDefaultValues() ->
            this.method_getNonDefaultValues = clazz_DataWatcher.getDeclaredMethod("c");

            // define(net.minecraft.network.syncher.EntityDataAccessor,java.lang.Object) ->
            this.method_SynchedEntityData_Define = clazz_DataWatcher.getMethod("a",
                clazz_DataWatcherObject, Object.class);

            // net.minecraft.network.syncher.SynchedEntityData$DataItem getItem(net.minecraft.network.syncher.EntityDataAccessor) ->
            // private <T> DataWatcher.Item<T> getItem(DataWatcherObject<T> datawatcherobject)
            methodName = ver.getMinecraftVersion() >= 1.20 ? "c" : "b";
            this.method_DataWatcher_GetItem = clazz_DataWatcher.getDeclaredMethod(methodName,
                clazz_DataWatcherObject);
            this.method_DataWatcher_GetItem.setAccessible(true);

            // net.minecraft.network.syncher.SynchedEntityData$DataItem ->
            //       net.minecraft.network.syncher.SynchedEntityData$DataValue value() ->
            this.method_DataWatcherItem_Value = clazz_DataWatcher_Item.getDeclaredMethod("e");
        }
    }

    private void buildFields() throws NoSuchFieldException {
        // net.minecraft.network.syncher.EntityDataSerializer OPTIONAL_COMPONENT
        this.field_OPTIONAL_COMPONENT = clazz_DataWatcherRegistry.getDeclaredField("f");

        // net.minecraft.network.syncher.EntityDataSerializer BOOLEAN
        this.field_BOOLEAN = clazz_DataWatcherRegistry.getDeclaredField("i");

        // # {"fileName":"ServerPlayer.java","id":"sourceFile"}
        // net.minecraft.server.level.ServerPlayer ->
        //    net.minecraft.server.network.ServerGamePacketListenerImpl connection ->
        String fieldName = ver.getMinecraftVersion() >= 1.20 ? "c" : "b";
        this.field_Connection = clazz_EntityPlayer.getDeclaredField(fieldName);

        if (ver.getMinorVersion() >= 19) {
            // net.minecraft.network.syncher.SynchedEntityData ->
            //   it.unimi.dsi.fastutil.ints.Int2ObjectMap itemsById ->
            // (decompiled) private final Int2ObjectMap<DataWatcher.Item<?>> itemsById

            final String methodName =  this.isOneNinteenThreeOrNewer() ?
                    "e" : "f";

            this.field_Int2ObjectMap = clazz_DataWatcher.getDeclaredField(methodName);
            this.field_Int2ObjectMap.setAccessible(true);
        }
    }

    private void buildConstructors() throws NoSuchMethodException {
        this.ctor_EntityDataAccessor = clazz_DataWatcherObject.getConstructor(
            int.class, clazz_DataWatcherSerializer);

        this.ctor_SynchedEntityData = clazz_DataWatcher.getConstructor(clazz_Entity);

        if (this.isOneNinteenThreeOrNewer) {
            // starting with 1.19.3 use this one:
            // public net.minecraft.network.protocol.game.PacketPlayOutEntityMetadata(int,java.util.List<DataWatcher.b<?>>)

            this.ctor_Packet = clazz_ClientboundSetEntityDataPacket.getConstructor(
                int.class, List.class);
        } else {
            // up to 1.19.2 use this one:
            this.ctor_Packet = clazz_ClientboundSetEntityDataPacket.getConstructor(
                int.class, clazz_DataWatcher, boolean.class);
        }
    }

    private void buildMythicMobs() throws
            NoSuchMethodException, NoSuchFieldException, ClassNotFoundException {
        // io.lumine.mythic.bukkit.MythicBukkit
        final Plugin mmMain = Bukkit.getPluginManager().getPlugin("MythicMobs");
        if (mmMain == null || !mmMain.isEnabled()) {
            return;
        }

        this.clazz_MM_ActiveMob = Class.forName("io.lumine.mythic.core.mobs.ActiveMob");
        this.clazz_MM_MobExecutor = Class.forName("io.lumine.mythic.core.mobs.MobExecutor");
        this.clazz_MM_MobType = Class.forName("io.lumine.mythic.core.mobs.MobType");

        this.method_MM_getActiveMob = this.clazz_MM_MobExecutor.getMethod("getActiveMob",
                UUID.class);

        this.field_MM_mobManager = mmMain.getClass().getDeclaredField("mobManager");
        this.field_MM_mobManager.setAccessible(true);
        this.field_MM_type = this.clazz_MM_ActiveMob.getDeclaredField("type");
        this.field_MM_type.setAccessible(true);
        this.field_MM_preventOtherDrops = this.clazz_MM_MobType.getDeclaredField(
                "preventOtherDrops"); // boolean
        this.field_MM_preventOtherDrops.setAccessible(true);
        this.field_MM_preventRandomEquipment = this.clazz_MM_MobType.getDeclaredField(
                "preventRandomEquipment"); // boolean
        this.field_MM_preventRandomEquipment.setAccessible(true);
        this.field_MM_internalName = this.clazz_MM_MobType.getDeclaredField(
                "internalName"); // string
        this.field_MM_internalName.setAccessible(true);
    }

    public boolean hasKiori() {
        return hasKiori;
    }

    public boolean isOneNinteenThreeOrNewer() {
        return isOneNinteenThreeOrNewer;
    }

    public void setUseLegacySerializer(final boolean useLegacySerializer){
        this.useLegacySerializer = useLegacySerializer;
    }

    public boolean getUseLegacySerializer(){
        return !this.hasMiniMessage || this.useLegacySerializer;
    }
}