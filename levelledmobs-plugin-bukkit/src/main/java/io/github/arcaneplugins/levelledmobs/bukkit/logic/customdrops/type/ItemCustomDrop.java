package io.github.arcaneplugins.levelledmobs.bukkit.logic.customdrops.type;

import io.github.arcaneplugins.levelledmobs.bukkit.logic.customdrops.CustomDrop;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.customdrops.misc.EnchantTuple;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.customdrops.recipient.CustomDropRecipient;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.bukkit.Material;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;

public class ItemCustomDrop extends CustomDrop {

    private final Material material;
    private String name = null;
    private int amount = 1;
    private Integer customModelData = null;
    private boolean noMultiplier = true;
    private final Collection<ItemFlag> itemFlags = EnumSet.noneOf(ItemFlag.class);
    private int durabilityLoss = 0;
    private final Collection<EnchantTuple> enchantments = new HashSet<>();
    private int equipmentChance = 0;
    private final Collection<EquipmentSlot> allowedEquipmentSlots = EnumSet
        .noneOf(EquipmentSlot.class);
    private String lmItemsExternalType = null;
    private String lmItemsExternalAmount = null;

    public ItemCustomDrop(
        final @Nonnull Material material,
        final @Nonnull CustomDropRecipient recipient
    ) {
        super(StandardCustomDropType.ITEM.name(), recipient);
        this.material = material;
    }

    /* getters and setters */

    public @Nonnull Collection<EnchantTuple> getEnchantments() {
        return enchantments;
    }

    public @Nonnull ItemCustomDrop withEnchantments(
        final @Nonnull Collection<EnchantTuple> enchantments
    ) {
        getEnchantments().addAll(enchantments);
        return this;
    }

    public int getDurabilityLoss() {
        return durabilityLoss;
    }

    public @Nonnull ItemCustomDrop withDurabilityLoss(int durabilityLoss) {
        this.durabilityLoss = durabilityLoss;
        return this;
    }

    public boolean hasNoMultiplier() {
        return noMultiplier;
    }

    public @Nonnull ItemCustomDrop withNoMultiplier(boolean noMultiplier) {
        this.noMultiplier = noMultiplier;
        return this;
    }

    public @Nullable Integer getCustomModelData() {
        return customModelData;
    }

    public @Nonnull ItemCustomDrop withCustomModelData(
        @Nullable final Integer customModelData
    ) {
        this.customModelData = customModelData;
        return this;
    }

    public @Nonnull Collection<ItemFlag> getItemFlags() {
        return itemFlags;
    }

    public @Nonnull ItemCustomDrop withItemFlags(
        final @Nonnull Collection<ItemFlag> itemFlags
    ) {
        getItemFlags().addAll(itemFlags);
        return this;
    }

    public int getAmount() {
        return amount;
    }

    public @Nonnull ItemCustomDrop withAmount(int amount) {
        this.amount = amount;
        return this;
    }

    public String getName() {
        return name;
    }

    public @Nonnull ItemCustomDrop withName(String name) {
        this.name = name;
        return this;
    }

    public @Nonnull Material getMaterial() {
        assert material != null;
        return material;
    }

    public int getEquipmentChance() {
        return equipmentChance;
    }

    public @Nonnull ItemCustomDrop withEquipmentChance(int equipmentChance) {
        this.equipmentChance = equipmentChance;
        return this;
    }

    public @Nonnull Collection<EquipmentSlot> getAllowedEquipmentSlots() {
        return allowedEquipmentSlots;
    }

    public @Nonnull ItemCustomDrop withAllowedEquipmentSlots(
        final @Nonnull Collection<EquipmentSlot> equipmentSlots
    ) {
        getAllowedEquipmentSlots().addAll(equipmentSlots);
        return this;
    }

    public @Nullable String getLmItemsExternalType() {
        return lmItemsExternalType;
    }

    public @Nonnull ItemCustomDrop setLmItemsExternalType(String lmItemsExternalType) {
        this.lmItemsExternalType = lmItemsExternalType;
        return this;
    }

    public @Nullable String getLmItemsExternalAmount() {
        return lmItemsExternalAmount;
    }

    public @Nonnull ItemCustomDrop setLmItemsExternalAmount(String lmItemsExternalAmount) {
        this.lmItemsExternalAmount = lmItemsExternalAmount;
        return this;
    }
}
