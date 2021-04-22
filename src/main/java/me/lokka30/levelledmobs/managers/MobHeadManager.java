package me.lokka30.levelledmobs.managers;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.misc.Utils;
import me.lokka30.microlib.MessageUtils;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.material.Colorable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.*;

/**
 * TODO Describe...
 *
 * @author stumper66
 */
public class MobHeadManager {

    public MobHeadManager(final LevelledMobs main){
        this.main = main;
    }

    final private LevelledMobs main;
    private Map<EntityType, Map<String, MobDataInfo>> mobMap;

    public void loadTextures(final YamlConfiguration textureData){
        mobMap = new LinkedHashMap<>();

        final List<LinkedHashMap<String, Object>> lst = (List<LinkedHashMap<String, Object>>) textureData.getList("Mobs");
        if (lst == null) return;

        for (final LinkedHashMap<String, Object> item : lst){
            final MobDataInfo mob = new MobDataInfo();
            mob.name = (String) item.get("Name");
            if (item.containsKey("Variant"))
                mob.variant = (String) item.get("Variant");
            mob.displayName = (String) item.get("DisplayName");
            mob.id = (String) item.get("ID");
            mob.textureCode = (String) item.get("Texture");

            EntityType entityType;
            try{
                entityType = EntityType.valueOf(mob.name.toUpperCase());
            } catch (Exception e){
                //Utils.logger.warning("Invalid mob in textures.yml: " + mob.name);
                continue;
            }

            Map<String, MobDataInfo> infos = this.mobMap.computeIfAbsent(
                    entityType, k -> new TreeMap<>(String.CASE_INSENSITIVE_ORDER));
            infos.put(mob.variant == null ? "" : mob.variant, mob);
        }
    }

    public ItemStack getMobHeadFromPlayerHead(final ItemStack playerHead, final LivingEntity livingEntity){
        final Material vanillaMaterial = checkForVanillaHeads(livingEntity);
        if (vanillaMaterial != Material.AIR) {
            final ItemStack newItem = new ItemStack(vanillaMaterial, playerHead.getAmount());
            final ItemMeta meta = playerHead.getItemMeta();
            if (meta != null){
                ItemMeta newMeta = meta.clone();
                newItem.setItemMeta(meta);
            }
            return newItem;
        }

        if (!this.mobMap.containsKey(livingEntity.getType())){
            Utils.logger.warning("Unable to get mob head for " + livingEntity.getName() + ", no texture data");
            return playerHead;
        }

        final Map<String, MobDataInfo> mobDatas = this.mobMap.get(livingEntity.getType());
        MobDataInfo mobData = null;

        if (mobDatas.size() > 1){
            MobDataInfo foundMob = getMobVariant(mobDatas, livingEntity);
            if (foundMob != null) mobData = foundMob;
        }

        if (mobData == null){
            // grab first one
            for (final String variant : mobDatas.keySet()){
                mobData = mobDatas.get(variant);
                break;
            }
        }

        if (mobData == null) return playerHead;

        final String code = mobData.textureCode;
        UUID id;
        try {
            id = UUID.fromString(mobData.id);
        }
        catch (IllegalArgumentException e){
            Utils.logger.warning("mob: " + livingEntity.getName() + ", exception getting UUID for mob head. " + e.getMessage());
            return playerHead;
        }
        final GameProfile profile = new GameProfile(id, null);
        profile.getProperties().put("textures", new Property("textures", code));

        SkullMeta meta = (SkullMeta) playerHead.getItemMeta();
        if (meta == null) return playerHead;

        Field profileField;
        try {
            profileField = meta.getClass().getDeclaredField("profile");
            profileField.setAccessible(true);
            profileField.set(meta, profile);
        } catch (NoSuchFieldException | IllegalArgumentException | IllegalAccessException e1) {
            Utils.logger.warning("Unable to set meta data in profile class for mob " + livingEntity.getName());
        }

        if (!Utils.isNullOrEmpty(livingEntity.getCustomName())) {
            String name = main.messagesCfg.getString("other.mob-head-drop-name", "%mob_name%'s head");
            name = Utils.replaceEx(name, "%mob_name%", livingEntity.getCustomName());
            mobData.displayName = name;
        }

        meta.setDisplayName(MessageUtils.colorizeAll(mobData.displayName));
        playerHead.setItemMeta(meta);

        return playerHead;
    }

    @NotNull
    private Material checkForVanillaHeads(final LivingEntity livingEntity){
        switch (livingEntity.getType()){
            case ENDER_DRAGON:
                return Material.DRAGON_HEAD;
            case ZOMBIE:
                return Material.ZOMBIE_HEAD;
            case SKELETON:
                return Material.SKELETON_SKULL;
            case WITHER_SKELETON:
            case WITHER:
                return Material.WITHER_SKELETON_SKULL;
            case CREEPER:
                if (!((Creeper) livingEntity).isPowered()) return Material.CREEPER_HEAD;
            default:
                return Material.AIR;
        }
    }

    @Nullable
    private MobDataInfo getMobVariant(final Map<String, MobDataInfo> mobDatas, final LivingEntity livingEntity){
        final EntityType et = livingEntity.getType();

        if (livingEntity instanceof Colorable){
            final DyeColor dyeColor = ((Colorable) livingEntity).getColor();
            if (dyeColor == null) return null;
            return mobDatas.get(dyeColor.name());
        }

        // uncharged creepers already got processed
        if (et.equals(EntityType.CREEPER))
            return mobDatas.get("Charged");

        if (et.equals(EntityType.CAT)){
            final Cat cat = (Cat) livingEntity;
            return mobDatas.get(cat.getCatType().name());
        }

        if (et.equals(EntityType.FOX)){
            if (((Fox) livingEntity).getFoxType().equals(Fox.Type.RED))
                return mobDatas.get("Normal");
            else
                return mobDatas.get("Snow");
        }

        if (et.equals(EntityType.HORSE)){
            final Horse horse = (Horse) livingEntity;
            return mobDatas.get(horse.getColor().name());
        }

        if (et.equals(EntityType.LLAMA)){
            final Llama llama = (Llama) livingEntity;
            return mobDatas.get(llama.getColor().name());
        }

        if (et.equals(EntityType.MUSHROOM_COW)){
            final MushroomCow mushroomCow = (MushroomCow) livingEntity;
            return mobDatas.get(
                    mushroomCow.getVariant().equals(MushroomCow.Variant.RED) ?
                            "" : "Brown"
            );
        }

        if (et.equals(EntityType.PANDA))
            return mobDatas.get(((Panda) livingEntity).getMainGene().name());

        if (et.equals(EntityType.RABBIT))
            return mobDatas.get(((Rabbit) livingEntity).getRabbitType().name());

        if (et.equals(EntityType.VILLAGER)){
            final Villager.Profession profession = ((Villager) livingEntity).getProfession();
            if (profession.equals(Villager.Profession.NONE) || profession.equals(Villager.Profession.NITWIT))
                return mobDatas.get("");
            else
                return mobDatas.get(profession.name());
        }

        if (et.equals(EntityType.WOLF)){
            return mobDatas.get(
                    ((Wolf) livingEntity).isTamed() ?
                            "Tamed" : "Wild"
            );
        }

        Utils.logger.warning("Had muliple variants for " + livingEntity.getName() + " in textures.yml");
        return null;
    }

    private static class MobDataInfo{
        public String name;
        public String variant;
        public String displayName;
        public String id;
        public String textureCode;
    }
}
