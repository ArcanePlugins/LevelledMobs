package io.github.arcaneplugins.levelledmobs.bukkit.listener.impl

import io.github.arcaneplugins.levelledmobs.bukkit.LevelledMobs
import io.github.arcaneplugins.levelledmobs.bukkit.api.data.EntityDataUtil
import io.github.arcaneplugins.levelledmobs.bukkit.api.data.ItemDataUtil
import io.github.arcaneplugins.levelledmobs.bukkit.debug.DebugCategory
import io.github.arcaneplugins.levelledmobs.bukkit.listener.ListenerWrapper
import io.github.arcaneplugins.levelledmobs.bukkit.logic.LogicHandler.evaluateExpression
import io.github.arcaneplugins.levelledmobs.bukkit.logic.LogicHandler.replacePapiAndContextPlaceholders
import io.github.arcaneplugins.levelledmobs.bukkit.logic.LogicHandler.runFunctionsWithTriggers
import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.Context
import io.github.arcaneplugins.levelledmobs.bukkit.logic.customdrops.CustomDrop
import io.github.arcaneplugins.levelledmobs.bukkit.logic.customdrops.CustomDropHandler.getDefinedCustomDropsForEntity
import io.github.arcaneplugins.levelledmobs.bukkit.logic.customdrops.cdevent.CustomDropsEventType
import io.github.arcaneplugins.levelledmobs.bukkit.logic.customdrops.type.CommandCustomDrop
import io.github.arcaneplugins.levelledmobs.bukkit.logic.customdrops.type.ItemCustomDrop
import io.github.arcaneplugins.levelledmobs.bukkit.logic.customdrops.type.StandardCustomDropType
import io.github.arcaneplugins.levelledmobs.bukkit.util.EquipmentUtils
import io.github.arcaneplugins.levelledmobs.bukkit.util.Log
import io.github.arcaneplugins.levelledmobs.bukkit.util.Log.debug
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack
import java.util.concurrent.ThreadLocalRandom
import java.util.function.Function
import java.util.function.Supplier
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

class EntityDeathListener : ListenerWrapper(true) {
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    fun handle(event: EntityDeathEvent){
        val context = Context(event.entity)

        if (EntityDataUtil.isLevelled(event.entity, true)) {
            debug(DebugCategory.DROPS_GENERIC) { "Entity is levelled, handling death drops" }
            handleItemDrops(event, context)
            handleExpDrops(event)
            debug(DebugCategory.DROPS_GENERIC) { "Finished handling death drops" }
        }

        runFunctionsWithTriggers(context, mutableListOf("on-entity-death"))
    }

    private fun handleItemDrops(
        event: EntityDeathEvent,
        context: Context
    ){
        val entity = event.entity
        val debugDropLister = Supplier {
            val sb = StringBuilder(
                "*** Total Drop Evaluation (${event.drops.size}): "
            )
            for (itemStack in event.drops) {
                if (itemStack == null) {
                    sb.append("[!!null!!]; ")
                } else {
                    sb.append("[${itemStack.type} x${itemStack.amount}]; ")
                }
            }
            sb.toString()
        }

        debug(DebugCategory.DROPS_GENERIC, debugDropLister)

        val vanillaDrops = mutableListOf<ItemStack>()
        val nonVanillaDrops = mutableListOf<ItemStack>()
        val customDrops = mutableListOf<CustomDrop>()
        val customDropsToDrop = mutableListOf<ItemStack>()

        sortItemDrops(event, vanillaDrops, nonVanillaDrops)
        generateCustomDrops(event, vanillaDrops, nonVanillaDrops, customDrops)
        multiplyItemDrops(
            event, context,
            vanillaDrops, nonVanillaDrops,
            customDrops, customDropsToDrop
        )

        debug(DebugCategory.DROPS_GENERIC) {
            """
            Custom Drops to Drop (pre-filtering): ${customDropsToDrop.size}.
            """
                .trimIndent()
        }

        customDropsToDrop.removeIf { itemStack: ItemStack? ->
            itemStack != null && EquipmentUtils.findSimilarItemStackInEntity(
                entity,
                itemStack
            ) { predicateStack: ItemStack ->
                ItemDataUtil.isItemCustom(predicateStack).toFalsyBoolean()
            } != null
        }

        debug(DebugCategory.DROPS_GENERIC) {
            """
            Vanilla Drops: ${vanillaDrops.size}; Non-Vanilla Drops: ${nonVanillaDrops.size}; Custom Drops: ${customDrops.size}; Custom Drops to Drop: ${customDropsToDrop.size}.
            """
                .trimIndent()
        }

        event.drops.clear()
        event.drops.addAll(vanillaDrops)
        event.drops.addAll(nonVanillaDrops)
        event.drops.addAll(customDropsToDrop)

        debug(DebugCategory.DROPS_GENERIC, debugDropLister)
    }

