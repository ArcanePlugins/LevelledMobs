package me.lokka30.levelledmobs.misc;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.util.Utils;
import me.lokka30.levelledmobs.wrappers.LivingEntityWrapper;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Holds various functions for interacting with items that
 * an entity has picked up previously and was record in
 * the entity's PDC
 *
 * @author stumper66
 * @since 3.14.0
 */
public class PickedUpEquipment {
    public PickedUpEquipment(final @NotNull LivingEntityWrapper lmEntity){
        this.lmEntity = lmEntity;
        this.slotMappings = new LinkedHashMap<>();
    }

    private final LivingEntityWrapper lmEntity;
    private EntityEquipment ee;
    private ItemStack itemStack;
    private final NamespacedKey itemsNamespace = LevelledMobs.getInstance().namespacedKeys.pickedUpItems;
    private final Map<Integer, String> slotMappings;

    public void checkEquipment(final @NotNull ItemStack itemStack){
        if (itemStack.getType() == Material.AIR) return;
        if (lmEntity.getLivingEntity().getEquipment() == null) return;

        this.itemStack = itemStack;
        this.ee = lmEntity.getLivingEntity().getEquipment();

        final int slotNumber = getItemEquippedSlot();
        if (slotNumber >= 0){
            storeItemInPDC(slotNumber);
        }

        lmEntity.free();
    }

    public @NotNull List<ItemStack> getMobPickedUpItems(){
        final List<ItemStack> results = new LinkedList<>();
        if (!lmEntity.getPDC().has(itemsNamespace, PersistentDataType.STRING)) return results;

        parseExistingKey(lmEntity.getPDC().get(itemsNamespace, PersistentDataType.STRING));
        if (this.slotMappings.isEmpty()) return results;

        for (final String itemsHex : this.slotMappings.values()){
            if (itemsHex.length() % 2 != 0){
                Utils.logger.info("Unable to deserialize picked up item, invalid length: " + itemsHex.length());
                continue;
            }

            final byte[] bytes = hexToByte(itemsHex);
            try{
                results.add(ItemStack.deserializeBytes(bytes));
            }
            catch (Exception e){
                Utils.logger.info("Unable to deserialize itemstack: " + e.getMessage());
            }
        }

        return results;
    }

    private void storeItemInPDC(final int slotNumber){
        synchronized (lmEntity.getLivingEntity().getPersistentDataContainer()){
            if (lmEntity.getPDC().has(itemsNamespace, PersistentDataType.STRING)){
                parseExistingKey(lmEntity.getPDC().get(itemsNamespace, PersistentDataType.STRING));
            }

            this.slotMappings.put(slotNumber, bytesToHex(this.itemStack.serializeAsBytes()));

            final String hexString = getNewKeyString();
            if (hexString != null)
                lmEntity.getPDC().set(itemsNamespace, PersistentDataType.STRING, hexString);
            else
                lmEntity.getPDC().remove(itemsNamespace);
        }
    }

    private @Nullable String getNewKeyString(){
        final StringBuilder sb = new StringBuilder();
        for (final int slotNumber : this.slotMappings.keySet()){
            final String hex = this.slotMappings.get(slotNumber);

            if (!sb.isEmpty()) sb.append(";");
            sb.append(slotNumber).append(":").append(hex);
        }

        return sb.isEmpty() ?
                null : sb.toString();
    }

    private void parseExistingKey(final @Nullable String pdcKey){
        this.slotMappings.clear();
        if (pdcKey == null || pdcKey.isEmpty()) return;

        for (final String key : pdcKey.split(";")){
            if (key.length() < 3 || key.length() % 2 != 0) continue;
            final int slotNumber = Character.getNumericValue(key.charAt(0));
            if (slotNumber < 0 || slotNumber > 6) continue;

            this.slotMappings.put(slotNumber, key.substring(2));
        }
    }

    private int getItemEquippedSlot(){
        if (itemStack.isSimilar(ee.getItemInMainHand())){
            return 0;
        }
        if (itemStack.isSimilar(ee.getItemInOffHand())){
            return 1;
        }
        if (ee.getHelmet() != null && itemStack.isSimilar(ee.getHelmet())){
            return 2;
        }
        if (ee.getChestplate() != null && itemStack.isSimilar(ee.getChestplate())){
            return 3;
        }
        if (ee.getLeggings() != null && itemStack.isSimilar(ee.getLeggings())){
            return 4;
        }
        if (ee.getBoots() != null && itemStack.isSimilar(ee.getBoots())){
            return 5;
        }
        if (itemStack.isSimilar(ee.getItemInMainHand())){
            return 6;
        }

        return -1;
    }

    // this code was originally from https://www.baeldung.com/java-byte-arrays-hex-strings
    @Contract("_ -> new")
    private static @NotNull String bytesToHex(final byte @NotNull [] bytes) {
        final char[] hexDigits = new char[bytes.length * 2];

        int currentIndex = 0;
        for (byte num : bytes) {
            hexDigits[currentIndex] = Character.forDigit((num >> 4) & 0xF, 16);
            hexDigits[++currentIndex] = Character.forDigit((num & 0xF), 16);
            currentIndex++;
        }

        return new String(hexDigits);
    }

    // this code was originally from https://www.baeldung.com/java-byte-arrays-hex-strings
    private static byte @NotNull [] hexToByte(final @NotNull String hexString) {
        byte[] result = new byte[hexString.length() / 2];

        int currentIndex = 0;
        for (int i = 0; i < hexString.length(); i+= 2){
            final int firstDigit = toDigit(hexString.charAt(i));
            final int secondDigit = toDigit(hexString.charAt(i + 1));
            result[currentIndex++] = (byte) ((firstDigit << 4) + secondDigit);
        }

        return result;
    }

    private static int toDigit(final char hexChar) {
        final int digit = Character.digit(hexChar, 16);
        if(digit == -1) {
            throw new IllegalArgumentException(
                    "Invalid Hexadecimal Character: "+ hexChar);
        }
        return digit;
    }
}
