/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.listeners;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.commands.MessagesBase;
import me.lokka30.levelledmobs.commands.subcommands.SpawnerBaseClass;
import me.lokka30.levelledmobs.commands.subcommands.SpawnerSubCommand;
import me.lokka30.levelledmobs.managers.LevelManager;
import me.lokka30.levelledmobs.misc.*;
import me.lokka30.microlib.messaging.MessageUtils;
import me.lokka30.microlib.other.VersionUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;


/**
 * Listens for when a player interacts with the environment.
 * Currently only used to check for LM spawners and LM spawn eggs
 *
 * @author stumper66
 * @since 3.1.2
 */
public class PlayerInteractEventListener extends MessagesBase implements Listener {
    public PlayerInteractEventListener(final LevelledMobs main) {
        super(main);
    }

    private final HashMap<UUID, Cooldown> cooldownMap = new HashMap<>();

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    void onPlayerInteractEvent(final @NotNull PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        commandSender = event.getPlayer();
        messageLabel = "lm";

        if (event.getMaterial().name().toLowerCase().endsWith("_spawn_egg")){
            if (processLMSpawnEgg(event)) return;
        }

        if (main.companion.spawner_InfoIds.isEmpty() && main.companion.spawner_CopyIds.isEmpty()) return;
        if (event.getHand() == null || event.getHand() != EquipmentSlot.HAND) return;

        final boolean doShowInfo = main.companion.spawner_InfoIds.contains(event.getPlayer().getUniqueId());
        final boolean doCopy = main.companion.spawner_CopyIds.contains(event.getPlayer().getUniqueId());

        if (!doCopy && !doShowInfo) return;

        if (event.getClickedBlock() == null || event.getClickedBlock().getType() != Material.SPAWNER)
            return;

        final UUID uuid = event.getPlayer().getUniqueId();
        final Point point = new Point(event.getClickedBlock().getLocation());
        if (cooldownMap.containsKey(uuid)) {
            if (cooldownMap.get(uuid).doesCooldownBelongToIdentifier(point.toString())) {
                if (!cooldownMap.get(uuid).hasCooldownExpired(2)) return;
            }
            cooldownMap.remove(uuid);
        }
        cooldownMap.put(uuid, new Cooldown(System.currentTimeMillis(), point.toString()));

        final CreatureSpawner cs = (CreatureSpawner) event.getClickedBlock().getState();
        if (doShowInfo)
            showInfo(event.getPlayer(), cs);
        else if (event.getMaterial() == Material.AIR)
            copySpawner(event.getPlayer(), cs);
    }

    private boolean processLMSpawnEgg(final @NotNull PlayerInteractEvent event){
        if (!VersionUtils.isRunningPaper()) return false;
        if (event.getItem() == null) return false;
        final ItemMeta meta = event.getItem().getItemMeta();
        if (meta == null) return false;
        if (event.getClickedBlock() == null) return false;
        if (!meta.getPersistentDataContainer().has(main.namespaced_keys.spawnerEgg, PersistentDataType.INTEGER)) return false;

        // we've confirmed it is a LM spawn egg. cancel the event and spawn the mob manually
        event.setCancelled(true);
        final Location location = event.getClickedBlock().getLocation().add(0, 1, 0);
        int minLevel = 1;
        int maxLevel = 1;
        String customDropId = null;
        EntityType spawnType = EntityType.ZOMBIE;

        if (meta.getPersistentDataContainer().has(main.namespaced_keys.keySpawner_MinLevel, PersistentDataType.INTEGER)) {
            final Integer temp = meta.getPersistentDataContainer().get(main.namespaced_keys.keySpawner_MinLevel, PersistentDataType.INTEGER);
            if (temp != null) minLevel = temp;
        }
        if (meta.getPersistentDataContainer().has(main.namespaced_keys.keySpawner_MaxLevel, PersistentDataType.INTEGER)) {
            final Integer temp = meta.getPersistentDataContainer().get(main.namespaced_keys.keySpawner_MaxLevel, PersistentDataType.INTEGER);
            if (temp != null) maxLevel = temp;
        }
        if (meta.getPersistentDataContainer().has(main.namespaced_keys.keySpawner_CustomDropId, PersistentDataType.STRING))
            customDropId = meta.getPersistentDataContainer().get(main.namespaced_keys.keySpawner_CustomDropId, PersistentDataType.STRING);

        if (meta.getPersistentDataContainer().has(main.namespaced_keys.keySpawner_SpawnType, PersistentDataType.STRING)) {
            final String temp = meta.getPersistentDataContainer().get(main.namespaced_keys.keySpawner_SpawnType, PersistentDataType.STRING);
            if (temp != null){
                try {
                    spawnType = EntityType.valueOf(temp);
                } catch (final Exception ignored)
                { Utils.logger.warning("Invalid spawn type on spawner egg: " + temp); }
            }
        }

        if (event.getClickedBlock().getBlockData().getMaterial() == Material.SPAWNER){
            final SpawnerBaseClass.CustomSpawnerInfo info = new SpawnerBaseClass.CustomSpawnerInfo(main, null);
            info.minLevel = minLevel;
            info.maxLevel = maxLevel;
            info.spawnType = spawnType;
            info.customDropId = customDropId;
            if (meta.getPersistentDataContainer().has(main.namespaced_keys.keySpawner_CustomName, PersistentDataType.STRING))
                info.customName = meta.getPersistentDataContainer().get(main.namespaced_keys.keySpawner_CustomName, PersistentDataType.STRING);
            if (meta.getPersistentDataContainer().has(main.namespaced_keys.keySpawner_Lore, PersistentDataType.STRING))
                info.lore = meta.getPersistentDataContainer().get(main.namespaced_keys.keySpawner_Lore, PersistentDataType.STRING);

            convertSpawner(event, info);
            return true;
        }

        final Entity entity = location.getWorld().spawnEntity(location, spawnType, CreatureSpawnEvent.SpawnReason.SPAWNER_EGG);
        if (!(entity instanceof LivingEntity)) return true;

        final LivingEntityWrapper lmEntity = LivingEntityWrapper.getInstance((LivingEntity) entity, main);

        synchronized (LevelManager.summonedOrSpawnEggs_Lock){
            main.levelManager.summonedOrSpawnEggs.put(lmEntity.getLivingEntity(), null);
        }

        int useLevel = minLevel;
        if (minLevel != maxLevel)
            useLevel = ThreadLocalRandom.current().nextInt(minLevel, maxLevel + 1);

        main.levelInterface.applyLevelToMob(lmEntity, useLevel, true, true, new HashSet<>(Collections.singletonList(AdditionalLevelInformation.NOT_APPLICABLE)));
        synchronized (lmEntity.getLivingEntity().getPersistentDataContainer()){
            lmEntity.getPDC().set(main.namespaced_keys.wasSummoned, PersistentDataType.INTEGER, 1);
            if (!Utils.isNullOrEmpty(customDropId))
                lmEntity.getPDC().set(main.namespaced_keys.keySpawner_CustomDropId, PersistentDataType.STRING, customDropId);
        }

        lmEntity.free();
        return true;
    }

