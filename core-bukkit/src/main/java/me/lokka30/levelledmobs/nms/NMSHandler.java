package me.lokka30.levelledmobs.nms;

import me.lokka30.levelledmobs.nms.nametag.NametagNMSHandler;
import me.lokka30.levelledmobs.nms.nametag.NametagNMSHandler_1_17_R1;
import me.lokka30.levelledmobs.nms.nametag.NametagNMSHandler_1_18_R1;
import me.lokka30.levelledmobs.util.Utils;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NMSHandler {

    public NMSHandler() {

    }

    private static final Pattern versionPattern = Pattern.compile(".*\\.(v\\d+_\\d+_R\\d+)(?:.+)?");
    private @NotNull String nmsVersionString = "unknown";

    private void getBukkitMajorVersion(){
        final Matcher nmsRegex = versionPattern.matcher(Bukkit.getServer().getClass().getCanonicalName());

        // v1_18_R1
        if (nmsRegex.find()) {
            nmsVersionString = nmsRegex.group(1);
        } else {
            Utils.LOGGER.warning("Could not match regex for bukkit version: " + Bukkit.getServer().getClass().getCanonicalName());
        }
    }

    @NotNull
    public NametagNMSHandler getCurrentUtil() {
        if ("v1_18_R1".equalsIgnoreCase(nmsVersionString)) {
            return new NametagNMSHandler_1_18_R1();
        } else if ("v1_17_R1".equalsIgnoreCase(nmsVersionString)) {
            return new NametagNMSHandler_1_17_R1();
        }
        throw new UnsupportedOperationException("Unsupported Bukkit version: " + nmsVersionString);
    }
}
