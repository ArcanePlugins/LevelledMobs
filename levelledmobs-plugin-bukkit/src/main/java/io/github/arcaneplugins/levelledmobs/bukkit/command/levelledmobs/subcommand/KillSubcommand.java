package io.github.arcaneplugins.levelledmobs.bukkit.command.levelledmobs.subcommand;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.TextArgument;
import dev.jorel.commandapi.arguments.WorldArgument;
import io.github.arcaneplugins.levelledmobs.bukkit.api.data.EntityDataUtil;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Tameable;

public final class KillSubcommand {

    public static CommandAPICommand createInstance1() {
        return new CommandAPICommand("kill")
            .withArguments(
                new EntitySelectorArgument.ManyEntities("entities"),
                new WorldArgument("world"),
                new TextArgument("fineTuningParams")
            )
            .withShortDescription("Allows using fine-tuning parameters to override default " +
                "logic in the kill subcommand.")
            .withFullDescription("Identical to the other `kill` subcommand, however, allowing " +
                "users to override default logic, such as disabling the standard nametag and " +
                "tamed mob protections.")
            .withPermission("levelledmobs.command.levelledmobs.kill")
            .executes(KillSubcommand::execute);
    }

    public static CommandAPICommand createInstance2() {
        return new CommandAPICommand("kill")
            .withArguments(
                new EntitySelectorArgument.ManyEntities("entities"),
                new WorldArgument("world")
            )
            .withPermission("levelledmobs.command.levelledmobs.kill")
            .withShortDescription("Kills levelled mobs on the server.")
            .withFullDescription("Kills levelled mobs on the server. Very similar to Minecraft's "
                + "own `/kill` command, but this only targets levelled mobs and has various "
                + "safety measures implemented, such as tamed pets being excluded by default.")
            .withPermission("levelledmobs.command.levelledmobs.kill")
            .executes(KillSubcommand::execute);
    }


    private static void execute(
        final @Nonnull CommandSender sender,
        final @Nonnull Object[] args
    ) {
        //noinspection unchecked
        final Collection<Entity> entities = (Collection<Entity>) args[0];

        final World world = (World) args[1];

        final Map<String, String> fineTuningParams = args.length == 2 ?
            Collections.emptyMap() :
            Arrays
                .stream(((String) args[2]).split(","))
                .map(str -> str.toLowerCase().split(":"))
                .collect(Collectors.toMap(e -> e[0], e -> e[1]));

        entities.removeIf(entity ->
            entity.getType() == EntityType.PLAYER ||
            !(entity instanceof LivingEntity));

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
            .equalsIgnoreCase("true");

        final boolean skipNametagged = fineTuningParams
            .getOrDefault("skip-nametagged", "true")
            .equalsIgnoreCase("true");

        final boolean remove = fineTuningParams
            .getOrDefault("remove", "true")
            .equalsIgnoreCase("true");

        for(final Entity entity : entities) {
            final LivingEntity lent = (LivingEntity) entity;

            if(!world.getUID().equals(entity.getWorld().getUID())) {
                skippedByWorld++;
                skippedTotal++;
                continue;
            }

            if(!EntityDataUtil.isLevelled(lent, true)) {
                skippedByNonLevelled++;
                skippedTotal++;
                continue;
            }

            if(skipTamed && entity instanceof final Tameable tameable && tameable.isTamed()) {
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

            if(remove) {
                lent.remove();
            } else {
                lent.setHealth(0.0d);
            }
            killed++;
        }

        sender.sendMessage("""
               LM: Killed %s of %s entities in world '%s'. Skipped %s entities:
               • %s (non-levelled)
               • %s (world)
               • %s (tamed)
               • %s (nametagged)"""
            .formatted(
                killed, selected, world.getName(),
                skippedTotal, skippedByNonLevelled, skippedByWorld,
                skippedByTamed, skippedByNametag
            )
        );

        //TODO Make this overview translatable. also only show skipped info when relevant
    }

}
