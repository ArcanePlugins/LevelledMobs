package io.github.lokka30.levelledmobs.commands.subcommands;

import io.github.lokka30.levelledmobs.LevelledMobs;
import net.md_5.bungee.api.ChatColor;
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

import java.util.Random;

public class SummonSubcommand implements Subcommand {

    // len:    1      2        3        4       5          6            7   8     9
    // arg:    0      1        2        3       4          5            6   7     8
    // lvlmobs summon <amount> <entity> <level> here
    // lvlmobs summon <amount> <entity> <level> atPlayer   <playername>
    // lvlmobs summon <amount> <entity> <level> atLocation <x>          <y> <z> [world]

    @Override
    public void parse(LevelledMobs instance, CommandSender sender, String label, String[] args) {
        if (!sender.hasPermission("levelledmobs.command.summon")) {
            instance.configUtils.sendNoPermissionMsg(sender);
            return;
        }

        if (args.length < 4) {
            sendMainUsage(sender, label);
            return;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[1]);
        } catch (NumberFormatException ex) {
            sender.sendMessage("Invalid amount " + args[1] + "!"); //TODO Customisable
            return;
        }

        EntityType entityType;
        try {
            entityType = EntityType.valueOf(args[2].toUpperCase());
        } catch (IllegalArgumentException ex) {
            sender.sendMessage("Invalid entity type " + args[2] + "!"); //TODO Customisable
            return;
        }

        int level;
        try {
            level = Integer.parseInt(args[3]);
        } catch (NumberFormatException ex) {
            sender.sendMessage("Invalid level " + args[3] + "!"); //TODO Customisable
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
                    sender.sendMessage("Invalid summon type " + args[4] + "!"); //TODO Customisable
                    return;
            }
        }

        if (summonType == SummonType.HERE) {
            if (sender instanceof Player) {
                final Player player = (Player) sender;
                summonMobs(instance, entityType, amount, sender, level, player.getLocation(), summonType, null);
            } else {
                sender.sendMessage("You must specify 'atPlayer' or 'atLocation' from console."); //TODO
            }
        } else if (summonType == SummonType.AT_PLAYER) {
            if (args.length == 6) {

                final Player target = Bukkit.getPlayer(args[5]);
                if (target == null) {
                    sender.sendMessage(args[5] + " isn't online."); //TODO
                    return;
                }

                summonMobs(instance, entityType, amount, sender, level, target.getLocation(), summonType, target);
            } else {
                sender.sendMessage("Usage: /" + label + " summon <amount> <entityType> <level> atPlayer <player>"); //TODO Customisable
            }
        } else {
            if (args.length > 9) {
                Location location = getRelativeLocation(sender, args[5], args[6], args[7], args[8]);

                if (location != null) {
                    summonMobs(instance, entityType, amount, sender, level, location, summonType, null);
                }
            }
        }
    }

    private enum SummonType {
        HERE,
        AT_PLAYER,
        AT_LOCATION
    }

    private void sendMainUsage(CommandSender sender, String label) { //TODO Customisable
        sender.sendMessage(ChatColor.GRAY + "Summon command syntax:");
        sender.sendMessage("/" + label + " summon <amount> <entity> <level> here");
        sender.sendMessage("/" + label + " summon <amount> <entity> <level> atPlayer <playername>");
        sender.sendMessage("/" + label + " summon <amount> <entity> <level> atLocation <x> <y> <z>");
    }

    private void summonMobs(LevelledMobs instance, EntityType entityType, int amount, CommandSender sender, int level, Location location, SummonType summonType, Player target) {
        if (instance.levelManager.isLevellable(entityType)) {
            //TODO complete

            int maxAmount = 100; //TODO Customisable
            if (amount > maxAmount) {
                amount = maxAmount;
                sender.sendMessage("Amount limited to " + maxAmount + " mobs."); //TODO customisable
            }

            assert location.getWorld() != null;
            int minLevel = instance.configUtils.getMinLevel(entityType, location.getWorld());
            if (level < minLevel) {
                level = minLevel;
                sender.sendMessage("Level limited to minimum of " + level + "."); //TODO Customsiable
            }

            int maxLevel = instance.configUtils.getMaxLevel(entityType, location.getWorld());
            if (level > maxLevel) {
                level = maxLevel;
                sender.sendMessage("Level limited to a maximum of " + level + "."); //TODO Customisable
            }

            if (summonType == SummonType.HERE) {
                location = addVarianceToLocation(location);
            }

            for (int i = 0; i < amount; i++) {
                assert location.getWorld() != null;
                Entity entity = location.getWorld().spawnEntity(location, entityType);
                instance.levelManager.creatureSpawnListener.processMobSpawn((LivingEntity) entity, CreatureSpawnEvent.SpawnReason.CUSTOM, level);
            }

            switch (summonType) {
                case HERE:
                    sender.sendMessage("Spawned " + amount + "x Lvl." + level + " " + entityType.toString() + "(s) at your location.");
                    break;
                case AT_LOCATION:
                    assert location.getWorld() != null;
                    sender.sendMessage("Spawned " + amount + "x Lvl." + level + " " + entityType.toString() + "(s) at X:" + location.getBlockX() + " Y:" + location.getBlockY() + " Z:" + location.getBlockZ() + " in world " + location.getWorld().getName() + ".");
                    break;
                case AT_PLAYER:
                    sender.sendMessage("Spawned " + amount + "x Lvl." + level + " " + entityType.toString() + "(s) at " + target.getName() + "'s location.");
                    break;
                default:
                    throw new IllegalStateException("Unexpected SummonType value of " + summonType.toString() + "!");
            }
        } else {
            sender.sendMessage(entityType.toString() + " is not levellable.");
        }
    }

    private Location getRelativeLocation(CommandSender sender, String xStr, String yStr, String zStr, String worldName) {
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

    private Location addVarianceToLocation(Location oldLocation) {
        double min = 0.5, max = 2.5;

        for (int i = 0; i < 20; i++) {
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
