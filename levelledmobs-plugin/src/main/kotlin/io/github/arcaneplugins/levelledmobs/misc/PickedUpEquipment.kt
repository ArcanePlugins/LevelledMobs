package io.github.arcaneplugins.levelledmobs.misc

import io.github.arcaneplugins.levelledmobs.util.Utils
import io.github.arcaneplugins.levelledmobs.wrappers.LivingEntityWrapper
import org.bukkit.Material
import org.bukkit.inventory.EntityEquipment
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.jetbrains.annotations.Contract

/**
 * Holds various functions for interacting with items that
 * an entity has picked up previously and was record in
 * the entity's PDC
 *
 * @author stumper66
 * @since 3.14.0
 */
class PickedUpEquipment(
    val lmEntity: LivingEntityWrapper
) {
    private var ee: EntityEquipment? = null
    private var itemStack: ItemStack? = null
    private val itemsNamespace = NamespacedKeys.pickedUpItems
    private val slotMappings = mutableMapOf<Int, String>()

    fun checkEquipment(itemStack: ItemStack) {
        if (itemStack.type == Material.AIR) return
        if (lmEntity.livingEntity.equipment == null) return

        this.itemStack = itemStack
        this.ee = lmEntity.livingEntity.equipment

        val slotNumber: Int = getItemEquippedSlot()
        if (slotNumber >= 0) {
            storeItemInPDC(slotNumber)
        }

        lmEntity.free()
    }

    fun getMobPickedUpItems(): MutableList<ItemStack> {
        val results = mutableListOf<ItemStack>()
        if (!lmEntity.pdc.has(itemsNamespace)) return results

        parseExistingKey(lmEntity.pdc.get(itemsNamespace, PersistentDataType.STRING))
        if (slotMappings.isEmpty()) return results

        for (itemsHex in slotMappings.values) {
            if (itemsHex.length % 2 != 0) {
                Utils.logger.info("Unable to deserialize picked up item, invalid length: " + itemsHex.length)
                continue
            }

            val bytes: ByteArray = hexToByte(itemsHex)
            try {
                results.add(ItemStack.deserializeBytes(bytes))
            } catch (e: Exception) {
                Utils.logger.info("Unable to deserialize itemstack: " + e.message)
            }
        }

        return results
    }

    private fun storeItemInPDC(slotNumber: Int) {
        synchronized(lmEntity.livingEntity.persistentDataContainer) {
            if (lmEntity.pdc.has(itemsNamespace)) {
                parseExistingKey(lmEntity.pdc.get(itemsNamespace, PersistentDataType.STRING))
            }
            slotMappings[slotNumber] = bytesToHex(itemStack!!.serializeAsBytes())

            val hexString = getNewKeyString()
            if (hexString != null) lmEntity.pdc
                .set(itemsNamespace, PersistentDataType.STRING, hexString)
            else lmEntity.pdc.remove(itemsNamespace)
        }
    }

    private fun getNewKeyString(): String? {
        val sb = StringBuilder()
        for (slotNumber in slotMappings.keys) {
            val hex = slotMappings[slotNumber]

            if (sb.isNotEmpty()) sb.append(";")
            sb.append(slotNumber).append(":").append(hex)
        }

        return if (sb.isEmpty()) null else sb.toString()
    }

    private fun parseExistingKey(pdcKey: String?) {
        slotMappings.clear()
        if (pdcKey.isNullOrEmpty()) return

        for (key in pdcKey.split(";".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) {
            if (key.length < 3 || key.length % 2 != 0) continue
            val slotNumber = Character.getNumericValue(key[0])
            if (slotNumber < 0 || slotNumber > 6) continue

            slotMappings[slotNumber] = key.substring(2)
        }
    }

    private fun getItemEquippedSlot(): Int {
        val itmStk = itemStack!!
        val eeNN = ee!!

        if (itmStk.isSimilar(eeNN.itemInMainHand)) {
            return 0
        }
        if (itmStk.isSimilar(eeNN.itemInOffHand)) {
            return 1
        }
        if (eeNN.helmet != null && itmStk.isSimilar(eeNN.helmet)) {
            return 2
        }
        if (eeNN.chestplate != null && itmStk.isSimilar(eeNN.chestplate)) {
            return 3
        }
        if (eeNN.leggings != null && itmStk.isSimilar(eeNN.leggings)) {
            return 4
        }
        if (eeNN.boots != null && itmStk.isSimilar(eeNN.boots)) {
            return 5
        }
        if (itmStk.isSimilar(eeNN.itemInMainHand)) {
            return 6
        }

        return -1
    }

    companion object {
        // this code was originally from https://www.baeldung.com/java-byte-arrays-hex-strings
        @Contract("_ -> new")
        private fun bytesToHex(bytes: ByteArray): String {
            val hexDigits = CharArray(bytes.size * 2)

            var currentIndex = 0
            for (num in bytes) {
                hexDigits[currentIndex] = Character.forDigit((num.toInt() shr 4) and 0xF, 16)
                hexDigits[++currentIndex] = Character.forDigit((num.toInt() and 0xF), 16)
                currentIndex++
            }

            return String(hexDigits)
        }

        // this code was originally from https://www.baeldung.com/java-byte-arrays-hex-strings
        private fun hexToByte(hexString: String): ByteArray {
            val result = ByteArray(hexString.length / 2)

            var currentIndex = 0
            var i = 0
            while (i < hexString.length) {
                val firstDigit = toDigit(hexString[i])
                val secondDigit = toDigit(hexString[i + 1])
                result[currentIndex++] = ((firstDigit shl 4) + secondDigit).toByte()
                i += 2
            }

            return result
        }

        private fun toDigit(hexChar: Char): Int {
            val digit = hexChar.digitToIntOrNull(16) ?: -1
            require(digit != -1) { "Invalid Hexadecimal Character: $hexChar" }
            return digit
        }
    }
}