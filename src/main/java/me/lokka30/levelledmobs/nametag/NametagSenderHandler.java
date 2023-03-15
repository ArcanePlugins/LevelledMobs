package me.lokka30.levelledmobs.nametag;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.managers.ExternalCompatibilityManager;
import me.lokka30.levelledmobs.util.Utils;
import me.lokka30.microlib.other.VersionUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Manages the ideal nametag sender implementation for the server's version
 *
 * @author PenalBuffalo (aka stumper66)
 * @since 3.6.0
 */
public class NametagSenderHandler {

    public NametagSenderHandler(
        final @NotNull LevelledMobs main
    ) {
        this.main = main;
        this.hasPaper = VersionUtils.isRunningPaper();
        this.versionInfo = new ServerVersionInfo();
    }

    private final LevelledMobs main;
    private NametagSender currentUtil;
    public boolean isUsingProtocolLib;
    public final ServerVersionInfo versionInfo;
    public final boolean hasPaper;

    @Nullable
    public NametagSender getCurrentUtil() {
        if (this.currentUtil != null) {
            return this.currentUtil;
        }

        // supported is spigot >= 1.17
        // otherwise protocollib is used

        if (versionInfo.getMinecraftVersion() >= 1.17) {
            // 1.18 and newer we support with direct nms (Paper)
            // or 1.19 spigot and newer
            this.currentUtil = new NmsNametagSender();

            Utils.logger.info(
                String.format("Using NMS version %s for nametag support",
                    versionInfo.getNMSVersion())
            );

        } else if (ExternalCompatibilityManager.hasProtocolLibInstalled()) {
            // we don't directly support this version, use ProtocolLib
            Utils.logger.info(
                "We don't have NMS support for this version of Minecraft, using ProtocolLib");

            this.currentUtil = new ProtocolLibNametagSender(main);
            this.isUsingProtocolLib = true;
        } else {
            Utils.logger.warning("ProtocolLib is not installed. No nametags will be visible");
        }

        return this.currentUtil;
    }

    public void refresh(){
        if (this.currentUtil instanceof final NmsNametagSender sender){
            sender.refresh();
        }
    }
}
