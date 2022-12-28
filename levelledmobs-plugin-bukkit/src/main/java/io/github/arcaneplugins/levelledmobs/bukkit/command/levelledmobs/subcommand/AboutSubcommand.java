package io.github.arcaneplugins.levelledmobs.bukkit.command.levelledmobs.subcommand;

import dev.jorel.commandapi.CommandAPICommand;
import io.github.arcaneplugins.levelledmobs.bukkit.LevelledMobs;
import io.github.arcaneplugins.levelledmobs.bukkit.config.translations.Message;
import org.bukkit.plugin.PluginDescriptionFile;

public final class AboutSubcommand {

    public static CommandAPICommand createInstance() {
        return new CommandAPICommand("about")
            .withPermission("levelledmobs.command.levelledmobs.about")
            .withShortDescription("View information about this version of LevelledMobs.")
            .withFullDescription("Displays miscellaneous information about this version of " +
                "LevelledMobs, such as the installed version, where to ask for help, and a list " +
                "LevelledMobs maintainers.")
            .executes((sender, args) -> {
                final PluginDescriptionFile pdf = LevelledMobs.getInstance().getDescription();

                Message.COMMAND_LEVELLEDMOBS_MAIN.sendTo(sender,
                    "%version%", pdf.getVersion(),
                    "%maintainers%", Message.joinDelimited(pdf.getAuthors())
                );
            });
    }

}
