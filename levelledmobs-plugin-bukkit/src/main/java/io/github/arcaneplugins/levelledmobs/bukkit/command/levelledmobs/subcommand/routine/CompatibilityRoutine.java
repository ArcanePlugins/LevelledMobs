package io.github.arcaneplugins.levelledmobs.bukkit.command.levelledmobs.subcommand.routine;

import dev.jorel.commandapi.CommandAPICommand;
import io.github.arcaneplugins.levelledmobs.bukkit.LevelledMobs;
import io.github.arcaneplugins.levelledmobs.bukkit.util.ClassUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

public final class CompatibilityRoutine {

    public static CommandAPICommand createInstance() {
        return new CommandAPICommand("compatibility")
            .withShortDescription("Run compatibility diagnostic tool.")
            .withPermission("levelledmobs.command.levelledmobs.routine.compatibility")
            .executes((sender, args) -> {
                sender.sendMessage(
                    """
                    %s%sLM4 Compatibility Checker Routine
                    
                    %s[Plugin Info]
                    %s • Plugin Version: %s
                    
                    %s[Server Software Info]
                    %s • Bukkit Version: %s
                     • Has Spigot API: %s
                     • Has Paper API: %s
                     • Has Adventure API: %s
                    
                    %s(...end of information...)
                    """.formatted(
                        //header
                        ChatColor.BLUE,
                        ChatColor.BOLD,

                        //levelledmobs info
                        ChatColor.AQUA,
                        ChatColor.GRAY,
                        LevelledMobs.getInstance().getDescription().getVersion(),

                        //server software info
                        ChatColor.AQUA,
                        ChatColor.GRAY,
                        Bukkit.getVersion(),
                        ClassUtils.classExists("net.md_5.bungee.api.ChatColor"),
                        ClassUtils.classExists("com.destroystokyo.paper.ParticleBuilder"),
                        ClassUtils.classExists("net.kyori.adventure.Adventure"),

                        //footer
                        ChatColor.DARK_GRAY
                    )
                );
            });
    }

}
