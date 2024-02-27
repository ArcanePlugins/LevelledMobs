package io.github.arcaneplugins.levelledmobs.commands.subcommands

import dev.jorel.commandapi.SuggestionInfo
import java.lang.reflect.Modifier
import java.util.TreeSet
import io.github.arcaneplugins.levelledmobs.LevelledMobs
import io.github.arcaneplugins.levelledmobs.MainCompanion
import io.github.arcaneplugins.levelledmobs.commands.MessagesBase
import io.github.arcaneplugins.levelledmobs.misc.NamespacedKeys
import io.github.arcaneplugins.levelledmobs.nametag.ServerVersionInfo
import io.github.arcaneplugins.levelledmobs.annotations.DoNotMerge
import io.github.arcaneplugins.levelledmobs.util.MessageUtils.colorizeAll
import io.github.arcaneplugins.levelledmobs.util.PaperUtils
import io.github.arcaneplugins.levelledmobs.util.SpigotUtils
import io.github.arcaneplugins.levelledmobs.util.Utils
import io.github.arcaneplugins.levelledmobs.util.Utils.colorizeAllInList
import java.util.Locale
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
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
    private var startingArgNum = 0

    fun getArgValue(
        key: String,
        args: MutableList<String>,
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
        args: MutableList<String>,
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

            if (ver.isRunningPaper && MainCompanion.instance.useAdventure
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
                if (ver.isRunningPaper && MainCompanion.instance.useAdventure) {
                    PaperUtils.updateItemMetaLore(meta, lore)
                } else {
                    SpigotUtils.updateItemMetaLore(meta, lore)
                }

                val sbLore = StringBuilder()
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
                if (ver.isRunningPaper && MainCompanion.instance.useAdventure) {
                    PaperUtils.updateItemMetaLore(meta, lore)
                } else {
                    SpigotUtils.updateItemMetaLore(meta, lore)
                }

                meta.persistentDataContainer
                    .set(NamespacedKeys.keySpawnerLore, PersistentDataType.STRING, useLore)
            }
        }
    }

    class CustomSpawnerInfo {
        @DoNotMerge
        var player: Player? = null
        var minLevel = -1
        var maxLevel = -1

        @DoNotMerge
        var noLore = false
        var delay: Int? = null
        var maxNearbyEntities: Int? = null
        var minSpawnDelay: Int? = null
        var maxSpawnDelay: Int? = null
        var requiredPlayerRange: Int? = null
        var spawnCount: Int? = null
        var spawnRange: Int? = null
        var customDropId: String? = null

        @DoNotMerge
        var customName: String? = null
        var spawnType: EntityType = EntityType.UNKNOWN

        @DoNotMerge
        var customLore: String? = null
        var lore: String? = null
    }

    protected fun buildTabSuggestions(
        allOptions: MutableList<String>,
        info: SuggestionInfo<CommandSender>
    ): MutableList<String>{
        val args = Utils.splitStringWithQuotes(info.currentArg)
        val hasEndingSpace = info.currentInput.toString().endsWith(" ")
        if (args.isEmpty())
            return checkTabCompletion(allOptions, args)

        var doCheckArg = false
        var checkArg = args[args.size - 1]
        if (checkArg.startsWith("/")) doCheckArg = true
        else if (!hasEndingSpace && args.size >= 2 && args[args.size - 2].startsWith("/")){
            checkArg = args[args.size - 2]
            doCheckArg = true
        }
        //Log.inf("doCheckArg: $doCheckArg, checkArg: '$checkArg'")

        if (doCheckArg) {
            when (checkArg.lowercase(Locale.getDefault())) {
                "/entity" -> {
                    val entityNames = mutableListOf<String>()
                    for (entityType in EntityType.entries) {
                        if (entityType == EntityType.PLAYER || entityType == EntityType.ARMOR_STAND) continue
                        entityNames.add(entityType.toString().lowercase(Locale.getDefault()))
                    }
                    return entityNames
                }

                "/giveplayer" -> {
                    val players = mutableListOf<String>()
                    for (player in Bukkit.getOnlinePlayers()) {
                        players.add(player.name)
                    }
                    players.sortWith(String.CASE_INSENSITIVE_ORDER)
                    return players
                }
            }
        }

        return checkTabCompletion(allOptions, args)
    }

    private fun checkTabCompletion(
        options: MutableList<String>,
        args: MutableList<String>
    ): MutableList<String> {
        val commandsList: MutableSet<String> = TreeSet(String.CASE_INSENSITIVE_ORDER)
        commandsList.addAll(options)
        if (args.isEmpty()) return commandsList.toMutableList()

        for (arg in args){
            if (commandsList.contains(arg))
                commandsList.remove(arg)
        }

        return commandsList.toMutableList()
    }
}