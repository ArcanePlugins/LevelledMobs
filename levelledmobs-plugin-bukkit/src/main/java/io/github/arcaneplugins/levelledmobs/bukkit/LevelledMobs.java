package io.github.arcaneplugins.levelledmobs.bukkit;

import io.github.arcaneplugins.levelledmobs.bukkit.command.CommandHandler;
import io.github.arcaneplugins.levelledmobs.bukkit.command.CommandHandler.LoadingStage;
import io.github.arcaneplugins.levelledmobs.bukkit.config.ConfigHandler;
import io.github.arcaneplugins.levelledmobs.bukkit.integration.IntegrationHandler;
import io.github.arcaneplugins.levelledmobs.bukkit.listener.ListenerHandler;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.LogicHandler;
import io.github.arcaneplugins.levelledmobs.bukkit.util.ClassUtils;
import io.github.arcaneplugins.levelledmobs.bukkit.util.Log;
import io.github.arcaneplugins.levelledmobs.bukkit.util.nms.Definitions;
import io.github.arcaneplugins.levelledmobs.bukkit.util.nms.PacketLabelSender;
import io.github.arcaneplugins.levelledmobs.bukkit.util.throwable.SilentException;
import java.util.Objects;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public final class LevelledMobs extends JavaPlugin {

    /* vars */

    private final CommandHandler commandHandler = new CommandHandler();
    private final ConfigHandler configHandler = new ConfigHandler();
    private final IntegrationHandler integrationHandler = new IntegrationHandler();
    private final ListenerHandler listenerHandler = new ListenerHandler();
    private final LogicHandler logicHandler = new LogicHandler();
    private final Definitions nmsDefinitions = new Definitions();
    private final PacketLabelSender packetLabelSender = new PacketLabelSender();

    /* methods */

    @Override
    public void onLoad() {
        instance = this;
        getCommandHandler().load(LoadingStage.ON_LOAD);
        Log.inf("Plugin initialized");
    }

    @Override
    public void onEnable() {

        try {
            assertRunningSpigot();
            getConfigHandler().load();
            getNametagSender().load();
            getListenerHandler().loadPrimary();
            getIntegrationHandler().load();
            getLogicHandler().load();
            getListenerHandler().loadSecondary();
            getCommandHandler().load(LoadingStage.ON_ENABLE);
        } catch(final Exception ex) {
            if(!(ex instanceof SilentException)) {
                Log.sev("""
                
                LevelledMobs was unable to enable itself. This is commonly caused by a configuration syntax mistake by the user.
                
                If you are unable to solve this issue on your own, support is provided by volunteers on the ArcanePlugins Discord Guild: < https://discord.gg/HqZwdcJ >
                
                Although Discord is preferred for all communication, you can also message the author if required: < https://www.spigotmc.org/conversations/add?to=lokka30 >
                
                A stack trace will be provided below to aid maintainers and advanced users in locating the root cause of this issue.
                
                Warning: No support can be possibly provided in the SpigotMC reviews section, do not post your issue there.
                
                -+- START EXCEPTION STACK TRACE -+-""");

                ex.printStackTrace();

                Log.sev("""
                
                -+- END EXCEPTION STACK TRACE -+-
                
                Warning: Before reporting this issue to LevelledMobs maintainers, read the information above the stack trace.
                
                Warning: No support can be possibly provided in the SpigotMC reviews section, do not post your issue there.""");
            }

            setEnabled(false);
            return;
        }

        Log.inf("Plugin enabled");
    }

    public void reload() {
        getConfigHandler().load();
        getLogicHandler().load();
    }

    @Override
    public void onDisable() {
        Log.inf("Plugin disabled");
    }

    /*
    Check if the server is running SpigotMC, or any derivative software.
     */
    private static boolean isRunningSpigot() {
        return ClassUtils.classExists("net.md_5.bungee.api.chat.TextComponent");
    }

    /*
    Ensure the server is running SpigotMC, or any derivative software.
     */
    private void assertRunningSpigot() {
        if(isRunningSpigot()) return;
        throw new SilentException("""
            LevelledMobs has detected that your server is not running the SpigotMC server software, or any derivative such as PaperMC.""");
    }

    /* getters and setters */

    public CommandHandler getCommandHandler() { return commandHandler; }
    public ConfigHandler getConfigHandler() { return configHandler; }
    public IntegrationHandler getIntegrationHandler() { return integrationHandler; }
    public ListenerHandler getListenerHandler() { return listenerHandler; }
    public LogicHandler getLogicHandler() { return logicHandler; }
    public Definitions getNmsDefinitions() {
        return nmsDefinitions;
    }
    public PacketLabelSender getNametagSender() { return packetLabelSender; }

    /* singleton */

    private static LevelledMobs instance;

    public static @NotNull LevelledMobs getInstance() {
        return Objects.requireNonNull(
            instance,
            """
                Attempted to access LevelledMobs.class instance before calling LevelledMobs#onLoad"""
        );
    }

}
