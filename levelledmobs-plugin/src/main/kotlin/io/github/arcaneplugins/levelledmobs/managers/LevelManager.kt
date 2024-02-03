package io.github.arcaneplugins.levelledmobs.managers

import io.papermc.paper.threadedregions.scheduler.ScheduledTask
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Collections
import java.util.LinkedList
import java.util.TreeMap
import java.util.WeakHashMap
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.TimeUnit
import java.util.function.Consumer
import io.github.arcaneplugins.levelledmobs.LevelInterface2
import io.github.arcaneplugins.levelledmobs.LevelledMobs
import io.github.arcaneplugins.levelledmobs.LivingEntityInterface
import io.github.arcaneplugins.levelledmobs.customdrops.EquippedItemsInfo
import io.github.arcaneplugins.levelledmobs.debug.DebugManager
import io.github.arcaneplugins.levelledmobs.events.MobPostLevelEvent
import io.github.arcaneplugins.levelledmobs.events.MobPreLevelEvent
import io.github.arcaneplugins.levelledmobs.events.SummonedMobPreLevelEvent
import io.github.arcaneplugins.levelledmobs.listeners.EntitySpawnListener
import io.github.arcaneplugins.levelledmobs.enums.Addition
import io.github.arcaneplugins.levelledmobs.result.AdditionalLevelInformation
import io.github.arcaneplugins.levelledmobs.debug.DebugType
import io.github.arcaneplugins.levelledmobs.enums.LevellableState
import io.github.arcaneplugins.levelledmobs.result.MinAndMaxHolder
import io.github.arcaneplugins.levelledmobs.misc.NamespacedKeys
import io.github.arcaneplugins.levelledmobs.misc.PickedUpEquipment
import io.github.arcaneplugins.levelledmobs.misc.QueueItem
import io.github.arcaneplugins.levelledmobs.misc.StringReplacer
import io.github.arcaneplugins.levelledmobs.result.NBTApplyResult
import io.github.arcaneplugins.levelledmobs.result.NametagResult
import io.github.arcaneplugins.levelledmobs.result.PlayerLevelSourceResult
import io.github.arcaneplugins.levelledmobs.result.PlayerNetherOrWorldSpawnResult
import io.github.arcaneplugins.levelledmobs.rules.CustomDropsRuleSet
import io.github.arcaneplugins.levelledmobs.enums.LevelledMobSpawnReason
import io.github.arcaneplugins.levelledmobs.enums.MobCustomNameStatus
import io.github.arcaneplugins.levelledmobs.enums.MobTamedStatus
import io.github.arcaneplugins.levelledmobs.enums.NametagVisibilityEnum
import io.github.arcaneplugins.levelledmobs.rules.strategies.RandomLevellingStrategy
import io.github.arcaneplugins.levelledmobs.rules.strategies.SpawnDistanceStrategy
import io.github.arcaneplugins.levelledmobs.rules.strategies.YDistanceStrategy
import io.github.arcaneplugins.levelledmobs.util.MythicMobUtils
import io.github.arcaneplugins.levelledmobs.util.Utils
import io.github.arcaneplugins.levelledmobs.wrappers.LivingEntityWrapper
import io.github.arcaneplugins.levelledmobs.wrappers.SchedulerResult
import io.github.arcaneplugins.levelledmobs.wrappers.SchedulerWrapper
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.attribute.Attribute
import org.bukkit.enchantments.EnchantmentTarget
import org.bukkit.entity.ChestedHorse
import org.bukkit.entity.Creeper
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.Horse
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.entity.Vehicle
import org.bukkit.entity.Zombie
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Generates levels and manages other functions related to levelling mobs
 *
 * @author lokka30, stumper66, CoolBoy, Esophose, 7smile7, Shevchik, Hugo5551, limzikiki
 * @since 2.4.0
 */
class LevelManager : LevelInterface2 {
    private var vehicleNoMultiplierItems = mutableListOf<Material>()
    val summonedOrSpawnEggs = WeakHashMap<LivingEntity, Any>()
    companion object{
        val summonedOrSpawnEggs_Lock = Any()
    }

    private var hasMentionedNBTAPIMissing = false
    var doCheckMobHash = false
    private val randomLevellingCache = mutableMapOf<String, RandomLevellingStrategy>()
    private var lastLEWCacheClearing: Instant? = null
    val entitySpawnListener = EntitySpawnListener()
    private var nametagAutoUpdateTask: SchedulerResult? = null
    private var nametagTimerTask: SchedulerResult? = null
    /**
     * The following entity types *MUST NOT* be levellable.
     */
    var forcedBlockedEntityTypes = mutableSetOf<EntityType>()

    fun load(){
        this.vehicleNoMultiplierItems.addAll(mutableListOf(
            Material.SADDLE,
            Material.LEATHER_HORSE_ARMOR,
            Material.IRON_HORSE_ARMOR,
            Material.GOLDEN_HORSE_ARMOR,
            Material.DIAMOND_HORSE_ARMOR
        ))

        this.forcedBlockedEntityTypes.addAll(
            mutableListOf(
                EntityType.AREA_EFFECT_CLOUD, EntityType.ARMOR_STAND, EntityType.ARROW, EntityType.BOAT,
                EntityType.DRAGON_FIREBALL, EntityType.DROPPED_ITEM, EntityType.EGG,
                EntityType.ENDER_CRYSTAL,
                EntityType.ENDER_PEARL, EntityType.ENDER_SIGNAL, EntityType.EXPERIENCE_ORB,
                EntityType.FALLING_BLOCK,
                EntityType.FIREBALL, EntityType.FIREWORK, EntityType.FISHING_HOOK,
                EntityType.ITEM_FRAME, EntityType.LEASH_HITCH, EntityType.LIGHTNING,
                EntityType.LLAMA_SPIT,
                EntityType.MINECART, EntityType.MINECART_CHEST, EntityType.MINECART_COMMAND,
                EntityType.MINECART_FURNACE,
                EntityType.MINECART_HOPPER, EntityType.MINECART_MOB_SPAWNER, EntityType.MINECART_TNT,
                EntityType.PAINTING,
                EntityType.PRIMED_TNT, EntityType.SMALL_FIREBALL, EntityType.SNOWBALL,
                EntityType.SPECTRAL_ARROW,
                EntityType.SPLASH_POTION, EntityType.THROWN_EXP_BOTTLE, EntityType.TRIDENT,
                EntityType.UNKNOWN,
                EntityType.WITHER_SKULL, EntityType.SHULKER_BULLET, EntityType.PLAYER
            )
        )
    }

    fun clearRandomLevellingCache() {
        randomLevellingCache.clear()
    }

    /**
     * This method generates a level for the mob. It utilises the levelling mode specified by the
     * administrator through the settings.yml configuration.
     *
     *
     * Thread-safety intended, but not tested.
     *
     * @param lmEntity the entity to generate a level for
     * @return a level for the entity
     */
    override fun generateLevel(lmEntity: LivingEntityWrapper): Int {
        return generateLevel(lmEntity, -1, -1)
    }

    /**
     * This method generates a level for the mob. It utilises the levelling mode specified by the
     * administrator through the settings.yml configuration.
     *
     *
     * Thread-safety intended, but not tested.
     *
     * @param lmEntity     the entity to generate a level for
     * @param minLevel the minimum level to be used for the mob
     * @param maxLevel the maximum level to be used for the mob
     * @return a level for the entity
     */
    override fun generateLevel(
        lmEntity: LivingEntityWrapper,
        minLevel: Int,
        maxLevel: Int
    ): Int {
        var useMinLevel = minLevel
        var useMaxLevel = maxLevel

        if (useMinLevel == -1 || useMaxLevel == -1) {
            val levels: MinAndMaxHolder = getMinAndMaxLevels(lmEntity)
            if (useMinLevel == -1) {
                useMinLevel = levels.min
            }
            if (useMaxLevel == -1) {
                useMaxLevel = levels.max
            }
        }

        val levellingStrategy = LevelledMobs.instance.rulesManager.getRuleLevellingStrategy(
            lmEntity
        )

        if (levellingStrategy is YDistanceStrategy
            || levellingStrategy is SpawnDistanceStrategy
        ) {
            return levellingStrategy.generateLevel(lmEntity, useMinLevel, useMaxLevel)
        }

        // if no levelling strategy was selected then we just use a random number between min and max
        if (useMinLevel == useMaxLevel) {
            return useMinLevel
        }

        val randomLevelling =
            if ((levellingStrategy is RandomLevellingStrategy)) levellingStrategy
            else null

        return generateRandomLevel(randomLevelling, useMinLevel, useMaxLevel)
    }

    private fun generateRandomLevel(
        randomLevellingPre: RandomLevellingStrategy?,
        minLevel: Int,
        maxLevel: Int
    ): Int {
        var randomLevelling = randomLevellingPre
        if (randomLevelling == null) {
            // used the caches defaults if it exists, otherwise add it to the cache
            if (randomLevellingCache.containsKey("default")) {
                randomLevelling = randomLevellingCache["default"]
            } else {
                randomLevelling = RandomLevellingStrategy()
                randomLevellingCache["default"] = randomLevelling
            }
        } else {
            // used the caches one if it exists, otherwise add it to the cache
            val checkName = "$minLevel-$maxLevel: $randomLevelling"

            if (randomLevellingCache.containsKey(checkName)) {
                randomLevelling = randomLevellingCache[checkName]
            } else {
                randomLevelling.populateWeightedRandom(minLevel, maxLevel)
                randomLevellingCache[checkName] = randomLevelling
            }
        }

        return randomLevelling!!.generateLevel(minLevel, maxLevel)
    }

