package io.github.arcaneplugins.levelledmobs.commands.subcommands

import java.lang.reflect.Modifier
import java.util.TreeSet
import io.github.arcaneplugins.levelledmobs.LevelledMobs
import io.github.arcaneplugins.levelledmobs.commands.MessagesBase
import io.github.arcaneplugins.levelledmobs.misc.NamespacedKeys
import io.github.arcaneplugins.levelledmobs.nametag.ServerVersionInfo
import io.github.arcaneplugins.levelledmobs.rules.DoNotMerge
import io.github.arcaneplugins.levelledmobs.util.MessageUtils.colorizeAll
import io.github.arcaneplugins.levelledmobs.util.PaperUtils
import io.github.arcaneplugins.levelledmobs.util.SpigotUtils
import io.github.arcaneplugins.levelledmobs.util.Utils
import io.github.arcaneplugins.levelledmobs.util.Utils.colorizeAllInList
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.persistence.PersistentDataType

/**
 * Provides common function between SpawnerSubCommand and SpawnerEggCommand
 *
 * @author stumper66
 * @since 3.3.0
 */
abstract class SpawnerBaseClass : MessagesBase() {
    var hadInvalidArg = false
    var startingArgNum = 0

    fun getArgValue(
        key: String,
        args: Array<String>,
        mustBeNumber: Boolean
    ): String? {
        var keyFlag = -1
        var nameStartFlag = -1
        var nameEndFlag = -1

        for (i in startingArgNum until args.size) {
            val arg = args[i]
            if (key.equals(arg, ignoreCase = true)) {
                keyFlag = i
            } else if (keyFlag == i - 1 && arg.startsWith("\"")) {
                nameStartFlag = i
            } else if (nameStartFlag > -1 && !arg.startsWith("/") && arg.endsWith("\"")) {
                nameEndFlag = i
                break
            }
        }

        if (keyFlag < 0) {
            return null
        }
        var keyValue: String?

        if (nameEndFlag > 0) {
            val sb = StringBuilder()
            for (i in nameStartFlag..nameEndFlag) {
                if (i > 0) {
                    sb.append(" ")
                }
                sb.append(args[i].trim { it <= ' ' })
            }
            keyValue = sb.toString().trim { it <= ' ' }
            keyValue = keyValue.substring(1, keyValue.length - 1)
        } else {
            keyValue = parseFlagValue(key, keyFlag, args, mustBeNumber)
        }

        return keyValue
    }

    private fun parseFlagValue(
        keyName: String,
        argNumber: Int,
        args: Array<String>,
        mustBeNumber: Boolean
    ): String? {
        if (argNumber + 1 >= args.size || args[argNumber + 1].startsWith("/")) {
            showMessage("command.levelledmobs.spawner.no-value", "%keyname%", keyName)
            hadInvalidArg = true
            return null
        }

        if (mustBeNumber && !Utils.isInteger(args[argNumber + 1])) {
            showMessage("command.levelledmobs.spawner.invalid-value", "%keyname%", keyName)
            hadInvalidArg = true
            return null
        }

        return args[argNumber + 1]
    }

