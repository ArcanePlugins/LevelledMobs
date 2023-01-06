package io.github.arcaneplugins.levelledmobs.bukkit.command.levelledmobs.subcommand.routine;

import static org.bukkit.ChatColor.GREEN;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.WorldArgument;
import org.bukkit.World;

public class TestRoutine {

    public static CommandAPICommand createInstance1() {
        return new CommandAPICommand("test1")
            .withShortDescription("Generic test routine")
            .withPermission("levelledmobs.command.levelledmobs.routine.test")
            .withArguments(new WorldArgument("world"))
            .executes((sender, args) -> {
                final World world = (World) args[0];
                sender.sendMessage(GREEN + "World: " + world.getName());
            });
    }

    public static CommandAPICommand createInstance2() {
        return new CommandAPICommand("test2")
            .withShortDescription("Generic test routine")
            .withPermission("levelledmobs.command.levelledmobs.routine.test")
            .executes((sender, args) -> {
                sender.sendMessage(GREEN + "Works without world argument.");
            });
    }

}
