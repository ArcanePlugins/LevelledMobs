/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.commands.subcommands;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.misc.RequestedLevel;
import me.lokka30.levelledmobs.misc.Utils;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.*;
import org.bukkit.metadata.FixedMetadataValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Allows you to kill LevelledMobs with various options including all levelled mobs, specific worlds
 * or levelled mobs in your proximity
 *
 * @author stumper66
 * @since 2.0
 */
public class KillSubcommand implements Subcommand {

    @Override
    public void parseSubcommand(final LevelledMobs main, final @NotNull CommandSender sender, final String label, final String[] args) {
        if (!sender.hasPermission("levelledmobs.command.kill")) {
            main.configUtils.sendNoPermissionMsg(sender);
            return;
        }

        if (args.length <= 1) {
            sendUsageMsg(sender, label, main);
            return;
        }

        int checkArgs = args.length;
        boolean useNoDrops = false;
        final RequestedLevel rl = getLevelFromCommand(sender, args);
        if (rl != null){
            if (rl.hadInvalidArguments) return;
            checkArgs -= 2;
        }

        for (final String arg : args) {
            if ("/nodrops".equalsIgnoreCase(arg)) {
                useNoDrops = true;
                break;
            }
        }

        switch (args[1].toLowerCase()) {
            case "all":
                if (!sender.hasPermission("levelledmobs.command.kill.all")) {
                    main.configUtils.sendNoPermissionMsg(sender);
                    return;
                }

                if (checkArgs == 2) {
                    if (sender instanceof Player) {
                        final Player player = (Player) sender;
                        parseKillAll(sender, Collections.singletonList(player.getWorld()), main, useNoDrops, rl);
                    } else {
                        List<String> messages = main.messagesCfg.getStringList("command.levelledmobs.kill.all.usage-console");
                        messages = Utils.replaceAllInList(messages, "%prefix%", main.configUtils.getPrefix());
                        messages = Utils.replaceAllInList(messages, "%label%", label);
                        messages = Utils.colorizeAllInList(messages);
                        messages.forEach(sender::sendMessage);
                    }
                } else if (checkArgs == 3 || checkArgs == 4) {
                    if (args[2].equals("*")) {
                        parseKillAll(sender, Bukkit.getWorlds(), main, useNoDrops, rl);
                        return;
                    }

                    if ("/nodrops".equalsIgnoreCase(args[2]))
                        parseKillAll(sender, Bukkit.getWorlds(), main, true, rl);
                    else {
                        World world = Bukkit.getWorld(args[2]);
                        if (world == null) {
                            List<String> messages = main.messagesCfg.getStringList("command.levelledmobs.kill.all.invalid-world");
                            messages = Utils.replaceAllInList(messages, "%prefix%", main.configUtils.getPrefix());
                            messages = Utils.colorizeAllInList(messages);
                            messages = Utils.replaceAllInList(messages, "%world%", args[2]); //This is after the list is colourised to ensure that an input of a world name '&aGreen' will not be colourised.
                            messages.forEach(sender::sendMessage);
                            return;
                        }
                        parseKillAll(sender, Collections.singletonList(world), main, useNoDrops, rl);
                    }
                } else {
                    List<String> messages = main.messagesCfg.getStringList("command.levelledmobs.kill.all.usage");
                    messages = Utils.replaceAllInList(messages, "%prefix%", main.configUtils.getPrefix());
                    messages = Utils.replaceAllInList(messages, "%label%", label);
                    messages = Utils.colorizeAllInList(messages);
                    messages.forEach(sender::sendMessage);
                }

                break;
            case "near":
                if (!sender.hasPermission("levelledmobs.command.kill.near")) {
                    main.configUtils.sendNoPermissionMsg(sender);
                    return;
                }

                if (checkArgs == 3 || checkArgs == 4) {
                    if (!(sender instanceof BlockCommandSender) && !(sender instanceof Player)) {
                        List<String> messages = main.messagesCfg.getStringList("common.players-only");
                        messages = Utils.replaceAllInList(messages, "%prefix%", main.configUtils.getPrefix());
                        messages = Utils.colorizeAllInList(messages);
                        messages.forEach(sender::sendMessage);
                        return;
                    }

                    int radius;
                    try {
                        radius = Integer.parseInt(args[2]);
                    } catch (NumberFormatException exception) {
                        List<String> messages = main.messagesCfg.getStringList("command.levelledmobs.kill.near.invalid-radius");
                        messages = Utils.replaceAllInList(messages, "%prefix%", main.configUtils.getPrefix());
                        messages = Utils.colorizeAllInList(messages);
                        messages = Utils.replaceAllInList(messages, "%radius%", args[2]); //After the list is colourised, so %radius% is not coloursied.
                        messages.forEach(sender::sendMessage);
                        return;
                    }

                    int maxRadius = 1000;
                    if (radius > maxRadius) {
                        radius = maxRadius;

                        List<String> messages = main.messagesCfg.getStringList("command.levelledmobs.kill.near.invalid-radius-max");
                        messages = Utils.replaceAllInList(messages, "%prefix%", main.configUtils.getPrefix());
                        messages = Utils.replaceAllInList(messages, "%maxRadius%", String.valueOf(maxRadius));
                        messages = Utils.colorizeAllInList(messages);
                        messages.forEach(sender::sendMessage);
                    }

                    int minRadius = 1;
                    if (radius < minRadius) {
                        radius = minRadius;

                        List<String> messages = main.messagesCfg.getStringList("command.levelledmobs.kill.near.invalid-radius-min");
                        messages = Utils.replaceAllInList(messages, "%prefix%", main.configUtils.getPrefix());
                        messages = Utils.replaceAllInList(messages, "%minRadius%", String.valueOf(minRadius));
                        messages = Utils.colorizeAllInList(messages);
                        messages.forEach(sender::sendMessage);
                    }

                    int killed = 0;
                    int skipped = 0;
                    final Collection<Entity> mobsToKill;
                    if (sender instanceof BlockCommandSender) {
                        final Block block = ((BlockCommandSender) sender).getBlock();
                        mobsToKill = block.getWorld().getNearbyEntities(block.getLocation(), radius, radius, radius);
                    } else
                        mobsToKill = ((Player) sender).getNearbyEntities(radius, radius, radius);

                    for (final Entity entity : mobsToKill) {
                        if (entity instanceof LivingEntity) {
                            final LivingEntity livingEntity = (LivingEntity) entity;
                            if (main.levelInterface.isLevelled(livingEntity)) {
                                if (skipKillingEntity(main, livingEntity, rl)) {
                                    skipped++;
                                } else {
                                    livingEntity.setMetadata("noCommands", new FixedMetadataValue(main, 1));

                                    if (useNoDrops)
                                        livingEntity.remove();
                                    else
                                        livingEntity.setHealth(0.0);
                                    killed++;
                                }
                            }
                        }
                    }

                    List<String> messages = main.messagesCfg.getStringList("command.levelledmobs.kill.near.success");
                    messages = Utils.replaceAllInList(messages, "%prefix%", main.configUtils.getPrefix());
                    messages = Utils.replaceAllInList(messages, "%killed%", String.valueOf(killed));
                    messages = Utils.replaceAllInList(messages, "%skipped%", String.valueOf(skipped));
                    messages = Utils.replaceAllInList(messages, "%radius%", String.valueOf(radius));
                    messages = Utils.colorizeAllInList(messages);
                    messages.forEach(sender::sendMessage);
                } else {
                    List<String> messages = main.messagesCfg.getStringList("command.levelledmobs.kill.near.usage");
                    messages = Utils.replaceAllInList(messages, "%prefix%", main.configUtils.getPrefix());
                    messages = Utils.replaceAllInList(messages, "%label%", label);
                    messages = Utils.colorizeAllInList(messages);
                    messages.forEach(sender::sendMessage);
                }

                break;
            default:
                sendUsageMsg(sender, label, main);
        }
    }

