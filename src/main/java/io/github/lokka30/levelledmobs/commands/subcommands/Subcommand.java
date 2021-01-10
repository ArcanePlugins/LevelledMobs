package io.github.lokka30.levelledmobs.commands.subcommands;

import io.github.lokka30.levelledmobs.LevelledMobs;
import org.bukkit.command.CommandSender;

public interface Subcommand {

    void parse(LevelledMobs instance, CommandSender sender, String label, String[] args);
}
