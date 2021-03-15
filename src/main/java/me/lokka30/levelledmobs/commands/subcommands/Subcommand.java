package me.lokka30.levelledmobs.commands.subcommands;

import me.lokka30.levelledmobs.LevelledMobs;
import org.bukkit.command.CommandSender;

import java.util.List;

/**
 * @author lokka30
 */
public interface Subcommand {

    void parseSubcommand(LevelledMobs main, CommandSender sender, String label, String[] args);

    List<String> parseTabCompletions(LevelledMobs main, CommandSender sender, String[] args);
}
