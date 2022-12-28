package io.github.arcaneplugins.levelledmobs.bukkit.command.levelledmobs.subcommand.routine;

import dev.jorel.commandapi.CommandAPICommand;
import io.github.arcaneplugins.levelledmobs.bukkit.LevelledMobs;
import io.github.arcaneplugins.levelledmobs.bukkit.util.ClassUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

public final class CompatibilityRoutine {

    public static CommandAPICommand createInstance() {
        return new CommandAPICommand("compatibility")
            .withShortDescription("Run compatibility diagnostic")
            .withPermission("levelledmobs.command.levelledmobs.routine.compatibility")
            .executes((sender, args) -> {
                sender.sendMessage(
                    """
                    %sLM4 Compatibility Checker Routine
                    
                    %s[LevelledMobs Info]
                    %s• Plugin Version: %s
                    
                    %s[Server Software Info]
                    %s• Bukkit Version: %s
                    • Has Spigot API: %s
                    • Has Paper API: %s
                    
                    %s(...end of information...)
                    """.formatted(
                        //header
                        ChatColor.BLUE,

                        //levelledmobs info
                        ChatColor.AQUA,
                        ChatColor.GRAY,
                        LevelledMobs.getInstance().getDescription().getVersion(),

                        //server software info
                        ChatColor.AQUA,
                        ChatColor.GRAY,
                        Bukkit.getVersion(),
                        ClassUtils.classExists("org.bukkit.entity.Player.Spigot"),
                        ClassUtils.classExists("com.destroystokyo.paper.ParticleBuilder"),

                        //footer
                        ChatColor.DARK_GRAY
                    )
                );
            });
    }

}
