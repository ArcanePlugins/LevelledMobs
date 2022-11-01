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
        } catch(Exception ex) {
            Log.sev("""
                
                LevelledMobs has encountered a fatal error during the startup process; it will disable itself to prevent possible issues resulting from the plugin malfunctioning.
                Note that this may be a user-error, such as a stray apostrophe in a configuration file.
                
                If you are unable to resolve the error through analysing the debug information provided below, feel free to ask our volunteer helpers for assistance on the ArcanePlugins Discord Guild:
                < https://www.discord.io/arcaneplugins >
                
                Notice: Do not use the reviews section to report this issue. Instead, join our Discord through the link provided above if you wish to receive assistance.
                
                -+- START EXCEPTION STACK TRACE -+-""");
            ex.printStackTrace();
            Log.sev("-+- END EXCEPTION STACK TRACE -+-");
            setEnabled(false);
            return;
        }

        final String version = getDescription().getVersion();
        if(version.contains("alpha") || version.contains("beta")) {
            Log.war("You are running an alpha/beta version of LevelledMobs. Please take care, "
            + "and beware that this version is unlikely to be tested.");
        }

        Log.inf("Plugin enabled");
    }

    public void reload() {
        getConfigHandler().load();
        getLogicHandler().load();
        getCommandHandler().load(LoadingStage.ON_RELOAD);
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
        throw new IllegalStateException("""
            This version of LevelledMobs is only able to run on SpigotMC-based servers, such as SpigotMC, PaperMC, and so on.
            You are likely using CraftBukkit – there is no reason to use CraftBukkit, switch to SpigotMC or a derivative.""");
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