    fun getPlayerLevelSourceNumber(
        player: Player?,
        lmEntity: LivingEntityWrapper,
        variableToUse: String
    ): PlayerLevelSourceResult {
        if (player == null) {
            return PlayerLevelSourceResult(1)
        }

        val origLevelSource: Double
        var homeNameUsed = "spawn"

        if ("%level%".equals(variableToUse, ignoreCase = true)) {
            origLevelSource = player.level.toDouble()
        } else if ("%exp%".equals(variableToUse, ignoreCase = true)) {
            origLevelSource = player.exp.toDouble()
        } else if ("%exp-to-level%".equals(variableToUse, ignoreCase = true)) {
            origLevelSource = player.expToLevel.toDouble()
        } else if ("%total-exp%".equals(variableToUse, ignoreCase = true)) {
            origLevelSource = player.totalExperience.toDouble()
        } else if ("%world_time_ticks%".equals(variableToUse, ignoreCase = true)) {
            origLevelSource = player.world.time.toDouble()
        } else if ("%home_distance%".equals(variableToUse, ignoreCase = true)
            || "%home_distance_with_bed%".equals(variableToUse, ignoreCase = true)
        ) {
            val allowBed = "%home_distance_with_bed%".equals(variableToUse, ignoreCase = true)
            val netherOrWorldSpawnResult: PlayerNetherOrWorldSpawnResult
            val result = ExternalCompatibilityManager.getPlayerHomeLocation(
                player, allowBed
            )
            if (result.homeNameUsed != null) {
                homeNameUsed = result.homeNameUsed!!
            }

            var useLocation = result.location
            if (useLocation == null || useLocation.world != player.world) {
                netherOrWorldSpawnResult = Utils.getPortalOrWorldSpawn(player)
                useLocation = netherOrWorldSpawnResult.location
                homeNameUsed = if (netherOrWorldSpawnResult.isWorldPortalLocation) {
                    "world_portal"
                } else if (netherOrWorldSpawnResult.isNetherPortalLocation) {
                    "nether_portal"
                } else {
                    "spawn"
                }
            }

            if (result.resultMessage != null) {
                DebugManager.log(DebugType.PLAYER_LEVELLING, lmEntity) { result.resultMessage }
            }

            origLevelSource = useLocation!!.distance(player.location)
        } else if ("%bed_distance%".equals(variableToUse, ignoreCase = true)) {
            var useLocation = player.bedSpawnLocation
            homeNameUsed = "bed"

            if (useLocation == null || useLocation.world !== player.world) {
                val result: PlayerNetherOrWorldSpawnResult = Utils.getPortalOrWorldSpawn(
                    player
                )
                useLocation = result.location
                homeNameUsed = if (result.isWorldPortalLocation) {
                    "world_portal"
                } else if (result.isNetherPortalLocation) {
                    "nether_portal"
                } else {
                    "spawn"
                }
            }

            origLevelSource = useLocation!!.distance(player.location)
        } else {
            var usePlayerLevel = false
            var papiResult: String? = null

            if (ExternalCompatibilityManager.hasPapiInstalled()) {
                papiResult = ExternalCompatibilityManager.getPapiPlaceholder(player, variableToUse)
                if (papiResult.isEmpty()) {
                    val l = player.location
                    DebugManager.log(DebugType.PLAYER_LEVELLING, lmEntity) {
                        String.format(
                            "Got blank result for '%s' from PAPI. Player %s at %s,%s,%s in %s",
                            variableToUse, player.name, l.blockX, l.blockY,
                            l.blockZ, player.world.name
                        )
                    }
                    usePlayerLevel = true
                }
            } else {
                Utils.logger.warning(
                    "PlaceHolderAPI is not installed, unable to get variable $variableToUse"
                )
                usePlayerLevel = true
            }

            if (usePlayerLevel) {
                origLevelSource = player.level.toDouble()
            } else {
                val l = player.location
                if (papiResult.isNullOrEmpty()) {
                    origLevelSource = player.level.toDouble()
                    DebugManager.log(DebugType.PLAYER_LEVELLING, lmEntity) {
                        String.format(
                            "Got blank result for '%s' from PAPI. Player %s at %s,%s,%s in %s",
                            variableToUse, player.name, l.blockX, l.blockY,
                            l.blockZ, player.world.name
                        )
                    }
                } else {
                    if (Utils.isDouble(papiResult)) {
                        origLevelSource = try {
                            papiResult.toDouble()
                        } catch (ignored: Exception) {
                            player.level.toDouble()
                        }
                    } else {
                        val result = PlayerLevelSourceResult(papiResult)
                        result.homeNameUsed = homeNameUsed
                        return result
                    }
                }
            }
        }

        val sourceResult = PlayerLevelSourceResult(Math.round(origLevelSource).toInt())
        val maxRandomVariance = LevelledMobs.instance.rulesManager.getRuleMaxRandomVariance(lmEntity)

        if (maxRandomVariance != null) {
            sourceResult.randomVarianceResult = ThreadLocalRandom.current().nextInt(0, maxRandomVariance + 1)
            if (ThreadLocalRandom.current().nextBoolean()) {
                sourceResult.randomVarianceResult = sourceResult.randomVarianceResult!! * -1
            }
        }

        sourceResult.homeNameUsed = homeNameUsed
        return sourceResult
    }

    fun getMinAndMaxLevels(lmInterface: LivingEntityInterface): MinAndMaxHolder {
        // final EntityType entityType, final boolean isAdultEntity, final String worldName
        // if called from summon command then lmEntity is null

        val main = LevelledMobs.instance
        var minLevel: Int = main.rulesManager.getRuleMobMinLevel(lmInterface)
        var maxLevel: Int = main.rulesManager.getRuleMobMaxLevel(lmInterface)

        if (main.configUtils.playerLevellingEnabled && lmInterface is LivingEntityWrapper && lmInterface.playerForLevelling != null) {
            val options = main.rulesManager.getRulePlayerLevellingOptions(
                lmInterface
            )

            var playerLevellingResults: MinAndMaxHolder? = null
            if (options != null && options.getEnabled) {
                playerLevellingResults = options.getPlayerLevels(lmInterface)
            }

            if (playerLevellingResults != null) {
                // this will only be false if no tiers were met and there was a cap specified
                if (playerLevellingResults.useMin) minLevel = playerLevellingResults.min
                maxLevel = playerLevellingResults.max
            }
        }

        // this will prevent an unhandled exception:
        minLevel = max(minLevel.toDouble(), 1.0).toInt()
        maxLevel = max(maxLevel.toDouble(), 1.0).toInt()
        minLevel = min(minLevel.toDouble(), maxLevel.toDouble()).toInt()

        return MinAndMaxHolder(minLevel, maxLevel)
    }

    // This sets the levelled currentDrops on a levelled mob that just died.
    fun setLevelledItemDrops(
        lmEntity: LivingEntityWrapper,
        currentDrops: MutableList<ItemStack>, disableItemBoost: Boolean
    ) {
        val vanillaDrops = currentDrops.size
        // this accomodates chested animals, saddles and armor on ridable creatures
        val dropsToMultiply = getDropsToMultiply(lmEntity, currentDrops)
        val customDrops = mutableListOf<ItemStack>()
        currentDrops.clear()

        val main = LevelledMobs.instance
        val doNotMultiplyDrops = disableItemBoost ||
                main.rulesManager.getRuleCheckIfNoDropMultiplierEntitiy(lmEntity)
        var hasOverride = false

        if (lmEntity.lockedCustomDrops != null || main.rulesManager.getRuleUseCustomDropsForMob(lmEntity).useDrops) {
            // custom drops also get multiplied in the custom drops handler
            val dropResult = main.customDropsHandler.getCustomItemDrops(
                lmEntity,
                customDrops, false
            )

            val mmInfo = MythicMobUtils.getMythicMobInfo(lmEntity)
            if (mmInfo != null && mmInfo.preventOtherDrops) {
                hasOverride = true
            }

            if (dropResult.hasOverride) {
                hasOverride = true
            }

            if (hasOverride) {
                removeVanillaDrops(lmEntity, dropsToMultiply)
            }
        }

        var additionUsed = 0

        if (!doNotMultiplyDrops && dropsToMultiply.isNotEmpty()) {
            // Get currentDrops added per level valu
            val additionValue: Float = main.mobDataManager.getAdditionsForLevel(
                lmEntity,
                Addition.CUSTOM_ITEM_DROP, 2.0f
            )
            if (additionValue == Float.MIN_VALUE) {
                DebugManager.log(DebugType.SET_LEVELLED_ITEM_DROPS, lmEntity) {
                    String.format(
                        "&7Mob: &b%s&7, mob-lvl: &b%s&7, removing any drops present",
                        lmEntity.nameIfBaby, lmEntity.getMobLevel()
                    )
                }
                currentDrops.clear()
                return
            }

            val addition = BigDecimal.valueOf(additionValue.toDouble())
                .setScale(0, RoundingMode.HALF_DOWN).intValueExact() // truncate double to int
            additionUsed = addition

            // Modify current drops
            for (currentDrop in dropsToMultiply) {
                multiplyDrop(lmEntity, currentDrop, addition.toDouble(), false)
            }
        }

        if (customDrops.isNotEmpty()) {
            currentDrops.addAll(customDrops)
        }
        if (dropsToMultiply.isNotEmpty()) {
            currentDrops.addAll(dropsToMultiply)
        }
        val nameWithOverride = if (hasOverride) lmEntity.nameIfBaby + " (override)" else lmEntity.nameIfBaby
        val additionUsedFinal = additionUsed
        DebugManager.log(DebugType.SET_LEVELLED_ITEM_DROPS, lmEntity) {
            String.format(
                "&7Mob: &b%s&7, mob-lvl: &b%s&7, vanilla drops: &b%s&7, all drops: &b%s&7, addition: &b%s&7.",
                nameWithOverride, lmEntity.getMobLevel(), vanillaDrops, currentDrops.size,
                additionUsedFinal
            )
        }
    }