    private void convertSpawner(final @NotNull PlayerInteractEvent event, final SpawnerBaseClass.CustomSpawnerInfo info){
        if (event.getClickedBlock() == null) return;

        if (!event.getPlayer().hasPermission("levelledmobs.convert-spawner")){
            showMessage("command.levelledmobs.spawner.permission-denied");
            return;
        }

        final CreatureSpawner cs = (CreatureSpawner) event.getClickedBlock().getState();
        final PersistentDataContainer pdc = cs.getPersistentDataContainer();
        final boolean wasLMSpawner = pdc.has(main.namespaced_keys.keySpawner, PersistentDataType.INTEGER);

        pdc.set(info.main.namespaced_keys.keySpawner, PersistentDataType.INTEGER, 1);
        pdc.set(info.main.namespaced_keys.keySpawner_MinLevel, PersistentDataType.INTEGER, info.minLevel);
        pdc.set(info.main.namespaced_keys.keySpawner_MaxLevel, PersistentDataType.INTEGER, info.maxLevel);

        updateKeyString(main.namespaced_keys.keySpawner_CustomDropId, pdc, info.customDropId);
        updateKeyString(info.main.namespaced_keys.keySpawner_SpawnType, pdc, info.spawnType.toString());
        updateKeyString(info.main.namespaced_keys.keySpawner_CustomName, pdc, info.customName);

        cs.setSpawnedType(info.spawnType);
        cs.update();

        if (Utils.isNullOrEmpty(info.customName))
            info.customName = "LM Spawner";

        if (!wasLMSpawner)
            showMessage("command.levelledmobs.spawner.spawner-converted", "%spawnername%", info.customName);
        else
            showMessage("command.levelledmobs.spawner.spawner-updated", "%spawnername%", info.customName);
    }

    private void updateKeyString(final NamespacedKey key, final @NotNull PersistentDataContainer pdc, final @Nullable String value){
        if (!Utils.isNullOrEmpty(value))
            pdc.set(key, PersistentDataType.STRING, value);
        else if (pdc.has(key, PersistentDataType.STRING))
            pdc.remove(key);
    }

