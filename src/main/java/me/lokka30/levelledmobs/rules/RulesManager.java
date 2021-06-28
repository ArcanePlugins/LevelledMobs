package me.lokka30.levelledmobs.rules;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.LivingEntityInterface;
import me.lokka30.levelledmobs.managers.ExternalCompatibilityManager;
import me.lokka30.levelledmobs.misc.*;
import me.lokka30.levelledmobs.rules.strategies.LevellingStrategy;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Manages all rules that are parsed from rules.yml and applied to various
 * defined mobs
 *
 * @author stumper66
 */
public class RulesManager {
    public RulesManager(final LevelledMobs main) {
        this.main = main;
        this.rulesInEffect = new TreeMap<>();
        this.levelNumbersWithBiasMapCache = new TreeMap<>();
        this.biomeGroupMappings = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        buildBiomeGroupMappings();
    }

    private final LevelledMobs main;
    private final Map<String, LevelNumbersWithBias> levelNumbersWithBiasMapCache;
    @NotNull
    public final SortedMap<Integer, List<RuleInfo>> rulesInEffect;
    @NotNull
    public final Map<String, List<String>> biomeGroupMappings;

    public boolean getRule_IsWorldAllowedInAnyRule(final World world){
        if (world == null) return false;
        boolean result = false;

        for (final RuleInfo ruleInfo : main.rulesParsingManager.getAllRules()){
            if (!ruleInfo.ruleIsEnabled) continue;
            if (ruleInfo.conditions_Worlds != null && ruleInfo.conditions_Worlds.isEnabledInList(world.getName(), null)){
                result = true;
                break;
            }
        }

        return result;
    }

    @Nullable
    public String getRule_NBT_Data(final LivingEntityWrapper lmEntity){
        String nbtData = null;

        for (final RuleInfo ruleInfo : lmEntity.getApplicableRules()){
            if (ruleInfo.mobNBT_Data != null)
                nbtData = ruleInfo.mobNBT_Data;
        }

        return nbtData;
    }

    public double getRule_SunlightBurnIntensity(final LivingEntityWrapper lmEntity){
        double result = 0.0;

        for (final RuleInfo ruleInfo : lmEntity.getApplicableRules()){
            if (ruleInfo.sunlightBurnAmount != null)
                result = ruleInfo.sunlightBurnAmount;
        }

        return result;
    }

    @Nullable
    public LevelNumbersWithBias getRule_LowerMobLevelBiasFactor(@NotNull final LivingEntityWrapper lmEntity, final int minLevel, final int maxLevel){
        Integer lowerMobLevelBiasFactor = null;

        for (final RuleInfo ruleInfo : lmEntity.getApplicableRules()){
            if (ruleInfo.lowerMobLevelBiasFactor != null) lowerMobLevelBiasFactor = ruleInfo.lowerMobLevelBiasFactor;
        }

        if (lowerMobLevelBiasFactor == null) return null;

        final String checkName = String.format("%s-%s-%s", minLevel, maxLevel, lowerMobLevelBiasFactor);
        if (this.levelNumbersWithBiasMapCache.containsKey(checkName))
            return this.levelNumbersWithBiasMapCache.get(checkName);

        LevelNumbersWithBias levelNumbersWithBias = new LevelNumbersWithBias(minLevel, maxLevel, lowerMobLevelBiasFactor);
        this.levelNumbersWithBiasMapCache.put(checkName, levelNumbersWithBias);
        return levelNumbersWithBias;
    }

    public int getRule_MaxRandomVariance(@NotNull final LivingEntityWrapper lmEntity){
        int result = 0;

        for (final RuleInfo ruleInfo : lmEntity.getApplicableRules()){
            if (ruleInfo.maxRandomVariance != null) result = ruleInfo.maxRandomVariance;
        }

        return result;
    }