    fun multiplyDrop(
        lmEntity: LivingEntityWrapper,
        currentDrop: ItemStack,
        addition: Double,
        isCustomDrop: Boolean
    ) {
        val oldAmount = currentDrop.amount

        if (isCustomDrop || LevelledMobs.instance.mobDataManager.isLevelledDropManaged(
                lmEntity.livingEntity.type, currentDrop.type
            )
        ) {
            var useAmount = Math.round(currentDrop.amount + (currentDrop.amount.toDouble() * addition)).toInt()
            if (useAmount > currentDrop.maxStackSize) {
                useAmount = currentDrop.maxStackSize
            }
            currentDrop.amount = useAmount
            DebugManager.log(DebugType.SET_LEVELLED_ITEM_DROPS, lmEntity) {
                String.format(
                    "&7Drop: &b%s&7, old amount: &b%s&7, addition value: &b%s&7, new amount: &b%s&7.",
                    currentDrop.type, oldAmount, addition, currentDrop.amount
                )
            }
        } else {
            DebugManager.log(DebugType.SET_LEVELLED_ITEM_DROPS, lmEntity) { "&7Item was unmanaged." }
        }
    }

    private fun getDropsToMultiply(
        lmEntity: LivingEntityWrapper,
        drops: MutableList<ItemStack>
    ): MutableList<ItemStack> {
        val results = mutableListOf<ItemStack>()
        results.addAll(drops)

        // we only need to check for chested animals and 'vehicles' since they can have saddles and armor
        // those items shouldn't get multiplied
        if (lmEntity.livingEntity is ChestedHorse
            && (lmEntity.livingEntity as ChestedHorse).isCarryingChest
        ) {
            val inv = (lmEntity.livingEntity as ChestedHorse).inventory
            val chestItems = inv.contents
            // look thru the animal's inventory for leather. That is the only item that will get duplicated
            for (item in chestItems) {
                if (item!!.type == Material.LEATHER) {
                    return mutableListOf(item)
                }
            }

            // if we made it here it didn't drop leather so don't return anything
            results.clear()
            return results
        }

        if (lmEntity.livingEntity !is Vehicle) {
            return results
        }

        for (i in results.indices.reversed()) {
            // remove horse armor or saddles
            val item = results[i]
            if (vehicleNoMultiplierItems.contains(item.type)) // saddle or horse armor
            {
                results.removeAt(i)
            }
        }

        return results
    }

    fun removeVanillaDrops(
        lmEntity: LivingEntityWrapper,
        drops: MutableList<ItemStack>
    ) {
        var hadSaddle = false
        val itemsToKeep = mutableListOf<ItemStack>()

        if (lmEntity.livingEntity is ChestedHorse
            && (lmEntity.livingEntity as ChestedHorse).isCarryingChest
        ) {
            val inv = (lmEntity.livingEntity as ChestedHorse).inventory
            inv.contents.plus(itemsToKeep)
            itemsToKeep.add(ItemStack(Material.CHEST))
        } else if (lmEntity.livingEntity is Vehicle) {
            for (itemStack in drops) {
                if (itemStack.type == Material.SADDLE) {
                    hadSaddle = true
                    break
                }
            }
        }

        if (LevelledMobs.instance.ver.isRunningPaper) {
            val pickedUpItems: List<ItemStack> = PickedUpEquipment(lmEntity).getMobPickedUpItems()

            for (mobItem in drops) {
                for (foundItem in pickedUpItems) {
                    if (mobItem.isSimilar(foundItem)) {
                        itemsToKeep.add(mobItem)
                        break
                    }
                }
            }
        }

        drops.clear()
        drops.addAll(itemsToKeep)
        if (hadSaddle) {
            drops.add(ItemStack(Material.SADDLE))
        }
    }

    //Calculates the XP dropped when a levellable creature dies.
    fun getLevelledExpDrops(
        lmEntity: LivingEntityWrapper,
        xp: Double
    ): Int {
        if (lmEntity.isLevelled) {
            val dropAddition: Float = LevelledMobs.instance.mobDataManager.getAdditionsForLevel(
                lmEntity,
                Addition.CUSTOM_XP_DROP, 3.0f
            )
            var newXp = 0.0

            if (dropAddition == Float.MIN_VALUE) {
                DebugManager.log(DebugType.SET_LEVELLED_XP_DROPS, lmEntity) {
                    String.format(
                        "&7Mob: &b%s&7: lvl: &b%s&7, xp-vanilla: &b%s&7, new-xp: &b0&7",
                        lmEntity.nameIfBaby, lmEntity.getMobLevel(), xp
                    )
                }
                return 0
            }

            if (dropAddition > -1) {
                newXp = Math.round(xp + (xp * dropAddition)).toDouble()
            }

            val newXpFinal = newXp.toInt()
            DebugManager.log(DebugType.SET_LEVELLED_XP_DROPS, lmEntity) {
                String.format(
                    "&7Mob: &b%s&7: lvl: &b%s&7, xp-vanilla: &b%s&7, new-xp: &b%s&7",
                    lmEntity.nameIfBaby, lmEntity.getMobLevel(), xp, newXpFinal
                )
            }
            return newXp.toInt()
        } else {
            return xp.toInt()
        }
    }

    fun getNametag(
        lmEntity: LivingEntityWrapper,
        isDeathNametag: Boolean
    ): NametagResult {
        return getNametag(lmEntity, isDeathNametag, false)
    }

    fun getNametag(
        lmEntity: LivingEntityWrapper,
        isDeathNametag: Boolean,
        preserveMobName: Boolean
    ): NametagResult {
        var usePreserveMobName = preserveMobName
        var nametag: StringReplacer
        var customDeathMessage: String? = null
        val main = LevelledMobs.instance
        if (isDeathNametag) {
            nametag = StringReplacer(main.rulesManager.getRuleNametagCreatureDeath(lmEntity))
        } else {
            checkLockedNametag(lmEntity)

            val nametagText =
                if (lmEntity.lockedNametag == null || lmEntity.lockedNametag!!.isEmpty()) main.rulesManager.getRuleNametag(
                    lmEntity
                ) else lmEntity.lockedNametag!!
            nametag = StringReplacer(nametagText)
        }

        if ("disabled".equals(nametag.text, ignoreCase = true) || "none".equals(nametag.text, ignoreCase = true)) {
            return NametagResult(null)
        }

        if (isDeathNametag) {
            val deathMessage = main.rulesManager.getDeathMessage(lmEntity)
            if (!deathMessage.isNullOrEmpty()) {
                nametag = StringReplacer(deathMessage.replace("%death_nametag%", nametag.text))
                val player = lmEntity.associatedPlayer
                nametag.replace("%player%", if (player != null) player.name + "&r" else "")
                nametag.text = replaceStringPlaceholders(nametag.text, lmEntity, true, player, usePreserveMobName)
                usePreserveMobName = true

                customDeathMessage =
                    if (nametag.contains("{DisplayName}")) main.rulesManager.getRuleNametagCreatureDeath(lmEntity) else nametag.text
            }
        }

        // ignore if 'disabled'
        if (nametag.isEmpty) {
            val useCustomNameForNametags: Boolean = main.helperSettings.getBoolean(
                "use-customname-for-mob-nametags"
            )
            return if (useCustomNameForNametags) {
                NametagResult(lmEntity.typeName)
            } else {
                @Suppress("DEPRECATION")
                NametagResult(lmEntity.livingEntity.customName) // CustomName can be null, that is meant to be the case.
            }
        }
        if (!lmEntity.isLevelled) {
            nametag.text = ""
        }

        return updateNametag(lmEntity, nametag, usePreserveMobName, customDeathMessage)
    }

    fun updateNametag(
        lmEntity: LivingEntityWrapper,
        nametag: StringReplacer,
        preserveMobName: Boolean,
        customDeathMessage: String?
    ): NametagResult {
        if (nametag.isEmpty) {
            val result = NametagResult(nametag.text)
            result.customDeathMessage = customDeathMessage
            return result
        }

        checkLockedNametag(lmEntity)
        val overridenName = if (lmEntity.lockedOverrideName == null) LevelledMobs.instance.rulesManager.getRuleEntityOverriddenName(
            lmEntity,
            false
        ) else lmEntity.lockedOverrideName!!

        replaceStringPlaceholders(nametag, lmEntity, false, null, preserveMobName)

        var indicatorStr = ""
        var colorOnly = ""

        if (nametag.text.contains("%health-indicator%") ||
            nametag.text.contains("%health-indicator-color%")
        ) {
            val indicator = LevelledMobs.instance.rulesManager.getRuleNametagIndicator(lmEntity)

            if (indicator != null) {
                val result = indicator.formatHealthIndicator(lmEntity)
                indicatorStr = result.formattedString + "&r"
                colorOnly = result.colorOnly
            }
        }

        nametag.replace("%health-indicator%", indicatorStr)
        nametag.replace("%health-indicator-color%", colorOnly)

        if (nametag.text.contains("%") && ExternalCompatibilityManager.hasPapiInstalled()) {
            nametag.text = ExternalCompatibilityManager.getPapiPlaceholder(null, nametag.text)
        }

        val result = NametagResult(nametag.text)
        // this field is only used for sending nametags to client
        result.overriddenName = overridenName
        result.customDeathMessage = customDeathMessage
        result.killerMob = lmEntity.livingEntity

        return result
    }

    private fun checkLockedNametag(lmEntity: LivingEntityWrapper) {
        synchronized(lmEntity.livingEntity.persistentDataContainer) {
            val doLockSettings: Int?
            if (lmEntity.pdc
                    .has(NamespacedKeys.lockSettings, PersistentDataType.INTEGER)
            ) {
                doLockSettings = lmEntity.pdc
                    .get(NamespacedKeys.lockSettings, PersistentDataType.INTEGER)
                if (doLockSettings == null || doLockSettings != 1) {
                    return
                }
            } else {
                return
            }

            if (lmEntity.pdc
                    .has(NamespacedKeys.lockedNametag, PersistentDataType.STRING)
            ) {
                lmEntity.lockedNametag = lmEntity.pdc
                    .get(NamespacedKeys.lockedNametag, PersistentDataType.STRING)
            }
            if (lmEntity.pdc
                    .has(NamespacedKeys.lockedNameOverride, PersistentDataType.STRING)
            ) {
                lmEntity.lockedOverrideName = lmEntity.pdc
                    .get(NamespacedKeys.lockedNameOverride, PersistentDataType.STRING)
            }
        }
    }