    @Nullable
    private RequestedLevel getLevelFromCommand(final @NotNull CommandSender sender, final String[] args){
        int rangeSpecifiedFlag = -1;

        for (int i = 0; i < args.length; i++) {
            if ("/levels".equalsIgnoreCase(args[i])){
                rangeSpecifiedFlag = i + 1;
            }
        }

        if (rangeSpecifiedFlag <= 0)
            return null;

        final RequestedLevel rl = new RequestedLevel();
        if (args.length <= rangeSpecifiedFlag){
            sender.sendMessage("No value was specified for /levels");
            rl.hadInvalidArguments = true;
            return rl;
        }

        final String value = args[rangeSpecifiedFlag];
        if (!rl.setLevelFromString(value)){
            sender.sendMessage("Invalid number or range specified for /levels");
            rl.hadInvalidArguments = true;
        }

        return rl;
    }

    @Override
    public List<String> parseTabCompletions(final LevelledMobs main, final @NotNull CommandSender sender, final String[] args) {
        if (!sender.hasPermission("levelledmobs.command.kill"))
            return Collections.emptyList();

        boolean containsNoDrops = false;
        boolean containsLevels = false;

        StringBuilder sb = new StringBuilder();
        for (String arg : args) {
            if ("/nodrops".equalsIgnoreCase(arg))
                containsNoDrops = true;
            else if ("/levels".equalsIgnoreCase(arg))
                containsLevels = true;
        }

        if (args.length == 2) {
            return Arrays.asList("all", "near");
        }

        if (args[1].equalsIgnoreCase("all") && (args.length == 3 || args.length == 4)) {
            if (sender.hasPermission("levelledmobs.command.kill.all")) {
                List<String> worlds = new LinkedList<>();

                if (!containsNoDrops) worlds.add("/nodrops");
                if (!containsLevels) worlds.add("/levels");
                if (args.length == 3 ) {
                    for (World world : Bukkit.getWorlds()) {
                        worlds.add("*");
                        if (main.rulesManager.getRule_IsWorldAllowedInAnyRule(world))
                            worlds.add(world.getName());
                    }
                }

                return worlds;
            }
        }
        if (args[1].equalsIgnoreCase("near")) {
            if (sender.hasPermission("levelledmobs.command.kill.near")) {
                if (args.length == 4 && "/levels".equalsIgnoreCase(args[3]))
                    return List.of("/levels");
                else if (args.length == 3)
                    return Utils.oneToNine;
            }
        }

        final List<String> result = new ArrayList<>();
        if (!containsNoDrops)
            result.add("/nodrops");
        if (!containsLevels)
            result.add("/levels");

        return result;
    }

