package io.github.arcaneplugins.levelledmobs.api;

import io.github.arcaneplugins.levelledmobs.LevelInterface;
import io.github.arcaneplugins.levelledmobs.enums.LevellableState;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
public class Main implements LevelInterface {
    private static Main instance;
    private final LevelInterface lmInterface;

    public Main() throws Exception {
        instance = this;
        final Plugin plugin = Bukkit.getPluginManager().getPlugin("LevelledMobs");

        if (plugin == null)
            throw new Exception("LevelledMobs doesn't appear to be installed");

        if (!plugin.isEnabled())
            throw new Exception("LevelledMobs is not enabled");

        this.lmInterface = (LevelInterface) plugin;
    }

    public static @NotNull Main getInstance(){
        return instance;
    }

    @Override
    public @NotNull LevellableState getLevellableState(@NotNull final LivingEntity livingEntity) {
        return lmInterface.getLevellableState(livingEntity);
    }

    @Override
    public boolean isLevelled(@NotNull final LivingEntity livingEntity) {
        return lmInterface.isLevelled(livingEntity);
    }

    @Override
    public int getLevelOfMob(@NotNull final LivingEntity livingEntity) {
        return lmInterface.getLevelOfMob(livingEntity);
    }

    @Override
    public void removeLevel(@NotNull final LivingEntity livingEntity) {
        lmInterface.removeLevel(livingEntity);
    }

    @Override
    public @Nullable String getMobNametag(@NotNull final LivingEntity livingEntity) {
        return lmInterface.getMobNametag(livingEntity);
    }
}