    fun replaceStringPlaceholders(
        text: String,
        lmEntity: LivingEntityWrapper,
        usePAPI: Boolean,
        player: Player?,
        preserveMobName: Boolean
    ): String {
        return replaceStringPlaceholders(
            StringReplacer(text),
            lmEntity,
            usePAPI,
            player,
            preserveMobName
        )
    }

    @Suppress("DEPRECATION")
    private fun replaceStringPlaceholders(
        text: StringReplacer,
        lmEntity: LivingEntityWrapper,
        usePAPI: Boolean,
        player: Player?,
        preserveMobName: Boolean
    ): String {
        val maxHealth: Double = getMobAttributeValue(lmEntity)
        val entityHealth: Double = getMobHealth(lmEntity)
        val entityHealthRounded = if (entityHealth < 1.0 && entityHealth > 0.0) 1 else Utils.round(entityHealth).toInt()
        val roundedMaxHealth: String = java.lang.String.valueOf(Utils.round(maxHealth))
        val roundedMaxHealthInt = (Utils.round(maxHealth).toInt()).toString()
        val percentHealthTemp = Math.round(entityHealth / maxHealth * 100.0).toDouble()
        val percentHealth = if (percentHealthTemp < 1.0) 1 else percentHealthTemp.toInt()
        val playerId = player?.uniqueId?.toString() ?: ""
        val playerName = player?.name ?: ""

        var tieredPlaceholder = LevelledMobs.instance.rulesManager.getRuleTieredPlaceholder(lmEntity)
        if (tieredPlaceholder == null) {
            tieredPlaceholder = ""
        }

        // replace them placeholders ;)
        text.replace("%mob-lvl%", lmEntity.getMobLevel())
        text.replace(
            "%entity-name%",
            Utils.capitalize(lmEntity.nameIfBaby.replace("_", " "))
        )
        text.replace("%entity-health%", Utils.round(entityHealth))
        text.replace("%entity-health-rounded%", entityHealthRounded)
        text.replace("%entity-max-health%", roundedMaxHealth)
        text.replace("%entity-max-health-rounded%", roundedMaxHealthInt)
        text.replace("%heart_symbol%", "❤")
        text.replace("%tiered%", tieredPlaceholder)
        text.replace("%wg_region%", lmEntity.wgRegionName)
        text.replace("%world%", lmEntity.worldName)
        text.replaceIfExists("%location%") {
                "${lmEntity.livingEntity.location.blockX} " +
                "${lmEntity.livingEntity.location.blockX} " +
                "${lmEntity.livingEntity.location.blockZ}"
        }
        text.replace("%health%-percent%", percentHealth)
        text.replace("%x%", lmEntity.livingEntity.location.blockX)
        text.replace("%y%", lmEntity.livingEntity.location.blockY)
        text.replace("%z%", lmEntity.livingEntity.location.blockZ)
        text.replace("%player-uuid%", playerId)
        text.replace("%player%", playerName)
        text.replaceIfExists("%displayname%") {
            val useCustomNameForNametags = LevelledMobs.instance.helperSettings.getBoolean(
        "use-customname-for-mob-nametags"
            )
            val overridenName =
                if (lmEntity.lockedOverrideName == null) LevelledMobs.instance.rulesManager.getRuleEntityOverriddenName(
                    lmEntity,
                    false
                ) else lmEntity.lockedOverrideName

            val hasOverridenName = !overridenName.isNullOrEmpty()
            var useDisplayname = overridenName

            if (preserveMobName) useDisplayname = "{DisplayName}"
            else if (!hasOverridenName) useDisplayname =
                Utils.capitalize(lmEntity.typeName.replace("_".toRegex(), " "))

            if (lmEntity.livingEntity.customName != null && !useCustomNameForNametags) {
                useDisplayname = lmEntity.livingEntity.customName
            }
            useDisplayname
        }

        if (usePAPI && text.contains("%") && ExternalCompatibilityManager.hasPapiInstalled()) {
            text.text = ExternalCompatibilityManager.getPapiPlaceholder(player, text.text)
        }

        return text.text
    }

    fun updateNametagWithDelay(lmEntity: LivingEntityWrapper) {
        val scheduler = SchedulerWrapper(lmEntity.livingEntity) {
            updateNametag(lmEntity)
            lmEntity.free()
        }

        lmEntity.inUseCount.getAndIncrement()
        scheduler.runDelayed(1L)
    }

    fun updateNametag(lmEntity: LivingEntityWrapper) {
        val nametag = getNametag(lmEntity, isDeathNametag = false, preserveMobName = true)

        val queueItem = QueueItem(
            lmEntity,
            nametag,
            lmEntity.livingEntity.world.players
        )

        LevelledMobs.instance.nametagQueueManager.addToQueue(queueItem)
    }

    fun updateNametag(
        lmEntity: LivingEntityWrapper,
        nametag: NametagResult,
        players: MutableList<Player>
    ) {
        LevelledMobs.instance.nametagQueueManager.addToQueue(QueueItem(lmEntity, nametag, players))
    }


    /*
     * Credit
     * - Thread: https://www.spigotmc.org/threads/changing-an-entitys-nametag-with-packets.482855/
     *
     * - Users:
     *   - @CoolBoy (https://www.spigotmc.org/members/CoolBoy.102500/)
     *   - @Esophose (https://www.spigotmc.org/members/esophose.34168/)
     *   - @7smile7 (https://www.spigotmc.org/members/7smile7.43809/)
     */

    fun startNametagAutoUpdateTask() {
        Utils.logger.info("&fTasks: &7Starting async nametag auto update task...")

        val main = LevelledMobs.instance
        val period = main.helperSettings.getInt(
            "async-task-update-period",6
        ).toLong() // run every ? seconds.
        this.doCheckMobHash = main.helperSettings.getBoolean("check-mob-hash", true)

        if (main.ver.isRunningFolia) {
            val bgThread =
                Consumer<ScheduledTask> { _: ScheduledTask? ->
                    if (Bukkit.getOnlinePlayers().isEmpty()) return@Consumer
                    var firstPlayer: Player? = null
                    for (player in Bukkit.getOnlinePlayers()) {
                        firstPlayer = player
                        break
                    }
                    if (firstPlayer == null) return@Consumer

                    val task =
                        Consumer<ScheduledTask> { _: ScheduledTask? ->
                            checkLEWCache()
                            val entitiesPerPlayer = enumerateNearbyEntities()
                            if (entitiesPerPlayer != null) {
                                runNametagCheckaSync(entitiesPerPlayer)
                            }
                        }
                    firstPlayer.scheduler.run(main, task, null)
                }

            nametagTimerTask =
                SchedulerResult(Bukkit.getAsyncScheduler().runAtFixedRate(main, bgThread, 0, period, TimeUnit.SECONDS))
        } else {
            val runnable = Runnable {
                val entitiesPerPlayer = enumerateNearbyEntities()
                if (entitiesPerPlayer != null) {
                    val runnable2 = Runnable {
                        checkLEWCache()
                        runNametagCheckaSync(entitiesPerPlayer)
                    }
                    Bukkit.getScheduler().runTaskAsynchronously(main, runnable2)
                }
            }
            nametagTimerTask = SchedulerResult(Bukkit.getScheduler().runTaskTimer(main, runnable, 0, 20 * period))
        }
    }

    private fun checkLEWCache() {
        if (lastLEWCacheClearing == null) {
            lastLEWCacheClearing = Instant.now()
            return
        }

        val duration = lastLEWCacheClearing!!.until(Instant.now(), ChronoUnit.MILLIS)
        val configDuration = LevelledMobs.instance.helperSettings.getIntTimeUnitMS(
             "lew-cache-clear-period", 180000L
        )!!

        if (duration >= configDuration) {
            DebugManager.log(DebugType.DEVELOPER_LEW_CACHE) {
                "Reached $configDuration ms, clearing LEW cache, " + LivingEntityWrapper.getLEWDebug()
            }

            lastLEWCacheClearing = Instant.now()
            LivingEntityWrapper.clearCache()
        }
    }

    private fun enumerateNearbyEntities(): MutableMap<Player, MutableList<Entity>>? {
        val entitiesPerPlayer = mutableMapOf<Player, MutableList<Entity>>()
        val checkDistance = LevelledMobs.instance.helperSettings.getInt(
            "async-task-max-blocks-from-player", 100
        )

        for (player in Bukkit.getOnlinePlayers()) {
            val entities = player.getNearbyEntities(
                checkDistance.toDouble(),
                checkDistance.toDouble(), checkDistance.toDouble()
            )
            entitiesPerPlayer[player] = entities
        }
        return if (entitiesPerPlayer.isEmpty()) {
            null
        } else {
            entitiesPerPlayer
        }
    }

    fun startNametagTimer() {
        val scheduler = SchedulerWrapper {  LevelledMobs.instance.nametagTimerChecker.checkNametags() }
        scheduler.runTaskTimerAsynchronously(0, 1000)
    }

