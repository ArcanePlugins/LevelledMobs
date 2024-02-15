package io.github.arcaneplugins.levelledmobs.util

import java.time.Duration
import java.time.Instant
import java.util.AbstractMap
import java.util.Locale
import java.util.regex.Pattern
import io.github.arcaneplugins.levelledmobs.LevelledMobs
import io.github.arcaneplugins.levelledmobs.MainCompanion
import io.github.arcaneplugins.levelledmobs.enums.DeathCause
import io.github.arcaneplugins.levelledmobs.debug.DebugManager
import io.github.arcaneplugins.levelledmobs.misc.CachedModalList
import io.github.arcaneplugins.levelledmobs.debug.DebugType
import io.github.arcaneplugins.levelledmobs.misc.NamespacedKeys
import io.github.arcaneplugins.levelledmobs.result.PlayerNetherOrWorldSpawnResult
import io.github.arcaneplugins.levelledmobs.enums.LevelledMobSpawnReason
import io.github.arcaneplugins.levelledmobs.rules.MinAndMax
import io.github.arcaneplugins.levelledmobs.rules.RulesManager
import io.github.arcaneplugins.levelledmobs.wrappers.LivingEntityWrapper
import org.bukkit.Chunk
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.block.Biome
import org.bukkit.command.CommandSender
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.entity.CreatureSpawnEvent
import org.bukkit.persistence.PersistentDataType
import org.jetbrains.annotations.Contract
import kotlin.math.max
import kotlin.math.pow

/**
 * Holds common utilities
 *
 * @author lokka30, stumper66
 * @since 2.5.0
 */
object Utils {
    private val timeUnitPattern: Pattern = Pattern.compile("(\\d+\\.?\\d+|\\d+)?(\\w+)")

    /**
     * Rounds value to 2 decimal points.
     *
     * @param value value to round
     * @return rounded value
     */
    fun round(value: Double): Double {
        return Math.round(value * 100) / 100.00
    }

    fun round(value: Double, digits: Int): Double {
        val scale = 10.0.pow(digits.toDouble())
        return Math.round(value * scale) / scale
    }

    /**
     * Replaces content of a message with case insensitivity.
     *
     * @param message     message that should be edited
     * @param replaceWhat the text to be replaced
     * @param replaceTo   the text to replace with
     * @return modified message
     * @author stumper66
     */
    fun replaceEx(
        message: String,
        replaceWhat: String,
        replaceTo: String
    ): String {
        var count = 0
        var position0 = 0
        var position1: Int
        val upperString = message.uppercase(Locale.getDefault())
        val upperPattern = replaceWhat.uppercase(Locale.getDefault())
        val inc = (message.length / replaceWhat.length) *
                (replaceTo.length - replaceWhat.length)
        val chars = CharArray((message.length + max(0.0, inc.toDouble())).toInt())
        while ((upperString.indexOf(upperPattern, position0).also { position1 = it }) != -1) {
            for (i in position0 until position1) {
                chars[count++] = message[i]
            }
            for (element in replaceTo) {
                chars[count++] = element
            }
            position0 = position1 + replaceWhat.length
        }
        if (position0 == 0) {
            return message
        }
        for (i in position0 until message.length) {
            chars[count++] = message[i]
        }

        return String(chars, 0, count)
    }

    /**
     * Check if str is an integer
     *
     * @param str str to check
     * @return if str is an integer (e.g. "1234" = true, "hello" = false)
     */
    fun isInteger(str: String?): Boolean {
        if (str.isNullOrEmpty()) return false

        try {
            str.toInt()
            return true
        } catch (ex: NumberFormatException) {
            return false
        }
    }

    fun isDouble(str: String?): Boolean {
        if (str.isNullOrEmpty()) return false

        try {
            str.toDouble()
            return true
        } catch (ex: NumberFormatException) {
            return false
        }
    }

    val oneToNine = mutableListOf(
        "1", "2", "3", "4", "5", "6", "7", "8", "9"
    )

    fun replaceAllInList(
        oldList: MutableList<String>,
        replaceWhat: String,
        replaceTo: String?
    ): MutableList<String> {
        val useReplaceTo = replaceTo ?: ""

        val newList = mutableListOf<String>()
        for (string in oldList) {
            newList.add(string.replace(replaceWhat, useReplaceTo))
        }
        return newList
    }

