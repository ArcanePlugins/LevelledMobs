package me.lokka30.levelledmobs.nametag;

import java.util.Objects;
import me.lokka30.levelledmobs.LevelledMobs;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author PenalBuffalo (aka stumper66)
 */
public class ComponentUtils {

    public static void appendComponents(
        final @NotNull Object component,
        final @Nullable Object appendingComponent
    ) {
        if (appendingComponent == null) {
            return;
        }

        final Definitions def = LevelledMobs.getInstance().getDefinitions();

        try {
            def.method_ComponentAppend.invoke(component, appendingComponent);
        } catch (final Exception ex) {
            ex.printStackTrace();
        }
    }

    public static @NotNull Object getEmptyComponent() {
        return Objects.requireNonNull(
            getTextComponent(null),
            "text component cannot be null"
        );
    }

    public static @Nullable Object getTextComponent(
        final @Nullable String text
    ) {
        final Definitions def = LevelledMobs.getInstance().getDefinitions();
        try {
            if (text == null && def.getServerVersionInfo().getMinecraftVersion() >= 1.19) {
                // #empty()
                return def.method_EmptyComponent.invoke(null);
            } else {
                // #nullToEmpty(text)
                return def.method_TextComponent.invoke(null, text);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static @Nullable Object getTranslatableComponent(
        final @NotNull String key
    ) {
        return getTranslatableComponent(key, (Object) null);
    }

    public static @Nullable Object getTranslatableComponent(
        final @NotNull String key,
        final @Nullable Object... args
    ) {
        final Definitions def = LevelledMobs.getInstance().getDefinitions();

        try {
            if (def.getServerVersionInfo().getMinecraftVersion() >= 1.19d) {
                if (args == null || args.length == 0) {
                    return def.method_Translatable.invoke(null, key);
                } else {
                    return def.method_TranslatableWithArgs.invoke(null, key, args);
                }
            } else {
                if (args == null || args.length == 0) {
                    return def.clazz_TranslatableComponent
                        .getConstructor(String.class)
                        .newInstance(key);
                } else {
                    return def.clazz_TranslatableComponent
                        .getConstructor(String.class, Object[].class)
                        .newInstance(key, args);
                }
            }
        } catch (final Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }
}
