package me.lokka30.levelledmobs.bukkit.commands.levelledmobs.subcommands;

import static me.lokka30.levelledmobs.bukkit.utils.TempConst.PREFIX_SEV;
import static org.bukkit.ChatColor.AQUA;
import static org.bukkit.ChatColor.BOLD;
import static org.bukkit.ChatColor.DARK_GRAY;
import static org.bukkit.ChatColor.GRAY;

import java.util.Collections;
import java.util.List;
import me.lokka30.levelledmobs.bukkit.LevelledMobs;
import me.lokka30.levelledmobs.bukkit.commands.CommandWrapper;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

/*
[command structure]
    index.. 0   1
    size... 1   2
            :   :
          - /lm |
          - /lm about
 */
public final class AboutSubcommand extends CommandWrapper {

    public AboutSubcommand() {
        super("about", "info", "information", "version", "ver");
    }

    @Override
    public void run(@NotNull CommandSender sender, @NotNull String[] args) {
        if(!hasPerm(sender, "levelledmobs.command.levelledmobs.about", true))
            return;

        if(args.length != 2) {
            sender.sendMessage(PREFIX_SEV + "Invalid usage; try '" + AQUA + "/lm about" + GRAY +
                "'.");
            return;
        }

        final var pdf = LevelledMobs.getInstance().getDescription();
        final var bullet = DARK_GRAY + " • " + GRAY;

        sender.sendMessage(
            "" + AQUA + BOLD + "LevelledMobs" + AQUA + " v" + pdf.getVersion(),
            bullet + "Spigot: " + DARK_GRAY + "https://spigotmc.org/resources/74304",
            bullet + "Wiki: " + DARK_GRAY + "https://github.com/lokka30/LevelledMobs/Wiki",
            bullet + "Source: " + DARK_GRAY + "https://github.com/lokka30/LevelledMobs",
            bullet + "Credits: " + DARK_GRAY + "https://github.com/lokka30/LevelledMobs/wiki/Credits"
        );
    }

    @Override
    public @NotNull List<String> suggest(
        @NotNull CommandSender sender,
        @NotNull String[] args
    ) {
        return Collections.emptyList();
    }
}