    private fun runNametagCheckaSync(
        entitiesPerPlayer: MutableMap<Player, MutableList<Entity>>
    ) {
        val entityToPlayer = mutableMapOf<LivingEntityWrapper, MutableList<Player>>()

        val scheduler = SchedulerWrapper {
            for (player in entitiesPerPlayer.keys) {
                for (entity in entitiesPerPlayer[player]!!) {
                    checkEntity(entity, player, entityToPlayer)
                }
            }
        }

        if (LevelledMobs.instance.ver.isRunningFolia) {
            var firstEntity: Entity? = null
            for (player in entitiesPerPlayer.keys) {
                for (entity in entitiesPerPlayer[player]!!) {
                    firstEntity = entity
                    break
                }
                if (firstEntity != null) break
            }
            scheduler.entity = firstEntity
        }

        scheduler.runDirectlyInBukkit = true
        scheduler.run()

        for ((lmEntity, value) in entityToPlayer) {
            if (entityToPlayer.containsKey(lmEntity)) {
                checkEntityForPlayerLevelling(lmEntity, value)
            }

            lmEntity.free()
        }
    }

    private fun checkEntity(
        entity: Entity,
        player: Player,
        entityToPlayer: MutableMap<LivingEntityWrapper, MutableList<Player>>
    ) {
        if (!entity.isValid) {
            return  // async task, entity can despawn whilst it is running
        }

        // Mob must be a livingentity that is ...living.
        if (entity !is LivingEntity || entity is Player
            || !entity.isValid()
        ) {
            return
        }
        // this is mostly so for spawner mobs and spawner egg mobs as they have a 20 tick delay in before proessing
        if (entity.getTicksLived() < 30) {
            return
        }

        var wrapperHasReference = false
        val lmEntity = LivingEntityWrapper.getInstance(entity)
        lmEntity.associatedPlayer = player
        if (doCheckMobHash && Utils.checkIfMobHashChanged(lmEntity)) {
            lmEntity.reEvaluateLevel = true
            lmEntity.isRulesForceAll = true
            lmEntity.wasPreviouslyLevelled = lmEntity.isLevelled
        }

        val main = LevelledMobs.instance
        if (lmEntity.isLevelled) {
            var skipLevelling = (lmEntity.getSpawnReason() == LevelledMobSpawnReason.LM_SPAWNER ||
                    lmEntity.getSpawnReason() == LevelledMobSpawnReason.LM_SUMMON
                    )
            if (main.configUtils.playerLevellingEnabled && !lmEntity.isRulesForceAll && !checkIfReadyForRelevelling(
                    lmEntity
                )
            ) {
                skipLevelling = true
            }
            if (main.configUtils.playerLevellingEnabled && !skipLevelling) {
                val hasKey = entityToPlayer.containsKey(lmEntity)
                val players = if (hasKey) entityToPlayer[lmEntity]!! else LinkedList()
                players.add(player)
                if (!hasKey) {
                    entityToPlayer[lmEntity] = players
                }
                wrapperHasReference = true
            }

            if (!lmEntity.isPopulated) {
                return
            }
            val nametagVisibilityEnums = main.rulesManager.getRuleCreatureNametagVisbility(
                lmEntity
            )
            val nametagVisibleTime = lmEntity.getNametagCooldownTime()
            if (nametagVisibleTime > 0L &&
                nametagVisibilityEnums.contains(NametagVisibilityEnum.TARGETED) &&
                lmEntity.livingEntity.hasLineOfSight(player)
            ) {
                if (lmEntity.playersNeedingNametagCooldownUpdate == null) {
                    lmEntity.playersNeedingNametagCooldownUpdate = HashSet()
                }
                lmEntity.playersNeedingNametagCooldownUpdate!!.add(player)
            }

            checkLevelledEntity(lmEntity, player)
        } else {
            val wasBabyMob: Boolean
            synchronized(lmEntity.livingEntity.persistentDataContainer) {
                wasBabyMob = lmEntity.pdc
                    .has(NamespacedKeys.wasBabyMobKey, PersistentDataType.INTEGER)
            }
            if (lmEntity.isPopulated
            ) { // a hack to prevent a null exception that was reported
                val levellableState = main.levelInterface.getLevellableState(
                    lmEntity
                )
                if (!lmEntity.isBabyMob &&
                    wasBabyMob && levellableState == LevellableState.ALLOWED
                ) {
                    // if the mob was a baby at some point, aged and now is eligable for levelling, we'll apply a level to it now
                    DebugManager.log(DebugType.ENTITY_MISC, lmEntity) {
                        ("&b" + lmEntity.typeName
                                + " &7was a baby and is now an adult, applying levelling rules")
                    }

                    main.mobsQueueManager.addToQueue(QueueItem(lmEntity, null))
                } else if (levellableState == LevellableState.ALLOWED) {
                    main.mobsQueueManager.addToQueue(QueueItem(lmEntity, null))
                }
            }
        }

        if (!wrapperHasReference) {
            lmEntity.free()
        }
    }

    private fun checkIfReadyForRelevelling(lmEntity: LivingEntityWrapper): Boolean {
        val opts = LevelledMobs.instance.rulesManager.getRulePlayerLevellingOptions(lmEntity)
        if (opts?.preserveEntityTime == null) {
            return true
        }

        if (!lmEntity.pdc.has(NamespacedKeys.lastDamageTime, PersistentDataType.LONG)) {
            return true
        }

        val lastLevelledTime: Long = lmEntity.pdc.get(NamespacedKeys.lastDamageTime, PersistentDataType.LONG)
            ?: return true

        val levelledTime = Instant.ofEpochMilli(lastLevelledTime)
        return Utils.getMillisecondsFromInstant(levelledTime) > opts.preserveEntityTime!!
    }

    private fun checkEntityForPlayerLevelling(
        lmEntity: LivingEntityWrapper,
        players: List<Player>
    ) {
        val mob = lmEntity.livingEntity
        var sortedPlayersSequence = players
            .asSequence()
            .filter { p: Player -> p.world == mob.world }
            .filter { p: Player -> p.gameMode != GameMode.SPECTATOR }
            .map { p: Player -> Pair(mob.location.distanceSquared(p.location), p) }
            .sortedBy { it.first }
            .map { it.second }

        if (LevelledMobs.instance.companion.excludePlayersInCreative){
            sortedPlayersSequence = sortedPlayersSequence.filter {
                p: Player -> p.gameMode != GameMode.CREATIVE
            }
        }

        val sortedPlayers = sortedPlayersSequence.toMutableList()

        var closestPlayer: Player? = null
        for (player in sortedPlayers) {
            if (ExternalCompatibilityManager.isMobOfCitizens(player)) {
                continue
            }

            closestPlayer = player
            break
        }

        if (closestPlayer == null) {
            return
        }

        // if player has been logged in for less than 5 seconds then ignore
        val logonTime =  LevelledMobs.instance.companion.getRecentlyJoinedPlayerLogonTime(closestPlayer)
        if (logonTime != null) {
            if (Utils.getMillisecondsFromInstant(logonTime) < 5000L) {
                return
            }
            LevelledMobs.instance.companion.removeRecentlyJoinedPlayer(closestPlayer)
        }

        if (doesMobNeedRelevelling(lmEntity, closestPlayer)) {
            lmEntity.pendingPlayerIdToSet = closestPlayer.uniqueId.toString()
            lmEntity.playerForLevelling = closestPlayer
            lmEntity.reEvaluateLevel = true
            LevelledMobs.instance.mobsQueueManager.addToQueue(QueueItem(lmEntity, null))
        }
    }

    private fun checkLevelledEntity(
        lmEntity: LivingEntityWrapper,
        player: Player
    ) {
        if (!lmEntity.livingEntity.isValid) {
            return
        }
        // square the distance we are using Location#distanceSquared. This is because it is faster than Location#distance since it does not need to sqrt which is taxing on the CPU.
        val maxDistance = 128.0.pow(2.0)
        val location = player.location
        val main = LevelledMobs.instance

        @Suppress("DEPRECATION")
        if (lmEntity.isRulesForceAll) {
            main.mobsQueueManager.addToQueue(QueueItem(lmEntity, null))
        } else if (lmEntity.livingEntity.customName != null
            && main.rulesManager.getRuleMobCustomNameStatus(lmEntity)
            === MobCustomNameStatus.NOT_NAMETAGGED
        ) {
            // mob has a nametag but is levelled so we'll remove it
            main.levelInterface.removeLevel(lmEntity)
        } else if (lmEntity.isMobTamed
            && main.rulesManager.getRuleMobTamedStatus(lmEntity) === MobTamedStatus.NOT_TAMED
        ) {
            // mob is tamed with a level but the rules don't allow it, remove the level
            main.levelInterface.removeLevel(lmEntity)
        } else if (lmEntity.livingEntity.isValid &&
            !main.helperSettings.getBoolean(
                "use-customname-for-mob-nametags",false
            ) && location.world != null && location.world == lmEntity.world && lmEntity.location.distanceSquared(
                location
            ) <= maxDistance
        ) {
            //if within distance, update nametag.
            val nametag = main.levelManager.getNametag(lmEntity, isDeathNametag = false, preserveMobName = true)
            main.nametagQueueManager.addToQueue(
                QueueItem(lmEntity, nametag, mutableListOf(player))
            )
        }
    }

    private fun doesMobNeedRelevelling(
        lmEntity: LivingEntityWrapper,
        player: Player
    ): Boolean {
        val mob = lmEntity.livingEntity
        val main = LevelledMobs.instance

        if (main.playerLevellingMinRelevelTime > 0L && main.playerLevellingEntities.containsKey(
                mob
            )
        ) {
            val lastCheck = main.playerLevellingEntities[mob]
            val duration = Duration.between(lastCheck, Instant.now())

            if (duration.toMillis() < main.playerLevellingMinRelevelTime) {
                return false
            }
        }

        val playerId: String?
        if (main.playerLevellingMinRelevelTime > 0L) {
            main.playerLevellingEntities[mob] = Instant.now()
        }

        synchronized(mob.persistentDataContainer) {
            if (!mob.persistentDataContainer
                    .has(
                        NamespacedKeys.playerLevellingId,
                        PersistentDataType.STRING
                    )
            ) {
                return true
            }
            playerId = mob.persistentDataContainer
                .get(
                    NamespacedKeys.playerLevellingId,
                    PersistentDataType.STRING
                )
        }

        if (playerId == null && main.playerLevellingMinRelevelTime <= 0L) {
            return true
        } else if (playerId == null || player.uniqueId.toString() != playerId) {
            return true
        }

        val opts = main.rulesManager.getRulePlayerLevellingOptions(lmEntity)
        if (player.uniqueId.toString() == playerId && opts != null && opts.getRecheckPlayers) {
            val previousResult: String =
                lmEntity.pdc.get(NamespacedKeys.playerLevellingSourceNumber, PersistentDataType.STRING)
                    ?: return true
            val variableToUse =
                if (opts.variable.isNullOrEmpty()) "%level%" else opts.variable!!
            val result = getPlayerLevelSourceNumber(player, lmEntity, variableToUse)
            val sourceNumberStr =
                if (result.isNumericResult) result.numericResult.toString() else result.stringResult

            return previousResult != sourceNumberStr
        }

        return player.uniqueId.toString() != playerId
    }

