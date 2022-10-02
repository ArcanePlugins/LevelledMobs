package me.lokka30.levelledmobs.bukkit.logic.nms;

import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;

public class ComponentUtils {
    public static void append(final @NotNull Object component, final @Nullable Object appendingComponent,
                              final @NotNull Definitions definitions) throws Exception {
        if (appendingComponent == null) return;

        if (component.getClass() != definitions.clazz_IChatMutableComponent){
            throw new Exception("Invalid type: " + component.getClass().getName());
        }
        if (appendingComponent.getClass() != definitions.clazz_IChatMutableComponent){
            throw new Exception("Invalid type: " + appendingComponent.getClass().getName());
        }

        definitions.method_ComponentAppend.invoke(component, appendingComponent);
    }

    public static @Nullable Object getEmptyComponent(final @NotNull Definitions definitions){
        return getTextComponent(null, definitions);
    }

    public static @Nullable Object getTextComponent(final @Nullable String text, final @NotNull Definitions definitions){
        try {
            if (text == null && definitions.getServerVersionInfo().getMinecraftVersion() >= 1.19) {
                // #empty()
                return definitions.method_EmptyComponent.invoke(null);
            } else {
                // #nullToEmpty(text)
                return definitions.method_TextComponent.invoke(null, text);
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }

        return null;
    }

    public static @Nullable Object getTranslatableComponent(
            final @NotNull Definitions definitions, final @NotNull String key){
        return getTranslatableComponent(definitions, key, (Object) null);
    }

    public static @Nullable Object getTranslatableComponent(
            final @NotNull Definitions definitions, final @NotNull String key, Object... args){
        try {
            if (definitions.getServerVersionInfo().getMinecraftVersion() >= 1.19){
                if (args == null || args.length == 0) {
                    return definitions.method_Translatable.invoke(key);
                }
                else {
                    return definitions.method_TranslatableWithArgs.invoke(key, args);
                }
            }
            else{
                if (args == null || args.length == 0) {
                    return definitions.clazz_TranslatableComponent.getConstructor(String.class).newInstance(key);
                }
                else {
                    return definitions.clazz_TranslatableComponent.getConstructor(
                            String.class, Object[].class).newInstance(key, args);
                }
            }
        } catch (IllegalAccessException | InvocationTargetException | InstantiationException | NoSuchMethodException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static @NotNull String getTranslationKey(final @NotNull LivingEntity livingEntity){
        // only needed for spigot. paper has a built-in method
        // TODO: implement the method below in reflection
        //return net.minecraft.world.entity.EntityType.byString(type.getName()).map(net.minecraft.world.entity.EntityType::getDescriptionId).orElse(null);
        return "";
    }
}