    private fun sortItemDrops(
        event: EntityDeathEvent,
        vanillaDrops: MutableList<ItemStack>,
        nonVanillaDrops: MutableList<ItemStack>,
    ){
        debug(DebugCategory.DROPS_GENERIC) { "[sort] Sorting item drops" }
        // Note that LM Equipment which is dropped from the mob is considered non-vanilla
        // in the item sorting, NOT as an itemstack to be added to the customDrops list.
        // The customDrops list contains a list of NEW itemstacks to be dropped by the mob
        // upon their death as defined in the custom drops file.

        // Note that LM Equipment which is dropped from the mob is considered non-vanilla
        // in the item sorting, NOT as an itemstack to be added to the customDrops list.
        // The customDrops list contains a list of NEW itemstacks to be dropped by the mob
        // upon their death as defined in the custom drops file.
        for (drop in event.drops) {
            if (isNonVanillaItemDrop(event.entity, drop)) {
                debug(DebugCategory.DROPS_GENERIC) {
                    "[sort] Sorted non-vanilla drop '${drop.type}'."
                }
                nonVanillaDrops.add(drop)
            } else {
                debug(DebugCategory.DROPS_GENERIC) {
                    "[sort] Sorted vanilla drop '${drop.type}'."
                }
                vanillaDrops.add(drop)
            }
        }
    }

    private fun isNonVanillaItemDrop(
        entity: LivingEntity,
        itemStack: ItemStack
    ): Boolean{
        debug(DebugCategory.DROPS_GENERIC) {
            "[isNonVanilla] Checking if '${itemStack.type}' is non-vanilla"
        }

        // Return true if the item was created by LM ('Custom').
        return if (ItemDataUtil.isItemCustom(itemStack)
                .toFalsyBoolean()
        ) true  // Return true if item is in the entity's inventory
        else entity is InventoryHolder && entity.inventory.contains(itemStack)
    }

