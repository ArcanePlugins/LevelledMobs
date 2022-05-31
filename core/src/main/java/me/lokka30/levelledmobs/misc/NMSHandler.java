package me.lokka30.levelledmobs.misc;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.managers.ExternalCompatibilityManager;
import me.lokka30.levelledmobs.nametags.NMSUtil;
import me.lokka30.levelledmobs.nametags.Nametags_18_R1;
import me.lokka30.levelledmobs.nametags.Nametags_18_R2;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NMSHandler {
    public NMSHandler(final @NotNull LevelledMobs main){
        this.main = main;
        getBukkitMajorVersion();
    }
    private final LevelledMobs main;
    private static final Pattern versionPattern = Pattern.compile(".*\\.(v\\d+_\\d+_R\\d+)(?:.+)?");
    private @NotNull String nmsVersionString = "unknown";
    private NMSUtil currentUtil;

    private void getBukkitMajorVersion(){
        final Matcher nmsRegex = versionPattern.matcher(Bukkit.getServer().getClass().getCanonicalName());

        // v1_18_R2
        if (nmsRegex.find())
            nmsVersionString = nmsRegex.group(1);
        else
            Utils.logger.warning("NMSHandler: Could not match regex for bukkit version: " + Bukkit.getServer().getClass().getCanonicalName());
    }

    @Nullable
    public NMSUtil getCurrentUtil() {
        if (this.currentUtil != null)
            return this.currentUtil;

        // TODO: add 1.19 once available

        if ("v1_18_R2".equalsIgnoreCase(nmsVersionString))
            this.currentUtil = new Nametags_18_R2();
        else if ("v1_18_R1".equalsIgnoreCase(nmsVersionString))
            this.currentUtil = new Nametags_18_R1();
        else if (ExternalCompatibilityManager.hasProtocolLibInstalled()){
            // we don't directly support this version, use ProtocolLib
            Utils.logger.info("We don't have NMS support for this version of Minecraft, using ProtocolLib");
            this.currentUtil = new ProtocolLibHandler(main);
        }

        return this.currentUtil;
    }
}
