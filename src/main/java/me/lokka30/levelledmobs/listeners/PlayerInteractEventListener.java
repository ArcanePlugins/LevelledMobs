/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.listeners;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.commands.subcommands.SpawnerSubCommand;
import me.lokka30.levelledmobs.misc.Cooldown;
import me.lokka30.levelledmobs.misc.Point;
import me.lokka30.levelledmobs.misc.Utils;
import me.lokka30.microlib.MessageUtils;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class PlayerInteractEventListener implements Listener {

    final private LevelledMobs main;

    public PlayerInteractEventListener(final LevelledMobs main) {
        this.main = main;
    }

    private final HashMap<UUID, Cooldown> cooldownMap = new HashMap<>();

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    void onPlayerInteractEvent(final PlayerInteractEvent event) {
        if (main.companion.spawner_InfoIds.isEmpty() && main.companion.spawner_CopyIds.isEmpty()) return;
        if (event.getHand() == null || !event.getHand().equals(EquipmentSlot.HAND)) return;
        if (!event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) return;

        final boolean doShowInfo = main.companion.spawner_InfoIds.contains(event.getPlayer().getUniqueId());
        final boolean doCopy = main.companion.spawner_CopyIds.contains(event.getPlayer().getUniqueId());

        if (!doCopy && !doShowInfo) return;

        if (event.getClickedBlock() == null || !event.getClickedBlock().getType().equals(Material.SPAWNER))
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
        else if (event.getMaterial().equals(Material.AIR))
            copySpawner(event.getPlayer(), cs);
    }

    private void copySpawner(final Player player, final CreatureSpawner cs){
        final SpawnerSubCommand.CustomSpawnerInfo info = new SpawnerSubCommand.CustomSpawnerInfo(main, player, "lm");
        final PersistentDataContainer pdc = cs.getPersistentDataContainer();

        if (!pdc.has(main.blockPlaceListener.keySpawner, PersistentDataType.INTEGER)){
            List<String> messages = main.messagesCfg.getStringList("command.levelledmobs.spawner.copy.vanilla-spawner");
            messages = Utils.replaceAllInList(messages, "%prefix%", main.configUtils.getPrefix());
            messages = Utils.replaceAllInList(messages, "%label%", "lm");
            messages = Utils.colorizeAllInList(messages);
            messages.forEach(player::sendMessage);
            return;
        }

        if (pdc.has(main.blockPlaceListener.keySpawner_CustomDropId, PersistentDataType.STRING))
            info.customDropId = pdc.get(main.blockPlaceListener.keySpawner_CustomDropId, PersistentDataType.STRING);
        if (pdc.has(main.blockPlaceListener.keySpawner_CustomName, PersistentDataType.STRING))
            info.customName = pdc.get(main.blockPlaceListener.keySpawner_CustomName, PersistentDataType.STRING);
        if (pdc.has(main.blockPlaceListener.keySpawner_MinLevel, PersistentDataType.INTEGER)) {
            final Integer minLevel = pdc.get(main.blockPlaceListener.keySpawner_MinLevel, PersistentDataType.INTEGER);
            if (minLevel != null) info.minLevel = minLevel;
        }
        if (pdc.has(main.blockPlaceListener.keySpawner_MaxLevel, PersistentDataType.INTEGER)) {
            final Integer maxLevel = pdc.get(main.blockPlaceListener.keySpawner_MaxLevel, PersistentDataType.INTEGER);
            if (maxLevel != null) info.maxLevel = maxLevel;
        }
        if (pdc.has(main.blockPlaceListener.keySpawner_Lore, PersistentDataType.STRING))
            info.lore = pdc.get(main.blockPlaceListener.keySpawner_Lore, PersistentDataType.STRING);

        info.spawnType = cs.getSpawnedType();
        info.minSpawnDelay = cs.getMinSpawnDelay();
        info.maxSpawnDelay = cs.getMaxSpawnDelay();
        info.maxNearbyEntities = cs.getMaxNearbyEntities();
        info.delay = cs.getDelay();
        info.requiredPlayerRange = cs.getRequiredPlayerRange();
        info.spawnCount = cs.getSpawnCount();
        info.spawnRange = cs.getSpawnRange();

        SpawnerSubCommand.generateSpawner(info);
    }

    private void showInfo(final Player player, final CreatureSpawner cs){
        final PersistentDataContainer pdc = cs.getPersistentDataContainer();
        final StringBuilder sb = new StringBuilder();

        if (pdc.has(main.blockPlaceListener.keySpawner, PersistentDataType.INTEGER)) {
            sb.append("LM Spawner");
            if (pdc.has(main.blockPlaceListener.keySpawner_CustomName, PersistentDataType.STRING)){
                sb.append(": &7");
                sb.append(pdc.get(main.blockPlaceListener.keySpawner_CustomName, PersistentDataType.STRING));
                sb.append("&r\n");
            }
        }
        else
            sb.append("Vanilla Spawner\n");

        addSpawnerAttributeFromPdc("min level", main.blockPlaceListener.keySpawner_MinLevel, pdc, sb);
        addSpawnerAttributeFromPdc("max level", main.blockPlaceListener.keySpawner_MaxLevel, pdc, sb);
        sb.append('\n');
        addSpawnerAttribute("delay", cs.getDelay(), sb);
        addSpawnerAttribute("max nearby entities", cs.getMaxNearbyEntities(), sb);
        addSpawnerAttribute("min spawn delay", cs.getMinSpawnDelay(), sb);
        sb.append('\n');
        addSpawnerAttribute("max spawn delay", cs.getMaxSpawnDelay(), sb);
        addSpawnerAttribute("required player range", cs.getRequiredPlayerRange(), sb);
        addSpawnerAttribute("spawn count", cs.getSpawnCount(), sb);
        sb.append('\n');
        addSpawnerAttributeFromPdc("custom drop id", main.blockPlaceListener.keySpawner_CustomDropId, pdc, sb);
        // customName
        addSpawnerAttribute("spawn type", cs.getSpawnedType(), sb);

        player.sendMessage(MessageUtils.colorizeAll(sb.toString()));
    }

    private void addSpawnerAttributeFromPdc(final String name, final NamespacedKey key, final PersistentDataContainer pdc, final StringBuilder sb){
        if (!pdc.has(key, PersistentDataType.INTEGER)) return;

        if (!sb.substring(sb.length() - 1).equals("\n"))
            sb.append(", ");

        sb.append("&7");
        sb.append(name);
        sb.append(": &b");
        sb.append(pdc.get(key, PersistentDataType.INTEGER));
        sb.append("&r");
    }

    private void addSpawnerAttribute(final String name, final Object value, final StringBuilder sb){
        if (!sb.substring(sb.length() - 1).equals("\n"))
            sb.append(", ");
        sb.append("&7");
        sb.append(name);
        sb.append(": &b");
        sb.append(value);
        sb.append("&r");
    }
}
