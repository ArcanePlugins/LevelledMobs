/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.commands.subcommands;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.commands.MessagesBase;
import me.lokka30.levelledmobs.misc.RequestedLevel;
import me.lokka30.levelledmobs.util.Utils;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.entity.ZombieVillager;
import org.bukkit.metadata.FixedMetadataValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Allows you to kill LevelledMobs with various options including all levelled mobs, specific worlds
 * or levelled mobs in your proximity
 *
 * @author stumper66
 * @since 2.0
 */
public class KillSubcommand extends MessagesBase implements Subcommand {

    public KillSubcommand(final LevelledMobs main) {
        super(main);
    }

    @Override
    public void parseSubcommand(final LevelledMobs main, final @NotNull CommandSender sender,
        final String label, final String[] args) {
        commandSender = sender;
        messageLabel = label;

        if (!sender.hasPermission("levelledmobs.command.kill")) {
            main.configUtils.sendNoPermissionMsg(sender);
            return;
        }

        if (args.length <= 1) {
            showMessage("command.levelledmobs.kill.usage");
            return;
        }

        int checkArgs = args.length;
        boolean useNoDrops = false;
        final RequestedLevel rl = getLevelFromCommand(sender, args);
        if (rl != null) {
            if (rl.hadInvalidArguments) {
                return;
            }
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
                        parseKillAll(List.of(player.getWorld()), main, useNoDrops, rl);
                    } else {
                        showMessage("command.levelledmobs.kill.all.usage-console");
                    }

                } else if (checkArgs == 3 || checkArgs == 4) {
                    if (args[2].equals("*")) {
                        parseKillAll(Bukkit.getWorlds(), main, useNoDrops, rl);
                        return;
                    }

                    if ("/nodrops".equalsIgnoreCase(args[2])) {
                        parseKillAll(Bukkit.getWorlds(), main, true, rl);
                    } else {
                        final World world = Bukkit.getWorld(args[2]);
                        if (world == null) {
                            showMessage("command.levelledmobs.kill.all.invalid-world", "%world%",
                                args[2]);
                            return;
                        }
                        parseKillAll(List.of(world), main, useNoDrops, rl);
                    }
                } else {
                    showMessage("command.levelledmobs.kill.all.usage");
                }
                break;
            case "near":
                if (!sender.hasPermission("levelledmobs.command.kill.near")) {
                    main.configUtils.sendNoPermissionMsg(sender);
                    return;
                }

                if (checkArgs == 3 || checkArgs == 4) {
                    if (!(sender instanceof BlockCommandSender) && !(sender instanceof Player)) {
                        showMessage("common.players-only");
                        return;
                    }

                    int radius;
                    try {
                        radius = Integer.parseInt(args[2]);
                    } catch (final NumberFormatException exception) {
                        showMessage("command.levelledmobs.kill.near.invalid-radius", "%radius%",
                            args[2]);
                        //messages = Utils.replaceAllInList(messages, "%radius%", args[2]); //After the list is colourised, so %radius% is not coloursied.
                        return;
                    }

                    final int maxRadius = 1000;
                    if (radius > maxRadius) {
                        radius = maxRadius;
                        showMessage("command.levelledmobs.kill.near.invalid-radius-max",
                            "%maxRadius%", String.valueOf(maxRadius));
                    }

                    final int minRadius = 1;
                    if (radius < minRadius) {
                        radius = minRadius;
                        showMessage("command.levelledmobs.kill.near.invalid-radius-min",
                            "%minRadius%", String.valueOf(minRadius));
                    }

                    int killed = 0;
                    int skipped = 0;
                    final Collection<Entity> mobsToKill;
                    if (sender instanceof BlockCommandSender) {
                        final Block block = ((BlockCommandSender) sender).getBlock();
                        mobsToKill = block.getWorld()
                            .getNearbyEntities(block.getLocation(), radius, radius, radius);
                    } else {
                        mobsToKill = ((Player) sender).getNearbyEntities(radius, radius, radius);
                    }

                    for (final Entity entity : mobsToKill) {
                        if (!(entity instanceof LivingEntity)) continue;

                        final LivingEntity livingEntity = (LivingEntity) entity;
                        if (!main.levelInterface.isLevelled(livingEntity)) continue;

                        if (skipKillingEntity(main, livingEntity, rl)) {
                            skipped++;
                            continue;
                        }

                        livingEntity.setMetadata("noCommands",
                            new FixedMetadataValue(main, 1));

                        if (useNoDrops) {
                            livingEntity.remove();
                        } else {
                            livingEntity.setHealth(0.0);
                        }
                        killed++;
                    }

                    showMessage("command.levelledmobs.kill.near.success",
                        new String[]{"%killed%", "%skipped%", "%radius%"},
                        new String[]{String.valueOf(killed), String.valueOf(skipped),
                            String.valueOf(radius)}
                    );
                } else {
                    showMessage("command.levelledmobs.kill.near.usage");
                }

