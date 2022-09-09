package me.lokka30.levelledmobs.nms;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;

import me.lokka30.levelledmobs.result.NametagResult;
import me.lokka30.levelledmobs.util.Utils;
import me.lokka30.microlib.messaging.MessageUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
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

    public NametagSender(final String nmsVersion) {
        this.nmsVersion = nmsVersion;
        boolean hasKiori = false;
        try {
            Class.forName("net.kyori.adventure.text.Component");
            hasKiori = true;
        } catch (ClassNotFoundException ignored) { }

        this.hasKiori = hasKiori;
    }

    private final String nmsVersion;
    private final boolean hasKiori;

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

        if (displayNameIndex < 0)
            return Optional.of(Component.literal(nametag.getNametagNonNull()));

        final String leftText = displayNameIndex > 0 ?
                resolveText(mobName.substring(0, displayNameIndex)) :
                null;
        final String rightText = mobName.length() > displayNameIndex + displayName.length() ?
                resolveText(mobName.substring(displayNameIndex + displayName.length())) :
                null;
        final Component mobNameComponent = nametag.overriddenName == null ?
                Component.translatable(livingEntity.getType().translationKey()) :
                Component.literal(nametag.overriddenName);

        MutableComponent comp = Component.empty();
        try {
            final Class<?> clazz_CraftChatMessage = Class.forName(
                    "org.bukkit.craftbukkit." + nmsVersion + ".util.CraftChatMessage");

            final Method method_fromString = clazz_CraftChatMessage.getDeclaredMethod("fromString",
                    String.class);

            if (leftText != null) {
                comp = comp.append(((Component[]) method_fromString.invoke(clazz_CraftChatMessage, leftText))[0]);
            }
            comp.append(mobNameComponent);
            if (rightText != null) {
                comp = comp.append(((Component[]) method_fromString.invoke(clazz_CraftChatMessage, rightText))[0]);
            }

        } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            e.printStackTrace();
        }

        return Optional.of(comp);
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
