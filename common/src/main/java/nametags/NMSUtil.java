package nametags;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface NMSUtil {
    void sendNametag(
            final @NotNull LivingEntity livingEntity,
            @Nullable String nametag,
            @NotNull Player player,
            final boolean alwaysVisible
    );
}