                break;
            default:
                showMessage("command.levelledmobs.kill.usage");
        }
    }

    @Nullable private RequestedLevel getLevelFromCommand(final @NotNull CommandSender sender,
        final String @NotNull [] args) {
        int rangeSpecifiedFlag = -1;

        for (int i = 0; i < args.length; i++) {
            if ("/levels".equalsIgnoreCase(args[i])) {
                rangeSpecifiedFlag = i + 1;
            }
        }

        if (rangeSpecifiedFlag <= 0) {
            return null;
        }

        final RequestedLevel rl = new RequestedLevel();
        if (args.length <= rangeSpecifiedFlag) {
            sender.sendMessage("No value was specified for /levels");
            rl.hadInvalidArguments = true;
            return rl;
        }

        final String value = args[rangeSpecifiedFlag];
        if (!rl.setLevelFromString(value)) {
            sender.sendMessage("Invalid number or range specified for /levels");
            rl.hadInvalidArguments = true;
        }

        return rl;
    }

    @Override
    public List<String> parseTabCompletions(final LevelledMobs main,
        final @NotNull CommandSender sender, final String[] args) {
        if (!sender.hasPermission("levelledmobs.command.kill")) {
            return Collections.emptyList();
        }

        boolean containsNoDrops = false;
        boolean containsLevels = false;

        for (final String arg : args) {
            if ("/nodrops".equalsIgnoreCase(arg)) {
                containsNoDrops = true;
            } else if ("/levels".equalsIgnoreCase(arg)) {
                containsLevels = true;
            }
        }

        if (args.length == 2) {
            return List.of("all", "near");
        }

        if (args[1].equalsIgnoreCase("all") && (args.length == 3 || args.length == 4)) {
            if (sender.hasPermission("levelledmobs.command.kill.all")) {
                final List<String> worlds = new LinkedList<>();

                if (!containsNoDrops) {
                    worlds.add("/nodrops");
                }
                if (!containsLevels) {
                    worlds.add("/levels");
                }
                if (args.length == 3) {
                    for (final World world : Bukkit.getWorlds()) {
                        worlds.add("*");
                        if (main.rulesManager.getRuleIsWorldAllowedInAnyRule(world)) {
                            worlds.add(world.getName());
                        }
                    }
                }

                return worlds;
            }
        }
        if (args[1].equalsIgnoreCase("near") && sender.hasPermission("levelledmobs.command.kill.near")) {
            if (args.length == 4 && "/levels".equalsIgnoreCase(args[3])) {
                return List.of("/levels");
            } else if (args.length == 3) {
                return Utils.oneToNine;
            }
        }

        final List<String> result = new LinkedList<>();
        if (!containsNoDrops) {
            result.add("/nodrops");
        }
        if (!containsLevels) {
            result.add("/levels");
        }

        return result;
    }

    private void parseKillAll(final @NotNull List<World> worlds, final LevelledMobs main,
        final boolean useNoDrops, final RequestedLevel rl) {
        int killed = 0;
        int skipped = 0;

        for (final World world : worlds) {
            for (final Entity entity : world.getEntities()) {
                if (!(entity instanceof LivingEntity)) {
                    continue;
                }
                final LivingEntity livingEntity = (LivingEntity) entity;
                if (!main.levelInterface.isLevelled(livingEntity)) {
                    continue;
                }

                if (skipKillingEntity(main, livingEntity, rl)) {
                    skipped++;
                    continue;
                }

                livingEntity.setMetadata("noCommands", new FixedMetadataValue(main, 1));

                if (useNoDrops) {
                    livingEntity.remove();
                } else {
                    livingEntity.setHealth(0.0);
                }

                killed++;
            }
        }

        showMessage("command.levelledmobs.kill.all.success",
            new String[]{"%killed%", "%skipped%", "%worlds%"},
            new String[]{String.valueOf(killed), String.valueOf(skipped),
                String.valueOf(worlds.size())}
        );
    }

    private boolean skipKillingEntity(final LevelledMobs main,
        final @NotNull LivingEntity livingEntity, final RequestedLevel rl) {
        if (livingEntity.getCustomName() != null && main.helperSettings.getBoolean(main.settingsCfg,
            "kill-skip-conditions.nametagged")) {
            return true;
        }

        if (rl != null) {
            final int mobLevel = main.levelInterface.getLevelOfMob(livingEntity);
            if (mobLevel < rl.getLevelMin() || mobLevel > rl.getLevelMax()) {
                return true;
            }
        }

        // Tamed
        if (livingEntity instanceof Tameable && ((Tameable) livingEntity).isTamed()
            && main.helperSettings.getBoolean(main.settingsCfg, "kill-skip-conditions.tamed")) {
            return true;
        }

        // Leashed
        if (livingEntity.isLeashed() && main.helperSettings.getBoolean(main.settingsCfg,
            "kill-skip-conditions.leashed")) {
            return true;
        }

        // Converting zombie villager
        return livingEntity.getType() == EntityType.ZOMBIE_VILLAGER &&
            ((ZombieVillager) livingEntity).isConverting() &&
            main.helperSettings.getBoolean(main.settingsCfg,
                "kill-skip-conditions.convertingZombieVillager");
    }
}
