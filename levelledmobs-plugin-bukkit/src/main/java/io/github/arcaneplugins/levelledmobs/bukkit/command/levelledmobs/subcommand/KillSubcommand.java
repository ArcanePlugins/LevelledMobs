package io.github.arcaneplugins.levelledmobs.bukkit.command.levelledmobs.subcommand;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.EntitySelector;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import io.github.arcaneplugins.levelledmobs.bukkit.api.data.EntityDataUtil;
import io.github.arcaneplugins.levelledmobs.bukkit.data.InternalEntityDataUtil;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.action.impl.SetPermanentLabelAction;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Tameable;

public class KillSubcommand {

    public static final CommandAPICommand INSTANCE =
        new CommandAPICommand("kill")
            .withArguments(
                new EntitySelectorArgument<Collection<Entity>>
                    ("entities", EntitySelector.MANY_ENTITIES),
                new GreedyStringArgument("worlds")
            )
            .withPermission("levelledmobs.command.levelledmobs.kill")
            .executes((sender, args) -> {
                //noinspection unchecked
                final Collection<Entity> entities = (Collection<Entity>) args[0];

                final Collection<String> worlds = Arrays.stream(((String) args[1]).split(" "))
                    .map(String::toLowerCase)
                    .collect(Collectors.toSet());

                entities.removeIf(entity -> entity.getType() == EntityType.PLAYER);

                for(final String world : worlds) {
                    if(Bukkit.getWorld(world) == null) {
                        throw CommandAPI.fail("Invalid world " + world);
                    }
                }

                // how many entities in the selector were skipped since they were not in any of
                // the specified worlds
                int selected = entities.size(); // e.g.: 37 entities were selected
                int killed = 0;                 // e.g.: 12 entities were killed
                int skippedTotal = 0;           // e.g.: 23 entities were skipped: ...specifics...
                int skippedByNonLevelled = 0;   // e.g.: entities skipped: 73 (non-levelled);
                int skippedByWorld = 0;         // e.g.: entities skipped: 230 (world);
                int skippedByTamed = 0;         // e.g.: entities skipped: 13 (tamed);
                int skippedByNametag = 0;       // e.g.: entities skipped: 5 (nametagged);

                for(final Entity entity : entities) {
                    if(!worlds.contains(entity.getWorld().getName().toLowerCase())) {
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

                    if(entity instanceof Tameable tent && tent.isTamed()) {
                        skippedByTamed++;
                        skippedTotal++;
                        continue;
                    }

                    //noinspection deprecation
                    if(lent.getCustomName() != null &&
                        !InternalEntityDataUtil
                            .getLabelHandlerFormulaMap(lent, true)
                            .containsKey(SetPermanentLabelAction.LABEL_ID)
                    ) {
                        skippedByNametag++;
                        skippedTotal++;
                        continue;
                    }

                    //TODO make a setting which allows use of entity#remove instead. it has no drops
                    lent.setHealth(0.0d);
                    killed++;
                }

                sender.sendMessage(
                    "LM: Killed %s of %s entities in worlds %s.".formatted(killed, selected, worlds),
                    ("LM: Skipped %s entities: %s (non-levelled); %s (world); %s (tamed); "
                        + "%s (nametagged).")
                        .formatted(skippedTotal, skippedByNonLevelled, skippedByWorld,
                            skippedByTamed, skippedByNametag)
                );

                //TODO Make this overview translatable.
            });

}
