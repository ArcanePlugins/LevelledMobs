package io.github.lokka30.levelledmobs.commands;

import io.github.lokka30.levelledmobs.LevelledMobs;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
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
            sender.sendMessage(instance.messageMethods.prefix(instance.PREFIX, "Available commands:"));
            sender.sendMessage(instance.messageMethods.colorize("&8 &m->&3 /levelledMobs &8- &7view plugin commands."));
            sender.sendMessage(instance.messageMethods.colorize("&8 &m->&3 /levelledMobs killall [world] &8- &7butcher levellable mobs."));
            sender.sendMessage(instance.messageMethods.colorize("&8 &m->&3 /levelledMobs summon <...> &8- &7summon a levelled mob."));
            sender.sendMessage(instance.messageMethods.colorize("&8 &m->&3 /levelledMobs reload &8- &7reload the settings file into memory."));
            sender.sendMessage(instance.messageMethods.colorize("&8 &m->&3 /levelledMobs info &8- &7view plugin information."));
            sender.sendMessage(" ");
        } else {
            if (args[0].equalsIgnoreCase("killall")) {
                if (!sender.hasPermission("levelledmobs.killall")) {
                    sender.sendMessage(instance.messageMethods.prefix(instance.PREFIX, "You don't have access to that."));
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
                                sender.sendMessage(instance.messageMethods.prefix(instance.PREFIX, "Killed &b" + killed + " levellable entities &7in world '&b" + w.getName() + "&7'."));
                            } else {
                                sender.sendMessage(instance.messageMethods.prefix(instance.PREFIX, "Usage (console): &b/" + label + " killAll <world>"));
                            }
                            return true;
                        case 2:
                            int killed = 0;

                            if (Bukkit.getWorld(args[1]) == null) {
                                sender.sendMessage(instance.messageMethods.prefix(instance.PREFIX, "Invalid world &b" + args[1] + "&7."));
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
                                sender.sendMessage(instance.messageMethods.prefix(instance.PREFIX, "Killed &b" + killed + "&7 levellable entities in world '&b" + w.getName() + "&7'."));
                            }
                            return true;
                        default:
                            sender.sendMessage(instance.messageMethods.prefix(instance.PREFIX, "Usage: &b/levelledmobs killall [world]"));
                            return true;
                    }
                }
            } else if (args[0].equalsIgnoreCase("reload")) {
                if (sender.hasPermission("levelledmobs.reload")) {
                    sender.sendMessage(instance.messageMethods.prefix(instance.PREFIX, "Reload started..."));
                    instance.loadFiles();
                    sender.sendMessage(instance.messageMethods.prefix(instance.PREFIX, "...reload complete."));
                } else {
                    sender.sendMessage(instance.messageMethods.prefix(instance.PREFIX, "You don't have access to that."));
                }
                return true;
            } else if (args[0].equalsIgnoreCase("summon")) {
                if (sender.hasPermission("levelledmobs.summon")) {
                    sender.sendMessage(instance.messageMethods.prefix(instance.PREFIX, "Please avoid using this command as it isn't complete yet."));

                    if (args.length == 3) { //Spawn the mob at their current coords (must be player)
                        if (sender instanceof Player) {
                            final Player player = (Player) sender;
                            EntityType entityType;
                            int level;

                            try {
                                entityType = EntityType.valueOf(args[1]);
                            } catch (IllegalStateException ex) {
                                //TODO UNKNOWN ENTITY TYPE
                                return true;
                            }

                            try {
                                level = Integer.parseInt(args[2]);
                            } catch (NumberFormatException ex) {
                                //TODO INVALID INTEGER
                                return true;
                            }

                            player.sendMessage("entitytype = " + entityType.toString() + ", level = " + level);

                            //TODO SPAWN THE MOB WITH ENTITY TYPE AND LEVEL
                        } else {
                            //TODO CONSOLE USAGE.
                            sender.sendMessage("console usage");
                        }
                    } else if (args.length == 4) { //Spawn the mob at the specified player (console or player can use)
                        //TODO AT SPECIFIED PLAYER
                        sender.sendMessage("summon at specified player");
                    } else if (args.length == 6) { //Spawn the mob at the specified coordinates (console or player can use)
                        //TODO AT SPECIFIED COORDS
                        sender.sendMessage("summon at specified coords");
                    } else {
                        //TODO USAGE
                        sender.sendMessage("usage");
                    }
                } else {
                    sender.sendMessage(instance.messageMethods.prefix(instance.PREFIX, "You don't have access to that."));
                }
                return true;
            } else if (args[0].equalsIgnoreCase("info")) {
                if (args.length == 1) {
                    sender.sendMessage(" ");
                    sender.sendMessage(instance.messageMethods.colorize("&b&lLevelledMobs&b v" + instance.getDescription().getVersion() + "&8 | &7Developed by &3&olokka30&7."));
                    sender.sendMessage(" ");
                    sender.sendMessage(instance.messageMethods.colorize("&f&nSpigotMC Resource Link:"));
                    sender.sendMessage(instance.messageMethods.colorize("&8https://www.spigotmc.org/resources/%E2%99%A6-levelledmobs-%E2%99%A6-for-1-15-x.74304/"));
                    sender.sendMessage(" ");
                } else {
                    sender.sendMessage(instance.messageMethods.prefix(instance.PREFIX, "Usage: &b/" + label + " info"));
                }
                return true;
            } else {
                sender.sendMessage(instance.messageMethods.prefix(instance.PREFIX, "For a list of available commands, please run &b/" + label + "&7."));
            }
        }
        return true;
    }
}