    private fun generateCustomDrops(
        event: EntityDeathEvent,
        vanillaDrops: MutableList<ItemStack>,
        nonVanillaDrops: MutableList<ItemStack>,
        customDrops: MutableList<CustomDrop>
    ){
        val entity = event.entity

        debug(DebugCategory.DROPS_GENERIC) {
            "[GenCustDrp] Generating custom item drops. " +
                    "Collection size: ${customDrops.size}"
        }

        val context = Context(entity).withEvent(event)

        debug(DebugCategory.DROPS_GENERIC) { "attempting to retrieve player context" }
        var player: Player? = null
        if (entity.lastDamageCause != null) {
            debug(DebugCategory.DROPS_GENERIC) { "last damage cause found" }
            if (entity.lastDamageCause is EntityDamageByEntityEvent) {
                val lastDamage = entity.lastDamageCause as EntityDamageByEntityEvent
                debug(DebugCategory.DROPS_GENERIC) { "last damage cause is instanceof ...ByEntity" }
                if (lastDamage.damager is Player) {
                    player = lastDamage.damager as Player
                    context.withPlayer(player)
                    debug(DebugCategory.DROPS_GENERIC) { "player context found" }
                }
            }
        }

        customDrops.addAll(
            getDefinedCustomDropsForEntity(
                entity,
                context
            )
        )

        debug(DebugCategory.DROPS_GENERIC) {"[GenCustDrp] Generated ${customDrops.size} custom drops."}
        debug(DebugCategory.DROPS_GENERIC) { "[GenCustDrp] Filtering custom drops by chance" }
        customDrops.removeIf { drop: CustomDrop ->
            drop.chance == 0f ||
                    drop.chance < ThreadLocalRandom.current().nextFloat(0f, 100f)
        }
        debug(DebugCategory.DROPS_GENERIC) {"[GenCustDrp] Drops chance-filtered, resulting in ${customDrops.size} custom drops."}
        debug(DebugCategory.DROPS_GENERIC) { "[GenCustDrp] Filtering custom drops by death cause" }

        customDrops.removeIf { drop: CustomDrop ->
            debug(DebugCategory.DROPS_GENERIC) { "START Checking drop of type " + drop.type }
            val deathCauses: Collection<String> = drop.deathCauses
            debug(DebugCategory.DROPS_GENERIC) { "Allowed death causes: $deathCauses(OK)" }
            if (deathCauses.isEmpty()) return@removeIf false
            debug(DebugCategory.DROPS_GENERIC) { "Death causes is not empty; continuing (OK)" }
            val ed = entity.lastDamageCause ?: return@removeIf false
            debug(DebugCategory.DROPS_GENERIC) { "Last damage cause is defined (OK)" }
            if (ed is EntityDamageByEntityEvent &&
                ed.damager.type == EntityType.PLAYER
            ) {
                debug(DebugCategory.DROPS_GENERIC) {
                    "removing drop due to player death cause: " + !deathCauses.contains("PLAYER")
                }
                return@removeIf !deathCauses.contains("PLAYER")
            }
            debug(DebugCategory.DROPS_GENERIC) { "checking death cause " + ed.cause.name }
            !deathCauses.contains(ed.cause.name)
        }
        debug(DebugCategory.DROPS_GENERIC) {
            "[GenCustDrp] Drops cause-filtered, resulting in ${customDrops.size} custom drops."
        }
        /*
        We want to increase the chance of custom item drops if the player is using a looting item.
         */

        if (player != null) {
            val lootingLevel = getPlayerItemLootingEnchLevel(player)
            debug(DebugCategory.DROPS_GENERIC) {
                "[GenCustDrp] Player looting level: $lootingLevel"
            }
            if (lootingLevel > 0) {
                for (drop in customDrops) {
                    if (drop.type != StandardCustomDropType.ITEM.name) return
                    drop.withChance(
                        max(0.0, min(100.0, (drop.chance + 1.0f * lootingLevel).toDouble()))
                            .toFloat()
                    )
                }
            }
        }

        if (customDrops.stream().anyMatch(CustomDrop::overridesVanillaDrops)) vanillaDrops.clear()
        if (customDrops.stream().anyMatch(CustomDrop::overridesNonVanillaDrops)) nonVanillaDrops.clear()
    }

