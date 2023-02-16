package me.lokka30.levelledmobs.nametag;

import java.lang.reflect.Method;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.result.NametagResult;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

/**
 * @author PenalBuffalo (aka stumper66)
 */
public class KyoriNametags {

    public static @NotNull Object generateComponent(
        final @NotNull LivingEntity livingEntity,
        final @NotNull NametagResult nametagResult
    ) {

        final String nametag = nametagResult.getNametagNonNull();
        final String mobKey = livingEntity.getType().translationKey();

        // this component holds the component of the mob name and will show the translated name on clients
        net.kyori.adventure.text.Component mobNameComponent;
        if (nametagResult.overriddenName == null){
            if (LevelledMobs.getInstance().getDefinitions().useTranslationComponents){
                mobNameComponent = net.kyori.adventure.text.Component.translatable(mobKey);
            }
            else{
                mobNameComponent = net.kyori.adventure.text.Component.text(livingEntity.getName());
            }
        }
        else{
            mobNameComponent = LegacyComponentSerializer
                    .legacyAmpersand()
                    .deserialize(nametagResult.overriddenName);
        }

        // replace placeholders and set the new death message
        final Component result = LegacyComponentSerializer
            .legacyAmpersand()
            .deserialize(nametag)
            .replaceText(
                TextReplacementConfig.builder()
                    .matchLiteral("{DisplayName}")
                    .replacement(mobNameComponent).build()
            );

        // PaperAdventure.asVanilla(kyoriComponent)
        try {
            final Class<?> clazz = Class
                .forName("io.papermc.paper.adventure.PaperAdventure");

            final Method asVanilla = clazz
                .getDeclaredMethod("asVanilla", Component.class);

            return asVanilla.invoke(clazz, result);
        } catch (final Exception ex) {
            ex.printStackTrace();
        }

        return ComponentUtils.getEmptyComponent();
    }
}
