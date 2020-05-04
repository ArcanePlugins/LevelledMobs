package io.github.lokka30.levelledmobs.commands;

import io.github.lokka30.levelledmobs.LevelledMobs;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class LevelledMobsCommand implements CommandExecutor {

    private LevelledMobs instance;
    public LevelledMobsCommand(final LevelledMobs instance) {
        this.instance = instance;
    }

    public boolean onCommand(@NotNull final CommandSender sender, @NotNull final Command command, @NotNull final String label, final String[] args) {
        if (args.length == 0) {
            sender.sendMessage(" ");
            sender.sendMessage(instance.colorize("&b&lLevelledMobs: &7Available commands:"));
            sender.sendMessage(instance.colorize("&8 &m->&3 /levelledMobs &8- &7view plugin commands."));
            sender.sendMessage(instance.colorize("&8 &m->&3 /levelledMobs killall [world] &8- &7butcher levellable mobs."));
            sender.sendMessage(instance.colorize("&8 &m->&3 /levelledMobs info &8- &7view plugin information."));
            sender.sendMessage(" ");
        } else {
            if (args[0].equalsIgnoreCase("killall")) {
                if (sender instanceof Player && !sender.hasPermission("levelledmobs.killall")) {
                    sender.sendMessage(instance.colorize("&b&lLevelledMobs: &7You don't have access to that."));
                    return true;
                } else {
                    switch (args.length) {
                        case 1:
                            if (sender instanceof Player) {
                                final Player p = (Player) sender;
                                int killed = 0;
                                final World w = p.getWorld();
                                for (Entity e : w.getEntities()) {
                                    if (e instanceof LivingEntity) {
                                        final LivingEntity livingEntity = (LivingEntity) e;
                                        if (instance.levelManager.isLevellable(livingEntity)) {
                                            e.remove();
                                            killed++;
                                        }
                                    }
                                }
                                sender.sendMessage(instance.colorize("&b&lLevelledMobs: &7Killed &b" + killed + " levellable entities &7in world '&b" + w.getName() + "&7'."));
                            } else {
                                sender.sendMessage(instance.colorize("&b&lLevelledMobs: &7Usage (console): &b/" + label + " killAll <world>"));
                            }
                            return true;
                        case 2:
                            int killed = 0;

                            if (Bukkit.getWorld(args[1]) == null) {
                                sender.sendMessage(instance.colorize("&b&lLevelledMobs: &7Invalid world &b" + args[1] + "&7."));
                            } else {
                                final World w = Bukkit.getWorld(args[1]);
                                assert w != null;
                                for (Entity e : w.getEntities()) {
                                    if (e instanceof LivingEntity) {
                                        final LivingEntity livingEntity = (LivingEntity) e;
                                        if (instance.levelManager.isLevellable(livingEntity)) {
                                            e.remove();
                                            killed++;
                                        }
                                    }
                                }
                                sender.sendMessage(instance.colorize("&b&lLevelledMobs: &7Killed &b" + killed + "&7 levellable entities in world '&b" + w.getName() + "&7'."));
                            }
                            return true;
                        default:
                            sender.sendMessage(instance.colorize("&b&lLevelledMobs: &7Usage: &b/levelledmobs killall [world]"));
                            return true;
                    }
                }
            } else if (args[0].equalsIgnoreCase("info")) {
                if (args.length == 1) {
                    sender.sendMessage(" ");
                    sender.sendMessage(instance.colorize("&7Running &bLevelledMobs v" + instance.getDescription().getVersion() + "&7, designed to run on &bMC v" + instance.utils.getRecommendedServerVersion() + "&7."));
                    sender.sendMessage(instance.colorize("&7This resource is available on &bSpigotMC.org&7."));
                    sender.sendMessage(" ");
                } else {
                    sender.sendMessage(instance.colorize("&b&lLevelledMobs: &7Usage: &b/" + label + " info"));
                }
                return true;
            }
            sender.sendMessage(instance.colorize("&b&lLevelledMobs: &7For a list of available commands, run &b/" + label + "&7."));
        }
        return true;
    }
}
