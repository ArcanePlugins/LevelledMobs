package me.lokka30.levelledmobs.plugin.bukkit.data.mob;

import java.util.Locale;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

public enum Flag {

    FROM_MOB_SPAWNER(PersistentDataType.BYTE);

    private final String id;
    private final PersistentDataType<?, ?> dataType;

    @NotNull
    public String getId() { return id; }

    @NotNull
    public PersistentDataType<?, ?> getDataType() { return dataType; }

    Flag(final @NotNull String id, final @NotNull PersistentDataType<?, ?> dataType) {
        this.id = id;
        this.dataType = dataType;
    }

    Flag(final @NotNull PersistentDataType<?, ?> dataType) {
        this.id = this.name()
            .toLowerCase(Locale.ROOT)
            .replace("_", "-");
        this.dataType = dataType;
    }
}