    fun stopNametagAutoUpdateTask() {
        if (!LevelledMobs.instance.nametagQueueManager.hasNametagSupport) {
            return
        }

        if (nametagAutoUpdateTask != null && !nametagAutoUpdateTask!!.isCancelled()) {
            Utils.logger.info("&fTasks: &7Stopping async nametag auto update task...")
            nametagAutoUpdateTask!!.cancelTask()
        }

        if (nametagTimerTask != null && !nametagTimerTask!!.isCancelled()) {
            nametagTimerTask!!.cancelTask()
        }
    }

    private fun applyLevelledAttributes(
        lmEntity: LivingEntityWrapper,
        addition: Addition
    ) {
        assert(lmEntity.isLevelled)
        // This functionality should be added into the enum.
        val attribute: Attribute
        when (addition) {
            Addition.ATTRIBUTE_MAX_HEALTH -> attribute = Attribute.GENERIC_MAX_HEALTH
            Addition.ATTRIBUTE_ATTACK_DAMAGE -> attribute = Attribute.GENERIC_ATTACK_DAMAGE
            Addition.ATTRIBUTE_MOVEMENT_SPEED -> attribute = Attribute.GENERIC_MOVEMENT_SPEED
            Addition.ATTRIBUTE_HORSE_JUMP_STRENGTH -> attribute = Attribute.HORSE_JUMP_STRENGTH
            Addition.ATTRIBUTE_ARMOR_BONUS -> attribute = Attribute.GENERIC_ARMOR
            Addition.ATTRIBUTE_ARMOR_TOUGHNESS -> attribute = Attribute.GENERIC_ARMOR_TOUGHNESS
            Addition.ATTRIBUTE_KNOCKBACK_RESISTANCE -> attribute = Attribute.GENERIC_KNOCKBACK_RESISTANCE
            Addition.ATTRIBUTE_FLYING_SPEED -> attribute = Attribute.GENERIC_FLYING_SPEED
            Addition.ATTRIBUTE_ATTACK_KNOCKBACK -> attribute = Attribute.GENERIC_ATTACK_KNOCKBACK
            Addition.ATTRIBUTE_FOLLOW_RANGE -> attribute = Attribute.GENERIC_FOLLOW_RANGE
            Addition.ATTRIBUTE_ZOMBIE_SPAWN_REINFORCEMENTS -> {
                if (lmEntity.getSpawnReason() == LevelledMobSpawnReason.REINFORCEMENTS) {
                    return
                }
                attribute = Attribute.ZOMBIE_SPAWN_REINFORCEMENTS
            }

            else -> throw IllegalStateException(
                "Addition must be an Attribute, if so, it has not been considered in this method"
            )
        }
        // Attr instance for the mob
        if (lmEntity.livingEntity.getAttribute(attribute) == null) return

        // Don't try to apply an addition to their attribute if they don't have it

        // Apply additions
        LevelledMobs.instance.mobDataManager.setAdditionsForLevel(lmEntity, attribute, addition)
    }

    private fun applyCreeperBlastRadius(lmEntity: LivingEntityWrapper) {
        val creeper = lmEntity.livingEntity as Creeper
        val main = LevelledMobs.instance

        val tuning = main.rulesManager.getFineTuningAttributes(lmEntity)
        if (tuning == null) {
            // make sure creeper explosion is at vanilla defaults incase of a relevel, etc
            if (creeper.explosionRadius != 3) {
                creeper.explosionRadius = 3
            }
            DebugManager.log(
                DebugType.CREEPER_BLAST_RADIUS, lmEntity
            ) { "lvl: ${lmEntity.getMobLevel()}, mulp: null, result: 3" }
            return
        }

        val maxRadius: Int = main.rulesManager.getRuleCreeperMaxBlastRadius(lmEntity)
        val damage = main.mobDataManager.getAdditionsForLevel(
            lmEntity,
            Addition.CREEPER_BLAST_DAMAGE, 3f
        )
        if (damage == 0.0f) {
            return
        }

        var blastRadius = 3 + floor(damage).toInt()

        if (blastRadius > maxRadius) {
            blastRadius = maxRadius
        } else if (blastRadius < 0) {
            blastRadius = 0
        }

        val blastRadiusFinal = blastRadius
        DebugManager.log(DebugType.CREEPER_BLAST_RADIUS, lmEntity) {
            java.lang.String.format(
                "lvl: %s, mulp: %s, max: %s, result: %s",
                lmEntity.getMobLevel(), Utils.round(damage.toDouble(), 3), maxRadius, blastRadiusFinal
            )
        }

        if (blastRadius < 0) {
            blastRadius = 0
        }

        creeper.explosionRadius = blastRadius
    }

    /**
     * Add configured equipment to the levelled mob LivingEntity MUST be a levelled mob
     *
     *
     * Thread-safety unknown.
     *
     * @param lmEntity a levelled mob to apply levelled equipment to
     * @param level    the level of the levelled mob
     */
    private fun applyLevelledEquipment(
        lmEntity: LivingEntityWrapper,
        level: Int
    ) {
        if (!lmEntity.isLevelled) {
            // if you summon a mob and it isn't levelled due to a config rule (baby zombies exempt for example)
            // then we'll be here with a non-levelled entity
            return
        }
        if (level < 1) {
            return
        }

        // Custom Drops must be enabled.
        val customDropsRuleSet: CustomDropsRuleSet = LevelledMobs.instance.rulesManager.getRuleUseCustomDropsForMob(lmEntity)
        if (!customDropsRuleSet.useDrops) {
            return
        }

        val scheduler = SchedulerWrapper {
            applyLevelledEquipmentNonAsync(lmEntity, customDropsRuleSet)
            lmEntity.free()
        }

        scheduler.entity = lmEntity.livingEntity
        lmEntity.inUseCount.getAndIncrement()
        scheduler.run()
    }

    private fun applyLevelledEquipmentNonAsync(
        lmEntity: LivingEntityWrapper,
        customDropsRuleSet: CustomDropsRuleSet
    ) {
        val mmInfo = MythicMobUtils.getMythicMobInfo(lmEntity)
        if (mmInfo != null && mmInfo.preventRandomEquipment) {
            return
        }

        val main = LevelledMobs.instance
        val items = mutableListOf<ItemStack>()
        val dropResult = main.customDropsHandler.getCustomItemDrops(
            lmEntity,
            items, true
        )
        if (items.isEmpty()) {
            return
        }

        val equipment = lmEntity.livingEntity.equipment ?: return

        if (lmEntity.lockEntitySettings && customDropsRuleSet.useDropTableIds.isNotEmpty()) {
            val customDrops: String = java.lang.String.join(";", customDropsRuleSet.useDropTableIds)
            lmEntity.pdc.set(NamespacedKeys.lockedDropRules, PersistentDataType.STRING, customDrops)
            if (customDropsRuleSet.chunkKillOptions!!.getDisableVanillaDrops()) lmEntity.pdc
                .set(NamespacedKeys.lockedDropRulesOverride, PersistentDataType.INTEGER, 1)
        }

        var hadMainItem = false
        var hadPlayerHead = false
        val equippedItemsInfo = EquippedItemsInfo()
        val equippedCountPerGroup: MutableMap<String?, Int> = TreeMap()
        var equippedSoFar = 0

        dropResult.stackToItem.shuffle()

        for ((itemStack, item) in dropResult.stackToItem) {
            val material: Material = itemStack.type
            val groupLimits = main.customDropsHandler.getGroupLimits(item)
            val hasEquipLimits = item.hasGroupId && groupLimits != null && groupLimits.hasCapEquipped

            if (hasEquipLimits) {
                if (equippedCountPerGroup.containsKey(item.groupId)) {
                    equippedSoFar = equippedCountPerGroup[item.groupId]!!
                }

                if (groupLimits!!.hasReachedCapEquipped(equippedSoFar)) {
                    DebugManager.log(DebugType.GROUP_LIMITS, lmEntity) {
                        String.format(
                            "Reached equip limit of %s, mob: %s, item: %s, group: %s",
                            groupLimits.capEquipped, lmEntity.nameIfBaby, material, item.groupId
                        )
                    }
                    continue
                }
            }

            if (EnchantmentTarget.ARMOR_FEET.includes(material)) {
                equipment.setBoots(itemStack, true)
                equipment.bootsDropChance = 0f
                equippedItemsInfo.boots = item
            } else if (EnchantmentTarget.ARMOR_LEGS.includes(material)) {
                equipment.setLeggings(itemStack, true)
                equipment.leggingsDropChance = 0f
                equippedItemsInfo.leggings = item
            } else if (EnchantmentTarget.ARMOR_TORSO.includes(material)) {
                equipment.setChestplate(itemStack, true)
                equipment.chestplateDropChance = 0f
                equippedItemsInfo.chestplate = item
            } else if (EnchantmentTarget.ARMOR_HEAD.includes(material)
                || material.name.endsWith("_HEAD") || (item.equipOnHelmet
                        && !hadPlayerHead)
            ) {
                equipment.setHelmet(itemStack, true)
                equipment.helmetDropChance = 0f
                equippedItemsInfo.helmet = item
                if (material == Material.PLAYER_HEAD) {
                    hadPlayerHead = true
                }
            } else {
                if (!hadMainItem) {
                    equipment.setItemInMainHand(itemStack)
                    equipment.itemInMainHandDropChance = 0f
                    equippedItemsInfo.mainHand = item
                    hadMainItem = true
                } else if (item.equipOffhand) {
                    equipment.setItemInOffHand(itemStack)
                    equipment.itemInOffHandDropChance = 0f
                    equippedItemsInfo.offhand = item
                }
            }

            equippedSoFar++

            if (hasEquipLimits) {
                equippedCountPerGroup[item.groupId] = equippedSoFar
            }
        }

        main.customDropsHandler.addEntityEquippedItems(
            lmEntity.livingEntity,
            equippedItemsInfo
        )
    }

