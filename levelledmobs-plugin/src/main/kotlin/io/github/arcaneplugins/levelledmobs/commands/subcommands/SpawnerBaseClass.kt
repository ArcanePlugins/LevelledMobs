package io.github.arcaneplugins.levelledmobs.commands.subcommands

import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import java.lang.reflect.Modifier
import java.util.TreeSet
import io.github.arcaneplugins.levelledmobs.LevelledMobs
import io.github.arcaneplugins.levelledmobs.MainCompanion
import io.github.arcaneplugins.levelledmobs.misc.NamespacedKeys
import io.github.arcaneplugins.levelledmobs.annotations.DoNotMerge
import io.github.arcaneplugins.levelledmobs.util.Log
import io.github.arcaneplugins.levelledmobs.util.MessageUtils.colorizeAll
import io.github.arcaneplugins.levelledmobs.util.PaperUtils
import io.github.arcaneplugins.levelledmobs.util.SpigotUtils
import io.github.arcaneplugins.levelledmobs.util.Utils
import io.github.arcaneplugins.levelledmobs.util.Utils.colorizeAllInList
import io.papermc.paper.command.brigadier.CommandSourceStack
import java.util.concurrent.CompletableFuture
import org.bukkit.Bukkit
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.persistence.PersistentDataType

/**
 * Provides common function between SpawnerSubCommand and SpawnerEggCommand
 *
 * @author stumper66
 * @since 3.3.0
 */
