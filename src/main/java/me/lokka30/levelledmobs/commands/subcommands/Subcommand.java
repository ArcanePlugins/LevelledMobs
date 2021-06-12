package me.lokka30.levelledmobs.commands.subcommands;

import me.lokka30.levelledmobs.LevelledMobs;
import org.bukkit.command.CommandSender;

import java.util.List;

/**
 * Provides the interface for parsing commands sent to LevelledMobs
 *
 * @author lokka30
 */
public interface Subcommand {

    void parseSubcommand(final LevelledMobs main, final CommandSender sender, final String label, final String[] args);

    List<String> parseTabCompletions(final LevelledMobs main, final CommandSender sender, final String[] args);
}
