package me.lokka30.levelledmobs.commands.subcommands;

import me.lokka30.levelledmobs.misc.LivingEntityPlaceHolder;
import me.lokka30.levelledmobs.misc.RequestedLevel;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SummonMobOptions {
    public SummonMobOptions(@NotNull final LivingEntityPlaceHolder lmPlaceHolder, final CommandSender sender){
        this.lmPlaceHolder = lmPlaceHolder;
        this.sender = sender;
    }

    @NotNull
    public final LivingEntityPlaceHolder lmPlaceHolder;
    public final CommandSender sender;
    public SummonSubcommand.SummonType summonType;
    public int amount;
    public RequestedLevel requestedLevel;
    public Player player;
    public boolean override;
    public String nbtData;
}
