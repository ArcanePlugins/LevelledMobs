package me.lokka30.levelledmobs.bukkit.command.levelledmobs.subcommand.summon;

import java.util.Collections;
import java.util.List;
import me.lokka30.levelledmobs.bukkit.LevelledMobs;
import me.lokka30.levelledmobs.bukkit.api.data.keys.EntityKeyStore;
import me.lokka30.levelledmobs.bukkit.command.CommandWrapper;
import me.lokka30.levelledmobs.bukkit.config.translations.Message;
import me.lokka30.levelledmobs.bukkit.util.EnumUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.jetbrains.annotations.NotNull;

/*
[command structure]
    index.. 0   1
    size... 1   2
            :   :
          - /lm |
          - /lm summon ... FIXME ...
 */
public class SummonSubcommand extends CommandWrapper {

    public SummonSubcommand() {
        super("summon");
    }

    @Override
    public void run(
        @NotNull CommandSender sender,
        @NotNull String[] args
    ) {
        if(!hasPerm(sender, "levelledmobs.command.levelledmobs.summon", true))
            return;

        if(!isSenderPlayer(sender, true))
            return;

        final Player player = (Player) sender;

        final var entityType = EntityType.ZOMBIE;

        final var entityClass = entityType.getEntityClass();

        if(entityClass == null) {
            Message.COMMAND_LEVELLEDMOBS_SUBCOMMAND_SUMMON_NOT_SUMMONABLE.sendTo(sender,
                "%entity-type%", EnumUtils.formatEnumConstant(entityType));
            //TODO use translated entity name instead
            return;
        }

        Message.COMMAND_LEVELLEDMOBS_SUBCOMMAND_SUMMON_SUMMONING.sendTo(sender,
            "%entity-name%", EnumUtils.formatEnumConstant(entityType));
        //TODO use translated entity name instead

        player.getWorld().spawn(player.getLocation(), entityClass, preSpawnEntity -> {
            /*
            this code block runs *before* entity spawn event is fired!
            */

            // we want to make sure EntitySpawnListener knows that this mob was summoned.
            // EntitySpawnListener will convert this non-persistent metadata to be persistent.
            preSpawnEntity.setMetadata(
                EntityKeyStore.WAS_SUMMONED.toString(),
                new FixedMetadataValue(LevelledMobs.getInstance(), 1)
            );
        });
    }

    @Override
    public @NotNull List<String> suggest(
        @NotNull CommandSender sender,
        @NotNull String[] args
    ) {
        return Collections.emptyList();
    }
}