abstract class SpawnerBaseClass(
    basePermission: String
) : CommandBase(basePermission) {
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
            if (key.equals(arg, ignoreCase = true))
                keyFlag = i
            else if (keyFlag == i - 1 && arg.startsWith("\""))
                nameStartFlag = i
            else if (nameStartFlag > -1 && !arg.startsWith("/") && arg.endsWith("\"")) {
                nameEndFlag = i
                break
            }
        }

        if (keyFlag < 0) return null

        var keyValue: String?

        if (nameEndFlag > 0) {
            val sb = StringBuilder()
            for (i in nameStartFlag..nameEndFlag) {
                if (i > 0) sb.append(" ")

                sb.append(args[i].trim { it <= ' ' })
            }
            keyValue = sb.toString().trim { it <= ' ' }
            keyValue = keyValue.substring(1, keyValue.length - 1)
        }
        else
            keyValue = parseFlagValue(key, keyFlag, args, mustBeNumber)

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
            if (meta == null) return

            val ver = LevelledMobs.instance.ver

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
                    if (!Modifier.isPublic(f.modifiers)) continue

                    if (f[info] == null) continue

                    val name = f.name
                    if (f.isAnnotationPresent(DoNotMerge::class.java))
                        continue

                    if ("-1" == f[info].toString() && (name == "minLevel" || name == "maxLevel"))
                        continue

                    if (itemsCount > 2) {
                        lore.add(loreLine.toString())
                        loreLine.setLength(0)
                        itemsCount = 0
                    }

                    if (loreLine.isNotEmpty()) loreLine.append(", ")

                    loreLine.append("&7${name}: &b${f[info]}&7")
                    itemsCount++
                }

                if (itemsCount > 0) lore.add(loreLine.toString())
            } catch (e: Exception) {
                e.printStackTrace()
            }

            if (!info.noLore && info.lore == null && info.customLore == null) {
                lore = colorizeAllInList(lore)
                if (ver.isRunningPaper && MainCompanion.instance.useAdventure)
                    PaperUtils.updateItemMetaLore(meta, lore)
                else
                    SpigotUtils.updateItemMetaLore(meta, lore)

                val sbLore = StringBuilder()
                for (loreLine in lore) {
                    if (sbLore.isNotEmpty()) sbLore.append("\n")

                    sbLore.append(loreLine)
                }
                meta.persistentDataContainer
                    .set(
                        NamespacedKeys.keySpawnerLore, PersistentDataType.STRING,
                        sbLore.toString()
                    )
            } else if (!info.noLore || info.customLore != null) {
                val useLore =
                    if (info.customLore == null)
                        info.lore!!
                    else
                        colorizeAll(info.customLore).replace("\\n", "\n")

                lore.clear()
                lore.addAll(useLore.split("\n"))
                if (ver.isRunningPaper && MainCompanion.instance.useAdventure)
                    PaperUtils.updateItemMetaLore(meta, lore)
                else
                    SpigotUtils.updateItemMetaLore(meta, lore)

                meta.persistentDataContainer
                    .set(NamespacedKeys.keySpawnerLore, PersistentDataType.STRING, useLore)
            }
        }
    }

    class CustomSpawnerInfo(val isSpawnEgg: Boolean) {
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
        ctx: CommandContext<CommandSourceStack>,
        builder: SuggestionsBuilder
    ): CompletableFuture<Suggestions>{
        commandSender = ctx.source.sender
        val args = splitStringWithQuotes(ctx.input, true)
        val hasEndingSpace = args.last().endsWith(' ')
        val prefix = StringBuilder()
        val existingItems = mutableListOf<String>()
        val isSpawnerEgg = "spawner-egg".equals(ctx.nodes[1].node.name, ignoreCase = true)
        val argStart = if (isSpawnerEgg) 2 else 3

        for (i in argStart..<args.size){
            existingItems.add(args[i].lowercase())
            prefix.append(args[i]).append(' ')
        }

        if (args.size <= argStart)
            return checkTabCompletion(builder, prefix, allOptions, args)

        var doCheckArg = false
        var checkArg = args[args.size - 1]
        if (checkArg.startsWith("/")) doCheckArg = true
        else if (!hasEndingSpace && args.size >= 2 && args[args.size - 2].startsWith("/")){
            checkArg = args[args.size - 2]
            doCheckArg = true
        }

        if (doCheckArg) {
            when (checkArg.lowercase()) {
                "/entity" -> {
                    for (entityType in EntityType.entries) {
                        if (entityType == EntityType.PLAYER || entityType == EntityType.ARMOR_STAND) continue
                        val item = entityType.toString().lowercase()
                        if (!existingItems.contains(item))
                            builder.suggest(prefix.toString() + item)
                    }
                    return builder.buildFuture()
                }

                "/giveplayer" -> {
                    val players = mutableListOf<String>()
                    for (player in Bukkit.getOnlinePlayers()) {
                        players.add(player.name)
                    }
                    players.sortWith(String.CASE_INSENSITIVE_ORDER)
                    players.forEach { player ->
                        if (!existingItems.contains(player))
                            builder.suggest("$prefix$player")
                    }
                    return builder.buildFuture()
                }
            }
        }

        return checkTabCompletion(builder, prefix, allOptions, args)
    }

    private fun checkTabCompletion(
        builder: SuggestionsBuilder,
        prefix: StringBuilder,
        options: MutableList<String>,
        args: MutableList<String>
    ): CompletableFuture<Suggestions> {
        val commandsList: MutableSet<String> = TreeSet(String.CASE_INSENSITIVE_ORDER)
        commandsList.addAll(options)
        if (args.isEmpty()) return builder.buildFuture()

        args.forEach { arg ->
            if (commandsList.contains(arg))
                commandsList.remove(arg)
        }

        commandsList.forEach { command -> builder.suggest("$prefix$command") }

        return builder.buildFuture()
    }

    fun setMetadata(
        item: ItemStack,
        info: CustomSpawnerInfo,
        defaultName: String
    ){
        val meta = item.itemMeta ?: return

        setMetaItems(meta, info, defaultName)

        meta.persistentDataContainer
            .set(if (info.isSpawnEgg) NamespacedKeys.spawnerEgg else NamespacedKeys.keySpawner,
                PersistentDataType.INTEGER, 1
            )

        meta.persistentDataContainer
            .set(
                NamespacedKeys.keySpawnerMinLevel, PersistentDataType.INTEGER,
                info.minLevel
            )
        meta.persistentDataContainer
            .set(
                NamespacedKeys.keySpawnerMaxLevel, PersistentDataType.INTEGER,
                info.maxLevel
            )
        if (!info.customDropId.isNullOrEmpty()) {
            meta.persistentDataContainer
                .set(
                    NamespacedKeys.keySpawnerCustomDropId,
                    PersistentDataType.STRING, info.customDropId!!
                )
        }
        if (info.spawnType != EntityType.UNKNOWN) {
            meta.persistentDataContainer
                .set(
                    NamespacedKeys.keySpawnerSpawnType, PersistentDataType.STRING,
                    info.spawnType.toString()
                )
        }
        if (info.spawnRange != null) {
            meta.persistentDataContainer
                .set(
                    NamespacedKeys.keySpawnerSpawnRange,
                    PersistentDataType.INTEGER, info.spawnRange!!
                )
        }
        if (info.minSpawnDelay != null) {
            meta.persistentDataContainer
                .set(
                    NamespacedKeys.keySpawnerMinSpawnDelay,
                    PersistentDataType.INTEGER, info.minSpawnDelay!!
                )
        }
        if (info.maxSpawnDelay != null) {
            meta.persistentDataContainer
                .set(
                    NamespacedKeys.keySpawnerMaxSpawnDelay,
                    PersistentDataType.INTEGER, info.maxSpawnDelay!!
                )
        }
        if (info.spawnCount != null) {
            meta.persistentDataContainer
                .set(
                    NamespacedKeys.keySpawnerSpawnCount,
                    PersistentDataType.INTEGER, info.spawnCount!!
                )
        }
        if (info.requiredPlayerRange != null) {
            meta.persistentDataContainer
                .set(
                    NamespacedKeys.keySpawnerRequiredPlayerRange,
                    PersistentDataType.INTEGER, info.requiredPlayerRange!!
                )
        }
        if (info.maxNearbyEntities != null) {
            meta.persistentDataContainer
                .set(
                    NamespacedKeys.keySpawnerMaxNearbyEntities,
                    PersistentDataType.INTEGER, info.maxNearbyEntities!!
                )
        }
        if (info.delay != null) {
            meta.persistentDataContainer
                .set(
                    NamespacedKeys.keySpawnerDelay, PersistentDataType.INTEGER,
                    info.delay!!
                )
        }
        if (info.customName != null) {
            meta.persistentDataContainer
                .set(
                    NamespacedKeys.keySpawnerCustomName, PersistentDataType.STRING,
                    info.customName!!
                )
        }

        item.setItemMeta(meta)
    }

    internal fun giveItemToPlayer(
        item: ItemStack,
        info: CustomSpawnerInfo
    ){
        var useInvSlotNum: Int = info.player!!.inventory.heldItemSlot
        if (info.player!!.inventory.getItem(useInvSlotNum) != null)
            useInvSlotNum = -1

        if (useInvSlotNum == -1) {
            for (i in 0..35) {
                if (info.player!!.inventory.getItem(i) == null) {
                    useInvSlotNum = i
                    break
                }
            }
        }

        if (useInvSlotNum == -1) {
            showMessage( "command.levelledmobs.spawner.inventory-full", info.player!!.name, "")
            return
        }

        info.player!!.inventory.setItem(useInvSlotNum, item)

        val playerName = if (LevelledMobs.instance.ver.isRunningPaper) PaperUtils.getPlayerDisplayName(info.player)
        else SpigotUtils.getPlayerDisplayName(info.player)

        val message: MutableList<String>
        if (info.isSpawnEgg){
            message = getMessage(
                "command.levelledmobs.spawn_egg.give-message-console",
                mutableListOf("%minlevel%", "%maxlevel%", "%playername%", "%entitytype%"),
                mutableListOf(
                    info.minLevel.toString(), info.maxLevel.toString(), playerName,
                    info.spawnType.name
                )
            )
        }
        else{
            message = getMessage(
                "command.levelledmobs.spawner.spawner-give-message-console",
                mutableListOf("%minlevel%", "%maxlevel%", "%playername%"),
                mutableListOf(info.minLevel.toString(), info.maxLevel.toString(), playerName)
            )
        }

        if (message.isNotEmpty()) {
            val consoleMsg = message.first()
                .replace(LevelledMobs.instance.configUtils.prefix + " ", "&r")
            Log.inf(consoleMsg)
        }

        if (info.isSpawnEgg){
            showMessage(
                "command.levelledmobs.spawn_egg.give-message",
                mutableListOf("%minlevel%", "%maxlevel%", "%playername%", "%entitytype%"),
                mutableListOf(
                    info.minLevel.toString(), info.maxLevel.toString(), playerName,
                    info.spawnType.name
                ),
                info.player!!
            )
        }
        else{
            showMessage("command.levelledmobs.spawner.spawner-give-message",
                info.player!!.name,
                ""
            )
        }
    }
}