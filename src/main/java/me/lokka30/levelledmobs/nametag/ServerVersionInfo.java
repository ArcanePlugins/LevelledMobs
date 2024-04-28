package me.lokka30.levelledmobs.nametag;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import me.lokka30.levelledmobs.util.MessageUtils;
import me.lokka30.levelledmobs.util.Utils;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

/**
 * Holds various parsed data on the server verion
 * that the server is running
 *
 * @author stumper66
 * @since 3.10.3
 */
@SuppressWarnings("unused")
public class ServerVersionInfo {

    public ServerVersionInfo() {
        parseServerVersion();
    }

    private int majorVersion;
    private MinecraftMajorVersion majorVersionEnum;
    private int minorVersion;
    private int revision;
    private double minecraftVersion;
    // preliminary fabric support. not entirely there yet
    private Boolean isRunningFabric;
    private Boolean isRunningSpigot;
    private Boolean isRunningPaper;
    private Boolean isRunningFolia;
    private @NotNull String nmsVersion = "unknown";
    private static final Pattern versionPattern =
        Pattern.compile(".*\\.(v\\d+_\\d+_R\\d+)(?:.+)?");
    private static final Pattern versionShortPattern =
        Pattern.compile(".*\\.(v\\d+_\\d+)(?:.+)?");

    private void parseServerVersion(){
        if (getIsRunningPaper())
            parsePaperVersion();

        final boolean isOneTwentyFiveOrNewer =
                getMinorVersion() == 20 && getRevision() >= 5 ||
                        getMinorVersion() >= 21;

        if (!getIsRunningPaper() || !isOneTwentyFiveOrNewer) {
            parseNMSVersion();
            parseBukkitVersion();
        }
    }

    private void parsePaperVersion(){
        final String minecraftVersion = Bukkit.getServer().getMinecraftVersion();
        // 1.20.4
        String[] versions = minecraftVersion.split("\\.");
        for (int i = 0; i < versions.length; i++) {
            switch (i) {
                case 0 -> this.majorVersion = Integer.parseInt(versions[i]);
                case 1 -> this.minorVersion = Integer.parseInt(versions[i]);
                case 2 -> this.revision = Integer.parseInt(versions[i]);
            }
        }

        this.majorVersionEnum =
                MinecraftMajorVersion.valueOf(String.format("V%s_%s", majorVersion, minorVersion));
        this.minecraftVersion = Double.parseDouble(majorVersion + "." + minorVersion);
    }

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
        if (getIsRunningFabric()) return;
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
                Bukkit.getLogger().warning(
                    String.format("Could not extract the minecraft version from '%s'. %s",
                        Bukkit.getServer().getClass().getCanonicalName(), e.getMessage()));
            }
        }

        // example: v1_18_R2
        if (nmsRegex.find()) {
            this.nmsVersion = nmsRegex.group(1);
        } else {
            Bukkit.getLogger().warning(
                "LevelledMobs: NMSHandler: Could not match regex for bukkit version: " + Bukkit.getServer()
                    .getClass().getCanonicalName()
            );
        }
    }

    public boolean getIsRunningFabric(){
        if (this.isRunningFabric == null){
            try {
                Class.forName("net.fabricmc.loader.api.FabricLoader");
                this.isRunningFabric = true;
            } catch (ClassNotFoundException ignored) {
                this.isRunningFabric = false;
            }
        }

        return this.isRunningFabric;
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

    /**
     * @return The first digit of the version (1.20.3 would return 1)
     */
    public int getMajorVersion() {
        return this.majorVersion;
    }

    /**
     * @return An enum of the Minecraft version
     */
    public MinecraftMajorVersion getMajorVersionEnum(){
        return this.majorVersionEnum;
    }

    /**
     * @return The second digit of the version (1.20.3 would return 20)
     */
    public int getMinorVersion() {
        return this.minorVersion;
    }

    /**
     * @return The last digit of the version (1.20.3 would return 3)
     */
    public int getRevision() {
        return this.revision;
    }

    /**
     * @return A double representing the last 2 digits of the version (1.20.3 would return 20.3)
     */
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
        V1_16, V1_17, V1_18, V1_19, V1_20, V1_21
    }
}
