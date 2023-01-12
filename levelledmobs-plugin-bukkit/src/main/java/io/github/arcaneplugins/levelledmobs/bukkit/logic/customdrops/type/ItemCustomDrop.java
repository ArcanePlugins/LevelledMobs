package io.github.arcaneplugins.levelledmobs.bukkit.logic.customdrops.type;

import static io.github.arcaneplugins.levelledmobs.bukkit.debug.DebugCategory.DROPS_GENERIC;

import io.github.arcaneplugins.levelledmobs.bukkit.api.data.ItemDataUtil;
import io.github.arcaneplugins.levelledmobs.bukkit.config.translations.Message;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.customdrops.CustomDrop;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.customdrops.recipient.CustomDropRecipient;
import io.github.arcaneplugins.levelledmobs.bukkit.util.EnchantTuple;
import io.github.arcaneplugins.levelledmobs.bukkit.util.Log;
import io.github.arcaneplugins.levelledmobs.bukkit.util.math.RangedInt;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

@SuppressWarnings("UnusedReturnValue")
public class ItemCustomDrop extends CustomDrop {

    private final Material material;
    private String name = null;
    private RangedInt amount = new RangedInt(1);
    private Integer customModelData = null;
    private boolean noMultiplier = true; //TODO
    private final Collection<ItemFlag> itemFlags = EnumSet.noneOf(ItemFlag.class);
    private int durabilityLoss = 0;
    private final Collection<EnchantTuple> enchantments = new HashSet<>();
    private float equipmentChance = 0;
    private final Collection<EquipmentSlot> allowedEquipmentSlots = EnumSet
        .noneOf(EquipmentSlot.class);
    private String lmItemsExternalType = null; //TODO
    private String lmItemsExternalAmount = null; //TODO
    private boolean onlyDropIfEquipped = true;

    public ItemCustomDrop(
        final @Nonnull Material material,
        final @Nonnull CustomDropRecipient recipient
    ) {
        super(StandardCustomDropType.ITEM.name(), recipient);
        this.material = material;
    }

    /* methods */

    public void attemptToApplyEquipment(
        final @Nonnull LivingEntity entity
    ) {
        if(getEquipmentChance() == 0f) return;
        if(getAllowedEquipmentSlots().isEmpty()) return;
        if(getEquipmentChance() > ThreadLocalRandom.current().nextFloat()) return;

        final EntityEquipment ee = entity.getEquipment();
        if(ee == null) return;

        for(final EquipmentSlot es : getAllowedEquipmentSlots()) {
            if(hasItemAtEquipmentSlot(ee, es)) continue;
            ee.setItem(es, toItemStack());
            ee.setDropChance(es, 0f);
        }
    }

    private boolean hasItemAtEquipmentSlot(
        final @Nonnull EntityEquipment ee,
        final @Nonnull EquipmentSlot es
    ) {
        try {
            final ItemStack is = ee.getItem(es);
            return is.getType() != Material.AIR;
        } catch(NullPointerException ex) {
            return false;
        }
    }

    public @Nonnull ItemStack toItemStack() {
        Log.debug(DROPS_GENERIC, () -> "ItemCustomDrop#toItemStack begin");
        final ItemStack is = new ItemStack(getMaterial(), getAmount().choose());
        
        if(is.getItemMeta() == null) return is;

        ItemDataUtil.setIsItemCustom(is, true);

        final ItemMeta im = is.getItemMeta();

        Log.debug(DROPS_GENERIC, () -> "Processing enchant tuples");
        for(final EnchantTuple tuple : getEnchantments()) {
            Log.debug(DROPS_GENERIC, () -> "Processing enchant tuple BEGIN: " + tuple);

            final float randomChance = ThreadLocalRandom.current().nextFloat(0, 100);
            Log.debug(DROPS_GENERIC, () -> "tupleChance=%s; randomChance=%s"
                .formatted(tuple.getChance(), randomChance));
            if(tuple.getChance() < randomChance) continue;
            Log.debug(DROPS_GENERIC, () -> "Chance passed (OK)");

            final Enchantment enchantment = tuple.getEnchantment();
            final int strength = tuple.getStrength();

            if(im.hasEnchant(enchantment) && im.getEnchantLevel(enchantment) >= strength)
                continue;

            Log.debug(DROPS_GENERIC, () -> "Enchant is not already applied to item (OK)");

            im.addEnchant(enchantment, strength, true);
            Log.debug(DROPS_GENERIC, () -> "Enchant added (OK, DONE)");
        }

        if(im instanceof Damageable dim)
            dim.setDamage(getDurabilityLoss());

        if(getName() != null)
            im.displayName(Message.formatMd(new String[]{getName()}));

        Log.debug(DROPS_GENERIC, () ->
            "Item drop displayName: " + LegacyComponentSerializer.legacySection().serialize(
                Objects.requireNonNullElse(im.displayName(), Component.empty())
            )
        );

        im.setCustomModelData(getCustomModelData());

        im.addItemFlags(getItemFlags().toArray(new ItemFlag[0]));

        is.setItemMeta(im);

        Log.debug(DROPS_GENERIC, () -> "ItemCustomDrop#toItemStack done");
        return is;
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

    public boolean requiresNoMultiplier() {
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

    public RangedInt getAmount() {
        return amount;
    }

    public @Nonnull ItemCustomDrop withAmount(RangedInt amount) {
        this.amount = amount;
        return this;
    }

    public String getName() {
        return name;
    }

    public @Nonnull ItemCustomDrop withName(@Nullable String name) {
        this.name = name;
        return this;
    }

    public @Nonnull Material getMaterial() {
        assert material != null;
        return material;
    }

    public float getEquipmentChance() {
        return equipmentChance;
    }

    //TODO this needs to be parsed
    public @Nonnull ItemCustomDrop withEquipmentChance(float equipmentChance) {
        this.equipmentChance = equipmentChance;
        return this;
    }

    public @Nonnull Collection<EquipmentSlot> getAllowedEquipmentSlots() {
        return allowedEquipmentSlots;
    }

    //TODO this needs to be parsed
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

    public boolean shouldOnlyDropIfEquipped() {
        return onlyDropIfEquipped;
    }

    public @Nonnull ItemCustomDrop withOnlyDropIfEquipped(final boolean value) {
        onlyDropIfEquipped = value;
        return this;
    }

}
