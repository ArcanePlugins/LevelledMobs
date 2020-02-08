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

public class CLevelledMobs implements CommandExecutor {

    private LevelledMobs instance = LevelledMobs.getInstance();

    public boolean onCommand(final CommandSender s, final Command cmd, final String label, final String[] args) {
        if (args.length == 0) {
            s.sendMessage(instance.colorize("&8&m+-----------------------------------+"));
            s.sendMessage(instance.colorize("&7Running &a&lLevelledMobs&a v" + instance.getDescription().getVersion() + "&7."));
            s.sendMessage(instance.colorize("&7Developed for: &a" + instance.recommendedVersion + "&7."));
            s.sendMessage(instance.colorize("&8&m+-----------------------------------+"));
            s.sendMessage(instance.colorize("&a&lCommands:"));
            s.sendMessage(instance.colorize("&8&l \u00bb &f/levelledMobs &8- &7&oview cmds and plugin info."));
            s.sendMessage(instance.colorize("&8&l \u00bb &f/levelledMobs killAll [world] &8- &7&obutcher levellable mobs."));
            s.sendMessage(instance.colorize("&8&m+-----------------------------------+"));
            return true;
        } else {
            if (args[0].equalsIgnoreCase("killall")) {
                if (s instanceof Player && !s.hasPermission("levelledmobs.killall")) {
                    s.sendMessage(instance.colorize("&a&lLevelledMobs: &7You don't have access to that."));
                    return true;
                } else {
                    switch (args.length) {
                        case 1:
                            if (s instanceof Player) {
                                final Player p = (Player) s;
                                int killed = 0;
                                final World w = p.getWorld();
                                for (Entity e : w.getEntities()) {
                                    if (e instanceof LivingEntity) {
                                        final LivingEntity livingEntity = (LivingEntity) e;
                                        if (instance.isLevellable(livingEntity)) {
                                            e.remove();
                                            killed++;
                                        }
                                    }
                                }
                                s.sendMessage(instance.colorize("&a&lLevelledMobs: &7You killed &a" + killed + " entities &7in the world &a" + w.getName() + "&7."));
                            } else {
                                s.sendMessage(instance.colorize("&a&lLevelledMobs: &7Usage (console): &a/levelledMobs killAll <world>"));
                            }
                            return true;
                        case 2:
                            int killed = 0;

                            if (Bukkit.getWorld(args[1]) == null) {
                                s.sendMessage(instance.colorize("&a&lLevelledMobs: &7Invalid world &a" + args[1] + "&7."));
                            } else {
                                final World w = Bukkit.getWorld(args[1]);
                                assert w != null;
                                for (Entity e : w.getEntities()) {
                                    if (e instanceof LivingEntity) {
                                        final LivingEntity livingEntity = (LivingEntity) e;
                                        if (instance.isLevellable(livingEntity)) {
                                            e.remove();
                                            killed++;
                                        }
                                    }
                                }
                                s.sendMessage(instance.colorize("&a&lLevelledMobs: &7You killed &a" + killed + " entities &7in the world &a" + w.getName() + "&7."));
                            }
                            return true;
                        default:
                            s.sendMessage(instance.colorize("&a&lLevelledMobs: &7Usage: &a/levelledMobs killAll [world]"));
                            return true;
                    }
                }
            }
            s.sendMessage(instance.colorize("&a&lLevelledMobs: &7Unknown subcommand. For a list of commands, try &a/levelledMobs&7."));
            return true;
        }
    }
}
