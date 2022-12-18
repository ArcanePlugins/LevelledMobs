package io.github.arcaneplugins.levelledmobs.bukkit.util.nms;

import io.github.arcaneplugins.levelledmobs.bukkit.LevelledMobs;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
public class NMSComponent {
    private Object component;

    public void append(final @Nullable NMSComponent appendingComponent){
        if (appendingComponent == null) return;
        if (this.component == null){
            this.component = appendingComponent;
            return;
        }

        try{
            ComponentUtils.append(this.component, appendingComponent.getInternalComponent());
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    public static @NotNull NMSComponent empty(){
        NMSComponent nmsComponent = new NMSComponent();
        nmsComponent.component = ComponentUtils.getEmptyComponent();

        return nmsComponent;
    }

    public static @NotNull NMSComponent translatable(final @NotNull String key){
        return translatable(key, (Object) null);
    }

    public static @NotNull NMSComponent translatable(final @NotNull String key, final @Nullable Object... args){
        NMSComponent nmsComponent = new NMSComponent();
        nmsComponent.component = ComponentUtils.getTranslatableComponent(
                key, args);

        return nmsComponent;
    }

    public @Nullable Object getInternalComponent(){
        return this.component;
    }

    public @NotNull String getComponentType(){
        return this.component == null ?
                "null" : this.component.getClass().getTypeName();
    }

    public static @NotNull String getTranslationKey(final @NotNull LivingEntity livingEntity){
        final Definitions def = LevelledMobs.getInstance().getNmsDefinitions();
        if (def.getServerVersionInfo().getIsPaper())
            return livingEntity.getType().translationKey();
        else
            return def.getTranslationKey(livingEntity);
    }
}