    fun colorizeAllInList(
        oldList: MutableList<String>
    ): MutableList<String> {
        val newList = mutableListOf<String>()

        for (string in oldList) {
            newList.add(MessageUtils.colorizeAll(string))
        }

        return newList
    }

    /**
     * Puts the string into lowercase and makes every character that starts a word a capital
     * letter.
     *
     *
     * e.g. from: wiTheR sKeLeTOn to: Wither Skeleton
     *
     * @param str string to capitalize
     * @return a string with each word capitalized
     */
    fun capitalize(str: String): String {
        val builder = StringBuilder()
        val words = str.lowercase(Locale.getDefault()).split(" ".toRegex()).dropLastWhile { it.isEmpty() }
            .toTypedArray() // each word separated from str
        for (i in words.indices) {
            val word = words[i]
            if (word.isEmpty()) {
                continue
            }

            builder.append(word[0].toString().uppercase(Locale.getDefault())) // capitalize first letter
            if (word.length > 1) {
                builder.append(word.substring(1)) // append the rest of the word
            }

            // if there is another word to capitalize, then add a space
            if (i < words.size - 1) {
                builder.append(" ")
            }
        }

        return builder.toString()
    }

    fun isLivingEntityInModalList(
        list: CachedModalList<String>,
        lmEntity: LivingEntityWrapper,
        checkBabyMobs: Boolean
    ): Boolean {
        if (list.allowAll) {
            return true
        }
        if (list.excludeAll) {
            return false
        }
        if (list.isEmpty()) {
            return true
        }

        val checkName = if (checkBabyMobs) lmEntity.nameIfBaby else lmEntity.typeName

        for (group in lmEntity.getApplicableGroups()) {
            if (list.excludedGroups.contains(group)) {
                return false
            }
        }

        // for denies we'll check for both baby and adult variants regardless of baby-mobs-inherit-adult-setting
        if (list.excludedList.contains(lmEntity.typeName) || list.excludedList.contains(
                lmEntity.nameIfBaby
            ) || lmEntity.isBabyMob && list.excludedList.contains("baby_")
        ) {
            return false
        }

        for (group in lmEntity.getApplicableGroups()) {
            if (list.allowedGroups.contains(group)) {
                return true
            }
        }

        return list.isBlacklist || list.allowedList.contains(checkName) || lmEntity.isBabyMob && list.allowedList.contains(
            "baby_"
        )
    }

    fun isIntegerInModalList(
        list: CachedModalList<MinAndMax>,
        checkNum: Int
    ): Boolean {
        if (list.allowAll) {
            return true
        }
        if (list.excludeAll) {
            return false
        }
        if (list.isEmpty()) {
            return true
        }

        for (exclude in list.excludedList) {
            if (checkNum >= exclude.min && checkNum <= exclude.max) {
                return false
            }
        }

        if (list.isBlacklist) {
            return true
        }

        for (include in list.allowedList) {
            if (checkNum >= include.min && checkNum <= include.max) {
                return true
            }
        }

        return false
    }

    fun isBiomeInModalList(
        list: CachedModalList<Biome>,
        biome: Biome,
        rulesManager: RulesManager
    ): Boolean {
        if (list.allowAll) {
            return true
        }
        if (list.excludeAll) {
            return false
        }
        if (list.isEmpty()) {
            return true
        }

        for (group in list.excludedGroups) {
            if (rulesManager.biomeGroupMappings.containsKey(group) &&
                rulesManager.biomeGroupMappings[group]!!.contains(biome.toString())
            ) {
                return false
            }
        }

        if (list.excludedList.contains(biome)) {
            return false
        }

        for (group in list.allowedGroups) {
            if (rulesManager.biomeGroupMappings.containsKey(group) &&
                rulesManager.biomeGroupMappings[group]!!.contains(biome.toString())
            ) {
                return true
            }
        }

        return list.isBlacklist || list.allowedList.contains(biome)
    }

    fun isDamageCauseInModalList(
        list: CachedModalList<DeathCause>,
        cause: DeathCause
    ): Boolean {
        if (list.allowAll) {
            return true
        }
        if (list.excludeAll) {
            return false
        }
        if (list.isEmpty()) {
            return true
        }

        // note: no group support
        if (list.excludedList.contains(cause)) {
            return false
        }

        return list.isBlacklist || list.allowedList.contains(cause)
    }

