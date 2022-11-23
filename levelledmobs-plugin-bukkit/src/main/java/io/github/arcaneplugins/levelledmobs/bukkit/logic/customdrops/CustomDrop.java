package io.github.arcaneplugins.levelledmobs.bukkit.logic.customdrops;

import io.github.arcaneplugins.levelledmobs.bukkit.logic.customdrops.recipient.CustomDropRecipient;
import java.util.Collection;
import java.util.HashSet;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class CustomDrop {

    private final String type;
    private final CustomDropRecipient recipient;

    private float chance = 100.0f;
    private boolean noSpawner = true;
    private int priority = 0;
    private Integer maxDropsInGroup;
    private String dropGroupId = "0";
    private boolean chunkKillLimited = true;
    private final Collection<String> requiredPermissions = new HashSet<>();
    private final Collection<String> deathCauses = new HashSet<>();
    private Integer entityMinLevel = null;
    private Integer entityMaxLevel = null;
    private boolean overridesVanillaDrops = false;
    private String formulaCondition = null;

    public CustomDrop(
        final @Nonnull String type,
        final @Nonnull CustomDropRecipient recipient
    ) {
        this.type = type;
        this.recipient = recipient;
    }

    /* Getters and Setters */

    public @Nonnull String getType() { return type; }

    public @Nonnull CustomDropRecipient getRecipient() { return recipient; }

    public @Nonnull Collection<String> getDeathCauses() {
        return deathCauses;
    }

    public @Nonnull CustomDrop withDeathCauses(
        final @Nonnull Collection<String> deathCauses
    ) {
        getDeathCauses().addAll(deathCauses);
        return this;
    }

    public @Nullable String getFormulaCondition() {
        return formulaCondition;
    }

    public @Nonnull CustomDrop withFormulaCondition(
        final @Nullable String formulaCondition
    ) {
        this.formulaCondition = formulaCondition;
        return this;
    }

    public boolean shouldOverrideVanillaDrops() {
        return overridesVanillaDrops;
    }

    public @Nonnull CustomDrop withOverridesVanillaDrops(boolean overridesVanillaDrops) {
        this.overridesVanillaDrops = overridesVanillaDrops;
        return this;
    }

    public @Nonnull Collection<String> getRequiredPermissions() {
        return requiredPermissions;
    }

    public @Nonnull CustomDrop withRequiredPermissions(
        final Collection<String> requiredPermissions
    ) {
        getRequiredPermissions().addAll(requiredPermissions);
        return this;
    }

    public boolean isChunkKillLimited() {
        return chunkKillLimited;
    }

    public @Nonnull CustomDrop withChunkKillLimited(boolean chunkKillLimited) {
        this.chunkKillLimited = chunkKillLimited;
        return this;
    }

    public @Nonnull String getDropGroupId() {
        return dropGroupId;
    }

    public @Nonnull CustomDrop withDropGroupId(String dropGroupId) {
        this.dropGroupId = dropGroupId;
        return this;
    }

    public @Nullable Integer getMaxDropsInGroup() {
        return maxDropsInGroup;
    }

    public @Nonnull CustomDrop withMaxDropsInGroup(Integer maxDropsInGroup) {
        this.maxDropsInGroup = maxDropsInGroup;
        return this;
    }

    public int getPriority() {
        return priority;
    }

    public @Nonnull CustomDrop withPriority(int priority) {
        this.priority = priority;
        return this;
    }

    public boolean requiresNoSpawner() {
        return noSpawner;
    }

    public @Nonnull CustomDrop withNoSpawner(boolean noSpawner) {
        this.noSpawner = noSpawner;
        return this;
    }

    public float getChance() {
        return chance;
    }

    public @Nonnull CustomDrop withChance(float chance) {
        this.chance = chance;
        return this;
    }

    public @Nullable Integer getEntityMaxLevel() {
        return entityMaxLevel;
    }

    public @Nonnull CustomDrop withEntityMaxLevel(
        final @Nullable Integer entityMaxLevel
    ) {
        this.entityMaxLevel = entityMaxLevel;
        return this;
    }

    public @Nullable Integer getEntityMinLevel() {
        return entityMinLevel;
    }

    public @Nonnull CustomDrop withEntityMinLevel(
        final @Nullable Integer entityMinLevel
    ) {
        this.entityMinLevel = entityMinLevel;
        return this;
    }

}