    public boolean getRule_CheckIfNoDropMultiplierEntitiy(@NotNull final LivingEntityWrapper lmEntity){
        CachedModalList<String> entitiesList = null;

        for (final RuleInfo ruleInfo : lmEntity.getApplicableRules()){
            if (ruleInfo.conditions_NoDropEntities != null)
                entitiesList = ruleInfo.conditions_NoDropEntities;
        }

        return entitiesList != null && entitiesList.isEnabledInList(lmEntity.getNameIfBaby(), lmEntity);
    }

    @NotNull
    public CustomDropsRuleSet getRule_UseCustomDropsForMob(@NotNull final LivingEntityWrapper lmEntity){
        final CustomDropsRuleSet dropRules = new CustomDropsRuleSet();
        for (final RuleInfo ruleInfo : lmEntity.getApplicableRules()){
            if (ruleInfo.customDrops_UseForMobs != null) dropRules.useDrops = ruleInfo.customDrops_UseForMobs;
            if (ruleInfo.customDrops_UseOverride != null) dropRules.override = ruleInfo.customDrops_UseOverride;
            if (ruleInfo.customDrop_DropTableId != null) dropRules.useDropTableId = ruleInfo.customDrop_DropTableId;
        }

        return dropRules;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean getRule_IsMobAllowedInEntityOverride(@NotNull final LivingEntityInterface lmInterface){
        // check if it should be denied thru the entity override list
        boolean babyMobsInheritAdultSetting = true; // default
        CachedModalList<String> allowedEntitiesList = null;
        for (final RuleInfo ruleInfo : lmInterface.getApplicableRules()){
            if (ruleInfo.allowedEntities != null)
                allowedEntitiesList = ruleInfo.allowedEntities;
            if (ruleInfo.babyMobsInheritAdultSetting != null)
                babyMobsInheritAdultSetting = ruleInfo.babyMobsInheritAdultSetting;
        }

        if (lmInterface instanceof LivingEntityWrapper) {
            LivingEntityWrapper lmEntity = (LivingEntityWrapper) lmInterface;
            return (
                    allowedEntitiesList == null ||
                            !babyMobsInheritAdultSetting && lmEntity.isBabyMob() && Utils.isLivingEntityInModalList(allowedEntitiesList, lmEntity, true)) ||
                    Utils.isLivingEntityInModalList(allowedEntitiesList, lmEntity, babyMobsInheritAdultSetting);
        } else {
            return (
                    allowedEntitiesList == null || allowedEntitiesList.isEnabledInList(lmInterface.getTypeName(), null)
            );
        }
    }

    @Nullable
    public FineTuningAttributes getFineTuningAttributes(@NotNull final LivingEntityWrapper lmEntity){
        FineTuningAttributes allMobAttribs = null;
        FineTuningAttributes thisMobAttribs = null;

        for (final RuleInfo ruleInfo : lmEntity.getApplicableRules()){
            if (ruleInfo.allMobMultipliers != null) {
                if (allMobAttribs == null)
                    allMobAttribs = ruleInfo.allMobMultipliers.cloneItem();
                else
                    allMobAttribs.mergeAttributes(ruleInfo.allMobMultipliers);
            }

            if (ruleInfo.specificMobMultipliers != null && ruleInfo.specificMobMultipliers.containsKey(lmEntity.getNameIfBaby())){
                final FineTuningAttributes tempAttribs = ruleInfo.specificMobMultipliers.get(lmEntity.getNameIfBaby());
                if (thisMobAttribs == null)
                    thisMobAttribs = tempAttribs.cloneItem();
                else
                    thisMobAttribs.mergeAttributes(tempAttribs);
            }
        }

        if (allMobAttribs != null) {
            allMobAttribs.mergeAttributes(thisMobAttribs);
            return allMobAttribs;
        } else
            return thisMobAttribs;
    }

    @NotNull
    public Map<ExternalCompatibilityManager.ExternalCompatibility, Boolean> getRule_ExternalCompatibility(@NotNull final LivingEntityWrapper lmEntity){
        final Map<ExternalCompatibilityManager.ExternalCompatibility, Boolean> result = new TreeMap<>();

        for (final RuleInfo ruleInfo : lmEntity.getApplicableRules()){
            if (ruleInfo.enabledExtCompats != null) {
                for (final ExternalCompatibilityManager.ExternalCompatibility compatibility : ruleInfo.enabledExtCompats.keySet())
                    result.put(compatibility, ruleInfo.enabledExtCompats.get(compatibility));
            }
        }

        return result;
    }

    public int getRule_CreeperMaxBlastRadius(@NotNull final LivingEntityWrapper lmEntity){
        int maxBlast = 5;
        for (final RuleInfo ruleInfo : lmEntity.getApplicableRules()){
            if (ruleInfo.creeperMaxDamageRadius != null) maxBlast = ruleInfo.creeperMaxDamageRadius;
        }

        return maxBlast;
    }

    @Nullable
    public LevellingStrategy getRule_LevellingStrategy(@NotNull final LivingEntityWrapper lmEntity){
        LevellingStrategy levellingStrategy = null;

        for (final RuleInfo ruleInfo : lmEntity.getApplicableRules()){
            if (ruleInfo.useRandomLevelling != null && ruleInfo.useRandomLevelling) {
                // specifying random in strategies will nullify any levelling systems to that point
                levellingStrategy = null;
            } else if (ruleInfo.levellingStrategy != null) {
                if (levellingStrategy != null && levellingStrategy.getClass().equals(ruleInfo.levellingStrategy.getClass()))
                    levellingStrategy.mergeRule(ruleInfo.levellingStrategy);
                else
                    levellingStrategy = ruleInfo.levellingStrategy.cloneItem();
            }
        }

        return levellingStrategy;
    }

    public boolean getRule_MobLevelInheritance(@NotNull final LivingEntityWrapper lmEntity){
        boolean result = true;
        for (final RuleInfo ruleInfo : lmEntity.getApplicableRules()){
            if (ruleInfo.mobLevelInheritance != null) result = ruleInfo.mobLevelInheritance;
        }

        return result;
    }

    public MobCustomNameStatusEnum getRule_MobCustomNameStatus(@NotNull final LivingEntityWrapper lmEntity){
        MobCustomNameStatusEnum result = MobCustomNameStatusEnum.NOT_SPECIFIED;

        for (final RuleInfo ruleInfo : lmEntity.getApplicableRules())
            if (ruleInfo.conditions_MobCustomnameStatus != MobCustomNameStatusEnum.NOT_SPECIFIED) result = ruleInfo.conditions_MobCustomnameStatus;

        return result;
    }

    public MobTamedStatusEnum getRule_MobTamedStatus(@NotNull final LivingEntityWrapper lmEntity){
        MobTamedStatusEnum result = MobTamedStatusEnum.NOT_SPECIFIED;

        for (final RuleInfo ruleInfo : lmEntity.getApplicableRules())
            if (ruleInfo.conditions_MobTamedStatus != MobTamedStatusEnum.NOT_SPECIFIED) result = ruleInfo.conditions_MobTamedStatus;

        return result;
    }

    public int getRule_MobMinLevel(@NotNull final LivingEntityInterface lmInterface){
        int minLevel = 1;

        for (final RuleInfo ruleInfo : lmInterface.getApplicableRules()) {
            if (ruleInfo.restrictions_MinLevel != null) minLevel = ruleInfo.restrictions_MinLevel;
        }

        return minLevel;
    }

    public int getRule_MobMaxLevel(@NotNull final LivingEntityInterface lmInterface){
        int maxLevel = 10;

        for (final RuleInfo ruleInfo : lmInterface.getApplicableRules()) {
            if (ruleInfo.restrictions_MaxLevel != null) maxLevel = ruleInfo.restrictions_MaxLevel;
        }

        return maxLevel;
    }

    @NotNull
    public String getRule_Nametag(@NotNull final LivingEntityWrapper lmEntity){
        String nametag = "";
        for (final RuleInfo ruleInfo : lmEntity.getApplicableRules()) {
            if (!Utils.isNullOrEmpty(ruleInfo.nametag)) {
                nametag = "disabled".equalsIgnoreCase(ruleInfo.nametag) ?
                        "" : ruleInfo.nametag;
            }
        }

        return nametag;
    }

    @NotNull
    public String getRule_Nametag_CreatureDeath(@NotNull final LivingEntityWrapper lmEntity){
        String nametag = "";
        for (final RuleInfo ruleInfo : lmEntity.getApplicableRules()) {
            if (!Utils.isNullOrEmpty(ruleInfo.nametag_CreatureDeath)) nametag = ruleInfo.nametag_CreatureDeath;
        }

        return nametag;
    }

    public boolean getRule_CreatureNametagAlwaysVisible(@NotNull final LivingEntityWrapper lmEntity){
        boolean creatureNametagAlwaysVisible = false;

        for (final RuleInfo ruleInfo : lmEntity.getApplicableRules()){
            if (ruleInfo.CreatureNametagAlwaysVisible != null)
                creatureNametagAlwaysVisible = ruleInfo.CreatureNametagAlwaysVisible;
        }

        return creatureNametagAlwaysVisible;
    }

    @Nullable
    public String getRule_TieredPlaceholder(@NotNull final LivingEntityWrapper lmEntity){
        List<TieredColoringInfo> coloringInfo = null;
        String tieredText = null;

        for (final RuleInfo ruleInfo : lmEntity.getApplicableRules()){
            if (ruleInfo.tieredColoringInfos != null) coloringInfo = ruleInfo.tieredColoringInfos;
        }

        if (coloringInfo == null) return  null;

        final int mobLevel = lmEntity.getMobLevel();
        for (TieredColoringInfo info : coloringInfo){
            if (info.isDefault) tieredText = info.text;
            if (mobLevel >= info.minLevel && mobLevel <= info.maxLevel){
                tieredText = info.text;
                break;
            }
        }

        return tieredText;
    }

    @Nullable
    public String getRule_EntityOverriddenName(@NotNull final LivingEntityWrapper lmEntity){
        List<String> overridenNames = null;

        if (lmEntity.hasOverridenEntityName())
            return lmEntity.getOverridenEntityName();

        for (final RuleInfo ruleInfo : lmEntity.getApplicableRules()){
            if (ruleInfo.entityNameOverrides.containsKey(lmEntity.getNameIfBaby()))
                overridenNames = ruleInfo.entityNameOverrides.get(lmEntity.getNameIfBaby());
            else if (ruleInfo.entityNameOverrides.containsKey("all_entities"))
                overridenNames = ruleInfo.entityNameOverrides.get("all_entities");
        }

        if (overridenNames == null || overridenNames.isEmpty())
            return null;
        else if (overridenNames.size() > 1) {
            Collections.shuffle(overridenNames);
            // set a PDC key with the name otherwise the name will constantly change
            lmEntity.setOverridenEntityName(overridenNames.get(0));
        }

        final String entityName = Utils.capitalize(lmEntity.getNameIfBaby().replaceAll("_", " "));
        String result = overridenNames.get(0);
        result = result.replace("%entity-name%", entityName);
        result = result.replace("%displayname%", (lmEntity.getLivingEntity().getCustomName() == null ? entityName : lmEntity.getLivingEntity().getCustomName()));
        return result;
    }

    @NotNull
    public List<RuleInfo> getApplicableRules(final LivingEntityInterface lmInterface){
        final List<RuleInfo> applicableRules = new LinkedList<>();

        for (final int rulePriority : rulesInEffect.keySet()) {
            final List<RuleInfo> rules = rulesInEffect.get(rulePriority);
            for (final RuleInfo ruleInfo : rules) {

                if (!ruleInfo.ruleIsEnabled) continue;

                if (lmInterface instanceof LivingEntityWrapper && !isRuleApplicable_Entity((LivingEntityWrapper) lmInterface, ruleInfo))
                    continue;

                if (!isRuleApplicable_Interface(lmInterface, ruleInfo))
                    continue;

                if (ruleInfo.stopProcessingRules != null && ruleInfo.stopProcessingRules) {
                    Utils.debugLog(main, DebugType.DENIED_RULE_STOP_PROCESSING, String.format("&b%s&7, mob: &b%s&7, rule count: &b%s",
                            ruleInfo.getRuleName(), lmInterface.getEntityType().name(), applicableRules.size()));
                    break;
                }

                applicableRules.add(ruleInfo);
            }
        }

        boolean hasWorldListSpecified = false;
        for (final RuleInfo ri : applicableRules) {
            if (ri.conditions_Worlds != null && (!ri.conditions_Worlds.isEmpty() || ri.conditions_Worlds.allowAll)){
                hasWorldListSpecified = true;
                break;
            }
        }

        return hasWorldListSpecified ?
                applicableRules : Collections.emptyList();
    }

    private boolean isRuleApplicable_Entity(final LivingEntityWrapper lmEntity, @NotNull final RuleInfo ri){
        if (ri.conditions_MinLevel != null && (!lmEntity.isLevelled() || ri.conditions_MinLevel < lmEntity.getMobLevel())) {
            Utils.debugLog(main, DebugType.DENIED_RULE_MAXLEVEL, String.format("&b%s&7, mob: &b%s&7, mob lvl: &b%s&7, rule minlvl: &b%s&7",
                    ri.getRuleName(), lmEntity.getTypeName(), lmEntity.getMobLevel(), ri.conditions_MinLevel));
            return false;
        }
        if (ri.conditions_MaxLevel != null && (!lmEntity.isLevelled() || ri.conditions_MaxLevel > lmEntity.getMobLevel())) {
            Utils.debugLog(main, DebugType.DENIED_RULE_MAXLEVEL, String.format("&b%s&7, mob: &b%s&7, mob lvl: &b%s&7, rule maxlvl: &b%s&7",
                    ri.getRuleName(), lmEntity.getTypeName(), lmEntity.getMobLevel(), ri.conditions_MaxLevel));
            return false;
        }

        if (ri.conditions_CustomNames != null && lmEntity.getLivingEntity().getCustomName() != null &&
                !ri.conditions_CustomNames.isEnabledInList(lmEntity.getLivingEntity().getCustomName(), lmEntity)) {
            Utils.debugLog(main, DebugType.DENIED_RULE_CUSTOM_NAME, String.format("&b%s&7, mob: &b%s&7, name: &b%s&7",
                    ri.getRuleName(), lmEntity.getTypeName(), lmEntity.getLivingEntity().getCustomName()));
            return false;
        }

        if (ri.conditions_SpawnReasons != null && !ri.conditions_SpawnReasons.isEnabledInList(lmEntity.getSpawnReason(), lmEntity)){
            Utils.debugLog(main, DebugType.DENIED_RULE_SPAWN_REASON, String.format("&b%s&7, mob: &b%s&7, spawn reason: &b%s&7",
                    ri.getRuleName(), lmEntity.getTypeName(), lmEntity.getSpawnReason()));
            return false;
        }

        if (lmEntity.isMobOfExternalType() && ri.conditions_ApplyPlugins != null && !ri.conditions_ApplyPlugins.isEnabledInList(lmEntity.getTypeName(), lmEntity)){
            Utils.debugLog(main, DebugType.DENIED_RULE_PLUGIN_COMPAT, String.format("&b%s&7, mob: &b%s&7, mob plugin: &b%s&7",
                    ri.getRuleName(), lmEntity.getTypeName(), lmEntity.getMobExternalType()));
            return false;
        }

        if (ri.conditions_MM_Names != null && lmEntity.getMobExternalType().equals(ExternalCompatibilityManager.ExternalCompatibility.MYTHIC_MOBS)){
            String mm_Name = lmEntity.mythicMobInternalName;
            if (mm_Name == null) mm_Name = "";

            //noinspection RedundantIfStatement
            if (!ri.conditions_MM_Names.isEnabledInList(mm_Name, lmEntity)) return false;
        }

        return true;
    }

    private boolean isRuleApplicable_Interface(final LivingEntityInterface lmInterface, final RuleInfo ri){

        if (lmInterface instanceof LivingEntityWrapper) {
            if (ri.conditions_Entities != null && !Utils.isLivingEntityInModalList(ri.conditions_Entities, (LivingEntityWrapper) lmInterface)) {
                Utils.debugLog(main, DebugType.DENIED_RULE_ENTITIES_LIST, String.format("&b%s&7, mob: &b%s&7", ri.getRuleName(), lmInterface.getEntityType().name()));
                return false;
            }
        } else {
            // can't check groups if not a living entity wrapper
            if (ri.conditions_Entities != null && !ri.conditions_Entities.isEnabledInList(lmInterface.getTypeName(), null)) {
                Utils.debugLog(main, DebugType.DENIED_RULE_ENTITIES_LIST, String.format("&b%s&7, mob: &b%s&7", ri.getRuleName(), lmInterface.getEntityType().name()));
                return false;
            }
        }

        if (ri.conditions_Worlds != null && !ri.conditions_Worlds.isEnabledInList(lmInterface.getWorld().getName(), null)) {
            Utils.debugLog(main, DebugType.DENIED_RULE_WORLD_LIST, String.format("&b%s&7, mob: &b%s&7, mob world: &b%s&7",
                    ri.getRuleName(), lmInterface.getEntityType().name(), lmInterface.getWorld().getName()));
            return false;
        }

        if (ri.conditions_Biomes != null && !Utils.isBiomeInModalList(ri.conditions_Biomes, lmInterface.getLocation().getBlock().getBiome(), main.rulesManager)) {
            Utils.debugLog(main, DebugType.DENIED_RULE_BIOME_LIST, String.format("&b%s&7, mob: &b%s&7, mob biome: &b%s&7",
                    ri.getRuleName(), lmInterface.getEntityType().name(), lmInterface.getLocation().getBlock().getBiome().name()));
            return false;
        }

        if (ri.conditions_WGRegions != null){
            boolean isInList = false;
            final List<String> wgRegions = ExternalCompatibilityManager.getWGRegionsAtLocation(lmInterface);
            if (wgRegions != null) {
                for (final String regionName : wgRegions) {
                    if (ri.conditions_WGRegions.isEnabledInList(regionName, null)) {
                        isInList = true;
                        break;
                    }
                }
            }
            if (!isInList){
                Utils.debugLog(main, DebugType.DENIED_RULE_WG_REGION, String.format("&b%s&7, mob: &b%s&7, wg_regions: &b%s&7",
                        ri.getRuleName(), lmInterface.getEntityType().name(), wgRegions));
                return false;
            }
        }

        if (ri.conditions_ApplyAboveY != null && lmInterface.getLocation().getBlockY() < ri.conditions_ApplyAboveY){
            Utils.debugLog(main, DebugType.DENIED_RULE_Y_LEVEL, String.format("&b%s&7, mob: &b%s&7, y-level: &b%s&7, max-y: &b%s&7",
                    ri.getRuleName(), lmInterface.getEntityType().name(), lmInterface.getLocation().getBlockY(), ri.conditions_ApplyAboveY));
            return false;
        }

        if (ri.conditions_ApplyBelowY != null && lmInterface.getLocation().getBlockY() > ri.conditions_ApplyBelowY){
            Utils.debugLog(main, DebugType.DENIED_RULE_Y_LEVEL, String.format("&b%s&7, mob: &b%s&7, y-level: &b%s&7, min-y: &b%s&7",
                    ri.getRuleName(), lmInterface.getEntityType().name(), lmInterface.getLocation().getBlockY(), ri.conditions_ApplyBelowY));
            return false;
        }

        if (ri.conditions_Chance != null && ri.conditions_Chance < 1.0){
            final double chanceRole = (double) ThreadLocalRandom.current().nextInt(0, 100001) * 0.00001;
            if (chanceRole < ri.conditions_Chance){
                Utils.debugLog(main, DebugType.DENIED_RULE_CHANCE, String.format("&b%s&7, mob: &b%s&7, chance: &b%s&7, chance role: &b%s&7",
                        ri.getRuleName(), lmInterface.getTypeName(), ri.conditions_Chance, chanceRole));
                return false;
            }
        }

        return true;
    }

    private void buildBiomeGroupMappings(){
        this.biomeGroupMappings.put("SNOWY_BIOMES", Arrays.asList("SNOWY_TUNDRA", "ICE_SPIKES", "SNOWY_TAIGA", "SNOWY_TAIGA_MOUNTAINS",
                "SNOWY_TAIGA_HILLS", "FROZEN_RIVER", "SNOWY_BEACH", "SNOWY_MOUNTAINS"));

        this.biomeGroupMappings.put("COLD_BIOMES", Arrays.asList("MOUNTAINS", "GRAVELLY_MOUNTAINS", "MODIFIED_GRAVELLY_MOUNTAINS",
                "WOODED_MOUNTAINS", "TAIGA", "TAIGA_MOUNTAINS", "TAIGA_HILLS", "GIANT_TREE_TAIGA", "GIANT_TREE_TAIGA_HILLS",
                "GIANT_SPRUCE_TAIGA", "GIANT_SPRUCE_TAIGA_HILLS", "STONE_SHORE"));

        this.biomeGroupMappings.put("TEMPERATE_BIOMES", Arrays.asList("PLAINS", "SUNFLOWER_PLAINS", "FOREST", "FLOWER_FOREST",
                "BIRCH_FOREST", "BIRCH_FOREST_HILLS", "TALL_BIRCH_FOREST", "TALL_BIRCH_HILLS", "DARK_FOREST", "DARK_FOREST_HILLS",
                "SWAMP", "SWAMP_HILLS", "JUNGLE", "MODIFIED_JUNGLE", "JUNGLE_HILLS", "MODIFIED_JUNGLE_EDGE", "BAMBOO_JUNGLE",
                "BAMBOO_JUNGLE_HILLS", "RIVER", "BEACH", "MUSHROOM_FIELDS", "MUSHROOM_FIELD_SHORE", "WOODED_HILLS"));

        this.biomeGroupMappings.put("DRY_BIOMES", Arrays.asList("DESERT", "DESERT_LAKES", "DESERT_HILLS", "SAVANNA",
                "SHATTERED_SAVANNA", "SHATTERED_SAVANNA_PLATEAU", "BADLANDS", "ERODED_BADLANDS", "WOODED_BADLANDS_PLATEAU",
                "BADLANDS_PLATEAU", "SAVANNA_PLATEAU", "MODIFIED_BADLANDS_PLATEAU", "MODIFIED_WOODED_BADLANDS_PLATEAU", "MODIFIED_SAVANNA_PLATEAU"));

        this.biomeGroupMappings.put("OCEAN_BIOMES", Arrays.asList("WARM_OCEAN", "DEEP_WARM_OCEAN", "LUKEWARM_OCEAN", "DEEP_LUKEWARM_OCEAN", "OCEAN",
                "DEEP_OCEAN", "COLD_OCEAN", "DEEP_COLD_OCEAN", "FROZEN_OCEAN", "DEEP_FROZEN_OCEAN"));

        this.biomeGroupMappings.put("NETHER_BIOMES", Arrays.asList("NETHER_WASTES", "CRIMSON_FOREST", "WARPED_FOREST", "SOUL_SAND_VALLEY", "BASALT_DELTAS"));

        this.biomeGroupMappings.put("END_BIOMES", Arrays.asList("THE_END", "SMALL_END_ISLANDS", "END_MIDLANDS", "END_HIGHLANDS", "END_BARRENS"));
    }
}
