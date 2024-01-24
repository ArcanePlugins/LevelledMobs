package io.github.arcaneplugins.levelledmobs.listeners

import java.util.Locale
import java.util.UUID
import java.util.concurrent.ThreadLocalRandom
import io.github.arcaneplugins.levelledmobs.LevelledMobs
import io.github.arcaneplugins.levelledmobs.commands.MessagesBase
import io.github.arcaneplugins.levelledmobs.commands.subcommands.SpawnerBaseClass.CustomSpawnerInfo
import io.github.arcaneplugins.levelledmobs.managers.LevelManager
import io.github.arcaneplugins.levelledmobs.misc.AdditionalLevelInformation
import io.github.arcaneplugins.levelledmobs.misc.Cooldown
import io.github.arcaneplugins.levelledmobs.misc.NamespacedKeys
import io.github.arcaneplugins.levelledmobs.misc.Point
import io.github.arcaneplugins.levelledmobs.util.MessageUtils.colorizeAll
import io.github.arcaneplugins.levelledmobs.util.Utils
import io.github.arcaneplugins.levelledmobs.wrappers.LivingEntityWrapper
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.block.CreatureSpawner
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.entity.CreatureSpawnEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.persistence.PersistentDataType

class PlayerInteractEventListener : MessagesBase(), Listener {
    private val cooldownMap = mutableMapOf<UUID, Cooldown>()

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    fun onPlayerInteractEvent(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK) {
            return
        }

        commandSender = event.player
        messageLabel = "lm"

        if (event.material.name.lowercase(Locale.getDefault()).endsWith("_spawn_egg")) {
            if (processLMSpawnEgg(event)) {
                return
            }
        }

        val main = LevelledMobs.instance
        if (main.companion.spawnerInfoIds.isEmpty() && main.companion.spawnerCopyIds.isEmpty()) {
            return
        }
        if (event.hand == null || event.hand != EquipmentSlot.HAND) {
            return
        }

        val doShowInfo: Boolean = main.companion.spawnerInfoIds.contains(
            event.player.uniqueId
        )
        val doCopy: Boolean = main.companion.spawnerCopyIds.contains(
            event.player.uniqueId
        )

        if (!doCopy && !doShowInfo) {
            return
        }

        if (event.clickedBlock == null
            || event.clickedBlock!!.type != Material.SPAWNER
        ) {
            return
        }

        val uuid = event.player.uniqueId
        val point = Point(event.clickedBlock!!.location)
        if (cooldownMap.containsKey(uuid)) {
            if (cooldownMap[uuid]!!.doesCooldownBelongToIdentifier(point.toString())) {
                if (!cooldownMap[uuid]!!.hasCooldownExpired(2)) {
                    return
                }
            }
            cooldownMap.remove(uuid)
        }
        cooldownMap[uuid] = Cooldown(System.currentTimeMillis(), point.toString())

