package me.lokka30.levelledmobs.nms;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.managers.ExternalCompatibilityManager;
import me.lokka30.levelledmobs.util.Utils;
import org.bukkit.Bukkit;
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
        parseBukkitVersion();
    }

    private final LevelledMobs main;
    private static final Pattern versionPattern = Pattern.compile(".*\\.(v\\d+_\\d+_R\\d+)(?:.+)?");
    private static final Pattern versionShortPattern = Pattern.compile(".*\\.(v\\d+_\\d+)(?:.+)?");
    private @NotNull String nmsVersionString = "unknown";
    private NMSUtil currentUtil;
    public double minecraftVersion;

    private void parseBukkitVersion() {
        // example: org.bukkit.craftbukkit.v1_18_R2.CraftServer
        final Matcher nmsRegex = versionPattern.matcher(
            Bukkit.getServer().getClass().getCanonicalName());
        final Matcher nmsShortRegex = versionShortPattern.matcher(
            Bukkit.getServer().getClass().getCanonicalName());

        if (nmsShortRegex.find()) {
            // example: 1.18
            final String versionStr = nmsShortRegex.group(1).replace("_", ".").replace("v", "");
            try {
                minecraftVersion = Double.parseDouble(versionStr);
            } catch (Exception e) {
                Utils.logger.warning(
                    String.format("Could not extract the minecraft version from '%s'. %s",
                        Bukkit.getServer().getClass().getCanonicalName(), e.getMessage()));
            }
        }

        // example: v1_18_R2
        if (nmsRegex.find()) {
            nmsVersionString = nmsRegex.group(1);
        } else {
            Utils.logger.warning(
                "NMSHandler: Could not match regex for bukkit version: " + Bukkit.getServer()
                    .getClass().getCanonicalName());
        }
    }

    @Nullable public NMSUtil getCurrentUtil() {
        if (this.currentUtil != null) {
            return this.currentUtil;
        }

        if (this.minecraftVersion >= 1.18) {
            // 1.18 and newer we support with direct nms
            this.currentUtil = new NametagSender(nmsVersionString);
            Utils.logger.info(
                String.format("Using NMS version %s for nametag support", nmsVersionString));
        } else if (ExternalCompatibilityManager.hasProtocolLibInstalled()) {
            // we don't directly support this version, use ProtocolLib
            Utils.logger.info(
                "We don't have NMS support for this version of Minecraft, using ProtocolLib");
            this.currentUtil = new ProtocolLibHandler(main);
        }
        else{
            Utils.logger.warning(
                    "ProtocolLib is not installed. No nametags will be visible");
        }

        return this.currentUtil;
    }
}
