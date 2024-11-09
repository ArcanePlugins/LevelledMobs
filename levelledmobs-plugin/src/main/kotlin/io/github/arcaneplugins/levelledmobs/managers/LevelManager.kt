package io.github.arcaneplugins.levelledmobs.managers

import io.github.arcaneplugins.levelledmobs.LevelInterface2
import io.github.arcaneplugins.levelledmobs.LevelledMobs
import io.github.arcaneplugins.levelledmobs.LivingEntityInterface
import io.github.arcaneplugins.levelledmobs.MainCompanion
import io.github.arcaneplugins.levelledmobs.customdrops.EquippedItemsInfo
import io.github.arcaneplugins.levelledmobs.debug.DebugManager
import io.github.arcaneplugins.levelledmobs.debug.DebugType
import io.github.arcaneplugins.levelledmobs.enums.Addition
import io.github.arcaneplugins.levelledmobs.enums.InternalSpawnReason
import io.github.arcaneplugins.levelledmobs.enums.LevellableState
import io.github.arcaneplugins.levelledmobs.enums.MobCustomNameStatus
import io.github.arcaneplugins.levelledmobs.enums.MobTamedStatus
import io.github.arcaneplugins.levelledmobs.enums.NametagVisibilityEnum
import io.github.arcaneplugins.levelledmobs.events.MobPostLevelEvent
import io.github.arcaneplugins.levelledmobs.events.MobPreLevelEvent
import io.github.arcaneplugins.levelledmobs.events.SummonedMobPreLevelEvent
import io.github.arcaneplugins.levelledmobs.listeners.EntitySpawnListener
import io.github.arcaneplugins.levelledmobs.misc.EvaluationException
import io.github.arcaneplugins.levelledmobs.misc.NamespacedKeys
import io.github.arcaneplugins.levelledmobs.misc.PickedUpEquipment
import io.github.arcaneplugins.levelledmobs.misc.QueueItem
import io.github.arcaneplugins.levelledmobs.misc.StringReplacer
import io.github.arcaneplugins.levelledmobs.result.AdditionalLevelInformation
import io.github.arcaneplugins.levelledmobs.result.AttributePreMod
import io.github.arcaneplugins.levelledmobs.result.MinAndMaxHolder
import io.github.arcaneplugins.levelledmobs.result.NBTApplyResult
import io.github.arcaneplugins.levelledmobs.result.NametagResult
import io.github.arcaneplugins.levelledmobs.result.PlayerLevelSourceResult
import io.github.arcaneplugins.levelledmobs.result.PlayerNetherOrWorldSpawnResult
import io.github.arcaneplugins.levelledmobs.rules.CustomDropsRuleSet
import io.github.arcaneplugins.levelledmobs.rules.RulesManager
import io.github.arcaneplugins.levelledmobs.rules.strategies.RandomVarianceGenerator
import io.github.arcaneplugins.levelledmobs.rules.strategies.StrategyType
import io.github.arcaneplugins.levelledmobs.util.Log
import io.github.arcaneplugins.levelledmobs.util.MiscUtils
import io.github.arcaneplugins.levelledmobs.util.MythicMobUtils
import io.github.arcaneplugins.levelledmobs.util.Utils
import io.github.arcaneplugins.levelledmobs.wrappers.LivingEntityWrapper
import io.github.arcaneplugins.levelledmobs.wrappers.SchedulerResult
import io.github.arcaneplugins.levelledmobs.wrappers.SchedulerWrapper
import io.papermc.paper.threadedregions.scheduler.ScheduledTask
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Collections
import java.util.WeakHashMap
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Consumer
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
import org.bukkit.event.entity.CreatureSpawnEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.roundToInt


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
    private var lastLEWCacheClearing: Instant? = null
    val entitySpawnListener = EntitySpawnListener()
    private var nametagAutoUpdateTask: SchedulerResult? = null
    private var nametagTimerTask: SchedulerResult? = null
    private val asyncRunningCount = AtomicInteger()
    private val entitiesPerPlayer = mutableMapOf<Player, MutableList<Entity>>()
    private val entitiesPerPlayerLock = Any()
    private val attributeStringList = mutableMapOf<String, Attribute>()
    private val strategyPlaceholders = mutableMapOf<String, StrategyType>()
    /**
     * The following entity types *MUST NOT* be levellable.
     */
    var forcedBlockedEntityTypes = mutableSetOf<EntityType>()


    fun load(){
        attributeStringList.putAll(mutableMapOf(
            "%max-health%" to Attribute.GENERIC_MAX_HEALTH,
            "%movement-speed%" to Attribute.GENERIC_MOVEMENT_SPEED,
            "%attack-damage%" to Attribute.GENERIC_ATTACK_DAMAGE,
            "%follow-range%" to Attribute.GENERIC_FOLLOW_RANGE,
            "%armor-bonus%" to Attribute.GENERIC_ARMOR,
            "%armor-toughness%" to Attribute.GENERIC_ARMOR_TOUGHNESS,
            "%attack-knockback%" to Attribute.GENERIC_ATTACK_KNOCKBACK,
            "%knockback-resistance%" to Attribute.GENERIC_KNOCKBACK_RESISTANCE,
            "%zombie-spawn-reinforcements%" to Attribute.ZOMBIE_SPAWN_REINFORCEMENTS
        ))

        strategyPlaceholders.putAll(mutableMapOf(
            "%random%" to StrategyType.RANDOM,
            "%weighted-random%" to StrategyType.WEIGHTED_RANDOM,
            "%random-variance-mod%" to StrategyType.RANDOM_VARIANCE,
            "%custom-strategy%" to StrategyType.CUSTOM,
            "%distance-from-origin%" to StrategyType.SPAWN_DISTANCE,
            "%y-coordinate%" to StrategyType.Y_COORDINATE,
            "%player-variable-mod%" to StrategyType.PLAYER_VARIABLE
        ))

        this.vehicleNoMultiplierItems.addAll(mutableListOf(
            Material.SADDLE,
            Material.LEATHER_HORSE_ARMOR,
            Material.IRON_HORSE_ARMOR,
            Material.GOLDEN_HORSE_ARMOR,
            Material.DIAMOND_HORSE_ARMOR
        ))

        this.forcedBlockedEntityTypes.addAll(
            mutableListOf(
                EntityType.AREA_EFFECT_CLOUD,
                EntityType.ARMOR_STAND,
                EntityType.ARROW,
                EntityType.DRAGON_FIREBALL,
                EntityType.EGG,
                mapLegacyEntityTypeName("ENDER_CRYSTAL"),
                EntityType.ENDER_PEARL,
                mapLegacyEntityTypeName("ENDER_SIGNAL"),
                EntityType.EXPERIENCE_ORB,
                EntityType.FALLING_BLOCK,
                EntityType.FIREBALL,
                mapLegacyEntityTypeName("FIREWORK"),
                mapLegacyEntityTypeName("FISHING_HOOK"),
                EntityType.ITEM_FRAME,
                mapLegacyEntityTypeName("LEASH_HITCH"),
                mapLegacyEntityTypeName("LIGHTNING"),
                EntityType.LLAMA_SPIT,
                EntityType.MINECART,
                mapLegacyEntityTypeName("MINECART_CHEST"),
                mapLegacyEntityTypeName("MINECART_COMMAND"),
                mapLegacyEntityTypeName("MINECART_FURNACE"),
                mapLegacyEntityTypeName("MINECART_HOPPER"),
                mapLegacyEntityTypeName("MINECART_MOB_SPAWNER"),
                mapLegacyEntityTypeName("MINECART_TNT"),
                EntityType.PAINTING,
                mapLegacyEntityTypeName("PRIMED_TNT"),
                EntityType.SMALL_FIREBALL,
                EntityType.SNOWBALL,
                EntityType.SPECTRAL_ARROW,
                mapLegacyEntityTypeName("SPLASH_POTION"),
                mapLegacyEntityTypeName("THROWN_EXP_BOTTLE"),
                EntityType.TRIDENT,
                EntityType.UNKNOWN,
                EntityType.WITHER_SKULL,
                EntityType.SHULKER_BULLET,
                EntityType.PLAYER
            )
        )
    }

    private fun mapLegacyEntityTypeName(
        name: String
    ): EntityType{
        return if (LevelledMobs.instance.ver.minorVersion >= 21){
            when (name){
                "ENDER_CRYSTAL" -> EntityType.END_CRYSTAL
                "ENDER_SIGNAL" -> EntityType.EYE_OF_ENDER
                "FIREWORK" -> EntityType.FIREWORK_ROCKET
                "FISHING_HOOK" -> EntityType.FISHING_BOBBER
                "LEASH_HITCH" -> EntityType.LEASH_KNOT
                "LIGHTNING" -> EntityType.LIGHTNING_BOLT
                "MINECART_CHEST" -> EntityType.CHEST_MINECART
                "MINECART_COMMAND" -> EntityType.COMMAND_BLOCK_MINECART
                "MINECART_FURNACE" -> EntityType.FURNACE_MINECART
                "MINECART_HOPPER" -> EntityType.HOPPER_MINECART
                "MINECART_MOB_SPAWNER" -> EntityType.SPAWNER_MINECART
                "MINECART_TNT" -> EntityType.TNT_MINECART
                "PRIMED_TNT" -> EntityType.TNT
                "SPLASH_POTION" -> EntityType.POTION
                "THROWN_EXP_BOTTLE" -> EntityType.EXPERIENCE_BOTTLE
                else -> EntityType.UNKNOWN
            }
        }
        else
            EntityType.valueOf(name)
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
                useMinLevel = levels.minAsInt
            }
            if (useMaxLevel == -1) {
                useMaxLevel = levels.maxAsInt
            }
        }

        val levellingStrategies = LevelledMobs.instance.rulesManager.getRuleLevellingStrategies(
            lmEntity
        )
        val customStrategies = LevelledMobs.instance.rulesManager.getRuleCustomStrategies(lmEntity)

        var numberResult = 0f
        val debugId = DebugManager.startLongDebugMessage()

        try{
            for ((count, strategy) in levellingStrategies.withIndex()){
                val result = strategy.generateNumber(lmEntity, useMinLevel, useMaxLevel)
                lmEntity.strategyResults[strategy.strategyType] = result

                DebugManager.logLongMessage(debugId){
                    if (count > 0) ", ${strategy.strategyType}: $result"
                    else "${strategy.strategyType}: $result"
                }

                numberResult += result
            }

            for ((count, strategy) in customStrategies.withIndex()){
                val result = strategy.generateNumber(lmEntity, useMinLevel, useMaxLevel)
                lmEntity.customStrategyResults[strategy.placeholderName] = result

                DebugManager.logLongMessage(debugId){
                    if (count > 0) ", ${strategy.placeholderName}: $result"
                    else "${strategy.placeholderName}: $result"
                }

                numberResult += result
            }
        }
        finally {
            DebugManager.endLongMessage(debugId, DebugType.STRATEGY_RESULT, lmEntity)
        }

        // if no levelling strategy was selected then we just use a random number between min and max
        if (useMinLevel == useMaxLevel) {
            return useMinLevel
        }

        val generatedLevel = constructLevel(numberResult, lmEntity)
            .coerceAtMost(useMaxLevel)
            .coerceAtLeast(useMinLevel)

        return generatedLevel
    }

    private fun constructLevel(
        input: Float,
        lmEntity: LivingEntityWrapper
    ): Int{
        val formulaPre = LevelledMobs.instance.rulesManager.getRuleConstructLevel(lmEntity) ?: return input.roundToInt()
        val formula = replaceStringPlaceholdersForFormulas(formulaPre, lmEntity)
        val evalResult = MobDataManager.evaluateExpression(formula)
        if (evalResult.hadError){
            NotifyManager.notifyOfError("Error evaluating formula for construct-level on mob: ${lmEntity.nameIfBaby}, ${evalResult.error}")
            DebugManager.log(DebugType.CONSTRUCT_LEVEL, lmEntity){
                val msg = if (formula == formulaPre)
                    "   formula: '$formula'"
                else
                    "   formulaPre: '$formulaPre'\n" +
                    "   formula: '$formula'"

                "result (error, ${evalResult.error})\n$msg" }
            throw EvaluationException()
        }

        val result = floor(evalResult.result).toInt()

        DebugManager.log(DebugType.CONSTRUCT_LEVEL, lmEntity){
            val msg = if (formula == formulaPre)
                "   formula: '$formula'"
            else
                "   formulaPre: '$formulaPre'\n" +
                        "   formula: '$formula'"

            "result $result\n$msg"}

        return result
    }

    fun getPlayerLevelSourceNumber(
        player: Player?,
        lmEntity: LivingEntityWrapper,
        variableToUse: String
    ): PlayerLevelSourceResult {
        if (player == null) return PlayerLevelSourceResult(1f)

        val origLevelSource: Float
        var homeNameUsed = "spawn"

        if ("%level%".equals(variableToUse, ignoreCase = true))
            origLevelSource = player.level.toFloat()
        else if ("%exp%".equals(variableToUse, ignoreCase = true))
            origLevelSource = player.exp
        else if ("%exp-to-level%".equals(variableToUse, ignoreCase = true))
            origLevelSource = player.expToLevel.toFloat()
        else if ("%total-exp%".equals(variableToUse, ignoreCase = true))
            origLevelSource = player.totalExperience.toFloat()
        else if ("%world-time-ticks%".equals(variableToUse, ignoreCase = true))
            origLevelSource = player.world.time.toFloat()
        else if ("%home-distance%".equals(variableToUse, ignoreCase = true)
            || "%home-distance-with-bed%".equals(variableToUse, ignoreCase = true)
        ) {
            val allowBed = "%home-distance-with-bed%".equals(variableToUse, ignoreCase = true)
            val netherOrWorldSpawnResult: PlayerNetherOrWorldSpawnResult
            val result = ExternalCompatibilityManager.getPlayerHomeLocation(
                player, allowBed
            )
            if (result.homeNameUsed != null)
                homeNameUsed = result.homeNameUsed!!

            var useLocation = result.location
            if (useLocation == null || useLocation.world != player.world) {
                netherOrWorldSpawnResult = Utils.getPortalOrWorldSpawn(player)
                useLocation = netherOrWorldSpawnResult.location
                homeNameUsed = if (netherOrWorldSpawnResult.isWorldPortalLocation)
                    "world-portal"
                else if (netherOrWorldSpawnResult.isNetherPortalLocation)
                    "nether-portal"
                else
                    "spawn"
            }

            if (result.resultMessage != null)
                DebugManager.log(DebugType.PLAYER_LEVELLING, lmEntity) { result.resultMessage }

            origLevelSource = useLocation!!.distance(player.location).toFloat()
        } else if ("%bed-distance%".equals(variableToUse, ignoreCase = true)) {
            var useLocation = player.respawnLocation
            homeNameUsed = "bed"

            if (useLocation == null || useLocation.world !== player.world) {
                val result = Utils.getPortalOrWorldSpawn(player)
                useLocation = result.location
                homeNameUsed = if (result.isWorldPortalLocation)
                    "world-portal"
                else if (result.isNetherPortalLocation)
                    "nether-portal"
                else
                    "spawn"
            }

            origLevelSource = useLocation!!.distance(player.location).toFloat()
        } else {
            var usePlayerLevel = false
            var papiResult: String? = null

            if (ExternalCompatibilityManager.hasPapiInstalled) {
                papiResult = ExternalCompatibilityManager.getPapiPlaceholder(player, variableToUse)
                if (papiResult.isEmpty()) {
                    val l = player.location
                    DebugManager.log(DebugType.PLAYER_LEVELLING, lmEntity) {
                        "Got blank result for '$variableToUse' from PAPI. Player ${player.name} at ${l.blockX},${l.blockY},${l.blockZ} in ${player.world.name}"
                    }
                    usePlayerLevel = true
                }
            } else {
                Log.war("PlaceHolderAPI is not installed, unable to get variable $variableToUse" )
                usePlayerLevel = true
            }

            if (usePlayerLevel) {
                origLevelSource = player.level.toFloat()
            } else {
                val l = player.location
                if (papiResult.isNullOrEmpty()) {
                    origLevelSource = player.level.toFloat()
                    DebugManager.log(DebugType.PLAYER_LEVELLING, lmEntity) {
                        "Got blank result for '$variableToUse' from PAPI. Player ${player.name} at ${l.blockX},${l.blockY},${l.blockZ} in ${player.world.name}"
                    }
                } else {
                    if (Utils.isDouble(papiResult)) {
                        origLevelSource = try {
                            papiResult.toFloat()
                        } catch (ignored: Exception) {
                            player.level.toFloat()
                        }
                    } else {
                        val result = PlayerLevelSourceResult(papiResult)
                        result.homeNameUsed = homeNameUsed
                        return result
                    }
                }
            }
        }

        val sourceResult = PlayerLevelSourceResult(origLevelSource)
        val maxRandomVariance = LevelledMobs.instance.rulesManager.getRuleMaxRandomVariance(lmEntity)

        if (maxRandomVariance != null) {
            sourceResult.randomVarianceResult = ThreadLocalRandom.current().nextInt(0, maxRandomVariance.toInt() + 1).toFloat()
            if (ThreadLocalRandom.current().nextBoolean())
                sourceResult.randomVarianceResult = sourceResult.randomVarianceResult!! * -1
        }

        sourceResult.homeNameUsed = homeNameUsed
        return sourceResult
    }

    fun getMinAndMaxLevels(lmInterface: LivingEntityInterface): MinAndMaxHolder {
        // final EntityType entityType, final boolean isAdultEntity, final String worldName
        // if called from summon command then lmEntity is null

        val main = LevelledMobs.instance
        var minLevel = main.rulesManager.getRuleMobMinLevel(lmInterface)
        var maxLevel = main.rulesManager.getRuleMobMaxLevel(lmInterface)

        maxLevel = maxLevel.coerceAtLeast(0)
        minLevel = minLevel.coerceAtMost(maxLevel)

        return MinAndMaxHolder(minLevel, maxLevel)
    }

    // This sets the levelled currentDrops on a levelled mob that just died.
    fun setLevelledItemDrops(
        lmEntity: LivingEntityWrapper,
        currentDrops: MutableList<ItemStack>,
        disableItemBoost: Boolean
    ) {
        val vanillaDrops = currentDrops.size
        // this accomodates chested animals, saddles and armor on ridable creatures
        //val dropsToMultiply = getDropsToMultiply(lmEntity, currentDrops)
        val customDrops = mutableListOf<ItemStack>()
        //currentDrops.clear()

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
            if (mmInfo != null && mmInfo.preventOtherDrops)
                hasOverride = true

            if (dropResult.hasOverride) hasOverride = true
            //if (hasOverride) removeVanillaDrops(lmEntity)
            if (hasOverride) removeVanillaDrops(lmEntity, currentDrops)

        }

        var additionUsed = 0

        if (!doNotMultiplyDrops && currentDrops.isNotEmpty()) {
            // Get currentDrops added per level valu
            val additionValue = main.mobDataManager.getAdditionsForLevel(
                lmEntity,
                Addition.CUSTOM_ITEM_DROP, 2.0f
            ).amount
            if (additionValue == Float.MIN_VALUE) {
                DebugManager.log(DebugType.SET_LEVELLED_ITEM_DROPS, lmEntity) {
                    "removing any drops present"
                }
                removeVanillaDrops(lmEntity, currentDrops)
                return
            }

            additionUsed = additionValue.roundToInt()
            val itemsToNotMultiply = mutableListOf<ItemStack>()

            if (lmEntity.livingEntity.equipment != null){
                // make sure we don't multiply anything it has picked up
                itemsToNotMultiply.addAll(removePickedUpItems(lmEntity, currentDrops))
            }

            // Modify current drops
            for (currentDrop in currentDrops) {
                var skipItem = false
                val iterator = itemsToNotMultiply.iterator()
                while (iterator.hasNext()){
                    val itemToSkip = iterator.next()
                    if (itemToSkip.isSimilar(currentDrop)){
                        skipItem = true
                        iterator.remove()
                    }
                }

                if (!skipItem) multiplyDrop(lmEntity, currentDrop, additionValue)
            }
        }

        if (customDrops.isNotEmpty()) currentDrops.addAll(customDrops)

        val nameWithOverride = if (hasOverride) " (override), " else ""
        val additionUsedFinal = additionUsed
        DebugManager.log(DebugType.SET_LEVELLED_ITEM_DROPS, lmEntity) {
            "${nameWithOverride}, vanilla drops: &b$vanillaDrops&7, all drops: &b${currentDrops.size}&7, addition: &b$additionUsedFinal&7."
        }
    }

    fun multiplyDrop(
        lmEntity: LivingEntityWrapper,
        currentDrop: ItemStack,
        addition: Float,
    ) {
        if (LevelledMobs.instance.mobDataManager.isLevelledDropManaged(currentDrop.type)) {
            DebugManager.log(DebugType.SET_LEVELLED_ITEM_DROPS, lmEntity) { "&7Item was unmanaged." }
            return
        }

        val oldAmount = currentDrop.amount
        val useAmount = ((currentDrop.amount + (currentDrop.amount.toFloat() * addition)).roundToInt())
            .coerceAtMost(currentDrop.maxStackSize)

        currentDrop.amount = useAmount
        DebugManager.log(DebugType.SET_LEVELLED_ITEM_DROPS, lmEntity) {
            "&7Drop: &b${currentDrop.type}&7, old amount: &b$oldAmount&7, addition value: &b$addition&7, " +
                    "new amount: &b${currentDrop.amount}&7."
        }
    }

    private fun removePickedUpItems(
        lmEntity: LivingEntityWrapper,
        drops: MutableList<ItemStack>
    ): MutableList<ItemStack>{
        val removedItems = mutableListOf<ItemStack>()
        val pickedUpItems = PickedUpEquipment(lmEntity).getMobPickedUpItems()
        if (pickedUpItems.isEmpty())
            return removedItems

        val iteratorPickedUpItems = pickedUpItems.listIterator()

        while (iteratorPickedUpItems.hasNext()) {
            val foundItem = iteratorPickedUpItems.next()
            for (mobItem in drops) {
                if (mobItem.isSimilar(foundItem)) {
                    iteratorPickedUpItems.remove()
                    removedItems.add(mobItem)
                    break
                }
            }
        }

        return removedItems
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
            itemsToKeep.add(ItemStack(Material.CHEST))
            for (item in inv.contents){
                if (item != null) itemsToKeep.add(item)
            }
        } else if (lmEntity.livingEntity is Vehicle) {
            for (itemStack in drops) {
                if (itemStack.type == Material.SADDLE) {
                    hadSaddle = true
                    break
                }
            }
        }

        if (LevelledMobs.instance.ver.isRunningPaper) {
            val pickedUpItems = PickedUpEquipment(lmEntity).getMobPickedUpItems()
            itemsToKeep.addAll(pickedUpItems)
        }

        drops.clear()
        drops.addAll(itemsToKeep)
        if (hadSaddle) drops.add(ItemStack(Material.SADDLE))
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
            ).amount
            var newXp = 0.0

            if (dropAddition == Float.MIN_VALUE) {
                DebugManager.log(DebugType.SET_LEVELLED_XP_DROPS, lmEntity) {
                    "xp-vanilla: &b$xp&7, new-xp: &b0&7"
                }
                return 0
            }

            if (dropAddition > -1) newXp = Math.round(xp + (xp * dropAddition)).toDouble()

            val newXpFinal = newXp.toInt()
            DebugManager.log(DebugType.SET_LEVELLED_XP_DROPS, lmEntity) {
                "xp-vanilla: &b$xp&7, new-xp: &b$newXpFinal&7"
            }
            return newXp.toInt()
        }
        else
            return xp.toInt()
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
                nametag.text = replaceStringPlaceholders(nametag.text, lmEntity, true, player, false)
                usePreserveMobName = true

                customDeathMessage =
                    if (nametag.contains("{DisplayName}")) main.rulesManager.getRuleNametagCreatureDeath(lmEntity) else nametag.text
            }
        }

        // ignore if 'disabled'
        if (nametag.isEmpty) {
            val useCustomNameForNametags = main.helperSettings.getBoolean(
                "use-customname-for-mob-nametags"
            )
            return if (useCustomNameForNametags)
                NametagResult(lmEntity.typeName)
            else {
                @Suppress("DEPRECATION")
                NametagResult(lmEntity.livingEntity.customName) // CustomName can be null, that is meant to be the case.
            }
        }
        if (!lmEntity.isLevelled) nametag.text = ""

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

        if (nametag.text.contains("%") && ExternalCompatibilityManager.hasPapiInstalled)
            nametag.text = ExternalCompatibilityManager.getPapiPlaceholder(lmEntity.associatedPlayer, nametag.text)

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

    private fun getAttributesCache(lmEntity: LivingEntityWrapper, str: StringReplacer){
        if (lmEntity.attributeValuesCache != null) return

        val whichOnes = mutableListOf<Attribute>()
        for (placeholder in attributeStringList){
            if (str.text.contains(placeholder.key))
                whichOnes.add(placeholder.value)
        }

        MobDataManager.instance.getAllAttributeValues(lmEntity, whichOnes)
    }

    fun replaceStringPlaceholdersForFormulas(
        text: String,
        lmEntity: LivingEntityWrapper
    ): String{
        val str = StringReplacer(text)

        if (str.text.contains("%level-ratio%", ignoreCase = true)){
            val mobLevel = lmEntity.mobLevel ?: lmEntity.getMobLevel
            val maxLevel = RulesManager.instance.getRuleMobMaxLevel(lmEntity).toFloat()
            val minLevel = RulesManager.instance.getRuleMobMinLevel(lmEntity).toFloat()
            if (mobLevel == 0 || maxLevel == 0f){
                str.replace("%level-ratio%", "0")
                DebugManager.log(DebugType.LEVEL_RATIO, lmEntity){ "mob-lvl was 0 or maxlevel was 0" }
            }
            else{
                val newValue: Float
                val part1 = (mobLevel.toFloat() - minLevel)
                val part2 = (maxLevel - minLevel)
                newValue = if (part2 == 0f)
                    1f
                else if (part1 == 0f)
                    0f
                else
                    (part1 / part2).coerceAtLeast(0f)

                str.replace("%level-ratio%", newValue.toString())
                DebugManager.log(DebugType.LEVEL_RATIO, lmEntity){
                    "'(${mobLevel.toFloat()} - $minLevel) / ($maxLevel - $minLevel)', result: $newValue"
                }
            }
        }

        str.replaceIfExists("%ranged-attack-damage%"){
            if (lmEntity.rangedDamage != null) lmEntity.rangedDamage.toString() else "0"
        }

        for (placeholder in strategyPlaceholders){
            str.replaceIfExists(placeholder.key){
                lmEntity.strategyResults.getOrDefault(placeholder.value, 0f).toString()
            }
        }

        for (placeholder in RulesManager.instance.allCustomStrategyPlaceholders){
            str.replaceIfExists(placeholder){
                lmEntity.customStrategyResults.getOrDefault(placeholder, 0f).toString()
            }
        }

        getAttributesCache(lmEntity, str)

        str.replaceIfExists("%distance-from-spawn%"){ lmEntity.distanceFromSpawn.toString() }
        str.replace("%hotspots-mod%", "0")
        str.replace("%barricades-mod%", "0")
        str.replaceIfExists("%creeper-blast-damage%"){
            val creeper = lmEntity.livingEntity as? Creeper
            return@replaceIfExists creeper?.explosionRadius?.toString() ?: "0"
        }

        for (placeholder in attributeStringList){
            str.replaceIfExists(placeholder.key){ lmEntity.attributeValuesCache?.get(placeholder.value)?.baseValue.toString() }
        }

        str.replaceIfExists("%item-drop%"){ "1" }
        str.replaceIfExists("%xp-drop%"){ "1" }

        if (!str.text.contains("%")) return str.text

        return replaceStringPlaceholders(
            str, lmEntity, true, null, true
        )
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
        val roundedMaxHealth: String = Utils.round(maxHealth).toString()
        val roundedMaxHealthInt = (Utils.round(maxHealth).toInt()).toString()
        val percentHealthTemp = Math.round(entityHealth / maxHealth * 100.0).toDouble()
        val percentHealth = if (percentHealthTemp < 1.0) 1 else percentHealthTemp.toInt()
        val playerId = player?.uniqueId?.toString() ?: ""
        val playerName = player?.name ?: ""
        val rm = LevelledMobs.instance.rulesManager

        var tieredPlaceholder = rm.getRuleTieredPlaceholder(lmEntity)
        if (tieredPlaceholder == null) tieredPlaceholder = ""

        // replace them placeholders ;)
        text.replaceIfExists("%displayname%") {
            val overridenName = if (lmEntity.lockedOverrideName == null)
                rm.getRuleEntityOverriddenName(lmEntity, false)
            else
                lmEntity.lockedOverrideName

            if (!overridenName.isNullOrEmpty()) {
                return@replaceIfExists overridenName
            }

            if (lmEntity.livingEntity.customName != null)
                return@replaceIfExists if (LevelledMobs.instance.ver.isRunningPaper) "{CustomName}" else lmEntity.livingEntity.customName
            return@replaceIfExists if (preserveMobName)
                "{DisplayName}"
            else
                Utils.capitalize(lmEntity.typeName.replace("_", " "))
        }
        text.replace("%mob-lvl%", lmEntity.getMobLevel)
        text.replace(
            "%entity-name%",
            Utils.capitalize(lmEntity.typeName.replace("_", " "))
        )
        text.replace("%entity-name-raw%", lmEntity.typeName)
        text.replace("%entity-health%", Utils.round(entityHealth))
        text.replace("%entity-health-rounded%", entityHealthRounded)
        text.replaceIfExists("%entity-health-rounded-up%"){ ceil(entityHealth).toInt().toString() }
        text.replace("%entity-max-health%", roundedMaxHealth)
        text.replace("%entity-max-health-rounded%", roundedMaxHealthInt)
        text.replaceIfExists("%entity-max-health-rounded-up%"){ ceil(maxHealth).toInt().toString() }
        getHealthPercentRemaining(entityHealth, maxHealth, text)
        text.replaceIfExists("%base-health%"){
            val baseHealth = lmEntity.livingEntity.getAttribute(Attribute.GENERIC_MAX_HEALTH)?.baseValue
            if (baseHealth != null) return@replaceIfExists baseHealth.toString()
            else return@replaceIfExists "0"
        }
        text.replace("%heart-symbol%", "❤")
        text.replace("%tiered%", tieredPlaceholder)
        text.replace("%wg-region%", lmEntity.wgRegionName)
        text.replace("%world%", lmEntity.worldName)
        text.replaceIfExists("%location%") {
                "${lmEntity.livingEntity.location.blockX} " +
                "${lmEntity.livingEntity.location.blockY} " +
                "${lmEntity.livingEntity.location.blockZ}"
        }
        text.replaceIfExists("%min-level%"){ rm.getRuleMobMinLevel(lmEntity).toString() }
        text.replaceIfExists("%max-level%"){ rm.getRuleMobMaxLevel(lmEntity).toString() }
        text.replace("%health%-percent%", percentHealth)
        text.replace("%x%", lmEntity.livingEntity.location.blockX)
        text.replace("%y%", lmEntity.livingEntity.location.blockY)
        text.replace("%z%", lmEntity.livingEntity.location.blockZ)
        text.replace("%player-uuid%", playerId)
        text.replace("%player%", playerName)
        if (text.contains("%rand_")){
            RandomVarianceGenerator.generateVariance(lmEntity, text)
        }

        for (placeholder in ExternalCompatibilityManager.instance.externalPluginPlaceholders){
            text.replaceIfExists(placeholder.key){ placeholder.value.getPlaceholder(lmEntity) }
        }

        if (usePAPI && text.contains("%") && ExternalCompatibilityManager.hasPapiInstalled) {
            text.text = ExternalCompatibilityManager.getPapiPlaceholder(player, text.text)
        }

        return text.text
    }

    private fun getHealthPercentRemaining(
        currentHealth: Double,
        maxHealth: Double,
        text: StringReplacer
    ){
        val start = text.text.indexOf("%entity-max-health-percent")
        if (start < 0) return

        val end = text.text.indexOf("%", start + 25)
        if (end < 0) return

        val percentHealth = (currentHealth.toFloat() / maxHealth.toFloat() * 100f)
        val fullText = text.text.substring(start, end)
        val optional = fullText.substring(fullText.length - 2)
        var digits = 2
        if (optional[0] == '-' && optional[1].isDigit())
            digits = optional[1].digitToInt()

        text.text = text.text.replace(fullText, (
            if (digits == 0) percentHealth.roundToInt().toString()
            else Utils.round(percentHealth.toDouble(), digits).toString()
        ))
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
        Log.inf("&fTasks: &7Starting async nametag auto update task...")

        val main = LevelledMobs.instance
        val period = main.helperSettings.getInt(
            "async-task-update-period",6
        ).toLong() // run every ? seconds.
        this.doCheckMobHash = main.helperSettings.getBoolean("check-mob-hash", true)

        val runnable = Runnable {
            checkLEWCache()
            enumerateNearbyEntities()
        }

        if (main.ver.isRunningFolia) {
            val task = Consumer { _: ScheduledTask? -> runnable.run() }
            nametagTimerTask =
                SchedulerResult(Bukkit.getAsyncScheduler().runAtFixedRate(main, task, 0, period, TimeUnit.SECONDS))
        } else {
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

    private fun enumerateNearbyEntities() {
        entitiesPerPlayer.clear()
        asyncRunningCount.set(0)
        val checkDistance = LevelledMobs.instance.helperSettings.getInt(
            "async-task-max-blocks-from-player", 100
        )

        for (player in Bukkit.getOnlinePlayers()) {
            if (LevelledMobs.instance.ver.isRunningFolia) {
                val runnable = Runnable {
                    if (asyncRunningCount.get() == 0) runNametagCheckASync()
                }

                asyncRunningCount.getAndIncrement()
                val task =
                    Consumer<ScheduledTask> { _: ScheduledTask? ->
                        val entities = player.getNearbyEntities(
                            checkDistance.toDouble(),
                            checkDistance.toDouble(), checkDistance.toDouble()
                        )
                        synchronized(entitiesPerPlayerLock) {
                            entitiesPerPlayer.put(player, entities)
                        }

                        asyncRunningCount.getAndDecrement()
                        if (asyncRunningCount.get() == 0) runNametagCheckASync()
                    }
                player.scheduler.run(LevelledMobs.instance, task, runnable)
            } else {
                val entities = player.getNearbyEntities(
                    checkDistance.toDouble(),
                    checkDistance.toDouble(), checkDistance.toDouble()
                )
                entitiesPerPlayer[player] = entities
                runNametagCheckASync()
            }
        }
    }

    fun startNametagTimer() {
        val scheduler = SchedulerWrapper {  LevelledMobs.instance.nametagTimerChecker.checkNametags() }
        scheduler.runTaskTimerAsynchronously(0, 1000)
    }

    private fun runNametagCheckASync() {
        val entityToPlayer = mutableMapOf<LivingEntityWrapper, MutableList<Player>>()

        if (LevelledMobs.instance.ver.isRunningFolia) {
            for (player in entitiesPerPlayer.keys) {
                for (entity in entitiesPerPlayer[player]!!) {
                    val task =
                        Consumer { _: ScheduledTask? ->
                            checkEntity(
                                entity,
                                player,
                                entityToPlayer
                            )
                        }
                    entity.scheduler.run(LevelledMobs.instance, task, null)
                }
            }
        } else {
            for (player in entitiesPerPlayer.keys) {
                for (entity in entitiesPerPlayer[player]!!) {
                    checkEntity(entity, player, entityToPlayer)
                }
            }
        }

        for ((lmEntity, value) in entityToPlayer) {
            if (entityToPlayer.containsKey(lmEntity)) {
                checkEntityForPlayerLevelling(lmEntity, value)
            }

            lmEntity.free()
        }

        entitiesPerPlayer.clear()
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
            val internalSpawnReason = lmEntity.spawnReason.getInternalSpawnReason(lmEntity)
            var skipLevelling = (internalSpawnReason == InternalSpawnReason.LM_SPAWNER ||
                    internalSpawnReason == InternalSpawnReason.LM_SUMMON
                    )
            if (main.rulesManager.isPlayerLevellingEnabled() && !lmEntity.isRulesForceAll && !checkIfReadyForRelevelling(
                    lmEntity
                )
            ) {
                skipLevelling = true
            }
            if (main.rulesManager.isPlayerLevellingEnabled() && !skipLevelling) {
                val hasKey = entityToPlayer.containsKey(lmEntity)
                val players = if (hasKey) entityToPlayer[lmEntity]!! else mutableListOf()
                players.add(player)
                if (!hasKey) {
                    entityToPlayer[lmEntity] = players
                }
                wrapperHasReference = true
            }

            if (!lmEntity.isPopulated) {
                return
            }
            val nametagVisibilityEnums = lmEntity.nametagVisibilityEnum
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

        if (MainCompanion.instance.excludePlayersInCreative){
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
        val logonTime =  MainCompanion.instance.getRecentlyJoinedPlayerLogonTime(closestPlayer)
        if (logonTime != null) {
            if (Utils.getMillisecondsFromInstant(logonTime) < 5000L) {
                return
            }
            MainCompanion.instance.removeRecentlyJoinedPlayer(closestPlayer)
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
        } else if (lmEntity.livingEntity.isValid && location.world != null && location.world == lmEntity.world
            && lmEntity.location.distanceSquared(location) <= maxDistance) {
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

        if (lmEntity.spawnReason.getInternalSpawnReason(lmEntity) == InternalSpawnReason.LM_SUMMON)
            return false

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
            Log.inf("&fTasks: &7Stopping async nametag auto update task...")
            nametagAutoUpdateTask!!.cancelTask()
        }

        if (nametagTimerTask != null && !nametagTimerTask!!.isCancelled()) {
            nametagTimerTask!!.cancelTask()
        }
    }

    private fun applyLevelledAttributes(
        lmEntity: LivingEntityWrapper,
        additions: MutableList<Addition>,
        nbtDatas: MutableList<String>
    ) {
        if (!lmEntity.isLevelled) return
        val modInfo = mutableListOf<AttributePreMod>()

        if (lmEntity.attributeValuesCache.isNullOrEmpty())
            MobDataManager.instance.getAllAttributeValues(lmEntity)

        for (addition in additions){
            val attribute: Attribute
            when (addition) {
                Addition.ATTRIBUTE_MAX_HEALTH -> attribute = Attribute.GENERIC_MAX_HEALTH
                Addition.ATTRIBUTE_ATTACK_DAMAGE -> attribute = Attribute.GENERIC_ATTACK_DAMAGE
                Addition.ATTRIBUTE_MOVEMENT_SPEED -> attribute = Attribute.GENERIC_MOVEMENT_SPEED
                Addition.ATTRIBUTE_HORSE_JUMP_STRENGTH -> attribute = Attribute.GENERIC_JUMP_STRENGTH
                Addition.ATTRIBUTE_ARMOR_BONUS -> attribute = Attribute.GENERIC_ARMOR
                Addition.ATTRIBUTE_ARMOR_TOUGHNESS -> attribute = Attribute.GENERIC_ARMOR_TOUGHNESS
                Addition.ATTRIBUTE_KNOCKBACK_RESISTANCE -> attribute = Attribute.GENERIC_KNOCKBACK_RESISTANCE
                Addition.ATTRIBUTE_FLYING_SPEED -> attribute = Attribute.GENERIC_FLYING_SPEED
                Addition.ATTRIBUTE_ATTACK_KNOCKBACK -> attribute = Attribute.GENERIC_ATTACK_KNOCKBACK
                Addition.ATTRIBUTE_FOLLOW_RANGE -> attribute = Attribute.GENERIC_FOLLOW_RANGE
                Addition.ATTRIBUTE_ZOMBIE_SPAWN_REINFORCEMENTS -> {
                    if (lmEntity.spawnReason.getMinecraftSpawnReason(lmEntity) == CreatureSpawnEvent.SpawnReason.REINFORCEMENTS)
                        continue

                    attribute = Attribute.ZOMBIE_SPAWN_REINFORCEMENTS
                }
                else -> throw IllegalStateException(
                    "Addition must be an Attribute, if so, it has not been considered in this method"
                )
            }

            val result = MobDataManager.instance.prepareSetAttributes(lmEntity, attribute, addition)
            if (result != null) modInfo.add(result)
        }

        val scheduler = SchedulerWrapper(lmEntity.livingEntity){
            MobDataManager.instance.setAttributeMods(lmEntity, modInfo)

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

            applyNbtData(lmEntity, nbtDatas)

            if (lmEntity.livingEntity is Creeper) {
                lmEntity.main.levelManager.applyCreeperBlastRadius(lmEntity)
            }

            lmEntity.free()
        }
        lmEntity.inUseCount.getAndIncrement()
        scheduler.runDirectlyInFolia = true
        scheduler.run()
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
            ) { "mulp: null, result: 3" }
            return
        }

        val maxRadius: Int = main.rulesManager.getRuleCreeperMaxBlastRadius(lmEntity)
        val damage = main.mobDataManager.getAdditionsForLevel(
            lmEntity,
            Addition.CREEPER_BLAST_DAMAGE, 3f
        ).amount
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
            "mulp: ${Utils.round(damage.toDouble(), 3)}, max: $maxRadius, result: $blastRadiusFinal"
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

        if (LevelledMobs.instance.ver.isRunningFolia){
            applyLevelledEquipmentNonAsync(lmEntity, customDropsRuleSet)
            return
        }

        val scheduler = SchedulerWrapper {
            applyLevelledEquipmentNonAsync(lmEntity, customDropsRuleSet)
            lmEntity.free()
        }

        lmEntity.inUseCount.getAndIncrement()
        scheduler.entity = lmEntity.livingEntity
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
        if (items.isEmpty()) return

        val equipment = lmEntity.livingEntity.equipment ?: return

        if (lmEntity.lockEntitySettings && customDropsRuleSet.useDropTableIds.isNotEmpty()) {
            val customDrops = customDropsRuleSet.useDropTableIds.joinToString(";")
            lmEntity.pdc.set(NamespacedKeys.lockedDropRules, PersistentDataType.STRING, customDrops)
            if (customDropsRuleSet.chunkKillOptions!!.getDisableVanillaDrops()) lmEntity.pdc
                .set(NamespacedKeys.lockedDropRulesOverride, PersistentDataType.INTEGER, 1)
        }

        var hadMainItem = false
        var hadPlayerHead = false
        val equippedItemsInfo = EquippedItemsInfo()
        val equippedCountPerGroup = mutableMapOf<String?, Int>()
        var equippedSoFar = 0

        dropResult.stackToItem.shuffle()

        for ((itemStack, item) in dropResult.stackToItem) {
            val material = itemStack.type
            val groupLimits = main.customDropsHandler.getGroupLimits(item)
            val hasEquipLimits = item.hasGroupId && groupLimits != null && groupLimits.hasCapEquipped

            if (hasEquipLimits) {
                if (equippedCountPerGroup.containsKey(item.groupId)) {
                    equippedSoFar = equippedCountPerGroup[item.groupId]!!
                }

                if (groupLimits!!.hasReachedCapEquipped(equippedSoFar)) {
                    DebugManager.log(DebugType.GROUP_LIMITS, lmEntity) {
                        "Reached equip limit of ${groupLimits.capEquipped}, item: $material, group: ${item.groupId}"
                    }
                    continue
                }
            }

            if (EnchantmentTarget.ARMOR_FEET.includes(material)) {
                equipment.setBoots(itemStack, true)
                equipment.bootsDropChance = 0f
                equippedItemsInfo.boots = item.itemStack
            } else if (EnchantmentTarget.ARMOR_LEGS.includes(material)) {
                equipment.setLeggings(itemStack, true)
                equipment.leggingsDropChance = 0f
                equippedItemsInfo.leggings = item.itemStack
            } else if (EnchantmentTarget.ARMOR_TORSO.includes(material)) {
                equipment.setChestplate(itemStack, true)
                equipment.chestplateDropChance = 0f
                equippedItemsInfo.chestplate = item.itemStack
            } else if (EnchantmentTarget.ARMOR_HEAD.includes(material)
                || material.name.endsWith("_HEAD") || (item.equipOnHelmet
                        && !hadPlayerHead)
            ) {
                equipment.setHelmet(itemStack, true)
                equipment.helmetDropChance = 0f
                equippedItemsInfo.helmet = item.itemStack
                if (material == Material.PLAYER_HEAD) {
                    hadPlayerHead = true
                }
            } else {
                if (!hadMainItem) {
                    equipment.setItemInMainHand(itemStack)
                    equipment.itemInMainHandDropChance = 0f
                    equippedItemsInfo.mainHand = item.itemStack
                    hadMainItem = true
                } else if (item.equipOffhand) {
                    equipment.setItemInOffHand(itemStack)
                    equipment.itemInOffHandDropChance = 0f
                    equippedItemsInfo.offhand = item.itemStack
                }
            }

            equippedSoFar++

            if (hasEquipLimits) {
                equippedCountPerGroup[item.groupId] = equippedSoFar
            }
        }

        equippedItemsInfo.saveEquipment(lmEntity)
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

        if (!main.ver.isRunningFolia && Bukkit.isPrimaryThread()){
            val scheduler = SchedulerWrapper{
                applyLevelToMob(lmEntity, level, isSummoned, bypassLimits, additionalLevelInformation)
                lmEntity.free()
            }
            lmEntity.inUseCount.getAndIncrement()
            scheduler.run()
            return
        }

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
                useLevel = vehicle.getMobLevel
                lmEntity.setMobPrelevel(useLevel)
            }

            vehicle.free()
        }

        if (isSummoned) {
            lmEntity.spawnReason.setInternalSpawnReason(lmEntity, InternalSpawnReason.LM_SUMMON, true)
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
                lmEntity.shouldShowLMNametag = false
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
            ) { "&7 had &bnoLevelKey&7 attached" }
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

        if (nbtDatas!!.isNotEmpty() && !ExternalCompatibilityManager.hasNbtApiInstalled) {
            val msg = if (isSummoned)
                "NBT Data was supplied but the required plugin NBTAPI is not installed!"
            else
                "NBT Data has been specified in customdrops.yml but the required plugin NBTAPI is not installed!"
            if (!hasMentionedNBTAPIMissing) {
                Log.war(msg)
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

        try{
            applyLevelToMob2(lmEntity, nbtDatas, doSkipLMNametag)

            val levelCause =
                if (isSummoned) MobPostLevelEvent.LevelCause.SUMMONED
                else MobPostLevelEvent.LevelCause.NORMAL
            Bukkit.getPluginManager()
                .callEvent(MobPostLevelEvent(lmEntity, levelCause, additionalLevelInformation))

            val sb = StringBuilder().append("world: ").append(lmEntity.worldName).append(", level: ").append(useLevel)
            if (isSummoned)
                sb.append(" (summoned)")
            if (bypassLimits)
                sb.append(" (limit bypass)")

            DebugManager.log(DebugType.APPLY_LEVEL_RESULT, lmEntity, true, sb::toString)
        }
        catch (e: java.util.concurrent.TimeoutException){
            DebugManager.log(DebugType.APPLY_LEVEL_RESULT, lmEntity, false){
                "Timed out applying level to mob"
            }
        }
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
        LevelledMobs.instance.levelManager.applyLevelledEquipment(lmEntity, lmEntity.getMobLevel)
    }

    private fun applyAttribs(
        lmEntity: LivingEntityWrapper,
        nbtDatas: MutableList<String>
    ) {
        val main = LevelledMobs.instance
        val attribs = mutableListOf(
            Addition.ATTRIBUTE_ATTACK_DAMAGE,
            Addition.ATTRIBUTE_MAX_HEALTH,
            Addition.ATTRIBUTE_MOVEMENT_SPEED,
            Addition.ATTRIBUTE_ARMOR_BONUS,
            Addition.ATTRIBUTE_ARMOR_TOUGHNESS,
            Addition.ATTRIBUTE_ATTACK_KNOCKBACK,
            Addition.ATTRIBUTE_FLYING_SPEED,
            Addition.ATTRIBUTE_KNOCKBACK_RESISTANCE,
            Addition.ATTRIBUTE_FOLLOW_RANGE
        )

        if (lmEntity.livingEntity is Zombie)
            attribs.add(Addition.ATTRIBUTE_ZOMBIE_SPAWN_REINFORCEMENTS)
        else if (main.ver.minorVersion >= 20 && lmEntity.livingEntity is Horse)
            attribs.add(Addition.ATTRIBUTE_HORSE_JUMP_STRENGTH)

        main.levelManager.applyLevelledAttributes(lmEntity, attribs, nbtDatas)
    }

    private fun applyNbtData(
        lmEntity: LivingEntityWrapper,
        nbtDatas: MutableList<String>
    ){
        if (nbtDatas.isEmpty()) return
        var hadSuccess = false
        val allResults = mutableListOf<NBTApplyResult>()

        for (nbtData: String in nbtDatas) {
            val result: NBTApplyResult = NBTManager.applyNBTDataMob(
                lmEntity,
                nbtData
            )
            if (result.hadException) {
                if (lmEntity.summonedSender == null) {
                    Log.war(
                        "Error applying NBT data '$nbtData' to ${lmEntity.nameIfBaby}. Exception message: ${result.exceptionMessage}"
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
                "Applied NBT data, ${MiscUtils.getNBTDebugMessage(allResults)}"
            }
        }
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
                Log.war(
                    "Got ConcurrentModificationException in LevelManager checking entity isLevelled, succeeded on retry"
                )
            } else {
                Log.war(
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