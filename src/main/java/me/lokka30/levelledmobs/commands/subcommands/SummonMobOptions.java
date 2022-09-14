package me.lokka30.levelledmobs.commands.subcommands;

import me.lokka30.levelledmobs.misc.LivingEntityPlaceholder;
import me.lokka30.levelledmobs.misc.LivingEntityPlaceholder;
import me.lokka30.levelledmobs.misc.RequestedLevel;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SummonMobOptions {

    SummonMobOptions(
        @NotNull final LivingEntityPlaceholder lmPlaceholder,
        @NotNull final CommandSender sender
    ) {
        this.lmPlaceholder = lmPlaceholder;
        this.sender = sender;
    }

    public final LivingEntityPlaceholder lmPlaceholder;
    public final CommandSender sender;
    public SummonSubcommand.SummonType summonType;
    public int amount;
    public RequestedLevel requestedLevel;
    public Player player;
    public boolean override;
    public String nbtData;
}
