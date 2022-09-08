package me.lokka30.levelledmobs.nms;

import me.lokka30.levelledmobs.result.NametagResult;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;

public class KyoriNametags {
    public static @NotNull net.minecraft.network.chat.Component generateComponent(
            final @NotNull LivingEntity livingEntity, final @NotNull NametagResult nametagResult){

        final String nametag = nametagResult.getNametagNonNull();
        final String mobKey = livingEntity.getType().translationKey();

        // this component holds the component of the mob name and will show the translated name on clients
        final net.kyori.adventure.text.Component mobNameComponent = nametagResult.overriddenName == null ?
                net.kyori.adventure.text.Component.translatable(mobKey) :
                LegacyComponentSerializer.legacyAmpersand().deserialize(nametagResult.overriddenName);

        // replace placeholders and set the new death message
        final Component result = LegacyComponentSerializer.legacyAmpersand().deserialize(nametag)
                .replaceText(TextReplacementConfig.builder()
                        .matchLiteral("{DisplayName}").replacement(mobNameComponent).build());

        // PaperAdventure.asVanilla(kyoriComponent)
        try{
            final Class<?> clazz = Class.forName("io.papermc.paper.adventure.PaperAdventure");
            final Method asVanilla = clazz.getDeclaredMethod("asVanilla", Component.class);
            final Object mcComponent = asVanilla.invoke(clazz, result);
            return (net.minecraft.network.chat.Component) mcComponent;
        }
        catch (Exception e){
            e.printStackTrace();
        }

        return net.minecraft.network.chat.Component.empty();
    }
}