    private fun getMobAttributeValue(lmEntity: LivingEntityWrapper): Double {
        var result = 0.0
        synchronized(LevelledMobs.instance.attributeSyncObject) {
            val attrib = lmEntity.livingEntity
                .getAttribute(Attribute.GENERIC_MAX_HEALTH)
            if (attrib != null) {
                result = attrib.value
            }
        }

        return result
    }

    private fun getMobHealth(lmEntity: LivingEntityWrapper): Double {
        val result: Double
        synchronized(LevelledMobs.instance.attributeSyncObject) {
            result = lmEntity.livingEntity.health
        }

        return result
    }

    override fun getLevellableState(
        lmInterface: LivingEntityInterface
    ): LevellableState {
        /*
        Certain entity types are force-blocked, regardless of what the user has configured.
        This is also ran in getLevellableState(EntityType), however it is important that this is ensured
        before all other checks are made.
         */
        val main = LevelledMobs.instance
        if (forcedBlockedEntityTypes.contains(lmInterface.entityType)) {
            return LevellableState.DENIED_FORCE_BLOCKED_ENTITY_TYPE
        }

        if (lmInterface.getApplicableRules().isEmpty()) {
            return LevellableState.DENIED_NO_APPLICABLE_RULES
        }

        if (!main.rulesManager.getRuleIsMobAllowedInEntityOverride(lmInterface)) {
            return LevellableState.DENIED_CONFIGURATION_BLOCKED_ENTITY_TYPE
        }

        if (main.rulesManager.getRuleMobMaxLevel(lmInterface) < 1) {
            return LevellableState.DENIED_LEVEL_0
        }

        if (lmInterface !is LivingEntityWrapper) {
            return LevellableState.ALLOWED
        }

        val externalCompatResult = ExternalCompatibilityManager.checkAllExternalCompats(
            lmInterface
        )
        if (externalCompatResult != LevellableState.ALLOWED) {
            return externalCompatResult
        }

        if (lmInterface.isMobOfExternalType) {
            lmInterface.invalidateCache()

            if (!main.rulesManager.getRuleIsMobAllowedInEntityOverride(lmInterface)) {
                return LevellableState.DENIED_CONFIGURATION_BLOCKED_ENTITY_TYPE
            }
        }

        /*
        Check 'No Level Conditions'
         */
        // Nametagged mobs.
        @Suppress("DEPRECATION")
        if (lmInterface.livingEntity.customName != null &&
            main.rulesManager.getRuleMobCustomNameStatus(lmInterface)
            == MobCustomNameStatus.NOT_NAMETAGGED
        ) {
            return LevellableState.DENIED_CONFIGURATION_CONDITION_NAMETAGGED
        }

        return LevellableState.ALLOWED
    }

    override fun getLevellableState(livingEntity: LivingEntity): LevellableState {
        val lmEntity = LivingEntityWrapper.getInstance(livingEntity)
        try {
            return getLevellableState(lmEntity)
        }
        finally {
            lmEntity.free()
        }
    }

    /**
     * This method applies a level to the target mob.
     *
     *
     * You can run this method on a mob regardless if they are already levelled or not.
     *
     *
     * This method DOES NOT check if it is LEVELLABLE. It is assumed that plugins make sure this is
     * the case (unless they intend otherwise).
     *
     *
     * It is highly recommended to leave bypassLimits = false, unless the desired behaviour is to
     * override the user-configured limits.
     *
     *
     * Thread-safety intended, but not tested.
     *
     * @param lmEntity                   target mob
     * @param level                      the level the mob should have
     * @param isSummoned                 if the mob was spawned by LevelledMobs, not by the server
     * @param bypassLimits               whether LM should disregard max level, etc.
     * @param additionalLevelInformation used to determine the source event
     */
    override fun applyLevelToMob(
        lmEntity: LivingEntityWrapper,
        level: Int,
        isSummoned: Boolean,
        bypassLimits: Boolean,
        additionalLevelInformation: MutableSet<AdditionalLevelInformation>?
    ) {
        // this thread runs in async.  if adding any functions make sure they can be run in this fashion

        val main = LevelledMobs.instance
        var useLevel = level
        if (useLevel <= 0) {
            useLevel = generateLevel(lmEntity)
        }
        lmEntity.setMobPrelevel(useLevel)

        assert(bypassLimits || isSummoned || getLevellableState(lmEntity) == LevellableState.ALLOWED)
        var skipLMNametag = false

        if (lmEntity.livingEntity.isInsideVehicle
            && main.rulesManager.getRulePassengerMatchLevel(lmEntity)
            && lmEntity.livingEntity.vehicle is LivingEntity
        ) {
            // entity is a passenger. grab the level from the "vehicle" entity
            val vehicle = LivingEntityWrapper.getInstance(
                lmEntity.livingEntity.vehicle as LivingEntity
            )
            if (vehicle.isLevelled) {
                useLevel = vehicle.getMobLevel()
                lmEntity.setMobPrelevel(useLevel)
            }

            vehicle.free()
        }

        if (isSummoned) {
            lmEntity.setSpawnReason(LevelledMobSpawnReason.LM_SUMMON, true)
            val summonedMobPreLevelEvent = SummonedMobPreLevelEvent(
                lmEntity.livingEntity, useLevel
            )
            Bukkit.getPluginManager().callEvent(summonedMobPreLevelEvent)

            if (summonedMobPreLevelEvent.isCancelled) {
                return
            }
        } else {
            val mobPreLevelEvent = MobPreLevelEvent(
                lmEntity.livingEntity, useLevel, MobPreLevelEvent.LevelCause.NORMAL,
                additionalLevelInformation
            )

            Bukkit.getPluginManager().callEvent(mobPreLevelEvent)
            if (mobPreLevelEvent.isCancelled) {
                return
            }

            useLevel = mobPreLevelEvent.level
            lmEntity.setMobPrelevel(useLevel)
            if (!mobPreLevelEvent.showLMNametag) {
                skipLMNametag = true
                lmEntity.setShouldShowLMNametag(false)
            }
        }

        var hasNoLevelKey = false
        if (!isSummoned) {
            synchronized(lmEntity.livingEntity.persistentDataContainer) {
                hasNoLevelKey = lmEntity.pdc
                    .has(NamespacedKeys.noLevelKey, PersistentDataType.STRING)
            }
        }

        if (hasNoLevelKey) {
            DebugManager.log(
                DebugType.APPLY_LEVEL_RESULT,
                lmEntity,
                false
            ) { "Entity &b" + lmEntity.typeName + "&7 had &bnoLevelKey&7 attached" }
            return
        }

        synchronized(lmEntity.livingEntity.persistentDataContainer) {
            lmEntity.pdc.set(NamespacedKeys.levelKey, PersistentDataType.INTEGER, useLevel)
            lmEntity.pdc.set(
                NamespacedKeys.mobHash,
                PersistentDataType.STRING,
                main.rulesManager.currentRulesHash
            )
        }
        lmEntity.invalidateCache()

        val nbtDatas =
            if (lmEntity.nbtData != null && lmEntity.nbtData!!.isNotEmpty()) lmEntity.nbtData else main.rulesManager.getRuleNbtData(
                lmEntity
            )

        if (nbtDatas!!.isNotEmpty() && !ExternalCompatibilityManager.hasNbtApiInstalled()) {
            if (!hasMentionedNBTAPIMissing) {
                Utils.logger.warning(
                    "NBT Data has been specified in customdrops.yml but required plugin NBTAPI is not installed!"
                )
                hasMentionedNBTAPIMissing = true
            }
            nbtDatas.clear()
        }

        lmEntity.lockEntitySettings = main.rulesManager.getRuleDoLockEntity(lmEntity)
        if (lmEntity.lockEntitySettings && lmEntity.isNewlySpawned) {
            lmEntity.lockedNametag = main.rulesManager.getRuleNametag(lmEntity)
            lmEntity.lockedOverrideName = main.rulesManager.getRuleEntityOverriddenName(
                lmEntity,
                true
            )
        }

        val doSkipLMNametag = skipLMNametag

        val scheduler = SchedulerWrapper {
            applyLevelToMob2(lmEntity, nbtDatas, doSkipLMNametag)
            lmEntity.free()
        }

        lmEntity.inUseCount.getAndIncrement()
        scheduler.entity = lmEntity.livingEntity
        scheduler.run()

        val levelCause =
            if (isSummoned) MobPostLevelEvent.LevelCause.SUMMONED
            else MobPostLevelEvent.LevelCause.NORMAL
        Bukkit.getPluginManager()
            .callEvent(MobPostLevelEvent(lmEntity, levelCause, additionalLevelInformation))

        val sb = StringBuilder()
        sb.append("entity: ")
        sb.append(lmEntity.livingEntity.name)
        if (lmEntity.isBabyMob) {
            sb.append(" (baby)")
        }
        sb.append(", world: ")
        sb.append(lmEntity.worldName)
        sb.append(", level: ")
        sb.append(useLevel)
        if (isSummoned) {
            sb.append(" (summoned)")
        }
        if (bypassLimits) {
            sb.append(" (limit bypass)")
        }

        DebugManager.log(DebugType.APPLY_LEVEL_RESULT, lmEntity, true, sb::toString)
    }

