package me.lokka30.levelledmobs.bukkit;

import java.util.Objects;
import me.lokka30.levelledmobs.bukkit.commands.CommandHandler;
import me.lokka30.levelledmobs.bukkit.configs.ConfigHandler;
import me.lokka30.levelledmobs.bukkit.integrations.IntegrationHandler;
import me.lokka30.levelledmobs.bukkit.listeners.ListenerHandler;
import me.lokka30.levelledmobs.bukkit.logic.LogicHandler;
import me.lokka30.levelledmobs.bukkit.utils.Log;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public final class LevelledMobs extends JavaPlugin {

    /* vars */

    private final CommandHandler commandHandler = new CommandHandler();
    private final ConfigHandler configHandler = new ConfigHandler();
    private final IntegrationHandler integrationHandler = new IntegrationHandler();
    private final ListenerHandler listenerHandler = new ListenerHandler();
    private final LogicHandler logicHandler = new LogicHandler();

    /* methods */

    @Override
    public void onLoad() {
        instance = this;
        Log.inf("Plugin initialized.");
    }

    @Override
    public void onEnable() {
        if(!(
            getConfigHandler().load() &&
            getLogicHandler().load() &&
            getListenerHandler().load() &&
            getCommandHandler().load()
        )) {
            Log.sev("LevelledMobs encountered a fatal error during the startup process. " +
                "It will disable itself to prevent possible issues resulting from malfunction.");
            setEnabled(false);
            return;
        }

        final var version = getDescription().getVersion();
        if(version.contains("alpha") || version.contains("beta")) {
            Log.war("You are running an alpha/beta version of LevelledMobs. Please take care, "
            + "and beware that this version is unlikely to be tested.");
        }

        Log.inf("Plugin enabled.");
    }

    @Override
    public void onDisable() {
        Log.inf("Plugin disabled.");
    }

    /* getters and setters */

    public CommandHandler getCommandHandler() { return commandHandler; }
    public ConfigHandler getConfigHandler() { return configHandler; }
    public IntegrationHandler getIntegrationHandler() { return integrationHandler; }
    public ListenerHandler getListenerHandler() { return listenerHandler; }
    public LogicHandler getLogicHandler() { return logicHandler; }

    /* singleton */

    private static LevelledMobs instance;

    @NotNull
    public static LevelledMobs getInstance() {
        return Objects.requireNonNull(instance, "instance");
    }

}
