package me.lokka30.levelledmobs.nms;

import me.lokka30.levelledmobs.nms.nametag.NametagNMSHandler;
import me.lokka30.levelledmobs.nms.nametag.NametagNMSHandler_1_17_R1;
import me.lokka30.levelledmobs.nms.nametag.NametagNMSHandler_1_18_R1;
import me.lokka30.levelledmobs.util.Utils;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NMSHandler {

    private static final Pattern versionPattern = Pattern.compile(".*\\.(v\\d+_\\d+_R\\d+)(?:.+)?");
    private @NotNull
    String nmsVersionString = "unknown";

    private void getBukkitMajorVersion() {
        final Matcher nmsRegex = versionPattern.matcher(
            Bukkit.getServer().getClass().getCanonicalName());

        // v1_18_R1
        if (nmsRegex.find()) {
            nmsVersionString = nmsRegex.group(1);
        } else {
            Utils.LOGGER.warning(
                "Could not match regex for bukkit version: " + Bukkit.getServer().getClass()
                    .getCanonicalName());
        }
    }

    @NotNull
    public NametagNMSHandler getNametagNMSHandler() {
        switch (nmsVersionString.toUpperCase(Locale.ROOT)) {
            case "V1_18_R1":
                return new NametagNMSHandler_1_18_R1();
            case "V1_17_R1":
                return new NametagNMSHandler_1_17_R1();
            default:
                throw new UnsupportedOperationException(
                    "Unsupported Bukkit version: " + nmsVersionString);

        }
    }
}
