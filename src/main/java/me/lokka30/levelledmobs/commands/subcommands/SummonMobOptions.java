package me.lokka30.levelledmobs.commands.subcommands;

import me.lokka30.levelledmobs.misc.LivingEntityPlaceHolder;
import me.lokka30.levelledmobs.misc.RequestedLevel;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SummonMobOptions {

    SummonMobOptions(@NotNull final LivingEntityPlaceHolder lmPlaceHolder,
        final CommandSender sender) {
        this.lmPlaceHolder = lmPlaceHolder;
        this.sender = sender;
    }

    @NotNull
    final LivingEntityPlaceHolder lmPlaceHolder;
    public final CommandSender sender;
    SummonSubcommand.SummonType summonType;
    public int amount;
    RequestedLevel requestedLevel;
    public Player player;
    public boolean override;
    String nbtData;
}
