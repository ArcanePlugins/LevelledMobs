package me.lokka30.levelledmobs.bukkit.commands.levelledmobs.subcommands;

import java.util.Collections;
import java.util.List;
import me.lokka30.levelledmobs.bukkit.LevelledMobs;
import me.lokka30.levelledmobs.bukkit.commands.CommandWrapper;
import me.lokka30.levelledmobs.bukkit.configs.translations.Message;
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
            Message.COMMAND_LEVELLEDMOBS_SUBCOMMAND_ABOUT_INVALID_USAGE.sendTo(sender);
            return;
        }

        final var pdf = LevelledMobs.getInstance().getDescription();

        Message.COMMAND_LEVELLEDMOBS_SUBCOMMAND_ABOUT_SUCCESS.sendTo(sender,
            "%version%", pdf.getVersion(),
            "%maintainers%", Message.joinDelimited(pdf.getAuthors())
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
