package me.lokka30.levelledmobs.bukkit.command.levelledmobs.subcommand.summon;

import me.lokka30.levelledmobs.bukkit.LevelledMobs;
import me.lokka30.levelledmobs.bukkit.command.CommandWrapper;
import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// use this class for testing various things
// for now it is used for testing nametag packets
public class TestSubcommand extends CommandWrapper {
    public TestSubcommand(final @NotNull LevelledMobs main){
        super("test");
        this.main = main;
    }

    private final LevelledMobs main;

    @Override
    public void run(@NotNull CommandSender sender, @NotNull String[] args) {
        if(!hasPerm(sender, "levelledmobs.command.levelledmobs.test", true))
            return;

        if(!isSenderPlayer(sender, true))
            return;

        final Player player = (Player) sender;
        final List<Entity> entities = player.getNearbyEntities(100, 40, 100).stream()
                .filter(e -> e instanceof LivingEntity)
                .map(e -> Map.entry(e.getLocation().distanceSquared(player.getLocation()), e))
                .sorted(Comparator.comparingDouble(Map.Entry::getKey))
                .map(Map.Entry::getValue).toList();
        
        if (entities.isEmpty()){
            sender.sendMessage("no nearly entities found");
            return;
        }
        final LivingEntity le = (LivingEntity) entities.get(0);

        final String testNametag = "This is a test";
        main.getNametagSender().sendNametag(le, (Player) sender, testNametag);
    }

    @Override
    public @NotNull List<String> suggest(@NotNull CommandSender sender, @NotNull String[] args) {
        return Collections.emptyList();
    }
}
