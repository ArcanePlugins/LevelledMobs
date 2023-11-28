package io.github.arcaneplugins.levelledmobs.bukkit.logic.customdrops.type

import io.github.arcaneplugins.levelledmobs.bukkit.api.data.ItemDataUtil
import io.github.arcaneplugins.levelledmobs.bukkit.config.translations.Message
import io.github.arcaneplugins.levelledmobs.bukkit.debug.DebugCategory
import io.github.arcaneplugins.levelledmobs.bukkit.logic.customdrops.CustomDrop
import io.github.arcaneplugins.levelledmobs.bukkit.logic.customdrops.recipient.CustomDropRecipient
import io.github.arcaneplugins.levelledmobs.bukkit.util.EnchantTuple
import io.github.arcaneplugins.levelledmobs.bukkit.util.Log.debug
import io.github.arcaneplugins.levelledmobs.bukkit.util.math.RangedInt
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.LivingEntity
import org.bukkit.inventory.EntityEquipment
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.Damageable

import java.util.concurrent.ThreadLocalRandom

class ItemCustomDrop(
    val material: Material,
    recipient: CustomDropRecipient
): CustomDrop(StandardCustomDropType.ITEM.name, recipient) {
    val name: String? = null
    var amount = RangedInt(1)
    var customModelData: Int? = null
    var noMultiplier = true //TODO
    val itemFlags = mutableSetOf<ItemFlag>()
    var durabilityLoss = 0
    val enchantments = mutableSetOf<EnchantTuple>()
    var equipmentChance = 0f
    val allowedEquipmentSlots = mutableSetOf<EquipmentSlot>()
    var lmItemsExternalType: String? = null //TODO
    val lmItemsExternalAmount: String? = null //TODO
    var onlyDropIfEquipped = true

    fun attemptToApplyEquipment(
        entity: LivingEntity
    ){
        if (equipmentChance == 0f) return
        if (allowedEquipmentSlots.isEmpty()) return
        if (equipmentChance > ThreadLocalRandom.current().nextFloat()) return

        val ee = entity.equipment ?: return

        for (es in allowedEquipmentSlots){
            if (hasItemAtEquipmentSlot(ee, es)) continue
            ee.setItem(es, toItemStack())
            ee.setDropChance(es, 0f)
        }
    }

    private fun hasItemAtEquipmentSlot(
        ee: EntityEquipment,
        es: EquipmentSlot
    ): Boolean{
        return try{
            val itemStack = ee.getItem(es)
            itemStack.type != Material.AIR
        } catch (e: NullPointerException){
            false
        }
    }

    fun toItemStack(): ItemStack{
        debug(DebugCategory.DROPS_GENERIC) { "ItemCustomDrop#toItemStack begin" }

        val itemStack = ItemStack(material, amount.choose())

        if (itemStack.itemMeta == null) return itemStack
        ItemDataUtil.setIsItemCustom(itemStack, true)

        val itemMeta = itemStack.itemMeta

        debug(DebugCategory.DROPS_GENERIC) { "Processing enchant tuples" }
        for (tuple in enchantments){
            debug(DebugCategory.DROPS_GENERIC) { "Processing enchant tuple BEGIN: $tuple" }

            val randomChance = ThreadLocalRandom.current().nextFloat(0f, 100f)
            debug(DebugCategory.DROPS_GENERIC) {"tupleChance=${tuple.chance}; randomChance=$randomChance"}
            if (tuple.chance < randomChance) continue
            debug(DebugCategory.DROPS_GENERIC) { "Chance passed (OK)" }

            val enchantment: Enchantment = tuple.enchantment
            val strength: Int = tuple.strength

            if (itemMeta.hasEnchant(enchantment) && itemMeta.getEnchantLevel(enchantment) >= strength) continue

            debug(DebugCategory.DROPS_GENERIC) { "Enchant is not already applied to item (OK)" }

            itemMeta.addEnchant(enchantment, strength, true)
            debug(DebugCategory.DROPS_GENERIC) { "Enchant added (OK, DONE)" }
        }

        if (itemMeta is Damageable) itemMeta.damage = durabilityLoss

        if (name != null) itemMeta.displayName(
            Message.formatMd(mutableListOf(name))
        )

        debug(DebugCategory.DROPS_GENERIC) {
            "Item drop displayName: " + LegacyComponentSerializer.legacySection()
                .serialize(itemMeta.displayName()?: Component.empty())
        }

        itemMeta.setCustomModelData(customModelData)
        for (iFlag in itemFlags) itemMeta.addItemFlags(iFlag)
        itemStack.setItemMeta(itemMeta)

        debug(DebugCategory.DROPS_GENERIC) { "ItemCustomDrop#toItemStack done" }
        return itemStack
    }

    fun withEnchantments(
        enchantments: MutableList<EnchantTuple>
    ): ItemCustomDrop{
        enchantments.addAll(enchantments)
        return this
    }

    fun withDurabilityLoss(
        durabilityLoss: Int
    ): ItemCustomDrop{
        this.durabilityLoss = durabilityLoss
        return this
    }

    fun withNoMultiplier(
        noMultiplier: Boolean
    ): ItemCustomDrop{
        this.noMultiplier = noMultiplier
        return this
    }

    fun withCustomModelData(
        customModelData: Int
    ): ItemCustomDrop{
        this.customModelData = customModelData
        return this
    }

    fun withItemFlags(
        itemFlags: MutableList<ItemFlag>?
    ): ItemCustomDrop{
        if (itemFlags == null) return this
        this.itemFlags.addAll(itemFlags)
        return this
    }

    fun withAmount(
        amount: RangedInt
    ): ItemCustomDrop{
        this.amount = amount
        return this
    }

    fun withName(
        name: String?
    ): ItemCustomDrop{
        this.name
        return this
    }

    fun withEquipmentChance(
        equipmentChance: Float
    ): ItemCustomDrop {
        this.equipmentChance = equipmentChance
        return this
    }

    fun withAllowedEquipmentSlots(
        equipmentSlots: MutableList<EquipmentSlot>
    ): ItemCustomDrop {
        this.allowedEquipmentSlots.addAll(equipmentSlots)
        return this
    }

    fun setLmItemsExternalType(
        lmItemsExternalType: String?
    ): ItemCustomDrop {
        this.lmItemsExternalType = lmItemsExternalType
        return this
    }

    fun withOnlyDropIfEquipped(
        onlyDropIfEquipped: Boolean
    ): ItemCustomDrop {
        this.onlyDropIfEquipped = onlyDropIfEquipped
        return this
    }
}