    fun getMillisecondsFromInstant(instant: Instant): Long {
        return Duration.between(instant, Instant.now()).toMillis()
    }

    fun getPortalOrWorldSpawn(
        player: Player
    ): PlayerNetherOrWorldSpawnResult {
        var location: Location? = null
        var isNetherPortalCoord = false
        var isWorldPortalCoord = false

        if (player.world.environment == World.Environment.NETHER) {
            location = MainCompanion.instance.getPlayerNetherPortalLocation(player)
            isNetherPortalCoord = true
        } else if (player.world.environment == World.Environment.NORMAL) {
            location = MainCompanion.instance.getPlayerWorldPortalLocation(player)
            isWorldPortalCoord = true
        }

        if (location == null) {
            location = player.world.spawnLocation
            isNetherPortalCoord = false
            isWorldPortalCoord = false
        }

        return PlayerNetherOrWorldSpawnResult(
            location, isNetherPortalCoord,
            isWorldPortalCoord
        )
    }

    fun getChunkKey(chunk: Chunk): Long {
        if (LevelledMobs.instance.ver.isRunningPaper) {
            return chunk.chunkKey
        }

        val x = chunk.x shr 4
        val z = chunk.z shr 4
        return x.toLong() and 0xffffffffL or ((z.toLong() and 0xffffffffL) shl 32)
    }

    fun displayChunkLocation(location: Location): String {
        return "${location.chunk.x},${location.chunk.z}"
    }

    // take from https://www.techiedelight.com/five-alternatives-pair-class-java/
    @Contract(value = "_, _ -> new", pure = true)
    fun <T, U> getPair(first: T, second: U): Map.Entry<T, U> {
        return AbstractMap.SimpleEntry(first, second)
    }

    fun adaptVanillaSpawnReason(
        spawnReason: CreatureSpawnEvent.SpawnReason
    ): LevelledMobSpawnReason {
        return LevelledMobSpawnReason.valueOf(spawnReason.toString())
    }

