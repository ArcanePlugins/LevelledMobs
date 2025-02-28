package io.github.arcaneplugins.levelledmobs.managers

import java.util.UUID
import me.clip.placeholderapi.expansion.PlaceholderExpansion
import io.github.arcaneplugins.levelledmobs.LevelledMobs
import io.github.arcaneplugins.levelledmobs.misc.LastMobKilledInfo
import io.github.arcaneplugins.levelledmobs.misc.StringReplacer
import io.github.arcaneplugins.levelledmobs.util.MessageUtils.colorizeAll
import io.github.arcaneplugins.levelledmobs.util.MiscUtils
import io.github.arcaneplugins.levelledmobs.wrappers.LivingEntityWrapper
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.util.Vector

/**
 * Manages communication to PlaceholderAPI (PAPI)
 *
 * @author stumper66
 * @since 3.0.0
 */
@Suppress("DEPRECATION")
class PlaceholderApiIntegration : PlaceholderExpansion() {
    private val mobsByPlayerTracking = mutableMapOf<UUID, LastMobKilledInfo>()
    private val playerDeathInfo = mutableMapOf<UUID, LastMobKilledInfo>()

    fun putPlayerOrMobDeath(
        player: Player,
        lmEntity: LivingEntityWrapper?, isPlayerDeath: Boolean
    ) {
        val mobInfo = mobsByPlayerTracking.computeIfAbsent(
            player.uniqueId
        ) { _: UUID? -> LastMobKilledInfo() }

        mobInfo.entityLevel = if (lmEntity != null && lmEntity.isLevelled) lmEntity.getMobLevel else null

        mobInfo.entityName = if (lmEntity != null) LevelledMobs.instance.levelManager.getNametag(lmEntity, false).nametag else null

        if (isPlayerDeath) putPlayerKillerInfo(player, lmEntity)
    }

    private fun putPlayerKillerInfo(
        player: Player,
        lmEntity: LivingEntityWrapper?
    ) {
        val mobInfo = LastMobKilledInfo()
        playerDeathInfo[player.uniqueId] = mobInfo

        mobInfo.entityLevel = if (lmEntity != null && lmEntity.isLevelled) lmEntity.getMobLevel else null

        mobInfo.entityName = if (lmEntity != null) LevelledMobs.instance.levelManager.getNametag(lmEntity, false).nametag else null
    }

    fun playedLoggedOut(player: Player) {
        mobsByPlayerTracking.remove(player.uniqueId)
    }

    fun removePlayer(player: Player) {
        mobsByPlayerTracking.remove(player.uniqueId)
    }

    override fun persist(): Boolean {
        return true
    }

    override fun canRegister(): Boolean {
        return true
    }

    override fun getIdentifier(): String {
        return LevelledMobs.instance.description.name
    }

    override fun getAuthor(): String {
        return LevelledMobs.instance.description.authors.toString()
    }

    override fun getVersion(): String {
        return LevelledMobs.instance.description.version
    }

    override fun onPlaceholderRequest(player: Player?, identifier: String): String? {
        if (player == null) {
            return ""
        }

        if ("mob-lvl".equals(identifier, ignoreCase = true)) {
            return getLevelFromPlayer(player)
        } else if ("displayname".equals(identifier, ignoreCase = true)) {
            return getDisplaynameFromPlayer(player)
        } else if ("mob-target".equals(identifier, ignoreCase = true)) {
            return getMobNametagWithinPlayerSight(player)
        } else if ("killed-by".equals(identifier, ignoreCase = true)) {
            return getKilledByInfo(player)
        }

        return null
    }

    private fun getLevelFromPlayer(player: Player): String {
        if (!mobsByPlayerTracking.containsKey(player.uniqueId)) {
            return ""
        }

        val mobInfo = mobsByPlayerTracking[player.uniqueId]
        return if (mobInfo!!.entityLevel == null) "" else mobInfo.entityLevel.toString()
    }

    private fun getDisplaynameFromPlayer(player: Player): String {
        if (!mobsByPlayerTracking.containsKey(player.uniqueId)) {
            return ""
        }

        val mobInfo = mobsByPlayerTracking[player.uniqueId]
        return if (mobInfo?.entityName == null) "" else mobInfo.entityName + "&r"
    }

    private fun getKilledByInfo(player: Player): String {
        if (!playerDeathInfo.containsKey(player.uniqueId)) {
            return ""
        }

        val mobInfo = playerDeathInfo[player.uniqueId]
        return if (mobInfo?.entityName == null) "" else colorizeAll(mobInfo.entityName + "&r")
    }

    private fun getMobNametagWithinPlayerSight(player: Player?): String {
        if (player == null) {
            return ""
        }

        val targetMob: LivingEntity = getMobBeingLookedAt(player) ?: return ""
        val lmEntity = LivingEntityWrapper.getInstance(targetMob)
        var nametag = lmEntity.main.rulesManager.getRuleNametagPlaceholder(lmEntity)
        if (!nametag.isNullOrEmpty()) {
            val useCustomNameForNametags = lmEntity.main.helperSettings.getBoolean(
                 "use-customname-for-mob-nametags"
            )
            nametag = lmEntity.main.levelManager.updateNametag(
                lmEntity, StringReplacer(nametag),
                useCustomNameForNametags, null
            ).nametagNonNull

            if ("disabled".equals(nametag, ignoreCase = true)) {
                return ""
            }
        }

        if (nametag.isNullOrEmpty() && lmEntity.isLevelled) {
            nametag = lmEntity.main.levelManager.getNametag(lmEntity, false).nametag + "&r"
        }

        lmEntity.free()

        if (nametag != null) {
            if (nametag.endsWith("&r")) {
                nametag = nametag.substring(0, nametag.length - 2)
            }
            return nametag
        } else {
            return ""
        }
    }

    private fun getMobBeingLookedAt(player: Player): LivingEntity? {
        var livingEntity: LivingEntity? = null
        val eye = player.eyeLocation
        val maxBlocks = LevelledMobs.instance.helperSettings.getInt(
            "nametag-placeholder-maxblocks", 30
        )

        val radius = MiscUtils.retrieveLoadedChunkRadius(player.location, maxBlocks.toDouble())
        for (entity in player.getNearbyEntities(radius, radius, radius)) {
            if (entity !is LivingEntity) {
                continue
            }

            val toEntity: Vector = entity.eyeLocation.toVector().subtract(eye.toVector())
            val dot = toEntity.normalize().dot(eye.direction)
            if (dot >= 0.975) {
                livingEntity = entity
                break
            }
        }

        return livingEntity
    }
}