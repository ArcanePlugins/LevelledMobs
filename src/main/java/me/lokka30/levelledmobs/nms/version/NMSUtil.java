package me.lokka30.levelledmobs.nms.version;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/*
TODO
    - Add modules for each nms version.
    - Javadocs.
 */
public interface NMSUtil {

    void sendNametag(final @NotNull LivingEntity livingEntity, final @NotNull Player target, final @NotNull String nametag);

}