    companion object{
        fun setMetaItems(
            meta: ItemMeta?,
            info: CustomSpawnerInfo,
            defaultName: String
        ) {
            if (meta == null) {
                return
            }

            val ver: ServerVersionInfo = LevelledMobs.instance.ver

            if (ver.isRunningPaper
                && LevelledMobs.instance.companion.useAdventure
            ) {
                PaperUtils.updateItemDisplayName(
                    meta,
                    if (info.customName == null) defaultName else info.customName
                )
            } else {
                SpigotUtils.updateItemDisplayName(
                    meta,
                    if (info.customName == null) defaultName else info.customName
                )
            }

            var lore = mutableListOf<String>()

            try {
                var itemsCount = 0
                val loreLine = StringBuilder()
                for (f in info.javaClass.getDeclaredFields()) {
                    if (!Modifier.isPublic(f.modifiers)) {
                        continue
                    }
                    if (f[info] == null) {
                        continue
                    }
                    val name = f.name
                    if (f.isAnnotationPresent(DoNotMerge::class.java)) {
                        continue
                    }

                    if ("-1" == f[info].toString() && (name == "minLevel" || name == "maxLevel")) {
                        continue
                    }

                    if (itemsCount > 2) {
                        lore.add(loreLine.toString())
                        loreLine.setLength(0)
                        itemsCount = 0
                    }

                    if (loreLine.isNotEmpty()) {
                        loreLine.append(", ")
                    }
                    loreLine.append(String.format("&7%s: &b%s&7", name, f[info]))
                    itemsCount++
                }
                if (itemsCount > 0) {
                    lore.add(loreLine.toString())
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            if (!info.noLore && info.lore == null && info.customLore == null) {
                lore = colorizeAllInList(lore)
                if (ver.isRunningPaper && LevelledMobs.instance.companion.useAdventure) {
                    PaperUtils.updateItemMetaLore(meta, lore)
                } else {
                    SpigotUtils.updateItemMetaLore(meta, lore)
                }

                val sbLore = java.lang.StringBuilder()
                for (loreLine in lore) {
                    if (sbLore.isNotEmpty()) {
                        sbLore.append("\n")
                    }
                    sbLore.append(loreLine)
                }
                meta.persistentDataContainer
                    .set(
                        NamespacedKeys.keySpawnerLore, PersistentDataType.STRING,
                        sbLore.toString()
                    )
            } else if (!info.noLore || info.customLore != null) {
                val useLore =
                    if (info.customLore == null) info.lore!! else colorizeAll(info.customLore).replace("\\n", "\n")

                lore.clear()
                lore.addAll(useLore.split("\n"))
                if (ver.isRunningPaper && LevelledMobs.instance.companion.useAdventure) {
                    PaperUtils.updateItemMetaLore(meta, lore)
                } else {
                    SpigotUtils.updateItemMetaLore(meta, lore)
                }

                meta.persistentDataContainer
                    .set(NamespacedKeys.keySpawnerLore, PersistentDataType.STRING, useLore)
            }
        }
    }

    class CustomSpawnerInfo(
        @DoNotMerge val label: String?
    ) {
        @DoNotMerge var player: Player? = null
        var minLevel = -1
        var maxLevel = -1

        @DoNotMerge var noLore = false
        var delay: Int? = null
        var maxNearbyEntities: Int? = null
        var minSpawnDelay: Int? = null
        var maxSpawnDelay: Int? = null
        var requiredPlayerRange: Int? = null
        var spawnCount: Int? = null
        var spawnRange: Int? = null
        var customDropId: String? = null

        @DoNotMerge var customName: String? = null
        var spawnType: EntityType = EntityType.UNKNOWN

        @DoNotMerge var customLore: String? = null
        var lore: String? = null
    }

    fun checkTabCompletion(
        options: MutableList<String>,
        args: Array<String>
    ): MutableList<String> {
        val commandsList: MutableSet<String> = TreeSet(java.lang.String.CASE_INSENSITIVE_ORDER)
        commandsList.addAll(options)

        var inQuotes = false

        for (i in 1 until args.size) {
            val arg = args[i]

            if (arg.startsWith("\"") && !arg.endsWith("\"")) {
                inQuotes = true
            } else if (inQuotes && arg.endsWith("\"")) {
                inQuotes = false
            }

            commandsList.remove(arg)
        }

        val lastArg = args[args.size - 1]

        if (inQuotes || lastArg.isNotEmpty() && lastArg[lastArg.length - 1] == '\"') {
            return mutableListOf()
        }

        val result: MutableList<String> = ArrayList(commandsList.size)
        result.addAll(commandsList)
        return result
    }
}