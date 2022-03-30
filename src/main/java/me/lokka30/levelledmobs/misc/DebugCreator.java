package me.lokka30.levelledmobs.misc;

import me.lokka30.levelledmobs.LevelledMobs;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Creates debug.zip for troubleshooting purposes
 *
 * @author stumper66
 * @since 3.2.0
 */

public class DebugCreator {
    public static void createDebug(final @NotNull LevelledMobs main, final CommandSender sender){
        final String pluginDir = main.getDataFolder().getAbsolutePath();
        final List<String> srcFiles = List.of(
                "serverinfo.txt", "rules.yml", "settings.yml", "messages.yml", "customdrops.yml",
                Bukkit.getWorldContainer().getAbsolutePath().substring(0, Bukkit.getWorldContainer().getAbsolutePath().length() - 1) + "logs" + File.separator + "latest.log");
        final File serverInfoFile = new File(pluginDir, "serverinfo.txt");
        try{
            Files.writeString(serverInfoFile.toPath(), generateSystemInfo(main), StandardCharsets.UTF_8);
        }
        catch (final IOException e){
            e.printStackTrace();
        }

        FileOutputStream fos = null;
        ZipOutputStream zipOut = null;
        FileInputStream fis = null;
        final File zipFile = new File(pluginDir, "debug.zip");
        boolean result = false;

        try {
            fos = new FileOutputStream(zipFile);
            zipOut = new ZipOutputStream(fos);

            for (final String srcFile : srcFiles) {
                final File fileToZip = srcFile.contains(File.separator) ?
                        new File(srcFile) : new File(pluginDir, srcFile);

                fis = new FileInputStream(fileToZip);
                final ZipEntry zipEntry = new ZipEntry(fileToZip.getName());
                zipOut.putNextEntry(zipEntry);
                final byte[] bytes = new byte[1024];
                int length;
                while ((length = fis.read(bytes)) >= 0) {
                    zipOut.write(bytes, 0, length);
                }
                fis.close();
                fis = null;
            }

            result = true;
        }
        catch (final IOException e){
            e.printStackTrace();
        }
        finally {
            try {
                if (zipOut != null) zipOut.close();
                if (fis != null) fis.close();
                if (fos != null) fos.close();
            }
            catch (final Exception ignored) {}
        }

        final File serverInfo = new File(pluginDir, "serverinfo.txt");
        if (serverInfo.exists()) //noinspection ResultOfMethodCallIgnored
            serverInfo.delete();

        if (result)
            sender.sendMessage("Created file: " + zipFile.getAbsolutePath());
    }

    @NotNull
    private static String generateSystemInfo(final @NotNull LevelledMobs main){
        final File lmFile = new File(main.getClass().getProtectionDomain()
                .getCodeSource()
                .getLocation()
                .getPath().replace("%20", " "));
        MessageDigest shaDigest = null;
        final StringBuilder sb = new StringBuilder();

        try{
            shaDigest = MessageDigest.getInstance("SHA-256");
        }
        catch (final Exception ignored) {}

        sb.append(main.getDescription().getName());
        sb.append(" ");
        sb.append(main.getDescription().getVersion());
        sb.append(System.lineSeparator());
        sb.append("file size: ");
        sb.append(String.format("%,d", lmFile.length()));
        sb.append(System.lineSeparator());
        sb.append("sha256 hash: ");
        if (shaDigest != null)
            sb.append(getFileChecksum(shaDigest, lmFile));
        else
            sb.append("(error)");
        sb.append(System.lineSeparator());

        sb.append("server build: ");
        sb.append(Bukkit.getServer().getVersion());
        sb.append(System.lineSeparator());
        sb.append("bukkit version: ");
        sb.append(Bukkit.getBukkitVersion());
        sb.append(System.lineSeparator());
        sb.append("player count: ");
        sb.append(Bukkit.getOnlinePlayers().size());
        sb.append("/");
        sb.append(main.maxPlayersRecorded);
        sb.append(System.lineSeparator());
        sb.append(System.lineSeparator());
        sb.append("plugins:\n");

        final List<String> plugins = new ArrayList<>(Bukkit.getPluginManager().getPlugins().length);
        final StringBuilder sbPlugins = new StringBuilder();
        for (final Plugin p : Bukkit.getPluginManager().getPlugins()){
            sbPlugins.setLength(0);
            if (!p.isEnabled())
                sbPlugins.append("(disabled) ");

            sbPlugins.append(p.getName());
            sbPlugins.append(" ");
            sbPlugins.append(p.getDescription().getVersion());
            sbPlugins.append(" - ");
            sbPlugins.append(p.getDescription().getDescription());

            plugins.add(sbPlugins.toString());
        }

        plugins.sort(String.CASE_INSENSITIVE_ORDER);
        for (final String pluginInfo : plugins) {
            sb.append(pluginInfo);
            sb.append(System.lineSeparator());
        }

        return sb.toString();
    }

    @Nullable
    private static String getFileChecksum(final MessageDigest digest, final File file) {
        // taken from https://howtodoinjava.com/java/io/sha-md5-file-checksum-hash/

        final byte[] byteArray = new byte[1024];
        int bytesCount;

        try (final FileInputStream fis = new FileInputStream(file)){
            while ((bytesCount = fis.read(byteArray)) != -1) {
                digest.update(byteArray, 0, bytesCount);
            }
        }
        catch (final IOException e) {
            e.printStackTrace();
            return null;
        }

        final byte[] bytes = digest.digest();
        final StringBuilder sb = new StringBuilder();

        for (final byte aByte : bytes) sb.append(Integer.toString((aByte & 0xff) + 0x100, 16).substring(1));

        return sb.toString();
    }
}
