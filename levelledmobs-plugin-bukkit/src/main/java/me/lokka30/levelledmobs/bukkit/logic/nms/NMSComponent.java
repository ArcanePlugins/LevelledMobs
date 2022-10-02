package me.lokka30.levelledmobs.bukkit.logic.nms;

import me.lokka30.levelledmobs.bukkit.LevelledMobs;
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
            ComponentUtils.append(this.component, appendingComponent.getInternalComponent(),
                    LevelledMobs.getInstance().getNmsDefinitions());
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    public static @NotNull NMSComponent empty(){
        NMSComponent nmsComponent = new NMSComponent();
        nmsComponent.component = ComponentUtils.getEmptyComponent(
                LevelledMobs.getInstance().getNmsDefinitions());

        return nmsComponent;
    }

    public static @NotNull NMSComponent translatable(final @NotNull String key){
        return translatable(key, (Object) null);
    }

    public static @NotNull NMSComponent translatable(final @NotNull String key, final @Nullable Object... args){
        NMSComponent nmsComponent = new NMSComponent();
        nmsComponent.component = ComponentUtils.getTranslatableComponent(
                LevelledMobs.getInstance().getNmsDefinitions(), key, args);

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
        if (LevelledMobs.getInstance().getNmsDefinitions().getServerVersionInfo().getIsPaper())
            return livingEntity.getType().translationKey();
        else
            return ComponentUtils.getTranslationKey(livingEntity);
    }
}
