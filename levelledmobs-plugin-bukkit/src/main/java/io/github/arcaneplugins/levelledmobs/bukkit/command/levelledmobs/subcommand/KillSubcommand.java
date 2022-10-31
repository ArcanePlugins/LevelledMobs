package io.github.arcaneplugins.levelledmobs.bukkit.command.levelledmobs.subcommand;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.EntitySelector;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import io.github.arcaneplugins.levelledmobs.bukkit.api.data.EntityDataUtil;
import io.github.arcaneplugins.levelledmobs.bukkit.command.CommandHandler;
import java.util.Collection;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

public class KillSubcommand {

    public static final CommandAPICommand INSTANCE =
        new CommandAPICommand("kill")
            .withArguments(
                new EntitySelectorArgument<Collection<Entity>>
                    ("entities", EntitySelector.MANY_ENTITIES),
                CommandHandler.createWorldListArgument("worlds")
            )
            .withPermission("levelledmobs.command.levelledmobs.kill")
            .executes((sender, args) -> {
                //noinspection unchecked
                final Collection<Entity> entities = (Collection<Entity>) args[0];
                //noinspection unchecked
                final Collection<World> worlds = (Collection<World>) args[1];

                // how many entities in the selector were skipped since they were not in any of
                // the specified worlds
                int selected = entities.size(); // e.g.: 37 entities were selected
                int killed = 0;                 // e.g.: 12 entities were killed
                int skippedTotal = 0;           // e.g.: 23 entities were skipped: ...specifics...
                int skippedByNonLevelled = 0;   // e.g.: entities skipped: 73 (non-levelled);
                int skippedByWorld = 0;         // e.g.: entities skipped: 230 (world);

                for(final Entity entity : entities) {
                    if(!worlds.contains(entity.getWorld())) {
                        skippedByWorld++;
                        skippedTotal++;
                        continue;
                    }

                    if(!(entity instanceof LivingEntity lent) ||
                        !EntityDataUtil.isLevelled(lent, true)
                    ) {
                        skippedByNonLevelled++;
                        skippedTotal++;
                        continue;
                    }

                    //TODO make a setting which uses entity#remove instead.
                    lent.setHealth(0.0d);
                    killed++;
                }

                sender.sendMessage(
                    "LM: Killed %s of %s entities in worlds %s.".formatted(killed, selected, worlds),
                    "LM: Skipped %s entities: %s (non-levelled); %s (world)."
                        .formatted(skippedTotal, skippedByNonLevelled, skippedByWorld)
                );

                //TODO Make this overview translatable.
            });

}
