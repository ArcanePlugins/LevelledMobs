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
public class AdvancedSubcommand implements CommandHandler.Subcommand {

    /*
    cmd:    /lm advanced <method>
    arg:      -        0        1
    len:      0        1        2
     */

    final HashSet<AdvancedMethod> advancedMethods;
    final ArrayList<String> advancedMethodsLabels = new ArrayList<>();
    public AdvancedSubcommand() {
        // create methods list
        advancedMethods = new HashSet<>(Set.of(
                new TestOneAdvancedMethod(),
                new TestTwoAdvancedMethod()
        ));

        // add methods labels
        advancedMethods.forEach(advancedMethod -> advancedMethodsLabels.addAll(advancedMethod.getLabels()));
    }

    @Nullable
    public AdvancedMethod getAdvancedMethod(@NotNull final String label) {
        for(AdvancedMethod advancedMethod : advancedMethods) {
            if(advancedMethod.getLabels().contains(label)) {
                return advancedMethod;
            }
        }
        return null;
    }

    final HashSet<String> subcommandLabels = new HashSet<>(Set.of("ADVANCED"));
    @Override
    public @NotNull HashSet<String> getLabels() {
        return subcommandLabels;
    }

    @Override
    public void run(@NotNull LevelledMobs main, @NotNull CommandSender sender, @NotNull String baseCommandLabel, @NotNull String subCommandLabel, @NotNull String[] args) {
        if(CommandHandler.CommandUtils.senderDoesNotHaveRequiredPermission(main, sender, "levelledmobs.command.levelledmobs.subcommand.advanced")) return;

        if(args.length == 1) {
            sender.sendMessage("Please specify an advanced method to run - try '/" + baseCommandLabel + " " + subCommandLabel + " <method>'.");
            sender.sendMessage("Some advanced methods can be very dangerous to run. Use this subcommand under guidance from a LM developer only!");
            sender.sendMessage("Available advanced methods: " + advancedMethods);
        } else {
            final AdvancedMethod advancedMethod = getAdvancedMethod(args[1].toUpperCase(Locale.ROOT));
            if(advancedMethod == null) {
                sender.sendMessage("Invalid advanced method '" + args[1] + "! Available advanced methods: " + advancedMethodsLabels);
            } else {
                sender.sendMessage("Running advanced method '" + args[1] + "'...");
                final QuickTimer timer = new QuickTimer();
                advancedMethod.run(main, sender, baseCommandLabel, args);
                sender.sendMessage("... complete, took " + timer.getTimer() + "ms.");
            }
        }
    }

    @Override
    public @NotNull List<String> getSuggestions(@NotNull LevelledMobs main, @NotNull CommandSender sender, @NotNull String baseCommandLabel, @NotNull String subCommandLabel,  @NotNull String[] args) {

        if(args.length == 2) {
            return advancedMethodsLabels;
        }

        return Collections.emptyList();
    }

    private interface AdvancedMethod {

        @NotNull
        HashSet<String> getLabels();

        void run(@NotNull LevelledMobs main, @NotNull CommandSender sender, @NotNull String label, @NotNull String[] args);
    }

    private static class TestOneAdvancedMethod implements AdvancedMethod {

        @Override
        public @NotNull HashSet<String> getLabels() {
            return new HashSet<>(Set.of("TEST1"));
        }

        @Override
        public void run(@NotNull LevelledMobs main, @NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) {
            sender.sendMessage("Test 1");
        }
    }

    private static class TestTwoAdvancedMethod implements AdvancedMethod {

        @Override
        public @NotNull HashSet<String> getLabels() {
            return new HashSet<>(Set.of("TEST2"));
        }

        @Override
        public void run(@NotNull LevelledMobs main, @NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) {
            sender.sendMessage("Test 2");
        }
    }
}