        val cs = event.clickedBlock!!.state as CreatureSpawner
        if (doShowInfo) {
            showInfo(event.player, cs)
        } else if (event.material == Material.AIR) {
            copySpawner(event.player, cs)
        }
    }

    private fun processLMSpawnEgg(event: PlayerInteractEvent): Boolean {
        val main = LevelledMobs.instance
        if (!main.ver.isRunningPaper) {
            return false
        }
        if (event.item == null) {
            return false
        }
        val meta = event.item!!.itemMeta ?: return false
        if (event.clickedBlock == null) {
            return false
        }
        if (!meta.persistentDataContainer
                .has(NamespacedKeys.spawnerEgg, PersistentDataType.INTEGER)
        ) {
            return false
        }

        // we've confirmed it is a LM spawn egg. cancel the event and spawn the mob manually
        event.isCancelled = true
        val location = event.clickedBlock!!.location.add(0.0, 1.0, 0.0)
        var minLevel = 1
        var maxLevel = 1
        var customDropId: String? = null
        var spawnType = EntityType.ZOMBIE

        if (meta.persistentDataContainer
                .has(NamespacedKeys.keySpawnerMinLevel, PersistentDataType.INTEGER)
        ) {
            val temp = meta.persistentDataContainer
                .get(NamespacedKeys.keySpawnerMinLevel, PersistentDataType.INTEGER)
            if (temp != null) {
                minLevel = temp
            }
        }
        if (meta.persistentDataContainer
                .has(NamespacedKeys.keySpawnerMaxLevel, PersistentDataType.INTEGER)
        ) {
            val temp = meta.persistentDataContainer
                .get(NamespacedKeys.keySpawnerMaxLevel, PersistentDataType.INTEGER)
            if (temp != null) {
                maxLevel = temp
            }
        }
        if (meta.persistentDataContainer
                .has(NamespacedKeys.keySpawnerCustomDropId, PersistentDataType.STRING)
        ) {
            customDropId = meta.persistentDataContainer
                .get(NamespacedKeys.keySpawnerCustomDropId, PersistentDataType.STRING)
        }

        if (meta.persistentDataContainer
                .has(NamespacedKeys.keySpawnerSpawnType, PersistentDataType.STRING)
        ) {
            val temp = meta.persistentDataContainer
                .get(NamespacedKeys.keySpawnerSpawnType, PersistentDataType.STRING)
            if (temp != null) {
                try {
                    spawnType = EntityType.valueOf(temp)
                } catch (ignored: Exception) {
                    Utils.logger.warning("Invalid spawn type on spawner egg: $temp")
                }
            }
        }

        var eggName: String? = null
        if (meta.persistentDataContainer
                .has(NamespacedKeys.keySpawnerCustomName, PersistentDataType.STRING)
        ) {
            eggName = meta.persistentDataContainer
                .get(NamespacedKeys.keySpawnerCustomName, PersistentDataType.STRING)
        }

        if (eggName.isNullOrEmpty()) {
            eggName = "LM Spawn Egg"
        }

        if (event.clickedBlock!!.blockData.material == Material.SPAWNER) {
            val info = CustomSpawnerInfo(null)
            info.minLevel = minLevel
            info.maxLevel = maxLevel
            info.spawnType = spawnType
            info.customDropId = customDropId
            if (meta.persistentDataContainer
                    .has(NamespacedKeys.keySpawnerCustomName, PersistentDataType.STRING)
            ) {
                info.customName = meta.persistentDataContainer
                    .get(NamespacedKeys.keySpawnerCustomName, PersistentDataType.STRING)
            }
            if (meta.persistentDataContainer
                    .has(NamespacedKeys.keySpawnerLore, PersistentDataType.STRING)
            ) {
                info.lore = meta.persistentDataContainer
                    .get(NamespacedKeys.keySpawnerLore, PersistentDataType.STRING)
            }

            convertSpawner(event, info)
            return true
        }

        val entity = location.world
            .spawnEntity(location, spawnType, CreatureSpawnEvent.SpawnReason.SPAWNER_EGG)
        if (entity !is LivingEntity) {
            return true
        }

        val lmEntity = LivingEntityWrapper.getInstance(entity)

        synchronized(LevelManager.summonedOrSpawnEggs_Lock) {
            main.levelManager.summonedOrSpawnEggs.put(lmEntity.livingEntity, null)
        }

        var useLevel = minLevel
        if (minLevel != maxLevel) {
            useLevel = ThreadLocalRandom.current().nextInt(minLevel, maxLevel + 1)
        }

        synchronized(lmEntity.livingEntity.persistentDataContainer) {
            lmEntity.pdc.set(NamespacedKeys.wasSummoned, PersistentDataType.INTEGER, 1)
            if (!customDropId.isNullOrEmpty()) {
                lmEntity.pdc
                    .set(
                        NamespacedKeys.keySpawnerCustomDropId, PersistentDataType.STRING,
                        customDropId
                    )
            }
            lmEntity.pdc
                .set(NamespacedKeys.spawnerEggName, PersistentDataType.STRING, eggName)
        }

        main.levelInterface.applyLevelToMob(
            lmEntity, useLevel, isSummoned = true, bypassLimits = true,
            additionalLevelInformation = mutableSetOf(AdditionalLevelInformation.NOT_APPLICABLE)
        )

        lmEntity.free()
        return true
    }

    private fun convertSpawner(
        event: PlayerInteractEvent,
        info: CustomSpawnerInfo
    ) {
        if (event.clickedBlock == null) {
            return
        }

        if (!event.player.hasPermission("levelledmobs.convert-spawner")) {
            showMessage("command.levelledmobs.spawner.permission-denied")
            return
        }

        val cs = event.clickedBlock!!.state as CreatureSpawner
        val pdc = cs.persistentDataContainer
        val wasLMSpawner = pdc.has(
            NamespacedKeys.keySpawner,
            PersistentDataType.INTEGER
        )

        pdc.set(NamespacedKeys.keySpawner, PersistentDataType.INTEGER, 1)
        pdc.set(
            NamespacedKeys.keySpawnerMinLevel, PersistentDataType.INTEGER,
            info.minLevel
        )
        pdc.set(
            NamespacedKeys.keySpawnerMaxLevel, PersistentDataType.INTEGER,
            info.maxLevel
        )

        updateKeyString(NamespacedKeys.keySpawnerCustomDropId, pdc, info.customDropId)
        updateKeyString(
            NamespacedKeys.keySpawnerSpawnType, pdc,
            info.spawnType.toString()
        )
        updateKeyString(NamespacedKeys.keySpawnerCustomName, pdc, info.customName)

        cs.spawnedType = info.spawnType
        cs.update()

        if (info.customName.isNullOrEmpty()) {
            info.customName = "LM Spawner"
        }

        if (!wasLMSpawner) {
            showMessage(
                "command.levelledmobs.spawner.spawner-converted", "%spawnername%",
                info.customName!!
            )
        } else {
            showMessage(
                "command.levelledmobs.spawner.spawner-updated", "%spawnername%",
                info.customName!!
            )
        }
    }

    private fun updateKeyString(
        key: NamespacedKey,
        pdc: PersistentDataContainer,
        value: String?
    ) {
        if (!value.isNullOrEmpty()) {
            pdc.set(key, PersistentDataType.STRING, value)
        } else if (pdc.has(key, PersistentDataType.STRING)) {
            pdc.remove(key)
        }
    }

    private fun copySpawner(player: Player, cs: CreatureSpawner) {
        val info = CustomSpawnerInfo("lm")
        info.player = player
        val pdc = cs.persistentDataContainer
        val main = LevelledMobs.instance

        if (!pdc.has(NamespacedKeys.keySpawner, PersistentDataType.INTEGER)) {
            showMessage("command.levelledmobs.spawner.copy.vanilla-spawner")
            return
        }

        if (pdc.has(NamespacedKeys.keySpawnerCustomDropId, PersistentDataType.STRING)) {
            info.customDropId = pdc.get(
                NamespacedKeys.keySpawnerCustomDropId,
                PersistentDataType.STRING
            )
        }
        if (pdc.has(NamespacedKeys.keySpawnerCustomName, PersistentDataType.STRING)) {
            info.customName = pdc.get(
                NamespacedKeys.keySpawnerCustomName,
                PersistentDataType.STRING
            )
        }
        if (pdc.has(NamespacedKeys.keySpawnerMinLevel, PersistentDataType.INTEGER)) {
            val minLevel = pdc.get(
                NamespacedKeys.keySpawnerMinLevel,
                PersistentDataType.INTEGER
            )
            if (minLevel != null) {
                info.minLevel = minLevel
            }
        }
        if (pdc.has(NamespacedKeys.keySpawnerMaxLevel, PersistentDataType.INTEGER)) {
            val maxLevel = pdc.get(
                NamespacedKeys.keySpawnerMaxLevel,
                PersistentDataType.INTEGER
            )
            if (maxLevel != null) {
                info.maxLevel = maxLevel
            }
        }
        if (pdc.has(NamespacedKeys.keySpawnerLore, PersistentDataType.STRING)) {
            info.lore = pdc.get(NamespacedKeys.keySpawnerLore, PersistentDataType.STRING)
        }

        info.spawnType = cs.spawnedType!!
        info.minSpawnDelay = cs.minSpawnDelay
        info.maxSpawnDelay = cs.maxSpawnDelay
        info.maxNearbyEntities = cs.maxNearbyEntities
        info.delay = cs.delay
        info.requiredPlayerRange = cs.requiredPlayerRange
        info.spawnCount = cs.spawnCount
        info.spawnRange = cs.spawnRange

        main.levelledMobsCommand.spawnerSubCommand.generateSpawner(info)
    }

    private fun showInfo(
        player: Player,
        cs: CreatureSpawner
    ) {
        val pdc = cs.persistentDataContainer
        val sb = StringBuilder()

        if (pdc.has(NamespacedKeys.keySpawner, PersistentDataType.INTEGER)) {
            sb.append("LM Spawner")
            if (pdc.has(NamespacedKeys.keySpawnerCustomName, PersistentDataType.STRING)) {
                sb.append(": &7")
                sb.append(
                    pdc.get(NamespacedKeys.keySpawnerCustomName, PersistentDataType.STRING)
                )
                sb.append("&r\n")
            }
        } else {
            sb.append("Vanilla Spawner\n")
        }

        addSpawnerAttributeFromPdc_Int(
            "min level", NamespacedKeys.keySpawnerMinLevel, pdc,
            sb
        )
        addSpawnerAttributeFromPdc_Int(
            "max level", NamespacedKeys.keySpawnerMaxLevel, pdc,
            sb
        )
        sb.append('\n')
        addSpawnerAttribute("delay", cs.delay, sb)
        addSpawnerAttribute("max nearby entities", cs.maxNearbyEntities, sb)
        addSpawnerAttribute("min spawn delay", cs.minSpawnDelay, sb)
        sb.append('\n')
        addSpawnerAttribute("max spawn delay", cs.maxSpawnDelay, sb)
        addSpawnerAttribute("required player range", cs.requiredPlayerRange, sb)
        addSpawnerAttribute("spawn count", cs.spawnCount, sb)
        sb.append('\n')
        addSpawnerAttributeFromPdc_Str(NamespacedKeys.keySpawnerCustomDropId, pdc, sb)
        // customName
        if (cs.spawnedType != null)
            addSpawnerAttribute("spawn type", cs.spawnedType!!, sb)

        player.sendMessage(colorizeAll(sb.toString()))
    }

    private fun addSpawnerAttributeFromPdc_Int(
        name: String,
        key: NamespacedKey,
        pdc: PersistentDataContainer,
        sb: StringBuilder
    ) {
        if (!pdc.has(key, PersistentDataType.INTEGER)) {
            return
        }

        if (sb.substring(sb.length - 1) != "\n") {
            sb.append(", ")
        }

        sb.append("&7").append(name).append(": &b")
        sb.append(pdc.get(key, PersistentDataType.INTEGER))
        sb.append("&r")
    }

    private fun addSpawnerAttributeFromPdc_Str(
        key: NamespacedKey,
        pdc: PersistentDataContainer,
        sb: StringBuilder
    ) {
        if (!pdc.has(key, PersistentDataType.STRING)) {
            return
        }

        if (sb.substring(sb.length - 1) != "\n") {
            sb.append(", ")
        }

        sb.append("&7custom drop id: &b")
        sb.append(pdc.get(key, PersistentDataType.STRING)).append("&r")
    }

    private fun addSpawnerAttribute(
        name: String,
        value: Any,
        sb: StringBuilder
    ) {
        if (sb.substring(sb.length - 1) != "\n") {
            sb.append(", ")
        }
        sb.append("&7").append(name).append(": &b").append(value).append("&r")
    }
}