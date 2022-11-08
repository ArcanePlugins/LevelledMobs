package io.github.arcaneplugins.levelledmobs.bukkit.integration.type.entity;

import io.github.arcaneplugins.levelledmobs.bukkit.api.util.TriState;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public interface EntityOwner {

    @NotNull
    TriState ownsEntity(final @NotNull LivingEntity entity);

}
