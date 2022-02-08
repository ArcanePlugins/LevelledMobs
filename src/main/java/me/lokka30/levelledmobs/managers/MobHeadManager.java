/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.managers;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.customdrops.CustomDropItem;
import me.lokka30.levelledmobs.misc.LivingEntityWrapper;
import me.lokka30.levelledmobs.misc.PaperUtils;
import me.lokka30.levelledmobs.misc.SpigotUtils;
import me.lokka30.levelledmobs.misc.Utils;
import me.lokka30.microlib.messaging.MessageUtils;
import me.lokka30.microlib.other.VersionUtils;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Cat;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Fox;
import org.bukkit.entity.Horse;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Llama;
import org.bukkit.entity.MushroomCow;
import org.bukkit.entity.Panda;
import org.bukkit.entity.Player;
import org.bukkit.entity.Rabbit;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.material.Colorable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

/**
 * Holds the code used for dropping mob heads and applying textures
 *
 * @author stumper66
 * @since 3.0.0
 */
public class MobHeadManager {

    public MobHeadManager(final LevelledMobs main){
        this.main = main;
    }

    final private LevelledMobs main;
    private Map<EntityType, Map<String, MobDataInfo>> mobMap;

    public void loadTextures(@NotNull final YamlConfiguration textureData){
        mobMap = new LinkedHashMap<>();

        //noinspection unchecked
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

            final EntityType entityType;
            try{
                entityType = EntityType.valueOf(mob.name.toUpperCase());
            } catch (final Exception e){
                //Utils.logger.warning("Invalid mob in textures.yml: " + mob.name);
                continue;
            }

            final Map<String, MobDataInfo> infos = this.mobMap.computeIfAbsent(
                    entityType, k -> new TreeMap<>(String.CASE_INSENSITIVE_ORDER));
            infos.put(mob.variant == null ? "" : mob.variant, mob);
        }
    }

    public ItemStack getMobHeadFromPlayerHead(final ItemStack playerHead, final LivingEntityWrapper lmEntity, @NotNull final CustomDropItem dropItem){

        final String textureCode;
        final UUID id;
        MobDataInfo mobData = null;

        if (dropItem.customPlayerHeadId == null) {
            final Material vanillaMaterial = checkForVanillaHeads(lmEntity.getLivingEntity());
            if (vanillaMaterial != Material.AIR) {
                final ItemStack newItem = new ItemStack(vanillaMaterial, playerHead.getAmount());
                final ItemMeta meta = playerHead.getItemMeta();
                if (meta != null)
                    newItem.setItemMeta(meta.clone());

                return newItem;
            }

            if (!this.mobMap.containsKey(lmEntity.getLivingEntity().getType())){
                Utils.logger.warning("Unable to get mob head for " + lmEntity.getTypeName() + ", no texture data");
                return playerHead;
            }

            final Map<String, MobDataInfo> mobDatas = this.mobMap.get(lmEntity.getEntityType());

            if (mobDatas.size() > 1){
                final MobDataInfo foundMob = getMobVariant(mobDatas, lmEntity);
                if (foundMob != null) mobData = foundMob;
            }

            if (mobData == null){
                // grab first one
                for (final MobDataInfo mobDataInfo : mobDatas.values()){
                    mobData = mobDataInfo;
                    break;
                }
            }

            if (mobData == null) return playerHead;

            textureCode = mobData.textureCode;

            try {
                id = UUID.fromString(mobData.id);
            } catch (final IllegalArgumentException e) {
                Utils.logger.warning("mob: " + lmEntity.getTypeName() + ", exception getting UUID for mob head. " + e.getMessage());
                return playerHead;
            }
        } else {
            id = dropItem.customPlayerHeadId;
            textureCode = dropItem.mobHeadTexture;
        }

        final GameProfile profile = new GameProfile(id, null);
        if (textureCode != null)
            profile.getProperties().put("textures", new Property("textures", textureCode));

        final SkullMeta meta = (SkullMeta) playerHead.getItemMeta();
        if (meta == null) return playerHead;

        final Field profileField;
        try {
            profileField = meta.getClass().getDeclaredField("profile");
            profileField.setAccessible(true);
            profileField.set(meta, profile);
        } catch (final NoSuchFieldException | IllegalArgumentException | IllegalAccessException e1) {
            Utils.logger.warning("Unable to set meta data in profile class for mob " + lmEntity.getTypeName());
        }

        String useName;

        if (!Utils.isNullOrEmpty(dropItem.customName)) {
            String killerName = "";
            final Player killerPlayer = lmEntity.getLivingEntity().getKiller();
            if (killerPlayer != null)
                killerName = VersionUtils.isRunningPaper() ?
                        PaperUtils.getPlayerDisplayName(killerPlayer) : SpigotUtils.getPlayerDisplayName(killerPlayer);

            useName = main.levelManager.replaceStringPlaceholders(dropItem.customName, lmEntity);
            useName = MessageUtils.colorizeAll(useName);

            final String displayName = lmEntity.getLivingEntity().getCustomName() == null ?
                    useName : lmEntity.getLivingEntity().getCustomName();

            useName = useName.replace("%displayname%", displayName);
            useName = useName.replace("%playername%", killerName);

        } else
            useName = "Mob Head";

        if (VersionUtils.isRunningPaper() && main.companion.useAdventure)
            PaperUtils.updateItemDisplayName(meta, useName);
        else
            SpigotUtils.updateItemDisplayName(meta, useName);

        playerHead.setItemMeta(meta);

        return playerHead;
    }

    @NotNull
    private Material checkForVanillaHeads(@NotNull final LivingEntity livingEntity){
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
    private MobDataInfo getMobVariant(final Map<String, MobDataInfo> mobDatas, @NotNull final LivingEntityWrapper lmEntity){
        final EntityType et = lmEntity.getEntityType();
        final LivingEntity livingEntity = lmEntity.getLivingEntity();

        if (livingEntity instanceof Colorable){
            final DyeColor dyeColor = ((Colorable) livingEntity).getColor();
            if (dyeColor == null) return null;
            return mobDatas.get(dyeColor.name());
        }

        // uncharged creepers already got processed
        if (et == EntityType.CREEPER)
            return mobDatas.get("Charged");

        if (et == EntityType.CAT){
            final Cat cat = (Cat) livingEntity;
            return mobDatas.get(cat.getCatType().name());
        }

        if (et == EntityType.FOX){
            if (((Fox) livingEntity).getFoxType() == Fox.Type.RED)
                return mobDatas.get("Normal");
            else
                return mobDatas.get("Snow");
        }

        if (et == EntityType.HORSE){
            final Horse horse = (Horse) livingEntity;
            return mobDatas.get(horse.getColor().name());
        }

        if (et == EntityType.LLAMA){
            final Llama llama = (Llama) livingEntity;
            return mobDatas.get(llama.getColor().name());
        }

        if (et == EntityType.MUSHROOM_COW){
            final MushroomCow mushroomCow = (MushroomCow) livingEntity;
            return mobDatas.get(
                    mushroomCow.getVariant() == MushroomCow.Variant.RED ?
                            "" : "Brown"
            );
        }

        if (et == EntityType.PANDA)
            return mobDatas.get(((Panda) livingEntity).getMainGene().name());

        if (et == EntityType.RABBIT)
            return mobDatas.get(((Rabbit) livingEntity).getRabbitType().name());

        if (et == EntityType.VILLAGER){
            final Villager.Profession profession = ((Villager) livingEntity).getProfession();
            if (profession == Villager.Profession.NONE || profession == Villager.Profession.NITWIT)
                return mobDatas.get("");
            else
                return mobDatas.get(profession.name());
        }

        if (et == EntityType.WOLF){
            return mobDatas.get(
                    lmEntity.isMobTamed() ?
                            "Tamed" : "Wild"
            );
        }

        Utils.logger.warning("Had muliple variants for " + lmEntity.getTypeName() + " in textures.yml");
        return null;
    }

    private static class MobDataInfo{
        String name;
        String variant;
        String displayName;
        String id;
        String textureCode;
    }
}
