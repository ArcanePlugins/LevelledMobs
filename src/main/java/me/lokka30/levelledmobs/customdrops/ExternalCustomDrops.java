package me.lokka30.levelledmobs.customdrops;

import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public interface ExternalCustomDrops {
    void addCustomDrop(final @NotNull EntityType entityType, final @NotNull CustomDropInstance customDropInstance);

    void addCustomDropTable(final @NotNull String dropName, final @NotNull CustomDropInstance customDropInstance);

    @NotNull Map<EntityType, CustomDropInstance> getCustomDrops();

    @NotNull Map<String, CustomDropInstance> getCustomDropTables();

    void clearAllExternalCustomDrops();
}