    private fun applyLevelToMob2(
        lmEntity: LivingEntityWrapper,
        nbtDatas: MutableList<String>,
        doSkipLMNametag: Boolean
    ) {
        applyAttribs(lmEntity, nbtDatas)

        if (!doSkipLMNametag) {
            LevelledMobs.instance.levelManager.updateNametagWithDelay(lmEntity)
        }
        LevelledMobs.instance.levelManager.applyLevelledEquipment(lmEntity, lmEntity.getMobLevel())
    }

    private fun applyAttribs(
        lmEntity: LivingEntityWrapper,
        nbtDatas: MutableList<String>
    ) {
        val main = LevelledMobs.instance
        synchronized(main.attributeSyncObject) {
            main.levelManager.applyLevelledAttributes(
                lmEntity,
                Addition.ATTRIBUTE_ATTACK_DAMAGE
            )
            main.levelManager.applyLevelledAttributes(
                lmEntity,
                Addition.ATTRIBUTE_MAX_HEALTH
            )
            main.levelManager.applyLevelledAttributes(
                lmEntity,
                Addition.ATTRIBUTE_MOVEMENT_SPEED
            )
            main.levelManager.applyLevelledAttributes(
                lmEntity,
                Addition.ATTRIBUTE_ARMOR_BONUS
            )
            main.levelManager.applyLevelledAttributes(
                lmEntity,
                Addition.ATTRIBUTE_ARMOR_TOUGHNESS
            )
            main.levelManager.applyLevelledAttributes(
                lmEntity,
                Addition.ATTRIBUTE_ATTACK_KNOCKBACK
            )
            main.levelManager.applyLevelledAttributes(
                lmEntity,
                Addition.ATTRIBUTE_FLYING_SPEED
            )
            main.levelManager.applyLevelledAttributes(
                lmEntity,
                Addition.ATTRIBUTE_KNOCKBACK_RESISTANCE
            )
            main.levelManager.applyLevelledAttributes(
                lmEntity,
                Addition.ATTRIBUTE_FOLLOW_RANGE
            )
            if (lmEntity.livingEntity is Zombie) {
                main.levelManager.applyLevelledAttributes(
                    lmEntity,
                    Addition.ATTRIBUTE_ZOMBIE_SPAWN_REINFORCEMENTS
                )
            } else if (lmEntity.livingEntity is Horse) {
                main.levelManager.applyLevelledAttributes(
                    lmEntity,
                    Addition.ATTRIBUTE_HORSE_JUMP_STRENGTH
                )
            }
        }

        if (lmEntity.lockEntitySettings) {
            lmEntity.pdc
                .set(NamespacedKeys.lockSettings, PersistentDataType.INTEGER, 1)
            if (lmEntity.lockedNametag != null) {
                lmEntity.pdc
                    .set(
                        NamespacedKeys.lockedNametag, PersistentDataType.STRING,
                        lmEntity.lockedNametag!!
                    )
            }
            if (lmEntity.lockedOverrideName != null) {
                lmEntity.pdc
                    .set(
                        NamespacedKeys.lockedNameOverride, PersistentDataType.STRING,
                        lmEntity.lockedOverrideName!!
                    )
            }
        }

        if (nbtDatas.isNotEmpty()) {
            var hadSuccess = false
            val allResults = mutableListOf<NBTApplyResult>()

            for (nbtData: String in nbtDatas) {
                val result: NBTApplyResult = NBTManager.applyNBTDataMob(
                    lmEntity,
                    nbtData
                )
                if (result.hadException) {
                    if (lmEntity.summonedSender == null) {
                        Utils.logger.warning(
                            java.lang.String.format(
                                "Error applying NBT data '%s' to %s. Exception message: %s",
                                nbtData, lmEntity.nameIfBaby, result.exceptionMessage
                            )
                        )
                    } else {
                        lmEntity.summonedSender!!.sendMessage(
                            "Error applying NBT data to " + lmEntity.nameIfBaby
                                    + ". Exception message: " + result.exceptionMessage
                        )
                    }
                } else {
                    hadSuccess = true
                    allResults.add(result)
                }
            }
            if (hadSuccess) {
                DebugManager.log(DebugType.NBT_APPLICATION, lmEntity, true) {
                    ("Applied NBT data to '" + lmEntity.nameIfBaby +
                            "'. " + getNBTDebugMessage(allResults))
                }
            }
        }

        if (lmEntity.livingEntity is Creeper) {
            main.levelManager.applyCreeperBlastRadius(lmEntity)
        }
    }

    private fun getNBTDebugMessage(
        results: MutableList<NBTApplyResult>
    ): String {
        val sb = StringBuilder()

        for (result in results) {
            if (result.objectsAdded == null) {
                continue
            }

            for (i in 0 until result.objectsAdded!!.size) {
                if (i > 0) {
                    sb.append(", ")
                } else {
                    sb.append("added: ")
                }

                sb.append(result.objectsAdded!![i])
            }
        }

        for (result in results) {
            if (result.objectsUpdated == null) {
                continue
            }

            for (i in 0 until result.objectsUpdated!!.size) {
                if (i > 0 || sb.isNotEmpty()) {
                    sb.append(", ")
                }
                if (i == 0) {
                    sb.append("updated: ")
                }

                sb.append(result.objectsUpdated!![i])
            }
        }

        for (result in results) {
            if (result.objectsRemoved == null) {
                continue
            }

            for (i in 0 until result.objectsRemoved!!.size) {
                if (i > 0 || sb.isNotEmpty()) {
                    sb.append(", ")
                }
                if (i == 0) {
                    sb.append("removed: ")
                }

                sb.append(result.objectsRemoved!![i])
            }
        }

        return if (sb.isEmpty()) "" else sb.toString()
    }

    /**
     * Check if a LivingEntity is a levelled mob or not. This is determined *after*
     * MobPreLevelEvent.
     *
     *
     * Thread-safety intended, but not tested.
     *
     * @param livingEntity living entity to check
     * @return if the mob is levelled or not
     */
    override fun isLevelled(livingEntity: LivingEntity): Boolean {
        var hadError = false
        var succeeded = false
        var isLevelled = false

        for (i in 0..1) {
            try {
                synchronized(livingEntity.persistentDataContainer) {
                    isLevelled = livingEntity.persistentDataContainer
                        .has(NamespacedKeys.levelKey, PersistentDataType.INTEGER)
                }
                succeeded = true
                break
            } catch (ignored: ConcurrentModificationException) {
                hadError = true
                try {
                    Thread.sleep(10)
                } catch (ignored2: InterruptedException) {
                    return false
                }
            }
        }

        if (hadError) {
            if (succeeded) {
                Utils.logger.warning(
                    "Got ConcurrentModificationException in LevelManager checking entity isLevelled, succeeded on retry"
                )
            } else {
                Utils.logger.warning(
                    "Got ConcurrentModificationException (2x) in LevelManager checking entity isLevelled"
                )
            }
        }

        return isLevelled
    }

    /**
     * Retrieve the level of a levelled mob.
     *
     *
     * Thread-safety intended, but not tested.
     *
     * @param livingEntity the levelled mob to get the level of
     * @return the mob's level
     */
    override fun getLevelOfMob(livingEntity: LivingEntity): Int {
        synchronized(livingEntity.persistentDataContainer) {
            if (!livingEntity.persistentDataContainer
                    .has(NamespacedKeys.levelKey, PersistentDataType.INTEGER)
            ) {
                return -1
            }
            return livingEntity.persistentDataContainer
                    .get(NamespacedKeys.levelKey, PersistentDataType.INTEGER)!!
        }
    }

    /**
     * Un-level a mob.
     *
     * @param lmEntity levelled mob to un-level
     */
    override fun removeLevel(lmEntity: LivingEntityWrapper) {
        assert(lmEntity.isLevelled)
        // remove PDC value
        val main = LevelledMobs.instance
        synchronized(lmEntity.livingEntity.persistentDataContainer) {
            if (lmEntity.pdc.has(NamespacedKeys.levelKey, PersistentDataType.INTEGER)) {
                lmEntity.pdc.remove(NamespacedKeys.levelKey)
            }
            if (lmEntity.pdc
                    .has(NamespacedKeys.overridenEntityNameKey, PersistentDataType.STRING)
            ) {
                lmEntity.pdc.remove(NamespacedKeys.overridenEntityNameKey)
            }
        }

        // reset attributes
        synchronized(main.attributeSyncObject) {
            for (attribute in Attribute.entries) {
                val attInst = lmEntity.livingEntity
                    .getAttribute(attribute)

                if (attInst == null) {
                    continue
                }

                val existingMods =
                    Collections.enumeration(attInst.modifiers)
                while (existingMods.hasMoreElements()) {
                    val existingMod = existingMods.nextElement()

                    if (main.mobDataManager.vanillaMultiplierNames.containsKey(existingMod.name)) continue
                    attInst.removeModifier(existingMod)
                }
            }
        }

        if (lmEntity.livingEntity is Creeper) {
            (lmEntity.livingEntity as Creeper).explosionRadius = 3
        }

        lmEntity.invalidateCache()

        // update nametag
        main.levelManager.updateNametag(lmEntity)
    }

    override fun removeLevel(livingEntity: LivingEntity) {
        val lmEntity = LivingEntityWrapper.getInstance(livingEntity)
        removeLevel(lmEntity)
        lmEntity.free()
    }

    override fun getMobNametag(livingEntity: LivingEntity): String? {
        val lmEntity = LivingEntityWrapper.getInstance(livingEntity)
        try {
            return getNametag(lmEntity, false).nametag
        }
        finally {
            lmEntity.free()
        }
    }
}