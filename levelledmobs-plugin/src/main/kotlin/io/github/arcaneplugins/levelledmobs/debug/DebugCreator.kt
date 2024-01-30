package io.github.arcaneplugins.levelledmobs.debug

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import io.github.arcaneplugins.levelledmobs.LevelledMobs
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender

/**
 * Creates debug.zip for troubleshooting purposes
 *
 * @author stumper66
 * @since 3.2.0
 */
@Suppress("DEPRECATION")
object DebugCreator {
    fun createDebug(
        sender: CommandSender
    ) {
        val pluginDir = LevelledMobs.instance.dataFolder.absolutePath
        val srcFiles = mutableListOf(
            "serverinfo.txt", "rules.yml", "settings.yml", "messages.yml", "customdrops.yml",
            Bukkit.getWorldContainer().absolutePath
                .substring(0, Bukkit.getWorldContainer().absolutePath.length - 1) + "logs"
                    + File.separator + "latest.log"
        )
        val serverInfoFile = File(pluginDir, "serverinfo.txt")
        try {
            Files.writeString(
                serverInfoFile.toPath(), generateSystemInfo(),
                StandardCharsets.UTF_8
            )
        } catch (e: IOException) {
            e.printStackTrace()
        }

        var fos: FileOutputStream? = null
        var zipOut: ZipOutputStream? = null
        var fis: FileInputStream? = null
        val zipFile = File(pluginDir, "debug.zip")
        var result = false

        try {
            fos = FileOutputStream(zipFile)
            zipOut = ZipOutputStream(fos)

            for (srcFile: String in srcFiles) {
                val fileToZip = if (srcFile.contains(File.separator)) File(srcFile) else File(pluginDir, srcFile)

                fis = FileInputStream(fileToZip)
                val zipEntry = ZipEntry(fileToZip.name)
                zipOut.putNextEntry(zipEntry)
                val bytes = ByteArray(1024)
                var length: Int
                while ((fis.read(bytes).also { length = it }) >= 0) {
                    zipOut.write(bytes, 0, length)
                }
                fis.close()
                fis = null
            }

            result = true
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            try {
                zipOut?.close()
                fis?.close()
                fos?.close()
            } catch (ignored: Exception) {
            }
        }

        val serverInfo = File(pluginDir, "serverinfo.txt")
        if (serverInfo.exists()) {
            serverInfo.delete()
        }

        if (result) {
            sender.sendMessage("Created file: " + zipFile.absolutePath)
        }
    }

    private fun generateSystemInfo(): String {
        val main = LevelledMobs.instance
        val lmFile = File(
            main.javaClass.protectionDomain
                .codeSource
                .location
                .path.replace("%20", " ")
        )
        var shaDigest: MessageDigest? = null
        val sb = StringBuilder()

        try {
            shaDigest = MessageDigest.getInstance("SHA-256")
        } catch (ignored: java.lang.Exception) {
        }

        sb.append(main.description.name)
        sb.append(" ")
        sb.append(main.description.version)
        sb.append(System.lineSeparator())
        sb.append("file size: ")
        sb.append(String.format("%,d", lmFile.length()))
        sb.append(System.lineSeparator())
        sb.append("sha256 hash: ")
        if (shaDigest != null) {
            sb.append(getFileChecksum(shaDigest, lmFile))
        } else {
            sb.append("(error)")
        }
        sb.append(System.lineSeparator())

        sb.append("server build: ")
        sb.append(Bukkit.getServer().version)
        sb.append(System.lineSeparator())
        sb.append("bukkit version: ")
        sb.append(Bukkit.getBukkitVersion())
        sb.append(System.lineSeparator())
        sb.append("player count: ")
        sb.append(Bukkit.getOnlinePlayers().size)
        sb.append("/")
        sb.append(main.maxPlayersRecorded)
        sb.append(System.lineSeparator())
        sb.append(System.lineSeparator())
        sb.append("plugins:\n")

        val plugins: MutableList<String> = ArrayList(Bukkit.getPluginManager().plugins.size)
        val sbPlugins = StringBuilder()
        for (p in Bukkit.getPluginManager().plugins) {
            sbPlugins.setLength(0)
            if (!p.isEnabled) {
                sbPlugins.append("(disabled) ")
            }

            sbPlugins.append(p.name)
            sbPlugins.append(" ")
            sbPlugins.append(p.description.version)
            sbPlugins.append(" - ")
            sbPlugins.append(p.description.description)

            plugins.add(sbPlugins.toString())
        }

        plugins.sortWith(String.CASE_INSENSITIVE_ORDER)
        for (pluginInfo in plugins) {
            sb.append(pluginInfo)
            sb.append(System.lineSeparator())
        }

        return sb.toString()
    }

    private fun getFileChecksum(
        digest: MessageDigest,
        file: File
    ): String? {
        // taken from https://howtodoinjava.com/java/io/sha-md5-file-checksum-hash/

        val byteArray = ByteArray(1024)
        var bytesCount: Int

        try {
            FileInputStream(file).use { fis ->
                while ((fis.read(byteArray).also { bytesCount = it }) != -1) {
                    digest.update(byteArray, 0, bytesCount)
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
            return null
        }

        val bytes = digest.digest()
        val sb = java.lang.StringBuilder()

        for (aByte in bytes) {
            sb.append(((aByte.toInt() and 0xff) + 0x100).toString(16).substring(1))
        }

        return sb.toString()
    }
}