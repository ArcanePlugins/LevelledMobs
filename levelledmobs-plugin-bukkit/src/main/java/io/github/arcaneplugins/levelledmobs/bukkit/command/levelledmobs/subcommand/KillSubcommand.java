package io.github.arcaneplugins.levelledmobs.bukkit.command.levelledmobs.subcommand;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.EntitySelector;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.TextArgument;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import io.github.arcaneplugins.levelledmobs.bukkit.api.data.EntityDataUtil;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Tameable;

public class KillSubcommand {

    public static final CommandAPICommand INSTANCE_1 =
        new CommandAPICommand("kill")
            .withArguments(
                new EntitySelectorArgument<Collection<Entity>>
                    ("entities", EntitySelector.MANY_ENTITIES),
                new TextArgument("worlds"),
                new TextArgument("fineTuningParams")
            )
            .withShortDescription("Kills levelled mobs on the server.")
            .withFullDescription("Kills levelled mobs on the server, akin to how Minecraft's " +
                "kill command works. Provides safety in skipping mobs by default which may " +
                "not be intended for the mass-kill, such as nametagged or tamed mobs.")
            .withPermission("levelledmobs.command.levelledmobs.kill")
            .executes(KillSubcommand::execute);

    public static final CommandAPICommand INSTANCE_2 =
        new CommandAPICommand("kill")
            .withArguments(
                new EntitySelectorArgument<Collection<Entity>>
                    ("entities", EntitySelector.MANY_ENTITIES),
                new TextArgument("worlds")
            )
            .withPermission("levelledmobs.command.levelledmobs.kill")
            .executes(KillSubcommand::execute);

    private static void execute(
        final @Nonnull CommandSender sender,
        final @Nonnull Object[] args
    ) throws WrapperCommandSyntaxException {
        //noinspection unchecked
        final Collection<Entity> entities = (Collection<Entity>) args[0];

        final Set<String> worldNames = Arrays
            .stream(((String) args[1]).split(","))
            .map(String::toLowerCase)
            .collect(Collectors.toSet());

        final Map<String, String> fineTuningParams = args.length == 2 ?
            Collections.emptyMap() :
            Arrays
                .stream(((String) args[2]).split(","))
                .map(str -> str.toLowerCase().split(":"))
                .collect(Collectors.toMap(e -> e[0], e -> e[1]));

        entities.removeIf(entity ->
            entity.getType() == EntityType.PLAYER ||
            !(entity instanceof LivingEntity));

        for(final String worldName : worldNames) {
            if(worldName.equals("*")) continue;
            if(Bukkit.getWorld(worldName) == null) {
                throw CommandAPI.fail("Invalid world " + worldName);
                //TODO translatable error
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

        final boolean skipTamed = fineTuningParams
            .getOrDefault("skip-tamed", "true")
            .equals("true");

        final boolean skipNametagged = fineTuningParams
            .getOrDefault("skip-nametagged", "true")
            .equals("true");

        for(final Entity entity : entities) {
            final LivingEntity lent = (LivingEntity) entity;

            if(!worldNames.contains("*") &&
                !worldNames.contains(entity.getWorld().getName().toLowerCase())
            ) {
                skippedByWorld++;
                skippedTotal++;
                continue;
            }

            if(!EntityDataUtil.isLevelled(lent, true)) {
                skippedByNonLevelled++;
                skippedTotal++;
                continue;
            }

            if(skipTamed && entity instanceof Tameable tent && tent.isTamed()) {
                skippedByTamed++;
                skippedTotal++;
                continue;
            }

            // note: this condition gets in the way when someone decides to use permanent labels
            //noinspection deprecation
            if(skipNametagged && lent.getCustomName() != null) {
                skippedByNametag++;
                skippedTotal++;
                continue;
            }

            //TODO make a setting which allows use of entity#remove instead. it has no drops
            lent.setHealth(0.0d);
            killed++;
        }

        sender.sendMessage(
            """
               LM: Killed %s of %s entities in worlds %s. Skipped %s entities:
               • %s (non-levelled)
               • %s (world)
               • %s (tamed)
               • %s (nametagged)"""
                .formatted(
                    killed, selected, String.join(", ", worldNames),
                    skippedTotal, skippedByNonLevelled, skippedByWorld,
                    skippedByTamed, skippedByNametag
                )
        );

        //TODO Make this overview translatable. also only show skipped info when relevant
    }

}