    fun matchWildcardString(
        input: String,
        match: String
    ): Boolean {
        if (!match.contains("*")) {
            return input.equals(match, ignoreCase = true)
        }

        val chopped = match.split("\\*".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        // 0 = *, 1 = text, 2 = *
        if (chopped.size > 3) {
            Log.war("Invalid wildcard pattern: $match")
            return input.equals(match, ignoreCase = true)
        }

        val inputL = input.lowercase(Locale.getDefault())
        val matchL = match.lowercase(Locale.getDefault())

        val useSearch: String
        if (matchL.startsWith("*") && matchL.endsWith("*")) {
            useSearch = matchL.substring(1, matchL.length - 1)
            return inputL.contains(useSearch)
        } else if (matchL.startsWith("*")) {
            useSearch = matchL.substring(1)
            return inputL.endsWith(useSearch)
        } else {
            useSearch = matchL.substring(0, matchL.length - 1)
            return inputL.startsWith(useSearch)
        }
    }

    fun removeColorCodes(input: String): String {
        var formatted = input.replace("§", "&")

        if (input.contains("&")) {
            formatted = input.replace("&.".toRegex(), "")
        }
        return formatted
    }

    private fun showLocation(location: Location): String {
        return String.format(
            "%s at %s,%s,%s",
            location.world.name,
            location.blockX,
            location.blockY,
            location.blockZ
        )
    }

    fun checkIfMobHashChanged(
        lmEntity: LivingEntityWrapper
    ): Boolean {
        val main: LevelledMobs = LevelledMobs.instance

        if (!lmEntity.pdc.has(NamespacedKeys.mobHash, PersistentDataType.STRING)) {
            return true
        }

        var hadHash = false
        var mobHash: String? = null
        if (lmEntity.pdc.has(NamespacedKeys.mobHash, PersistentDataType.STRING)) {
            mobHash = lmEntity.pdc.get(NamespacedKeys.mobHash, PersistentDataType.STRING)
            hadHash = true
        }

        val hashChanged = main.rulesManager.currentRulesHash != mobHash
        if (hashChanged) {
            if (hadHash) {
                DebugManager.log(DebugType.MOB_HASH, lmEntity, false) {
                    String.format(
                        "Invalid hash for %s %s",
                        lmEntity.nameIfBaby, showLocation(lmEntity.location)
                    )
                }
            } else {
                DebugManager.log(DebugType.MOB_HASH, lmEntity, false) {
                    String.format(
                        "Hash missing for %s %s",
                        lmEntity.nameIfBaby, showLocation(lmEntity.location)
                    )
                }
            }

            // also setting the PDC key here because if the mob is not eligable for levelling then it will
            // run this same code repeatidly
            lmEntity.pdc
                .set(NamespacedKeys.mobHash, PersistentDataType.STRING, main.rulesManager.currentRulesHash)
        } else {
            DebugManager.log(DebugType.MOB_HASH, lmEntity, true) {
                String.format(
                    "Hash missing for %s %s",
                    lmEntity.nameIfBaby, showLocation(lmEntity.location)
                )
            }
        }

        return hashChanged
    }

    fun parseTimeUnit(
        input: String?,
        defaultTime: Long?,
        useMS: Boolean,
        sender: CommandSender?
    ): Long? {
        if (input == null) {
            return defaultTime
        }
        if ("0" == input) {
            return 0L
        }

        val match = timeUnitPattern.matcher(input)

        if (!match.matches() || match.groupCount() != 2) {
            if (sender != null) sender.sendMessage("Invalid time: $input")
            else Log.war("Invalid time: $input")

            return defaultTime
        }

        val time: Long
        var remainder = 0.0
        var numberPart = if (match.group(1) != null) match.group(1) else match.group(2)
        var unit = if (match.group(1) != null) match.group(2).lowercase(Locale.getDefault()) else ""

        if (isInteger(input)) {
            // number only, no time unit was specified
            numberPart = input
            unit = ""
        }

        if (numberPart.contains(".")) {
            val split = numberPart.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            try {
                remainder = 1.0 - ("0." + split[1]).toDouble()
                numberPart = split[0]
            } catch (e: Exception) {
                if (sender != null) sender.sendMessage("Invalid time: $input")
                else Log.war("Invalid time: $input")

                return defaultTime
            }
        }

        try {
            time = numberPart.toLong()
        } catch (e: Exception) {
            if (sender != null) sender.sendMessage("Invalid time: $input")
            else Log.war("Invalid time: $input")

            return defaultTime
        }

        var duration: Duration? = null
        when (unit) {
            "ms", "millisecond", "milliseconds" -> duration = Duration.ofMillis(time)
            "s", "second", "seconds" -> {
                duration = Duration.ofSeconds(time)
                if (remainder > 0.0) {
                    duration = duration.plusMillis((1000.0 * remainder).toLong())
                }
            }

            "m", "minute", "minutes" -> {
                duration = Duration.ofMinutes(time)
                if (remainder > 0.0) {
                    duration = duration.plusMillis((60000.0 * remainder).toLong())
                }
            }

            "h", "hour", "hours" -> {
                duration = Duration.ofHours(time)
                if (remainder > 0.0) {
                    duration = duration.plusMillis((3600000.0 * remainder).toLong())
                }
            }

            "d", "day", "days" -> {
                duration = Duration.ofDays(time)
                if (remainder > 0.0) {
                    duration = duration.plusSeconds((86400.0 * remainder).toLong())
                }
            }

            "" -> duration = if (useMS) Duration.ofMillis(time) else Duration.ofSeconds(time)
            else -> {
                if (sender != null) sender.sendMessage("Invalid time unit specified: $input ($unit)")
                else Log.war("Invalid time unit specified: $input ($unit)")
            }
        }
        if (duration != null) {
            return if (useMS) duration.toMillis() else duration.seconds
        }

        return defaultTime
    }

    fun filterPlayersList(
        entities: MutableList<Player>,
        mob: LivingEntity,
        maxDistance: Double?
    ): MutableList<Player>{
        var temp = entities.asSequence()
            .filter { p: Player -> p.world == mob.world }
            .filter { p: Player -> p.gameMode != GameMode.SPECTATOR }
            .map { p: Player -> Pair(mob.location.distanceSquared(p.location), p) }
            .filter { maxDistance != null && it.first <= maxDistance }
            .sortedBy { it.first }
            .map { it.second }

        if (MainCompanion.instance.excludePlayersInCreative){
            temp = temp.filter { e: Entity -> (e as Player).gameMode != GameMode.CREATIVE }
        }

        return temp.toMutableList()
    }
}