    /*
    Multiplies applicable items

    It also runs applicable Command custom drops
     */
    private fun multiplyItemDrops(
        event: EntityDeathEvent,
        context: Context,
        vanillaDrops: MutableList<ItemStack>,
        nonVanillaDrops: MutableList<ItemStack>,
        customDrops: MutableList<CustomDrop>,
        customDropsToDrop: MutableList<ItemStack>
    ){
        debug(DebugCategory.DROPS_GENERIC) { "[Mult] Multiplying item drops" }

        val entity = event.entity

        val evaluatedMultiplier = evaluateExpression(
            replacePapiAndContextPlaceholders(
                EntityDataUtil.getItemDropMultiplierFormula(entity, true),
                Context(entity)
            )
        )

        debug(DebugCategory.DROPS_GENERIC) { "[Mult] Evaluated multiplier: $evaluatedMultiplier" }

        val stackMultiplier =
            Function { stack: ItemStack ->
                val newAmount = (stack.amount * evaluatedMultiplier).toInt()
                debug(DebugCategory.DROPS_GENERIC) { "[Mult] NewAmount=$newAmount" }
                val additionalStacks =
                    floor(newAmount * 1.0 / stack.maxStackSize).toInt()
                val lastStack = newAmount - stack.maxStackSize * additionalStacks
                debug(DebugCategory.DROPS_GENERIC) { "[Mult] AdditionalStacks=$additionalStacks" }
                debug(DebugCategory.DROPS_GENERIC) { "[Mult] LastStack=$lastStack" }
                val result = mutableListOf<ItemStack>()
                    //arrayOfNulls<ItemStack>(additionalStacks + 1)
                for (i in 1..<additionalStacks) {
                    val itemStack = stack.clone()
                    itemStack.amount = stack.maxStackSize
                    result.add(itemStack)
                }
                stack.amount = lastStack
                if (result.isNotEmpty()){
                    result[additionalStacks] = stack
                }
                result
            }

        /*
        handle vanilla drops
         */
        debug(DebugCategory.DROPS_GENERIC) { "[Mult] Multiplying vanilla drops" }
        val newVanillaDrops = mutableListOf<ItemStack>()
        for (vanillaDrop in vanillaDrops) {
            newVanillaDrops.addAll(stackMultiplier.apply(vanillaDrop))
        }
        vanillaDrops.clear()
        vanillaDrops.addAll(newVanillaDrops)

        /*
        handle custom drops
         */

        debug(DebugCategory.DROPS_GENERIC) { "[Mult] Multiplying custom drops" }
        for (customDrop in customDrops){
            if (customDrop is ItemCustomDrop) {
                val itemStack = customDrop.toItemStack()

                if (customDrop.noMultiplier) {
                    customDropsToDrop.add(itemStack)
                } else {
                    customDropsToDrop.addAll(stackMultiplier.apply(itemStack))
                }
            } else if (customDrop is CommandCustomDrop) {
                debug(DebugCategory.DROPS_GENERIC) { "[Mult] Command drop detected, running if applicable" }
                if (customDrop.commandRunEvents.contains("ON_DEATH")) {
                    customDrop.execute(CustomDropsEventType.ON_DEATH, context)
                }
            }
        }

        /*
        handle non-vanilla drops
         */

        val multiplyNonVanillaDrops: Boolean = LevelledMobs.lmInstance
            .configHandler
            .settingsCfg
            .root!!.node("advanced", "multiply-non-vanilla-drops")
            .getBoolean(false)

        debug(DebugCategory.DROPS_GENERIC) { "[Mult] Multiply non-vanilla drops?=$multiplyNonVanillaDrops" }

        if (multiplyNonVanillaDrops) {
            debug(DebugCategory.DROPS_GENERIC) { "[Mult] Multiplying non-vanilla drops" }
            val newNonVanillaDrops = mutableListOf<ItemStack>()
            for (nonVanillaDrop in nonVanillaDrops) {
                newNonVanillaDrops.addAll(stackMultiplier.apply(nonVanillaDrop))
            }
            nonVanillaDrops.clear()
            nonVanillaDrops.addAll(newNonVanillaDrops)
        }
    }

    private fun handleExpDrops(
        event: EntityDeathEvent
    ){
        debug(DebugCategory.DROPS_GENERIC) { "[Exp] Handling exp; starting with " + event.droppedExp }
        val entity = event.entity

        val multFormula = EntityDataUtil
            .getExpDropMultiplierFormula(entity, true)

        if (multFormula.isNullOrBlank()) return

        val eval = evaluateExpression(
            replacePapiAndContextPlaceholders(
                multFormula,
                Context().withEntity(entity)
            )
        )

        debug(DebugCategory.DROPS_GENERIC) { "[Exp] Multiplying exp drops by $eval" }
        event.droppedExp = (event.droppedExp * eval).toInt()
        debug(DebugCategory.DROPS_GENERIC) { "[Exp] finishing with " + event.droppedExp }
    }

    companion object{
        private fun getPlayerPrimaryHeldItem(
            player: Player
        ): ItemStack{
            val inv = player.inventory

            return if (inv.itemInMainHand.type == Material.AIR) inv.itemInOffHand
            else inv.itemInMainHand
        }

        private fun getPlayerItemLootingEnchLevel(
            player: Player
        ): Int{
            return getPlayerPrimaryHeldItem(player).getEnchantmentLevel(Enchantment.LOOT_BONUS_MOBS)
        }
    }
}