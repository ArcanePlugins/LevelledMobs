package me.lokka30.levelledmobs.nametag;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import me.lokka30.levelledmobs.util.Utils;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

/**
 * @author PenalBuffalo (aka stumper66)
 */
@SuppressWarnings("unused")
public class ServerVersionInfo {

    public ServerVersionInfo() {
        parseBukkitVersion();
        parseNMSVersion();
    }

    private int majorVersion;
    private MinecraftMajorVersion majorVersionEnum;
    private int minorVersion;
    private int revision;
    private double minecraftVersion;
    private Boolean isRunningSpigot;
    private Boolean isRunningPaper;
    private Boolean isRunningFolia;
    private @NotNull String nmsVersion = "unknown";
    private static final Pattern versionPattern =
        Pattern.compile(".*\\.(v\\d+_\\d+_R\\d+)(?:.+)?");
    private static final Pattern versionShortPattern =
        Pattern.compile(".*\\.(v\\d+_\\d+)(?:.+)?");

    private void parseBukkitVersion() {
        final String bukkitVersion = Bukkit.getBukkitVersion();
        // 1.19.2-R0.1-SNAPSHOT --> 1.19.2
        final int firstDash = bukkitVersion.indexOf("-");
        final String[] versions = bukkitVersion.substring(0, firstDash).split("\\.");
        for (int i = 0; i < versions.length; i++) {
            switch (i) {
                case 0 -> this.majorVersion = Integer.parseInt(versions[i]);
                case 1 -> this.minorVersion = Integer.parseInt(versions[i]);
                case 2 -> this.revision = Integer.parseInt(versions[i]);
            }
        }
    }

    private void parseNMSVersion() {
        // example: org.bukkit.craftbukkit.v1_18_R2.CraftServer
        final Matcher nmsRegex = versionPattern.matcher(
            Bukkit.getServer().getClass().getCanonicalName());
        final Matcher nmsShortRegex = versionShortPattern.matcher(
            Bukkit.getServer().getClass().getCanonicalName());

        if (nmsShortRegex.find()) {
            // example: 1.18
            String versionStr = nmsShortRegex
                .group(1).toUpperCase();

            try {
                this.majorVersionEnum = MinecraftMajorVersion.valueOf(versionStr.toUpperCase());
                versionStr = versionStr.replace("_", ".").replace("V", "");
                this.minecraftVersion = Double.parseDouble(versionStr);
            } catch (Exception e) {
                Utils.logger.warning(
                    String.format("Could not extract the minecraft version from '%s'. %s",
                        Bukkit.getServer().getClass().getCanonicalName(), e.getMessage()));
            }
        }

        // example: v1_18_R2
        if (nmsRegex.find()) {
            this.nmsVersion = nmsRegex.group(1);
        } else {
            Utils.logger.warning(
                "NMSHandler: Could not match regex for bukkit version: " + Bukkit.getServer()
                    .getClass().getCanonicalName()
            );
        }
    }

    public boolean getIsRunningSpigot(){
        if (this.isRunningSpigot == null){
            try {
                Class.forName("net.md_5.bungee.api.ChatColor");
                this.isRunningSpigot = true;
            } catch (ClassNotFoundException ignored) {
                this.isRunningSpigot = false;
            }
        }

        return this.isRunningSpigot;
    }

    public boolean getIsRunningPaper(){
        if (this.isRunningPaper == null) {
            try {
                Class.forName("com.destroystokyo.paper.ParticleBuilder");
                this.isRunningPaper = true;
            } catch (ClassNotFoundException ignored) {
                this.isRunningPaper = false;
            }
        }

        return this.isRunningPaper;
    }

    public boolean getIsRunningFolia(){
        if (this.isRunningFolia == null) {
            try {
                Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
                this.isRunningFolia = true;
            } catch (ClassNotFoundException e) {
                this.isRunningFolia = false;
            }
        }
        return this.isRunningFolia;
    }

    public int getMajorVersion() {
        return this.majorVersion;
    }

    public MinecraftMajorVersion getMajorVersionEnum(){
        return this.majorVersionEnum;
    }

    public int getMinorVersion() {
        return this.minorVersion;
    }

    public int getRevision() {
        return this.revision;
    }

    public double getMinecraftVersion() {
        return minecraftVersion;
    }

    public @NotNull String getNMSVersion() {
        return this.nmsVersion;
    }

    public boolean isNMSVersionValid() {
        return !"unknown".equals(this.nmsVersion);
    }

    public String toString() {
        return String.format("%s.%s.%s - %s",
            this.majorVersion, this.minorVersion, this.revision, this.nmsVersion);
    }

    public enum MinecraftMajorVersion{
        V1_16, V1_17, V1_18, V1_19, V1_20
    }
}
