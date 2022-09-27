package me.lokka30.levelledmobs.nms;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;

import me.lokka30.levelledmobs.result.NametagResult;
import me.lokka30.levelledmobs.util.Utils;
import me.lokka30.microlib.messaging.MessageUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Sends NMS verison specific nametag packets to players
 *
 * @author stumper66
 * @since 3.6.0
 */
public class NametagSender implements NMSUtil {

    public NametagSender(final @NotNull ServerVersionInfo versionInfo, final boolean hasKiori) {
        this.versionInfo = versionInfo;
        this.nmsVersion = versionInfo.getNMSVersion();
        this.hasKiori = hasKiori;
        buildReflection();
    }

    private final ServerVersionInfo versionInfo;
    private final String nmsVersion;
    private final boolean hasKiori;
    private Method resolveStringMethod;
    private Method emptyComponentMethod;
    private Method appendComponentMethod;
    private Method nullToEmptyMethod;
    private Class<?> clazz_CraftChatMessage;
    private Class<?> clazz_TranslatableComponent;

    private void buildReflection(){
        String methodName;

        try {
            // we're only here if we have:
            // Paper 1.18.0 +
            // Spigot 1.19.0 +

            Utils.logger.info("vi: " + versionInfo);

            this.clazz_CraftChatMessage = Class.forName(
                    "org.bukkit.craftbukkit." + nmsVersion + ".util.CraftChatMessage");

            this.resolveStringMethod = clazz_CraftChatMessage.getDeclaredMethod("fromString",
                    String.class);

            final Class<?> clazz_IChatMutableComponent = Class.forName(
                    "net.minecraft.network.chat.IChatMutableComponent");

            final Class<?> clazz_IChatBaseComponent = Class.forName(
                    "net.minecraft.network.chat.IChatBaseComponent");

            if (versionInfo.getMinecraftVersion() < 1.19) {
                // this is basically TranslatableComponent
                this.clazz_TranslatableComponent = Class.forName(
                        "net.minecraft.network.chat.ChatMessage");
            }

            // 1.19.0 = a, 1.19.1 = b
            methodName = versionInfo.getRevision() == 0 || versionInfo.getMinecraftVersion() == 1.18
                    ? "a" : "b";

            // net.minecraft.network.chat.MutableComponent append(net.minecraft.network.chat.Component) ->
            this.appendComponentMethod = clazz_IChatMutableComponent.getDeclaredMethod(methodName, clazz_IChatBaseComponent);

            if (versionInfo.getMinecraftVersion() >= 1.19) {
                // 1.19.0 = g, 1.19.1 = h
                methodName = versionInfo.getRevision() == 0 ? "g" : "h";

                // net.minecraft.network.chat.Component ->
                //     net.minecraft.network.chat.MutableComponent empty()
                this.emptyComponentMethod = clazz_IChatBaseComponent.getDeclaredMethod(methodName);
            }
            else if (versionInfo.getMinecraftVersion() == 1.18){
                // 1.18 doesn't have #empty(), instead use #nullToEmpty()
                // net.minecraft.network.chat.Component -> qk:
                //    net.minecraft.network.chat.Component nullToEmpty(java.lang.String) -> a
                this.nullToEmptyMethod = clazz_IChatBaseComponent.getDeclaredMethod("a", String.class);
            }
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    public void sendNametag(final @NotNull LivingEntity livingEntity, @NotNull NametagResult nametag,
                            @NotNull Player player, final boolean doAlwaysVisible) {
        // org.bukkit.craftbukkit.v1_18_R1.entity.CraftLivingEntity
        if (!player.isOnline() || !player.isValid()) return;

        try {
            final Class<?> clazz_CraftLivingEntity = Class.forName(
                "org.bukkit.craftbukkit." + nmsVersion + ".entity.CraftLivingEntity");
            final Method method_getHandle = clazz_CraftLivingEntity.getDeclaredMethod("getHandle");
            final net.minecraft.world.entity.LivingEntity internalLivingEntity = (net.minecraft.world.entity.LivingEntity) method_getHandle.invoke(
                livingEntity);

            final SynchedEntityData entityData = cloneEntityData(
                internalLivingEntity.getEntityData(), internalLivingEntity);
            final EntityDataAccessor<Optional<Component>> customNameAccessor =
                new EntityDataAccessor<>(2, EntityDataSerializers.OPTIONAL_COMPONENT);

            final Optional<Component> customName = buildNametagComponent(livingEntity, nametag);
            entityData.set(customNameAccessor, customName);

            final EntityDataAccessor<Boolean> customNameVisibleAccessor =
                new EntityDataAccessor<>(3, EntityDataSerializers.BOOLEAN);

            entityData.set(customNameVisibleAccessor, !nametag.isNullOrEmpty() && doAlwaysVisible);

            final ClientboundSetEntityDataPacket packet = new ClientboundSetEntityDataPacket(
                internalLivingEntity.getId(), entityData, true
            );

            final Class<?> clazz_CraftPlayer = Class.forName(
                "org.bukkit.craftbukkit." + nmsVersion + ".entity.CraftPlayer");
            final Method method_PlayergetHandle = clazz_CraftPlayer.getDeclaredMethod("getHandle");
            final ServerPlayer serverPlayer = (ServerPlayer) method_PlayergetHandle.invoke(player);
            serverPlayer.connection.send(packet);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException |
                 ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private @NotNull Optional<Component> buildNametagComponent(final @NotNull LivingEntity livingEntity,
                                                               final @NotNull NametagResult nametag){
        if (nametag.isNullOrEmpty())
            return Optional.empty();

        if (hasKiori){
            // paper servers go here:
            return Optional.of(KyoriNametags.generateComponent(livingEntity, nametag));
        }

        // the rest of this method will only be used on spigot servers

        final String mobName = nametag.getNametagNonNull();
        final String displayName = "{DisplayName}";
        final int displayNameIndex = mobName.indexOf(displayName);

        if (displayNameIndex < 0) {
            Component comp = null;
            try {
                comp = ((Component[]) this.resolveStringMethod.invoke(
                        this.clazz_CraftChatMessage, resolveText(nametag.getNametagNonNull())))[0];
            } catch (IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
            if (comp == null)
                return Optional.empty();
            else
                return Optional.of(comp);
        }

        final String leftText = displayNameIndex > 0 ?
                resolveText(mobName.substring(0, displayNameIndex)) :
                null;
        final String rightText = mobName.length() > displayNameIndex + displayName.length() ?
                resolveText(mobName.substring(displayNameIndex + displayName.length())) :
                null;

        final Component mobNameComponent = nametag.overriddenName == null ?
                getTranslatableComponent(getTranslationKey(livingEntity.getType())) :
                Component.nullToEmpty(nametag.overriddenName);

        Object comp = null;
        try {
            // MutableComponent comp = Component.empty();
            comp = getEmptyComponent();

            if (leftText != null) {
                // comp.append(Component);
                appendComponentMethod.invoke(comp, ((Component[]) this.resolveStringMethod.invoke(this.clazz_CraftChatMessage, leftText))[0]);
            }

            appendComponentMethod.invoke(comp, mobNameComponent);
            if (rightText != null) {
                // comp.append(Component);
                appendComponentMethod.invoke(comp, ((Component[]) this.resolveStringMethod.invoke(this.clazz_CraftChatMessage, rightText))[0]);
            }

        } catch (InvocationTargetException | IllegalAccessException e) {
            e.printStackTrace();
        }

        return comp == null ?
                Optional.empty() : Optional.of((Component) comp);
    }

    private @Nullable Object getEmptyComponent() throws InvocationTargetException, IllegalAccessException {
        if (versionInfo.getMinecraftVersion() >= 1.19) {
            return this.emptyComponentMethod.invoke(null);
        }
        else {
            return this.nullToEmptyMethod.invoke(null, (Object) null);
        }
    }

    private @Nullable Component getTranslatableComponent(final @NotNull String key){
        if (versionInfo.getMinecraftVersion() >= 1.19){
            return Component.translatable(key);
        }
        else {
            Component result = null;
            try {
                result = (Component) this.clazz_TranslatableComponent.getConstructor(String.class).newInstance(key);
            } catch (Exception e) {
                e.printStackTrace();
            }

            return result;
        }
    }

    @SuppressWarnings("deprecation")
    private String getTranslationKey(final org.bukkit.entity.@NotNull EntityType type) {
        return net.minecraft.world.entity.EntityType.byString(type.getName()).map(net.minecraft.world.entity.EntityType::getDescriptionId).orElse(null);
    }

    private @Nullable String resolveText(final @Nullable String text){
        if (text == null || text.isEmpty()) return null;

        String result = text;
        if (text.contains("&#"))
            result = MessageUtils.colorizeHexCodes(text);
        if (text.contains("&"))
            result = MessageUtils.colorizeAll(result);

        return result;
    }

    @NotNull private static SynchedEntityData cloneEntityData(@NotNull final SynchedEntityData other,
        final Entity nmsEntity) {
        final SynchedEntityData entityData = new SynchedEntityData(nmsEntity);
        if (other.getAll() == null) {
            return entityData;
        }

        //noinspection rawtypes
        for (SynchedEntityData.DataItem dataItem : other.getAll())
        {
            //noinspection unchecked
            entityData.define(dataItem.getAccessor(), dataItem.getValue());
        }

        return entityData;
    }

    public String toString() {
        return "Nametags_NMS";
    }
}
