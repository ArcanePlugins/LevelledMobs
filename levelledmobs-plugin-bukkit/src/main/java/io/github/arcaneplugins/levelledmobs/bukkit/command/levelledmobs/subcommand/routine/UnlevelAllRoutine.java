package io.github.arcaneplugins.levelledmobs.bukkit.command.levelledmobs.subcommand.routine;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.TextArgument;
import io.github.arcaneplugins.levelledmobs.bukkit.api.data.EntityDataUtil;
import io.github.arcaneplugins.levelledmobs.bukkit.data.InternalEntityDataUtil;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;

public final class UnlevelAllRoutine {

    public static CommandAPICommand createInstance1() {
        return new CommandAPICommand("unlevel-all")
            .withShortDescription("Unlevels all mobs on the server")
            .withPermission("levelledmobs.command.levelledmobs.routine.unlevel-all")
            .executes((sender, args) -> {
                int affectedMobs = 0;
                int worlds = 0;

                for(final World world : Bukkit.getWorlds()) {
                    worlds++;

                    for(final LivingEntity lent : world.getLivingEntities()) {
                        if(!EntityDataUtil.isLevelled(lent, true)) continue;

                        InternalEntityDataUtil.unlevelMob(lent);
                        affectedMobs++;
                    }
                }

                sender.sendMessage(
                    "Unlevelled %s mobs in %s worlds."
                        .formatted(affectedMobs, worlds)
                );
            });
    }

    public static CommandAPICommand createInstance2() {
        return new CommandAPICommand("unlevel-all")
            .withShortDescription("Unlevels all mobs in specified worlds")
            .withPermission("levelledmobs.command.levelledmobs.routine.unlevel-all")
            .withArguments(
                new TextArgument("worlds")
            )
            .executes((sender, args) -> {
                int affectedMobs = 0;

                final String[] worldNames = ((String) args[0]).split(",");

                worldIter:
                for(final World world : Bukkit.getWorlds().stream()
                    .filter(world -> Arrays.stream(worldNames).anyMatch(worldName -> worldName.equalsIgnoreCase(world.getName())))
                    .collect(Collectors.toUnmodifiableSet())
                ) {
                    for(final LivingEntity lent : world.getLivingEntities()) {
                        if(!EntityDataUtil.isLevelled(lent, true)) continue;

                        InternalEntityDataUtil.unlevelMob(lent);
                        affectedMobs++;
                    }
                }

                sender.sendMessage(
                    "Unlevelled %s mobs in %s worlds."
                        .formatted(affectedMobs, worldNames.length)
                );
            });
    }

}
