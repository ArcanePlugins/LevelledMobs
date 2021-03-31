package me.lokka30.levelledmobs.commands.subcommands;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.misc.MobProcessReason;
import me.lokka30.levelledmobs.misc.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.CreatureSpawnEvent;

import java.util.*;

/**
 * @author stumper66
 * @contributors lokka30
 */
public class SummonSubcommand implements Subcommand {

    @Override
    public void parseSubcommand(final LevelledMobs main, final CommandSender sender, final String label, final String[] args) {
        boolean useOverride = false;
        final List<String> useArgs = new ArrayList<>();
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

    private void parseSubcommand2(final LevelledMobs main, final CommandSender sender, final String label, final String[] args, final boolean override) {
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

        int level;
        try {
            level = Integer.parseInt(args[3]);
        } catch (NumberFormatException ex) {
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
                    summonType = SummonType.HERE;
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
            if (sender instanceof Player) {
                final Player player = (Player) sender;

                if (args.length == 4 || args.length == 5) {

                    summonMobs(main, entityType, amount, sender, level, player.getLocation(), summonType, player, override);
                } else {
                    List<String> messages = main.messagesCfg.getStringList("command.levelledmobs.summon.here.usage");
                    messages = Utils.replaceAllInList(messages, "%prefix%", main.configUtils.getPrefix());
                    messages = Utils.replaceAllInList(messages, "%label%", label);
                    messages = Utils.colorizeAllInList(messages);
                    messages.forEach(sender::sendMessage);
                }
            } else {
                List<String> messages = main.messagesCfg.getStringList("command.levelledmobs.summon.invalid-summon-type-console");
                messages = Utils.replaceAllInList(messages, "%prefix%", main.configUtils.getPrefix());
                messages = Utils.colorizeAllInList(messages);
                messages.forEach(sender::sendMessage);
            }
        } else if (summonType == SummonType.AT_PLAYER) {
            if (args.length == 6) {

                boolean offline = false;
                final Player target = Bukkit.getPlayer(args[5]);
                if (target == null) {
                    offline = true;
                } else if (sender instanceof Player) {
                    // Vanished player compatibility.
                    final Player player = (Player) sender;
                    if (!player.canSee(target) && !player.isOp()) {
                        offline = true;
                    }
                }

                if (offline) {
                    List<String> messages = main.messagesCfg.getStringList("common.player-offline");
                    messages = Utils.replaceAllInList(messages, "%prefix%", main.configUtils.getPrefix());
                    messages = Utils.colorizeAllInList(messages);
                    messages = Utils.replaceAllInList(messages, "%player%", args[5]); // This is after colorize so that args[5] is not colorized.
                    messages.forEach(sender::sendMessage);
                    return;
                }

                summonMobs(main, entityType, amount, sender, level, target.getLocation(), summonType, target, override);
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
                    if (sender instanceof Player) {
                        final Player player = (Player) sender;
                        worldName = player.getWorld().getName();
                    } else {
                        List<String> messages = main.messagesCfg.getStringList("command.levelledmobs.summon.atLocation.usage-console");
                        messages = Utils.replaceAllInList(messages, "%prefix%", main.configUtils.getPrefix());
                        messages = Utils.replaceAllInList(messages, "%label%", label);
                        messages = Utils.colorizeAllInList(messages);
                        messages.forEach(sender::sendMessage);
                        return;
                    }
                } else { //args.length==9
                    World world = Bukkit.getWorld(args[8]);

                    if (world == null) {
                        List<String> messages = main.messagesCfg.getStringList("command.levelledmobs.summon.atLocation.invalid-world");
                        messages = Utils.replaceAllInList(messages, "%prefix%", main.configUtils.getPrefix());
                        messages = Utils.colorizeAllInList(messages);
                        messages = Utils.replaceAllInList(messages, "%world%", args[8]); //This is after colorize so that args[8] is not colorized.
                        messages.forEach(sender::sendMessage);
                        return;
                    } else {
                        worldName = world.getName();
                    }
                }

                Location location = getRelativeLocation(sender, args[5], args[6], args[7], worldName);

                if (location == null) {
                    List<String> messages = main.messagesCfg.getStringList("command.levelledmobs.summon.atLocation.invalid-location");
                    messages = Utils.replaceAllInList(messages, "%prefix%", main.configUtils.getPrefix());
                    messages = Utils.colorizeAllInList(messages);
                    messages.forEach(sender::sendMessage);
                } else {
                    summonMobs(main, entityType, amount, sender, level, location, summonType, null, override);
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
    public List<String> parseTabCompletions(final LevelledMobs main, final CommandSender sender, final String[] args) {

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
            List<String> entityNames = new ArrayList<>();
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
                        List<String> suggestions = new ArrayList<>();
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
                        List<String> worlds = new ArrayList<>();
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

    private void sendMainUsage(final CommandSender sender, final String label, final LevelledMobs main) {

        List<String> messages = main.messagesCfg.getStringList("command.levelledmobs.summon.usage");
        messages = Utils.replaceAllInList(messages, "%prefix%", main.configUtils.getPrefix());
        messages = Utils.replaceAllInList(messages, "%label%", label);
        messages = Utils.colorizeAllInList(messages);
        messages.forEach(sender::sendMessage);
    }

    private void summonMobs(final LevelledMobs main, final EntityType entityType, int amount, final CommandSender sender,
                            int level, Location location, final SummonType summonType, final Player target, final boolean override) {
        if (override || main.levelManager.isLevellable(entityType)) {

            if (location == null || location.getWorld() == null) {
                List<String> messages = main.messagesCfg.getStringList("command.levelledmobs.summon.invalid-location");
                messages = Utils.replaceAllInList(messages, "%prefix%", main.configUtils.getPrefix());
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

            int maxAmount = 100;
            if (amount > maxAmount) {
                amount = maxAmount;

                List<String> messages = main.messagesCfg.getStringList("command.levelledmobs.summon.amount-limited.max");
                messages = Utils.replaceAllInList(messages, "%prefix%", main.configUtils.getPrefix());
                messages = Utils.replaceAllInList(messages, "%maxAmount%", maxAmount + "");
                messages = Utils.colorizeAllInList(messages);
                messages.forEach(sender::sendMessage);
            }

            final int[] levels = main.levelManager.getMinAndMaxLevels(null, entityType, true, location.getWorld().getName(), null, CreatureSpawnEvent.SpawnReason.CUSTOM);
            final int minLevel = levels[0];
            final int maxLevel = levels[1];

            //int minLevel = instance.configUtils.getMinLevel(entityType, location.getWorld(), true, null, CreatureSpawnEvent.SpawnReason.CUSTOM);

            if (level < minLevel && !sender.hasPermission("levelledmobs.command.summon.bypass-level-limit") && !override) {
                level = minLevel;

                List<String> messages = main.messagesCfg.getStringList("command.levelledmobs.summon.level-limited.min");
                messages = Utils.replaceAllInList(messages, "%prefix%", main.configUtils.getPrefix());
                messages = Utils.replaceAllInList(messages, "%minLevel%", minLevel + "");
                messages = Utils.colorizeAllInList(messages);
                messages.forEach(sender::sendMessage);
            }

            //int maxLevel = instance.configUtils.getMaxLevel(entityType, location.getWorld(), true, null, CreatureSpawnEvent.SpawnReason.CUSTOM);
            if (level > maxLevel && !sender.hasPermission("levelledmobs.command.summon.bypass-level-limit") && !override) {
                level = maxLevel;

                List<String> messages = main.messagesCfg.getStringList("command.levelledmobs.summon.level-limited.max");
                messages = Utils.replaceAllInList(messages, "%prefix%", main.configUtils.getPrefix());
                messages = Utils.replaceAllInList(messages, "%maxLevel%", maxLevel + "");
                messages = Utils.colorizeAllInList(messages);
                messages.forEach(sender::sendMessage);
            }

            if (summonType == SummonType.HERE) {
                location = addVarianceToLocation(location);
            }

            if (summonType == SummonType.HERE || summonType == SummonType.AT_PLAYER) {
                final int distFromPlayer = main.settingsCfg.getInt("summon-command-spawn-distance-from-player", 5);
                if (distFromPlayer > 0) {
                    int useDistFromPlayer = distFromPlayer;
                    final double direction = target.getEyeLocation().getYaw();
                    final Location origLocation = location;
                    // try up to 50 times to find a open spot to spawn the mob.  Keep getting closer to the player if needed
                    for (int i = 0; i < 50; i++) {
                        useDistFromPlayer -= i;
                        if (useDistFromPlayer <= 0) break;
                        int newX = origLocation.getBlockX();
                        int newZ = origLocation.getBlockZ();
                        if (direction >= 225.0D && direction <= 314.9D) newX += useDistFromPlayer;
                        if (direction >= 45.0D && direction <= 134.9D) newX -= useDistFromPlayer;
                        if (direction >= 135.0D && direction <= 224.9D) newZ -= useDistFromPlayer;
                        if (direction >= 315.0D || direction <= 44.9D) newZ += useDistFromPlayer;
                        location = new Location(location.getWorld(), newX, location.getBlockY(), newZ);
                        Location location_YMinus1 = new Location(location.getWorld(), newX, location.getBlockY() - 1, newZ);
                        if (location.getBlock().isPassable() && location_YMinus1.getBlock().isPassable())
                            break; // found an open spot
                    }
                }
            }

            for (int i = 0; i < amount; i++) {
                Entity entity = Objects.requireNonNull(location.getWorld()).spawnEntity(location, entityType);
                final int mobLevel = main.levelManager.creatureSpawnListener.processMobSpawn(
                        (LivingEntity) entity, CreatureSpawnEvent.SpawnReason.CUSTOM, level, MobProcessReason.SUMMON, override);
                if (mobLevel >= 0 && main.settingsCfg.getBoolean("use-custom-item-drops-for-mobs"))
                    main.levelManager.creatureSpawnListener.processMobEquipment((LivingEntity) entity, mobLevel);
            }

            switch (summonType) {
                case HERE:
                    List<String> hereSuccessmessages = main.messagesCfg.getStringList("command.levelledmobs.summon.here.success");
                    hereSuccessmessages = Utils.replaceAllInList(hereSuccessmessages, "%prefix%", main.configUtils.getPrefix());
                    hereSuccessmessages = Utils.replaceAllInList(hereSuccessmessages, "%amount%", amount + "");
                    hereSuccessmessages = Utils.replaceAllInList(hereSuccessmessages, "%level%", level + "");
                    hereSuccessmessages = Utils.replaceAllInList(hereSuccessmessages, "%entity%", entityType.toString());
                    hereSuccessmessages = Utils.colorizeAllInList(hereSuccessmessages);
                    hereSuccessmessages.forEach(sender::sendMessage);
                    break;

                case AT_LOCATION:
                    List<String> atLocationSuccessMessages = main.messagesCfg.getStringList("command.levelledmobs.summon.atLocation.success");
                    atLocationSuccessMessages = Utils.replaceAllInList(atLocationSuccessMessages, "%prefix%", main.configUtils.getPrefix());
                    atLocationSuccessMessages = Utils.replaceAllInList(atLocationSuccessMessages, "%amount%", amount + "");
                    atLocationSuccessMessages = Utils.replaceAllInList(atLocationSuccessMessages, "%level%", level + "");
                    atLocationSuccessMessages = Utils.replaceAllInList(atLocationSuccessMessages, "%entity%", entityType.toString());
                    atLocationSuccessMessages = Utils.replaceAllInList(atLocationSuccessMessages, "%x%", location.getBlockX() + "");
                    atLocationSuccessMessages = Utils.replaceAllInList(atLocationSuccessMessages, "%y%", location.getBlockY() + "");
                    atLocationSuccessMessages = Utils.replaceAllInList(atLocationSuccessMessages, "%z%", location.getBlockZ() + "");
                    atLocationSuccessMessages = Utils.replaceAllInList(atLocationSuccessMessages, "%world%", location.getWorld().getName());
                    atLocationSuccessMessages = Utils.colorizeAllInList(atLocationSuccessMessages);
                    atLocationSuccessMessages.forEach(sender::sendMessage);
                    break;

                case AT_PLAYER:
                    List<String> atPlayerSuccessMessages = main.messagesCfg.getStringList("command.levelledmobs.summon.atPlayer.success");
                    atPlayerSuccessMessages = Utils.replaceAllInList(atPlayerSuccessMessages, "%prefix%", main.configUtils.getPrefix());
                    atPlayerSuccessMessages = Utils.replaceAllInList(atPlayerSuccessMessages, "%amount%", amount + "");
                    atPlayerSuccessMessages = Utils.replaceAllInList(atPlayerSuccessMessages, "%level%", level + "");
                    atPlayerSuccessMessages = Utils.replaceAllInList(atPlayerSuccessMessages, "%entity%", entityType.toString());
                    atPlayerSuccessMessages = Utils.replaceAllInList(atPlayerSuccessMessages, "%targetUsername%", target.getName());
                    atPlayerSuccessMessages = Utils.replaceAllInList(atPlayerSuccessMessages, "%targetDisplayname%", target.getDisplayName());
                    atPlayerSuccessMessages = Utils.colorizeAllInList(atPlayerSuccessMessages);
                    atPlayerSuccessMessages.forEach(sender::sendMessage);
                    break;
                default:
                    throw new IllegalStateException("Unexpected SummonType value of " + summonType.toString() + "!");
            }
        } else {
            List<String> messages = main.messagesCfg.getStringList("command.levelledmobs.summon.not-levellable");
            messages = Utils.replaceAllInList(messages, "%prefix%", main.configUtils.getPrefix());
            messages = Utils.replaceAllInList(messages, "%entity%", entityType.toString());
            messages = Utils.colorizeAllInList(messages);
            messages.forEach(sender::sendMessage);
        }
    }

    private Location getRelativeLocation(final CommandSender sender, final String xStr, final String yStr, final String zStr, final String worldName) {
        double x = 0, y = 0, z = 0;

        boolean xRelative = false, yRelative = false, zRelative = false;

        if (sender instanceof Player || sender instanceof BlockCommandSender) { //Player or Command blocks
            if (xStr.charAt(0) == '~') {
                if (sender instanceof Player) {
                    x = ((Player) sender).getLocation().getX();
                } else {
                    x = ((BlockCommandSender) sender).getBlock().getX();
                }

                if (xStr.length() > 1) {
                    double addition;
                    try {
                        addition = Double.parseDouble(xStr.substring(1));
                    } catch (NumberFormatException ex) {
                        return null;
                    }
                    x = x + addition;
                }

                xRelative = true;
            }
            if (yStr.charAt(0) == '~') {
                if (sender instanceof Player) {
                    y = ((Player) sender).getLocation().getY();
                } else {
                    y = ((BlockCommandSender) sender).getBlock().getY();
                }

                if (yStr.length() > 1) {
                    double addition;
                    try {
                        addition = Double.parseDouble(yStr.substring(1));
                    } catch (NumberFormatException ex) {
                        return null;
                    }

                    y = y + addition;
                }

                yRelative = true;
            }
            if (zStr.charAt(0) == '~') {
                if (sender instanceof Player) {
                    z = ((Player) sender).getLocation().getZ();
                } else {
                    z = ((BlockCommandSender) sender).getBlock().getZ();
                }

                if (zStr.length() > 1) {
                    double addition;
                    try {
                        addition = Double.parseDouble(zStr.substring(1));
                    } catch (NumberFormatException ex) {
                        return null;
                    }
                    z = z + addition;
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

        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return null;
        }

        return new Location(world, x, y, z);
    }

    private Location addVarianceToLocation(final Location oldLocation) {
        double min = 0.5, max = 2.5;

        for (int i = 0; i < 20; i++) {
            //Creates 3x new Random()s for a different seed each time
            double x = min + (max - min) * new Random().nextDouble();
            double y = min + (max - min) * new Random().nextDouble();
            double z = min + (max - min) * new Random().nextDouble();

            Location newLocation = new Location(oldLocation.getWorld(), x, y, z);
            if (newLocation.getBlock().isPassable() && newLocation.add(0, 1, 0).getBlock().isPassable()) {
                return newLocation;
            }
        }

        return oldLocation;
    }
}
