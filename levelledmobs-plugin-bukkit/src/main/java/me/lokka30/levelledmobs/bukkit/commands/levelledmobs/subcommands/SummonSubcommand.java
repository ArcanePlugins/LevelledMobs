package me.lokka30.levelledmobs.bukkit.commands.levelledmobs.subcommands;

import static me.lokka30.levelledmobs.bukkit.utils.TempConst.PREFIX_INF;
import static org.bukkit.ChatColor.AQUA;
import static org.bukkit.ChatColor.GRAY;

import java.util.Collections;
import java.util.List;
import me.lokka30.levelledmobs.bukkit.LevelledMobs;
import me.lokka30.levelledmobs.bukkit.api.data.keys.EntityKeyStore;
import me.lokka30.levelledmobs.bukkit.commands.CommandWrapper;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
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
    public void run(@NotNull CommandSender sender, @NotNull String[] args) {
        if(!hasPerm(sender, "levelledmobs.command.levelledmobs.summon", true))
            return;

        if(!isSenderPlayer(sender, true))
            return;

        final Player player = (Player) sender;

        player.sendMessage(PREFIX_INF + "Summoning entity...");

        final var entity = player.getWorld().spawn(player.getLocation(), Zombie.class, preSpawnEntity -> {
            /*
            this code block runs *before* entity spawn event is fired!
            */

            // we want to make sure EntitySpawnListener knows that this mob was summoned.
            // EntitySpawnListener will convert this non-persistent metadata to be persistent.
            preSpawnEntity.setMetadata(
                EntityKeyStore.wasSummoned.toString(),
                new FixedMetadataValue(LevelledMobs.getInstance(), 1)
            );
        });

        player.sendMessage(PREFIX_INF + "Successfully summoned a levelled '" + AQUA +
            entity.getName() + GRAY + "' mob.");
    }

    @Override
    public @NotNull List<String> suggest(@NotNull CommandSender sender,
        @NotNull String[] args) {
        //FIXME
        return Collections.emptyList();
    }
}
