package io.github.lokka30.levelledmobs.commands;

import io.github.lokka30.levelledmobs.LevelledMobs;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class CLevelledMobs implements CommandExecutor {

    private LevelledMobs instance = LevelledMobs.getInstance();

    public boolean onCommand(final CommandSender s, final Command cmd, final String label, final String[] args) {
        s.sendMessage(instance.colorize("&8&m+-----------------------------------+"));
        s.sendMessage(instance.colorize("&7Running &a&lLevelledMobs&a v" + instance.getDescription().getVersion() + "&7."));
        s.sendMessage(instance.colorize("&7Developed for: &a" + instance.recommendedVersion + "&7."));
        s.sendMessage(instance.colorize("&8&m+-----------------------------------+"));
        s.sendMessage(instance.colorize("&a&lCommands:"));
        s.sendMessage(instance.colorize("&8&l \u00bb &f/LevelledMobs &8- &7&oview cmds and plugin info."));
        s.sendMessage(instance.colorize("&8&m+-----------------------------------+"));
        return true;
    }
}
