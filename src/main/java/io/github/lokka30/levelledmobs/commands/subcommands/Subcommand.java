package io.github.lokka30.levelledmobs.commands.subcommands;

import io.github.lokka30.levelledmobs.LevelledMobs;
import org.bukkit.command.CommandSender;

import java.util.List;

public interface Subcommand {

    void parseSubcommand(LevelledMobs instance, CommandSender sender, String label, String[] args);

    List<String> parseTabCompletions(LevelledMobs instance, CommandSender sender, String[] args);
}
