package io.github.arcaneplugins.levelledmobs.bukkit.command.levelledmobs.subcommand.routine;

import static org.bukkit.ChatColor.GRAY;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.WorldArgument;
import io.github.arcaneplugins.levelledmobs.bukkit.api.data.EntityDataUtil;
import io.github.arcaneplugins.levelledmobs.bukkit.data.InternalEntityDataUtil;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;

public final class UnlevelAllRoutine {

    public static CommandAPICommand createInstance1() {
        return new CommandAPICommand("unlevel-all")
            .withShortDescription("Unlevels all entities in all loaded worlds.")
            .withPermission("levelledmobs.command.levelledmobs.routine.unlevel-all")
            .executes((sender, args) -> {
                int affectedMobs = 0;
                int worldCount = 0;

                for(final World world : Bukkit.getWorlds()) {
                    worldCount++;

                    for(final LivingEntity lent : world.getLivingEntities()) {
                        if(lent.getType() == EntityType.PLAYER) continue;
                        if(!EntityDataUtil.isLevelled(lent, true)) continue;

                        InternalEntityDataUtil.unlevelMob(lent);
                        affectedMobs++;
                    }
                }

                sender.sendMessage(
                    "%sUnlevelled %s entities in %s worlds."
                        .formatted(GRAY, affectedMobs, worldCount)
                );
            });
    }

    public static CommandAPICommand createInstance2() {
        return new CommandAPICommand("unlevel-all")
            .withShortDescription("Unlevels all entities in a specified world.")
            .withPermission("levelledmobs.command.levelledmobs.routine.unlevel-all")
            .withArguments(
                new WorldArgument("world")
            )
            .executes((sender, args) -> {
                int affectedMobs = 0;

                final World world = (World) args[0];

                for(final LivingEntity lent : world.getLivingEntities()) {
                    if(lent.getType() == EntityType.PLAYER) continue;
                    if(!EntityDataUtil.isLevelled(lent, true)) continue;

                    InternalEntityDataUtil.unlevelMob(lent);
                    affectedMobs++;
                }

                sender.sendMessage(
                    "%sUnlevelled %s entities in world '%s'."
                        .formatted(GRAY, affectedMobs, world.getName())
                );
            });
    }

}
