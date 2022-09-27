package me.lokka30.levelledmobs.nms;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.managers.ExternalCompatibilityManager;
import me.lokka30.levelledmobs.util.Utils;
import me.lokka30.microlib.other.VersionUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Gets the correct NMS version for sending nametag packets
 *
 * @author stumper66
 * @since 3.6.0
 */
public class NMSHandler {

    public NMSHandler(final @NotNull LevelledMobs main) {
        this.main = main;
        this.hasPaper = VersionUtils.isRunningPaper();
        this.versionInfo = new ServerVersionInfo();
    }

    private final LevelledMobs main;
    private NMSUtil currentUtil;
    public boolean isUsingProtocolLib;
    public final ServerVersionInfo versionInfo;
    public final boolean hasPaper;

    @Nullable public NMSUtil getCurrentUtil() {
        if (this.currentUtil != null) {
            return this.currentUtil;
        }

        // supported is paper >= 1.18 or spigot >= 1.19
        // otherwise protocollib is used

        if (hasPaper && versionInfo.getMinecraftVersion() >= 1.18 ||
            !hasPaper && versionInfo.getMinecraftVersion() >= 1.19) {
            // 1.18 and newer we support with direct nms (Paper)
            // or 1.19 spigot and newer
            this.currentUtil = new NametagSender(versionInfo, hasPaper);
            Utils.logger.info(
                String.format("Using NMS version %s for nametag support", versionInfo.getMinecraftVersion()));
        } else if (ExternalCompatibilityManager.hasProtocolLibInstalled()) {
            // we don't directly support this version, use ProtocolLib
            Utils.logger.info(
                "We don't have NMS support for this version of Minecraft, using ProtocolLib");
            this.currentUtil = new ProtocolLibHandler(main);
            this.isUsingProtocolLib = true;
        }
        else{
            Utils.logger.warning(
                    "ProtocolLib is not installed. No nametags will be visible");
        }

        return this.currentUtil;
    }
}
