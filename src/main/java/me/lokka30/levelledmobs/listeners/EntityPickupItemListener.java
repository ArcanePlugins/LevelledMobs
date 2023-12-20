package me.lokka30.levelledmobs.listeners;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.wrappers.LivingEntityWrapper;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public class EntityPickupItemListener implements Listener {
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityPickupItemEvent(final @NotNull EntityPickupItemEvent event){
        // sorry guys this is a Paper only feature
        if (!LevelledMobs.getInstance().getVerInfo().getIsRunningPaper()) return;
        if (event.getEntity() instanceof Player) return;

        final LivingEntityWrapper lmEntity = LivingEntityWrapper.getInstance(
                event.getEntity(), LevelledMobs.getInstance());

        if (!lmEntity.isLevelled() || lmEntity.getLivingEntity().getEquipment() == null){
            lmEntity.free();
            return;
        }

        storeItemInPDC(event.getItem().getItemStack(), lmEntity);
        lmEntity.free();
    }

    private void storeItemInPDC(final @NotNull ItemStack itemStack, final @NotNull LivingEntityWrapper lmEntity){
        String hexString = bytesToHex(itemStack.serializeAsBytes());
        final NamespacedKey pickedUpItems = LevelledMobs.getInstance().namespacedKeys.pickedUpItems;

        synchronized (lmEntity.getLivingEntity().getPersistentDataContainer()){
            if (lmEntity.getPDC().has(pickedUpItems)){
                final String existingHex = lmEntity.getPDC().get(pickedUpItems, PersistentDataType.STRING);
                if (existingHex != null && !existingHex.isEmpty()){
                    hexString = existingHex + ";" + hexString;
                }
            }

            lmEntity.getPDC().set(pickedUpItems, PersistentDataType.STRING, hexString);
        }
    }

    // this code was originally from https://www.baeldung.com/java-byte-arrays-hex-strings
    @Contract("_ -> new")
    private @NotNull String bytesToHex(final byte @NotNull [] bytes) {
        final char[] hexDigits = new char[bytes.length * 2];

        int currentIndex = 0;
        for (byte num : bytes) {
            hexDigits[currentIndex] = Character.forDigit((num >> 4) & 0xF, 16);
            hexDigits[++currentIndex] = Character.forDigit((num & 0xF), 16);
            currentIndex++;
        }

        return new String(hexDigits);
    }
}
