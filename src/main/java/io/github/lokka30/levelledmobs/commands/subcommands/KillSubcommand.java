package io.github.lokka30.levelledmobs.commands.subcommands;

import io.github.lokka30.levelledmobs.LevelledMobs;
import io.github.lokka30.levelledmobs.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class KillSubcommand implements Subcommand {

    @Override
    public void parseSubcommand(LevelledMobs instance, CommandSender sender, String label, String[] args) {
        if (!sender.hasPermission("levelledmobs.command.kill")) {
            instance.configUtils.sendNoPermissionMsg(sender);
            return;
        }

        // LEN  0    1    2    3
        // ARG  -    0    1    2
        // /lvlmobs kill all [world]
        // /lvlmobs kill near <radius>

        if (args.length > 1) {
            switch (args[1].toLowerCase()) {
                case "all":
                    if (!sender.hasPermission("levelledmobs.command.kill.all")) {
                        instance.configUtils.sendNoPermissionMsg(sender);
                        return;
                    }

                    if (args.length == 2) {
                        if (sender instanceof Player) {
                            final Player player = (Player) sender;
                            parseKillAll(sender, Collections.singletonList(player.getWorld()));
                        } else {
                            //TODO Console Usage
                            sender.sendMessage("Usage (console): /lvlmobs kill all <world/*>");
                        }
                    } else if (args.length == 3) {
                        //TODO Proceed

                        if (args[2].equals("*")) {
                            parseKillAll(sender, Bukkit.getWorlds());
                            return;
                        }

                        World world = Bukkit.getWorld(args[2]);
                        if (world == null) {
                            sender.sendMessage("Invalid world " + args[2] + "!");
                            return;
                        }

                        parseKillAll(sender, Collections.singletonList(world));
                    } else {
                        //TODO Usage
                        sender.sendMessage("Usage: /lvlmobs kill all [world/*]");
                    }

                    break;
                case "near":
                    if (!sender.hasPermission("levelledmobs.command.kill.near")) {
                        instance.configUtils.sendNoPermissionMsg(sender);
                        return;
                    }

                    if (args.length == 3) {
                        if (sender instanceof Player) {
                            Player player = (Player) sender;

                            int radius;
                            try {
                                radius = Integer.parseInt(args[2]);
                            } catch (NumberFormatException exception) {
                                sender.sendMessage("Invalid radius: " + args[2]); //TODO Customisable
                                return;
                            }

                            int killed = 0;
                            for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
                                if (entity instanceof LivingEntity) {
                                    final LivingEntity livingEntity = (LivingEntity) entity;
                                    if (livingEntity.getPersistentDataContainer().has(instance.levelManager.isLevelledKey, PersistentDataType.STRING)) {
                                        livingEntity.setHealth(0.0);
                                        killed++;
                                    }
                                }
                            }

                            sender.sendMessage("Killed " + killed + " levelled entities within a radius of " + radius + " blocks"); //TODO Customisable
                        } else {
                            sender.sendMessage("Console can't use this command."); //TODO Customisable
                        }
                    } else {
                        sender.sendMessage("Usage: /lvlmobs kill near <radius>"); //TODO Customisable
                    }

                    break;
                default:
                    sendUsageMsg(sender, label);
            }
        } else {
            sendUsageMsg(sender, label);
        }
    }

    @Override
    public List<String> parseTabCompletions(LevelledMobs instance, CommandSender sender, String[] args) {

        if (args.length == 2) {
            return Arrays.asList("all", "near");
        }

        if (args.length == 3) {
            if (args[1].equalsIgnoreCase("all")) {
                if (sender.hasPermission("levelledmobs.command.kill.all")) {
                    return Collections.singletonList("worldNameHere"); //TODO Replace with 'Enabled Worlds' list.
                }
            } else if (args[1].equalsIgnoreCase("near")) {
                if (sender.hasPermission("levelledmobs.command.kill.near")) {
                    return Utils.oneToNine;
                }
            }
        }

        // Nothing to suggest.
        return null;
    }

    private void sendUsageMsg(CommandSender sender, String label) {
        sender.sendMessage("Usage: /" + label + " kill <all/near>"); //TODO Customisable
    }

    private void parseKillAll(CommandSender sender, List<World> worlds) {
        int killedAmount = 0;

        for (World world : worlds) {
            for (Entity entity : world.getEntities()) {
                if (entity instanceof LivingEntity) {
                    LivingEntity livingEntity = (LivingEntity) entity;
                    if (livingEntity.hasMetadata("levelled")) {
                        livingEntity.setHealth(0.0);
                        killedAmount++;
                    }
                }
            }
        }

        //TODO
        sender.sendMessage("Killed " + killedAmount + " levelled mobs in " + worlds.size() + " world(s)");
    }
}
