package io.github.arcaneplugins.levelledmobs.bukkit.util.nms;

import io.github.arcaneplugins.levelledmobs.bukkit.LevelledMobs;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ComponentUtils {
    public static void append(
            final @NotNull Object component,
            final @Nullable Object appendingComponent
    ) {
        if (appendingComponent == null) return;
        final Definitions def = LevelledMobs.getInstance().getNmsDefinitions();

        try {
            if (component.getClass() != def.clazz_IChatMutableComponent) {
                throw new Exception("Invalid type: " + component.getClass().getName());
            }

            if (appendingComponent.getClass() != def.clazz_IChatMutableComponent){
                throw new Exception("Invalid type: " + appendingComponent.getClass().getName());
            }

            def.method_ComponentAppend.invoke(component, appendingComponent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static @NotNull Object getEmptyComponent() {
        final Object result = getTextComponent(null);
        assert result != null;
        return result;
    }

    public static @Nullable Object getTextComponent(
            final @Nullable String text
    ) {
        final Definitions def = LevelledMobs.getInstance().getNmsDefinitions();
        try {
            if (text == null && def.getServerVersionInfo().getMinecraftVersion() >= 1.19) {
                // #empty()
                return def.method_EmptyComponent.invoke(null);
            } else {
                // #nullToEmpty(text)
                return def.method_TextComponent.invoke(null, text);
            }
        } catch (Exception e){
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
        final Definitions def = LevelledMobs.getInstance().getNmsDefinitions();
        try {
            if (def.getServerVersionInfo().getMinecraftVersion() >= 1.19){
                if (args == null || args.length == 0) {
                    return def.method_Translatable.invoke(null, key);
                }
                else {
                    return def.method_TranslatableWithArgs.invoke(null, key, args);
                }
            }
            else{
                if (args == null || args.length == 0) {
                    return def.clazz_TranslatableComponent.getConstructor(String.class).newInstance(key);
                }
                else {
                    return def.clazz_TranslatableComponent.getConstructor(
                            String.class, Object[].class).newInstance(key, args);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
