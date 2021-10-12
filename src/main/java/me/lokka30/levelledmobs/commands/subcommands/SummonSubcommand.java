/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.commands.subcommands;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.misc.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Summons a levelled mob with a specific level and criteria
 *
 * @author stumper66
 * @since v2.0.0
 */
public class SummonSubcommand implements Subcommand {

    @Override
    public void parseSubcommand(final LevelledMobs main, final CommandSender sender, final String label, @NotNull final String @NotNull [] args) {
        boolean useOverride = false;
        final List<String> useArgs = new LinkedList<>();
        for (final String arg : args) {
            if ("/override".equalsIgnoreCase(arg))
                useOverride = true;
            else
                useArgs.add(arg);
        }

        final String[] useArgs2 = new String[useArgs.size()];
        useArgs.toArray(useArgs2);
        parseSubcommand2(main, sender, label, useArgs2, useOverride);
    }

    private void parseSubcommand2(final LevelledMobs main, @NotNull final CommandSender sender, final String label, final String[] args, final boolean override) {
        if (!sender.hasPermission("levelledmobs.command.summon")) {
            main.configUtils.sendNoPermissionMsg(sender);
            return;
        }

        if (args.length < 4) {
            sendMainUsage(sender, label, main);
            return;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[1]);
        } catch (NumberFormatException ex) {
            List<String> messages = main.messagesCfg.getStringList("command.levelledmobs.summon.invalid-amount");
            messages = Utils.replaceAllInList(messages, "%prefix%", main.configUtils.getPrefix());
            messages = Utils.colorizeAllInList(messages);
            messages = Utils.replaceAllInList(messages, "%amount%", args[1]); // This is after colorize so that args[1] is not colorized.
            messages.forEach(sender::sendMessage);
            return;
        }

        EntityType entityType;
        try {
            entityType = EntityType.valueOf(args[2].toUpperCase());
        } catch (IllegalArgumentException ex) {
            List<String> messages = main.messagesCfg.getStringList("command.levelledmobs.summon.invalid-entity-type");
            messages = Utils.replaceAllInList(messages, "%prefix%", main.configUtils.getPrefix());
            messages = Utils.colorizeAllInList(messages);
            messages = Utils.replaceAllInList(messages, "%entityType%", args[2]); // This is after colorize so that args[2] is not colorized.
            messages.forEach(sender::sendMessage);
            return;
        }

        final RequestedLevel requestedLevel = new RequestedLevel();
        if (!requestedLevel.setLevelFromString(args[3])){
            List<String> messages = main.messagesCfg.getStringList("command.levelledmobs.summon.invalid-level");
            messages = Utils.replaceAllInList(messages, "%prefix%", main.configUtils.getPrefix());
            messages = Utils.colorizeAllInList(messages);
            messages = Utils.replaceAllInList(messages, "%level%", args[3]); // This is after colorize so that args[3] is not colorized.
            messages.forEach(sender::sendMessage);
            return;
        }

        SummonType summonType = SummonType.HERE;
        if (args.length > 4) {
            switch (args[4].toLowerCase()) {
                case "here":
                    break;
                case "atplayer":
                    summonType = SummonType.AT_PLAYER;
                    break;
                case "atlocation":
                    summonType = SummonType.AT_LOCATION;
                    break;
                default:
                    List<String> messages = main.messagesCfg.getStringList("command.levelledmobs.summon.invalid-summon-type");
                    messages = Utils.replaceAllInList(messages, "%prefix%", main.configUtils.getPrefix());
                    messages = Utils.colorizeAllInList(messages);
                    messages = Utils.replaceAllInList(messages, "%summonType%", args[4]); // This is after colorize so args[4] is not colorized.
                    messages.forEach(sender::sendMessage);
                    return;
            }
        }

