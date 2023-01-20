package io.github.arcaneplugins.levelledmobs.bukkit.command.levelledmobs.subcommand;

import static io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.action.impl.setlevel.SetLevelAction.getMinPossibleLevel;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.EntityTypeArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.IntegerRangeArgument;
import dev.jorel.commandapi.arguments.LocationArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.wrappers.IntegerRange;
import io.github.arcaneplugins.levelledmobs.bukkit.data.InternalEntityDataUtil;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;

public final class SummonSubcommand {

    public static CommandAPICommand createInstance() {
        return new CommandAPICommand("summon")
            .withPermission("levelledmobs.command.levelledmobs.summon")
            .withShortDescription("Summons a levelled mob of chosen specifications.")
            .withFullDescription("Summons a levelled mob of chosen specifications, similar to " +
                "Minecraft's `/summon` command.")
            .withSubcommands(
                new CommandAPICommand("entity")
                    .withShortDescription("Summons a levelled entity of the specified type.")
                    .withFullDescription("Summons a levelled entity of the specified type.")
                    .withArguments(
                        new EntityTypeArgument("entityType"),
                        new IntegerRangeArgument("amountRange"),
                        new IntegerRangeArgument("levelRange"),
                        new IntegerArgument("minLevel", 0),
                        new IntegerArgument("maxLevel", 0),
                        new LocationArgument("location")
                    )
                    .executes((sender, args) -> {
                        final EntityType entityType = (EntityType) args[0];
                        final IntegerRange amountRange = (IntegerRange) args[1];
                        final IntegerRange levelRange = (IntegerRange) args[2];
                        final int minLevel = (int) args[3];
                        final int maxLevel = (int) args[4];
                        final Location location = (Location) args[5];

                        if(amountRange.getLowerBound() <= 0)
                            throw CommandAPI.failWithString(
                                "Amount range lower bound must be greater than 0, but got: " +
                                    amountRange
                            );

                        if(levelRange.getLowerBound() < getMinPossibleLevel())
                            throw CommandAPI.failWithString(
                                "Level range lower bound must be at least " +
                                    getMinPossibleLevel() + ", but got: " +
                                    levelRange.getLowerBound()
                            );

                        if(minLevel < getMinPossibleLevel())
                            throw CommandAPI.failWithString(
                                "Minimum level must be at least " + getMinPossibleLevel() + ", " +
                                    "but got: " + minLevel
                            );

                        if(maxLevel < getMinPossibleLevel())
                            throw CommandAPI.failWithString(
                                "Maximum level must be at least " + getMinPossibleLevel() + ", " +
                                    "but got: " + maxLevel
                            );

                        final Class<? extends Entity> entityClass = entityType.getEntityClass();

                        if(entityClass == null ||
                            !LivingEntity.class.isAssignableFrom(entityClass)
                        ) {
                            throw CommandAPI.failWithString(
                                "Entity is not summonable: " + entityType.name()
                            );
                        }

                        final int amount = ThreadLocalRandom.current().nextInt(
                            amountRange.getLowerBound(),
                            amountRange.getUpperBound() + 1
                        );

                        for(int i = 0; i < amount; i++) {
                            final int level = ThreadLocalRandom.current().nextInt(
                                levelRange.getLowerBound(),
                                levelRange.getUpperBound() + 1
                            );

                            InternalEntityDataUtil.summonMob(
                                entityType, location, level, minLevel, maxLevel
                            );
                        }

                        sender.sendMessage("Summoned in " + amount + "x " + entityType);
                    }),
                new CommandAPICommand("custom")
                    .withShortDescription("Summons a levelled entity fro the specified custom " +
                        "entity template.")
                    .withFullDescription("Summons a levelled entity fro the specified custom " +
                        "entity template.")
                    .withArguments(
                        // TODO may want to use a MultiLiteralArgument for the custom-entity id
                        new StringArgument("customEntityId"),
                        new IntegerRangeArgument("amountRange"),
                        new IntegerRangeArgument("levelRange"),
                        new IntegerArgument("minLevel", 0),
                        new IntegerArgument("maxLevel", 0),
                        new LocationArgument("location")
                    )
                    .executes((sender, args) -> {
                        //TODO impl
                        sender.sendMessage("LM: 'custom' summoning is not implemented yet.");
                    })
            );
    }

}
