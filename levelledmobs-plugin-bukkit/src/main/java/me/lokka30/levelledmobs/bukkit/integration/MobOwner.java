package me.lokka30.levelledmobs.bukkit.integration;

import me.lokka30.levelledmobs.bukkit.util.TriState;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public interface MobOwner {

    @NotNull
    TriState ownsMob(final @NotNull LivingEntity entity);

}