    private void sendUsageMsg(final @NotNull CommandSender sender, final String label, final @NotNull LevelledMobs instance) {
        List<String> messages = instance.messagesCfg.getStringList("command.levelledmobs.kill.usage");
        messages = Utils.replaceAllInList(messages, "%prefix%", instance.configUtils.getPrefix());
        messages = Utils.replaceAllInList(messages, "%label%", label);
        messages = Utils.colorizeAllInList(messages);
        messages.forEach(sender::sendMessage);
    }

    private void parseKillAll(final CommandSender sender, final @NotNull List<World> worlds, final LevelledMobs main, final boolean useNoDrops, final RequestedLevel rl) {
        int killed = 0;
        int skipped = 0;

        for (final World world : worlds) {
            for (final Entity entity : world.getEntities()) {
                if (!(entity instanceof LivingEntity)) continue;
                final LivingEntity livingEntity = (LivingEntity) entity;
                if (!main.levelInterface.isLevelled(livingEntity)) continue;

                if (skipKillingEntity(main, livingEntity, rl)) {
                    skipped++;
                    continue;
                }

                livingEntity.setMetadata("noCommands", new FixedMetadataValue(main, 1));

                if (useNoDrops)
                    livingEntity.remove();
                else
                    livingEntity.setHealth(0.0);

                killed++;
            }
        }

        List<String> messages = main.messagesCfg.getStringList("command.levelledmobs.kill.all.success");
        messages = Utils.replaceAllInList(messages, "%prefix%", main.configUtils.getPrefix());
        messages = Utils.replaceAllInList(messages, "%killed%", String.valueOf(killed));
        messages = Utils.replaceAllInList(messages, "%skipped%", String.valueOf(skipped));
        messages = Utils.replaceAllInList(messages, "%worlds%", String.valueOf(worlds.size()));
        messages = Utils.colorizeAllInList(messages);
        messages.forEach(sender::sendMessage);
    }

    private boolean skipKillingEntity(final LevelledMobs main, final @NotNull LivingEntity livingEntity, final RequestedLevel rl) {
        if (livingEntity.getCustomName() != null && main.helperSettings.getBoolean(main.settingsCfg,"kill-skip-conditions.nametagged"))
            return true;

        if (rl != null){
            final int mobLevel = main.levelInterface.getLevelOfMob(livingEntity);
            if (mobLevel < rl.getLevelMin() || mobLevel > rl.getLevelMax())
                return true;
        }

        // Tamed
        if (livingEntity instanceof Tameable && ((Tameable) livingEntity).isTamed() && main.helperSettings.getBoolean(main.settingsCfg,"kill-skip-conditions.tamed"))
            return true;

        // Leashed
        if (livingEntity.isLeashed() && main.helperSettings.getBoolean(main.settingsCfg,"kill-skip-conditions.leashed")) return true;

        // Converting zombie villager
        return livingEntity.getType() == EntityType.ZOMBIE_VILLAGER &&
                ((ZombieVillager) livingEntity).isConverting() &&
                main.helperSettings.getBoolean(main.settingsCfg,"kill-skip-conditions.convertingZombieVillager");
    }
}
