/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.commands.levelledmobs.subcommands;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.commands.CommandHandler;
import me.lokka30.microlib.maths.QuickTimer;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * @author lokka30
 * @since v4.0.0
 * This is a subcommand of the '/levelledmobs' command.
 * This subcommand provides various functions for LM developers or highly advanced LM users.
 * @see me.lokka30.levelledmobs.commands.levelledmobs.LevelledMobsCommand
 * @see CommandHandler
 */
public class ExecuteSubcommand implements CommandHandler.Subcommand {

    /*
    cmd:    /lm execute <method>
    arg:      -       0        1
    len:      0       1        2
     */

    /*
    TODO LIST:
        - Test if the subcommand is working correctly.
        - Add customisable messages to the run method.
        - Test if the customisable messages work correctly.
     */

    final HashSet<Executable> executables;
    final ArrayList<String> executablesLabels = new ArrayList<>();
    public ExecuteSubcommand() {
        // create executables list
        executables = new HashSet<>(Set.of(
                new TestOneExecutable(),
                new TestTwoExecutable()
        ));

        // add all executables labels
        executables.forEach(executable -> executablesLabels.addAll(executable.getLabels()));
    }

    @Nullable
    public ExecuteSubcommand.Executable getExecutable(@NotNull final String label) {
        for(Executable executable : executables) {
            if(executable.getLabels().contains(label)) {
                return executable;
            }
        }
        return null;
    }

    @Override
    public @NotNull String getMainLabel() {
        return "execeute";
    }

    final HashSet<String> subcommandLabels = new HashSet<>(Set.of("EXECUTE", "EXEC", "EXE"));
    @Override
    public @NotNull HashSet<String> getLabels() {
        return subcommandLabels;
    }

    @Override
    public @NotNull String getUsage() {
        return "<method>";
    }

    @Override
    public void run(@NotNull LevelledMobs main, @NotNull CommandSender sender, @NotNull String baseCommandLabel, @NotNull String subCommandLabel, @NotNull String[] args) {
        if(CommandHandler.CommandUtils.senderDoesNotHaveRequiredPermission(main, sender, "levelledmobs.command.levelledmobs.subcommand.advanced")) return;

        if(args.length == 1) {
            sender.sendMessage("Please specify an executable to run - try '/" + baseCommandLabel + " " + subCommandLabel + " <method>'.");
            sender.sendMessage("Some executables can be very dangerous to run. Use this subcommand under guidance from a LM developer only!");
            sender.sendMessage("Available executables: " + executablesLabels);
        } else {
            final Executable executable = getExecutable(args[1].toUpperCase(Locale.ROOT));
            if(executable == null) {
                sender.sendMessage("Invalid executable '" + args[1] + "!");
                sender.sendMessage("Available executables: " + executablesLabels);
            } else {
                sender.sendMessage("Running executable '" + args[1].toUpperCase(Locale.ROOT) + "'...");
                final QuickTimer timer = new QuickTimer();
                executable.run(main, sender, baseCommandLabel, args);
                sender.sendMessage("... complete, took " + timer.getTimer() + "ms.");
            }
        }
    }

    @Override
    public @NotNull List<String> getSuggestions(@NotNull LevelledMobs main, @NotNull CommandSender sender, @NotNull String baseCommandLabel, @NotNull String subCommandLabel,  @NotNull String[] args) {

        if(args.length == 2) {
            return executablesLabels;
        }

        return Collections.emptyList();
    }

    private interface Executable {

        @NotNull
        HashSet<String> getLabels();

        void run(@NotNull LevelledMobs main, @NotNull CommandSender sender, @NotNull String label, @NotNull String[] args);
    }

    private static class TestOneExecutable implements Executable {

        @Override
        public @NotNull HashSet<String> getLabels() {
            return new HashSet<>(Set.of("TEST1"));
        }

        @Override
        public void run(@NotNull LevelledMobs main, @NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) {
            sender.sendMessage("Test 1 successful.");
        }
    }

    private static class TestTwoExecutable implements Executable {

        @Override
        public @NotNull HashSet<String> getLabels() {
            return new HashSet<>(Set.of("TEST2"));
        }

        @Override
        public void run(@NotNull LevelledMobs main, @NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) {
            sender.sendMessage("Test 2 successful.");
        }
    }
}
