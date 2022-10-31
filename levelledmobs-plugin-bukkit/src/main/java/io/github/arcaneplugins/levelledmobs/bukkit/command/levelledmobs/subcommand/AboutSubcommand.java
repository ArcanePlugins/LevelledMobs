package io.github.arcaneplugins.levelledmobs.bukkit.command.levelledmobs.subcommand;

import dev.jorel.commandapi.CommandAPICommand;
import io.github.arcaneplugins.levelledmobs.bukkit.LevelledMobs;
import io.github.arcaneplugins.levelledmobs.bukkit.config.translations.Message;
import org.bukkit.plugin.PluginDescriptionFile;

public class AboutSubcommand {

    public static final CommandAPICommand INSTANCE =
        new CommandAPICommand("about")
            .withPermission("levelledmobs.command.levelledmobs.about")
            .executes((sender, args) -> {
                final PluginDescriptionFile pdf = LevelledMobs.getInstance().getDescription();

                Message.COMMAND_LEVELLEDMOBS_MAIN.sendTo(sender,
                    "%version%", pdf.getVersion(),
                    "%maintainers%", Message.joinDelimited(pdf.getAuthors())
                );
            });

}