        if (summonType == SummonType.HERE) {
            if (!(sender instanceof Player) && !(sender instanceof BlockCommandSender)){
                List<String> messages = main.messagesCfg.getStringList("command.levelledmobs.summon.invalid-summon-type-console");
                messages = Utils.replaceAllInList(messages, "%prefix%", main.configUtils.getPrefix());
                messages = Utils.colorizeAllInList(messages);
                messages.forEach(sender::sendMessage);
                return;
            }


            if (args.length == 4 || args.length == 5) {
                Player player = null;
                if (sender instanceof Player) player = (Player) sender;
                final Location location = (player != null) ?
                        ((Player) sender).getLocation() : ((BlockCommandSender) sender).getBlock().getLocation();

                if (location.getWorld() == null) {
                    List<String> messages = main.messagesCfg.getStringList("common.player-offline");
                    messages = Utils.replaceAllInList(messages, "%prefix%", main.configUtils.getPrefix());
                    messages = Utils.colorizeAllInList(messages);
                    messages = Utils.replaceAllInList(messages, "%player%", (player != null) ?
                            player.getDisplayName() : "(null)");
                    messages.forEach(sender::sendMessage);
                    return;
                }

                final LivingEntityPlaceHolder lmPlaceHolder = LivingEntityPlaceHolder.getInstance(entityType, location, main);
                summonMobs(lmPlaceHolder, amount, sender, requestedLevel, summonType, player, override);
                lmPlaceHolder.free();
            } else {
                List<String> messages = main.messagesCfg.getStringList("command.levelledmobs.summon.here.usage");
                messages = Utils.replaceAllInList(messages, "%prefix%", main.configUtils.getPrefix());
                messages = Utils.replaceAllInList(messages, "%label%", label);
                messages = Utils.colorizeAllInList(messages);
                messages.forEach(sender::sendMessage);
            }
        } else if (summonType == SummonType.AT_PLAYER) {
            if (args.length == 6) {

                boolean offline = false;
                Location location = null;
                World world = null;

                final Player target = Bukkit.getPlayer(args[5]);
                if (target == null) {
                    offline = true;
                } else if (sender instanceof Player) {
                    // Vanished player compatibility.
                    final Player player = (Player) sender;
                    if (!player.canSee(target) && !player.isOp()) {
                        offline = true;
                    }
                    location = (target.getLocation());
                    world = location.getWorld();
                }

                if (offline || world == null) {
                    List<String> messages = main.messagesCfg.getStringList("common.player-offline");
                    messages = Utils.replaceAllInList(messages, "%prefix%", main.configUtils.getPrefix());
                    messages = Utils.colorizeAllInList(messages);
                    messages = Utils.replaceAllInList(messages, "%player%", args[5]); // This is after colorize so that args[5] is not colorized.
                    messages.forEach(sender::sendMessage);
                    return;
                }

                final LivingEntityPlaceHolder lmPlaceHolder = LivingEntityPlaceHolder.getInstance(entityType, location, main);
                summonMobs(lmPlaceHolder, amount, sender, requestedLevel, summonType, target, override);
                lmPlaceHolder.free();
            } else {
                List<String> messages = main.messagesCfg.getStringList("command.levelledmobs.summon.atPlayer.usage");
                messages = Utils.replaceAllInList(messages, "%prefix%", main.configUtils.getPrefix());
                messages = Utils.replaceAllInList(messages, "%label%", label);
                messages = Utils.colorizeAllInList(messages);
                messages.forEach(sender::sendMessage);
            }
        } else { // At Location
            if (args.length == 8 || args.length == 9) {
                final String worldName;

                if (args.length == 8) {
                    if (sender instanceof Player)
                        worldName = ((Player) sender).getWorld().getName();
                    else if (sender instanceof BlockCommandSender)
                        worldName = ((BlockCommandSender) sender).getBlock().getWorld().getName();
                    else {
                        List<String> messages = main.messagesCfg.getStringList("command.levelledmobs.summon.atLocation.usage-console");
                        messages = Utils.replaceAllInList(messages, "%prefix%", main.configUtils.getPrefix());
                        messages = Utils.replaceAllInList(messages, "%label%", label);
                        messages = Utils.colorizeAllInList(messages);
                        messages.forEach(sender::sendMessage);
                        return;
                    }
                } else { //args.length==9
                    final World world = Bukkit.getWorld(args[8]);

                    if (world == null) {
                        List<String> messages = main.messagesCfg.getStringList("command.levelledmobs.summon.atLocation.invalid-world");
                        messages = Utils.replaceAllInList(messages, "%prefix%", main.configUtils.getPrefix());
                        messages = Utils.colorizeAllInList(messages);
                        messages = Utils.replaceAllInList(messages, "%world%", args[8]); //This is after colorize so that args[8] is not colorized.
                        messages.forEach(sender::sendMessage);
                        return;
                    } else
                        worldName = world.getName();
                }

                Location location = getRelativeLocation(sender, args[5], args[6], args[7], worldName);

                if (location == null || location.getWorld() == null) {
                    List<String> messages = main.messagesCfg.getStringList("command.levelledmobs.summon.atLocation.invalid-location");
                    messages = Utils.replaceAllInList(messages, "%prefix%", main.configUtils.getPrefix());
                    messages = Utils.colorizeAllInList(messages);
                    messages.forEach(sender::sendMessage);
                } else {
                    LivingEntityPlaceHolder lmPlaceHolder = LivingEntityPlaceHolder.getInstance(entityType, location, main);
                    summonMobs(lmPlaceHolder, amount, sender, requestedLevel, summonType, null, override);
                    lmPlaceHolder.free();
                }
            } else {
                List<String> messages = main.messagesCfg.getStringList("command.levelledmobs.summon.atLocation.usage");
                messages = Utils.replaceAllInList(messages, "%prefix%", main.configUtils.getPrefix());
                messages = Utils.replaceAllInList(messages, "%label%", label);
                messages = Utils.colorizeAllInList(messages);
                messages.forEach(sender::sendMessage);
            }
        }
    }

    @Override
    public List<String> parseTabCompletions(final LevelledMobs main, final @NotNull CommandSender sender, @NotNull final String[] args) {
        if (!sender.hasPermission("levelledmobs.command.summon"))
            return null;

        // len:    1      2        3        4       5          6            7   8     9     10
        // arg:    0      1        2        3       4          5            6   7     8     9
        // lvlmobs summon <amount> <entity> <level> here       /override
        // lvlmobs summon <amount> <entity> <level> atPlayer   <playername> /override
        // lvlmobs summon <amount> <entity> <level> atLocation <x>          <y> <z> [world] /override

        // <amount>
        if (args.length == 2) {
            return Utils.oneToNine;
        }

        // <entity>
        if (args.length == 3) {
            List<String> entityNames = new LinkedList<>();
            for (EntityType entityType : EntityType.values()) {
                entityNames.add(entityType.toString().toLowerCase());
            }
            return entityNames;
        }

        // <level>
        if (args.length == 4) {
            return Utils.oneToNine;
        }

        // here, atPlayer, atLocation
        if (args.length == 5) {
            return Arrays.asList("here", "atPlayer", "atLocation", "/override");
        }

        // no suggestions for 'here' since it is the last argument for itself
        // these are for atPlayer and atLocation
        if (args.length > 5) {
            switch (args[4].toLowerCase()) {
                case "atplayer":
                    if (args.length == 6) {
                        List<String> suggestions = new LinkedList<>();
                        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                            if (sender instanceof Player) {
                                Player player = (Player) sender;
                                if (player.canSee(onlinePlayer) || player.isOp()) {
                                    suggestions.add(onlinePlayer.getName());
                                }
                            } else {
                                suggestions.add(onlinePlayer.getName());
                            }
                        }
                        return suggestions;
                    } else if (args.length == 7){
                        return Collections.singletonList("/override");
                    }
                    break;

                case "atlocation":
                    if (args.length < 9) { // args 6, 7 and 8 = x, y and z
                        return Collections.singletonList("~");
                    } else if (args.length == 9) {
                        List<String> worlds = new LinkedList<>();
                        Bukkit.getWorlds().forEach(world -> worlds.add(world.getName()));
                        return worlds;
                    } else if (args.length == 10){
                        return Collections.singletonList("/override");
                    }

                    break;
                case "here":
                    return Collections.singletonList("/override");
                default:
                    return null;
            }
        }

        return null;
    }

    private enum SummonType {
        HERE,
        AT_PLAYER,
        AT_LOCATION
    }

    private void sendMainUsage(@NotNull final CommandSender sender, final String label, @NotNull final LevelledMobs main) {

        List<String> messages = main.messagesCfg.getStringList("command.levelledmobs.summon.usage");
        messages = Utils.replaceAllInList(messages, "%prefix%", main.configUtils.getPrefix());
        messages = Utils.replaceAllInList(messages, "%label%", label);
        messages = Utils.colorizeAllInList(messages);
        messages.forEach(sender::sendMessage);
    }

    private void summonMobs(@NotNull final LivingEntityPlaceHolder lmPlaceHolder, int amount, final CommandSender sender,
                            RequestedLevel requestedLevel, final SummonType summonType, final @Nullable Player target, final boolean override) {

        final LevelledMobs main = lmPlaceHolder.getMainInstance();
        Location location = lmPlaceHolder.getLocation();

        if (main.levelManager.FORCED_BLOCKED_ENTITY_TYPES.contains(lmPlaceHolder.getTypeName())) {
            List<String> messages = main.messagesCfg.getStringList("command.levelledmobs.summon.not-levellable");
            messages = Utils.replaceAllInList(messages, "%prefix%", main.configUtils.getPrefix());
            messages = Utils.replaceAllInList(messages, "%entity%", lmPlaceHolder.getTypeName());
            messages = Utils.colorizeAllInList(messages);
            messages.forEach(sender::sendMessage);
            return;
        }

        if (!sender.isOp() && !override && main.levelInterface.getLevellableState(lmPlaceHolder) != LevellableState.ALLOWED) {
            List<String> messages = main.messagesCfg.getStringList("command.levelledmobs.summon.not-levellable");
            messages = Utils.replaceAllInList(messages, "%prefix%", main.configUtils.getPrefix());
            messages = Utils.replaceAllInList(messages, "%entity%", lmPlaceHolder.getTypeName());
            messages = Utils.colorizeAllInList(messages);
            messages.forEach(sender::sendMessage);
            return;
        }

        if (amount < 1) {
            List<String> messages = main.messagesCfg.getStringList("command.levelledmobs.summon.amount-limited.min");
            messages = Utils.replaceAllInList(messages, "%prefix%", main.configUtils.getPrefix());
            messages = Utils.colorizeAllInList(messages);
            messages.forEach(sender::sendMessage);
        }

        final int maxAmount = main.helperSettings.getInt(main.settingsCfg, "customize-summon-command-limit", 100);
        if (amount > maxAmount) {
            amount = maxAmount;

            List<String> messages = main.messagesCfg.getStringList("command.levelledmobs.summon.amount-limited.max");
            messages = Utils.replaceAllInList(messages, "%prefix%", main.configUtils.getPrefix());
            messages = Utils.replaceAllInList(messages, "%maxAmount%", maxAmount + "");
            messages = Utils.colorizeAllInList(messages);
            messages.forEach(sender::sendMessage);
        }

        final int[] levels = main.levelManager.getMinAndMaxLevels(lmPlaceHolder);
        final int minLevel = levels[0];
        final int maxLevel = levels[1];

        if (requestedLevel.getLevelMin() < minLevel && !sender.hasPermission("levelledmobs.command.summon.bypass-level-limit") && !override) {
            requestedLevel.setMinAllowedLevel(minLevel);

            List<String> messages = main.messagesCfg.getStringList("command.levelledmobs.summon.level-limited.min");
            messages = Utils.replaceAllInList(messages, "%prefix%", main.configUtils.getPrefix());
            messages = Utils.replaceAllInList(messages, "%minLevel%", minLevel + "");
            messages = Utils.colorizeAllInList(messages);
            messages.forEach(sender::sendMessage);
        }

        if (requestedLevel.getLevelMax() > maxLevel && !sender.hasPermission("levelledmobs.command.summon.bypass-level-limit") && !override) {
            requestedLevel.setMaxAllowedLevel(maxLevel);

            List<String> messages = main.messagesCfg.getStringList("command.levelledmobs.summon.level-limited.max");
            messages = Utils.replaceAllInList(messages, "%prefix%", main.configUtils.getPrefix());
            messages = Utils.replaceAllInList(messages, "%maxLevel%", maxLevel + "");
            messages = Utils.colorizeAllInList(messages);
            messages.forEach(sender::sendMessage);
        }

        if (summonType == SummonType.HERE)
            location = addVarianceToLocation(location);

        if (summonType == SummonType.HERE || summonType == SummonType.AT_PLAYER) {
            final int distFromPlayer = main.settingsCfg.getInt("summon-command-spawn-distance-from-player", 5);
            if (distFromPlayer > 0 && target != null) {
                int useDistFromPlayer = distFromPlayer;
                final double direction = target.getEyeLocation().getYaw();
                final Location origLocation = location;
                // try up to 50 times to find a open spot to spawn the mob.  Keep getting closer to the player if needed
                for (int i = 0; i < 50; i++) {
                    useDistFromPlayer -= i;
                    if (useDistFromPlayer <= 0) break;

                    location = getLocationNearPlayer(target, origLocation, useDistFromPlayer);
                    final Location location_YMinus1 = new Location(location.getWorld(), location.getBlockX(), location.getBlockY() - 1, location.getBlockZ());
                    if (location.getBlock().isPassable() && location_YMinus1.getBlock().isPassable())
                        break; // found an open spot
                }
            } else if (target == null && sender instanceof BlockCommandSender) {
                BlockCommandSender bcs = (BlockCommandSender) sender;
                // increase the y by one so they don't spawn inside the command block
                location = new Location(location.getWorld(), location.getBlockX(), location.getBlockY() + 1, location.getBlockZ());
            }
        }

        main.levelManager.summonedEntityType = lmPlaceHolder.getEntityType();
        main.levelManager.summonedLocation = location;

        for (int i = 0; i < amount; i++) {
            assert location.getWorld() != null;

            final Entity entity = location.getWorld().spawnEntity(location, lmPlaceHolder.getEntityType());

            final int useLevel = requestedLevel.hasLevelRange ?
                ThreadLocalRandom.current().nextInt(requestedLevel.levelRangeMin, requestedLevel.levelRangeMax + 1) :
                requestedLevel.level;


            if (entity instanceof LivingEntity) {
                final LivingEntityWrapper lmEntity = LivingEntityWrapper.getInstance((LivingEntity) entity, main);
                main.levelInterface.applyLevelToMob(lmEntity, useLevel, true, override, new HashSet<>(Collections.singletonList(AdditionalLevelInformation.NOT_APPLICABLE)));
                synchronized (lmEntity.getLivingEntity().getPersistentDataContainer()){
                    lmEntity.getPDC().set(main.namespaced_keys.wasSummoned, PersistentDataType.INTEGER, 1);
                }
                lmEntity.free();
            }
        }

        main.levelManager.summonedEntityType = EntityType.UNKNOWN;
        main.levelManager.summonedLocation = null;

        switch (summonType) {
            case HERE:
                List<String> hereSuccessmessages = main.messagesCfg.getStringList("command.levelledmobs.summon.here.success");
                hereSuccessmessages = Utils.replaceAllInList(hereSuccessmessages, "%prefix%", main.configUtils.getPrefix());
                hereSuccessmessages = Utils.replaceAllInList(hereSuccessmessages, "%amount%", amount + "");
                hereSuccessmessages = Utils.replaceAllInList(hereSuccessmessages, "%level%", requestedLevel.toString());
                hereSuccessmessages = Utils.replaceAllInList(hereSuccessmessages, "%entity%", lmPlaceHolder.getTypeName());
                hereSuccessmessages = Utils.colorizeAllInList(hereSuccessmessages);
                hereSuccessmessages.forEach(sender::sendMessage);
                break;

            case AT_LOCATION:
                List<String> atLocationSuccessMessages = main.messagesCfg.getStringList("command.levelledmobs.summon.atLocation.success");
                atLocationSuccessMessages = Utils.replaceAllInList(atLocationSuccessMessages, "%prefix%", main.configUtils.getPrefix());
                atLocationSuccessMessages = Utils.replaceAllInList(atLocationSuccessMessages, "%amount%", amount + "");
                atLocationSuccessMessages = Utils.replaceAllInList(atLocationSuccessMessages, "%level%", requestedLevel.toString());
                atLocationSuccessMessages = Utils.replaceAllInList(atLocationSuccessMessages, "%entity%", lmPlaceHolder.getTypeName());
                atLocationSuccessMessages = Utils.replaceAllInList(atLocationSuccessMessages, "%x%", location.getBlockX() + "");
                atLocationSuccessMessages = Utils.replaceAllInList(atLocationSuccessMessages, "%y%", location.getBlockY() + "");
                atLocationSuccessMessages = Utils.replaceAllInList(atLocationSuccessMessages, "%z%", location.getBlockZ() + "");
                atLocationSuccessMessages = Utils.replaceAllInList(atLocationSuccessMessages, "%world%", location.getWorld() == null ? "(null)" : location.getWorld().getName());
                atLocationSuccessMessages = Utils.colorizeAllInList(atLocationSuccessMessages);
                atLocationSuccessMessages.forEach(sender::sendMessage);
                break;

            case AT_PLAYER:
                List<String> atPlayerSuccessMessages = main.messagesCfg.getStringList("command.levelledmobs.summon.atPlayer.success");
                atPlayerSuccessMessages = Utils.replaceAllInList(atPlayerSuccessMessages, "%prefix%", main.configUtils.getPrefix());
                atPlayerSuccessMessages = Utils.replaceAllInList(atPlayerSuccessMessages, "%amount%", amount + "");
                atPlayerSuccessMessages = Utils.replaceAllInList(atPlayerSuccessMessages, "%level%", requestedLevel.toString());
                atPlayerSuccessMessages = Utils.replaceAllInList(atPlayerSuccessMessages, "%entity%", lmPlaceHolder.getTypeName());
                atPlayerSuccessMessages = Utils.replaceAllInList(atPlayerSuccessMessages, "%targetUsername%", target == null ? "(null)" : target.getName());
                atPlayerSuccessMessages = Utils.replaceAllInList(atPlayerSuccessMessages, "%targetDisplayname%", target == null ? "(null)" : target.getDisplayName());
                atPlayerSuccessMessages = Utils.colorizeAllInList(atPlayerSuccessMessages);
                atPlayerSuccessMessages.forEach(sender::sendMessage);
                break;
            default:
                throw new IllegalStateException("Unexpected SummonType value of " + summonType + "!");
        }
    }

    @Contract("_, _, _ -> new")
    @NotNull
    private Location getLocationNearPlayer(final @NotNull Player player, final @NotNull Location location, final int useDistFromPlayer){
        int newX = location.getBlockX();
        int newZ = location.getBlockZ();

        double rotation = (player.getLocation().getYaw() - 180) % 360;
        if (rotation < 0)
            rotation += 360.0;

        if (0 <= rotation && rotation < 22.5) // N
            newZ -= useDistFromPlayer;
        else if (22.5 <= rotation && rotation < 67.5) { // NE
            newX += useDistFromPlayer;
            newZ -= useDistFromPlayer;
        } else if (67.5 <= rotation && rotation < 112.5) // E
            newX += useDistFromPlayer;
        else if (112.5 <= rotation && rotation < 157.5) { // SE
            newX += useDistFromPlayer;
            newZ += useDistFromPlayer;
        } else if (157.5 <= rotation && rotation < 202.5) // S
            newZ += useDistFromPlayer;
        else if (202.5 <= rotation && rotation < 247.5) { // SW
            newX -= useDistFromPlayer;
            newZ += useDistFromPlayer;
        } else if (247.5 <= rotation && rotation < 292.5) // W
            newX -= useDistFromPlayer;
        else if (292.5 <= rotation && rotation < 337.5) { // NW
            newX -= useDistFromPlayer;
            newZ -= useDistFromPlayer;
        } else // N
            newZ -= useDistFromPlayer;

        return new Location(location.getWorld(), newX, location.getBlockY(), newZ);
    }

    @Nullable
    private Location getRelativeLocation(final CommandSender sender, final String xStr, final String yStr, final String zStr, final String worldName) {
        double x = 0, y = 0, z = 0;
        boolean xRelative = false, yRelative = false, zRelative = false;

        if (sender instanceof Player || sender instanceof BlockCommandSender) { //Player or Command blocks
            if (xStr.charAt(0) == '~') {
                if (sender instanceof Player)
                    x = ((Player) sender).getLocation().getX();
                else
                    x = ((BlockCommandSender) sender).getBlock().getX();

                if (xStr.length() > 1) {
                    double addition;
                    try {
                        addition = Double.parseDouble(xStr.substring(1));
                    } catch (NumberFormatException ex) {
                        return null;
                    }
                    x += addition;
                }

                xRelative = true;
            }
            if (yStr.charAt(0) == '~') {
                if (sender instanceof Player)
                    y = ((Player) sender).getLocation().getY();
                else
                    y = ((BlockCommandSender) sender).getBlock().getY();

                if (yStr.length() > 1) {
                    double addition;
                    try {
                        addition = Double.parseDouble(yStr.substring(1));
                    } catch (NumberFormatException ex) {
                        return null;
                    }

                    y += addition;
                }

                yRelative = true;
            }
            if (zStr.charAt(0) == '~') {
                if (sender instanceof Player)
                    z = ((Player) sender).getLocation().getZ();
                else
                    z = ((BlockCommandSender) sender).getBlock().getZ();

                if (zStr.length() > 1) {
                    double addition;
                    try {
                        addition = Double.parseDouble(zStr.substring(1));
                    } catch (NumberFormatException ex) {
                        return null;
                    }
                    z += addition;
                }

                zRelative = true;
            }
        }

        if (!xRelative) {
            try {
                x = Double.parseDouble(xStr);
            } catch (NumberFormatException ex) {
                return null;
            }
        }
        if (!yRelative) {
            try {
                y = Double.parseDouble(yStr);
            } catch (NumberFormatException ex) {
                return null;
            }
        }
        if (!zRelative) {
            try {
                z = Double.parseDouble(zStr);
            } catch (NumberFormatException ex) {
                return null;
            }
        }

        final World world = Bukkit.getWorld(worldName);
        if (world == null)
            return null;

        return new Location(world, x, y, z);
    }

    private Location addVarianceToLocation(final Location oldLocation) {
        final double min = 0.5;
        final double max = 2.5;

        for (int i = 0; i < 20; i++) {
            //Creates 3x new Random()s for a different seed each time
            final double x = min + (max - min) * new Random().nextDouble();
            final double y = min + (max - min) * new Random().nextDouble();
            final double z = min + (max - min) * new Random().nextDouble();

            final Location newLocation = new Location(oldLocation.getWorld(), x, y, z);
            if (newLocation.getBlock().isPassable() && newLocation.add(0, 1, 0).getBlock().isPassable())
                return newLocation;
        }

        return oldLocation;
    }
}
