package me.lokka30.levelledmobs.bukkit.command.levelledmobs;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import me.lokka30.levelledmobs.bukkit.LevelledMobs;
import me.lokka30.levelledmobs.bukkit.command.BaseCommandWrapper;
import me.lokka30.levelledmobs.bukkit.command.CommandWrapper;
import me.lokka30.levelledmobs.bukkit.command.levelledmobs.subcommand.summon.SummonSubcommand;
import me.lokka30.levelledmobs.bukkit.command.levelledmobs.subcommand.summon.TestSubcommand;
import me.lokka30.levelledmobs.bukkit.config.translations.Message;
import me.lokka30.levelledmobs.bukkit.util.Log;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

/*
[description]
    This class contains the code which handles the `/levelledmobs` command, the primary command in
    LevelledMobs which allows the plugin to be managed. There are several subcommands which cater to
    various different functions that server administrators may wish to use, such as creating custom
    spawners and reloading the configuration files.

[command structure]
    index.. 0   1
    size... 1   2
            :   :
      cmd.. /lm |
         .. /lm <subcommand>
 */
public class LevelledMobsBaseCommand extends BaseCommandWrapper {
    public LevelledMobsBaseCommand(final @NotNull LevelledMobs main){
        super("levelledmobs");
        this.main = main;
        this.subcommands = new LinkedHashSet<>(Set.of(
                new SummonSubcommand(),
                new TestSubcommand(main)
        ));
    }

    private final LevelledMobs main;
    private final LinkedHashSet<CommandWrapper> subcommands;

    /* methods */

    @Override
    public void run(
        @NotNull CommandSender sender,
        @NotNull String[] args
    ) {
        if(!hasPerm(sender, "levelledmobs.command.levelledmobs", true))
            return;

        // make sure the user specified a command
        if (args.length <= 1) {
            final var pdf = LevelledMobs.getInstance().getDescription();

            Message.COMMAND_LEVELLEDMOBS_MAIN.sendTo(sender,
                "%version%", pdf.getVersion(),
                "%maintainers%", Message.joinDelimited(pdf.getAuthors())
            );
            return;
        }

        final var label = args[1].toLowerCase(Locale.ROOT);

        for(var subcommand : getSubcommands()) {
            if(subcommand.getLabels().contains(label)) {
                subcommand.run(sender, args);
                return;
            }
        }

        // user entered an invalid subcommand
        Message.COMMAND_LEVELLEDMOBS_INVALID_SUBCOMMAND.sendTo(sender,
            "%subcommand%", args[1]);
    }

    @Override
    public @NotNull List<String> suggest(
        @NotNull CommandSender sender,
        @NotNull String[] args
    ) {
        if(!hasPerm(sender, "levelledmobs.command.levelledmobs", false))
            return Collections.emptyList();

        if(args.length < 2) {
            return Collections.emptyList();
        } else if(args.length == 2) {
            final List<String> suggestions = new LinkedList<>();

            for(var subcommand : getSubcommands()) {
                if (subcommand.getLabels().isEmpty()){
                    // this happens if you forgot to call the super class constructor with your label name
                    Log.sev("command " + subcommand.getClass().getName() + " is missing required labels!");
                    continue;
                }
                suggestions.add(subcommand.getLabels().iterator().next());
            }

            return suggestions;
        } else {
            final var label = args[1].toLowerCase(Locale.ROOT);

            for(var subcommand : getSubcommands()) {
                if(subcommand.getLabels().contains(label)) {
                    /*
                    Note: LevelledMobs does not care if the sender is able to access the suggested
                    subcommands or not. We don't want to make excess calls to the permission plugin
                    since these calls are ran on the main thread. This can be changed once Treasury
                    offers a permissions API, which will allow asynchronous permission checking.
                     */
                    return subcommand.suggest(sender, args);
                }
            }

            return Collections.emptyList();
        }
    }

    /* getters and setters */

    public LinkedHashSet<CommandWrapper> getSubcommands() { return subcommands; }
}
