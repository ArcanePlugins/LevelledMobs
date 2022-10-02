package me.lokka30.levelledmobs.bukkit.integration.type.entity;

import me.lokka30.levelledmobs.bukkit.util.TriState;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public interface EntityOwner {

    @NotNull
    TriState ownsEntity(final @NotNull LivingEntity entity);

}