    private void copySpawner(final Player player, final @NotNull CreatureSpawner cs){
        final SpawnerSubCommand.CustomSpawnerInfo info = new SpawnerSubCommand.CustomSpawnerInfo(main, "lm");
        info.player = player;
        final PersistentDataContainer pdc = cs.getPersistentDataContainer();

        if (!pdc.has(main.namespaced_keys.keySpawner, PersistentDataType.INTEGER)){
            showMessage("command.levelledmobs.spawner.copy.vanilla-spawner");
            return;
        }

        if (pdc.has(main.namespaced_keys.keySpawner_CustomDropId, PersistentDataType.STRING))
            info.customDropId = pdc.get(main.namespaced_keys.keySpawner_CustomDropId, PersistentDataType.STRING);
        if (pdc.has(main.namespaced_keys.keySpawner_CustomName, PersistentDataType.STRING))
            info.customName = pdc.get(main.namespaced_keys.keySpawner_CustomName, PersistentDataType.STRING);
        if (pdc.has(main.namespaced_keys.keySpawner_MinLevel, PersistentDataType.INTEGER)) {
            final Integer minLevel = pdc.get(main.namespaced_keys.keySpawner_MinLevel, PersistentDataType.INTEGER);
            if (minLevel != null) info.minLevel = minLevel;
        }
        if (pdc.has(main.namespaced_keys.keySpawner_MaxLevel, PersistentDataType.INTEGER)) {
            final Integer maxLevel = pdc.get(main.namespaced_keys.keySpawner_MaxLevel, PersistentDataType.INTEGER);
            if (maxLevel != null) info.maxLevel = maxLevel;
        }
        if (pdc.has(main.namespaced_keys.keySpawner_Lore, PersistentDataType.STRING))
            info.lore = pdc.get(main.namespaced_keys.keySpawner_Lore, PersistentDataType.STRING);

        info.spawnType = cs.getSpawnedType();
        info.minSpawnDelay = cs.getMinSpawnDelay();
        info.maxSpawnDelay = cs.getMaxSpawnDelay();
        info.maxNearbyEntities = cs.getMaxNearbyEntities();
        info.delay = cs.getDelay();
        info.requiredPlayerRange = cs.getRequiredPlayerRange();
        info.spawnCount = cs.getSpawnCount();
        info.spawnRange = cs.getSpawnRange();

        main.levelledMobsCommand.spawnerSubCommand.generateSpawner(info);
    }

    private void showInfo(final Player player, final @NotNull CreatureSpawner cs){
        final PersistentDataContainer pdc = cs.getPersistentDataContainer();
        final StringBuilder sb = new StringBuilder();

        if (pdc.has(main.namespaced_keys.keySpawner, PersistentDataType.INTEGER)) {
            sb.append("LM Spawner");
            if (pdc.has(main.namespaced_keys.keySpawner_CustomName, PersistentDataType.STRING)){
                sb.append(": &7");
                sb.append(pdc.get(main.namespaced_keys.keySpawner_CustomName, PersistentDataType.STRING));
                sb.append("&r\n");
            }
        }
        else
            sb.append("Vanilla Spawner\n");

        addSpawnerAttributeFromPdc_Int("min level", main.namespaced_keys.keySpawner_MinLevel, pdc, sb);
        addSpawnerAttributeFromPdc_Int("max level", main.namespaced_keys.keySpawner_MaxLevel, pdc, sb);
        sb.append('\n');
        addSpawnerAttribute("delay", cs.getDelay(), sb);
        addSpawnerAttribute("max nearby entities", cs.getMaxNearbyEntities(), sb);
        addSpawnerAttribute("min spawn delay", cs.getMinSpawnDelay(), sb);
        sb.append('\n');
        addSpawnerAttribute("max spawn delay", cs.getMaxSpawnDelay(), sb);
        addSpawnerAttribute("required player range", cs.getRequiredPlayerRange(), sb);
        addSpawnerAttribute("spawn count", cs.getSpawnCount(), sb);
        sb.append('\n');
        addSpawnerAttributeFromPdc_Str(main.namespaced_keys.keySpawner_CustomDropId, pdc, sb);
        // customName
        addSpawnerAttribute("spawn type", cs.getSpawnedType(), sb);

        player.sendMessage(MessageUtils.colorizeAll(sb.toString()));
    }

    private void addSpawnerAttributeFromPdc_Int(final String name, final NamespacedKey key, final @NotNull PersistentDataContainer pdc, final StringBuilder sb){
        if (!pdc.has(key, PersistentDataType.INTEGER)) return;

        if (!sb.substring(sb.length() - 1).equals("\n"))
            sb.append(", ");

        sb.append("&7");
        sb.append(name);
        sb.append(": &b");
        sb.append(pdc.get(key, PersistentDataType.INTEGER));
        sb.append("&r");
    }

    private void addSpawnerAttributeFromPdc_Str(final NamespacedKey key, final @NotNull PersistentDataContainer pdc, final StringBuilder sb){
        if (!pdc.has(key, PersistentDataType.STRING)) return;

        if (!sb.substring(sb.length() - 1).equals("\n"))
            sb.append(", ");

        sb.append("&7custom drop id: &b");
        sb.append(pdc.get(key, PersistentDataType.STRING));
        sb.append("&r");
    }

    private void addSpawnerAttribute(final String name, final Object value, final @NotNull StringBuilder sb){
        if (!sb.substring(sb.length() - 1).equals("\n"))
            sb.append(", ");
        sb.append("&7");
        sb.append(name);
        sb.append(": &b");
        sb.append(value);
        sb.append("&r");
    }
}
