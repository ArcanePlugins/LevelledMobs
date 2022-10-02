package me.lokka30.levelledmobs.bukkit;

import java.util.Objects;
import me.lokka30.levelledmobs.bukkit.command.CommandHandler;
import me.lokka30.levelledmobs.bukkit.config.ConfigHandler;
import me.lokka30.levelledmobs.bukkit.integration.IntegrationHandler;
import me.lokka30.levelledmobs.bukkit.listener.ListenerHandler;
import me.lokka30.levelledmobs.bukkit.logic.LogicHandler;
import me.lokka30.levelledmobs.bukkit.logic.nms.Definitions;
import me.lokka30.levelledmobs.bukkit.logic.nms.NametagSender;
import me.lokka30.levelledmobs.bukkit.util.ClassUtils;
import me.lokka30.levelledmobs.bukkit.util.Log;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public final class LevelledMobs extends JavaPlugin {

    /* vars */

    private CommandHandler commandHandler;
    private ConfigHandler configHandler;
    private IntegrationHandler integrationHandler;
    private ListenerHandler listenerHandler;
    private LogicHandler logicHandler;
    private Definitions nmsDefinitions;
    private NametagSender nametagSender;

    /* methods */

    @Override
    public void onLoad() {
        instance = this;
        Log.inf("Plugin initialized");
    }

    @Override
    public void onEnable() {
        if(!assertRunningSpigot()) {
            setEnabled(false);
            return;
        }

        this.commandHandler = new CommandHandler(this);
        this.configHandler = new ConfigHandler();
        this.integrationHandler = new IntegrationHandler();
        this.listenerHandler = new ListenerHandler();
        this.logicHandler = new LogicHandler();
        this.nmsDefinitions = new Definitions();
        this.nametagSender = new NametagSender(this);

        //TODO check for a runtime exception rather than comparing booleans
        if(!(assertRunningSpigot() &&
            getConfigHandler().load() &&
            getListenerHandler().loadPrimary() &&
            getIntegrationHandler().load() &&
            getLogicHandler().load() &&
            getListenerHandler().loadSecondary() &&
            getCommandHandler().load()
        )) {
            Log.sev("LevelledMobs encountered a fatal error during the startup process; " +
                "it will disable itself to prevent possible issues resulting from malfunction.",
                true);

            //TODO send a message to online players as well (in case of reload)

            // TODO make it still operational for reloading instead of just disabling.

            setEnabled(false);
            return;
        }

        final var version = getDescription().getVersion();
        if(version.contains("alpha") || version.contains("beta")) {
            Log.war("You are running an alpha/beta version of LevelledMobs. Please take care, "
            + "and beware that this version is unlikely to be tested.", false);
        }

        runTestingProcedures();

        Log.inf("Plugin enabled");
    }

    @Override
    public void onDisable() {
        Log.inf("Plugin disabled");
    }

    /*
    Check if the server is running SpigotMC, or any derivative software.
     */
    private boolean isRunningSpigot() {
        return ClassUtils.classExists("net.md_5.bungee.api.chat.TextComponent");
    }

    /*
    Ensure the server is running SpigotMC, or any derivative software.
     */
    private boolean assertRunningSpigot() {
        if(isRunningSpigot()) return true;

        Log.sev("LevelledMobs does not run on CraftBukkit or other software which is not " +
            "based upon the SpigotMC software. Switch to PaperMC or SpigotMC software - there " +
            "is no reason to run CraftBukkit.", false);

        return false;
    }

    private void runTestingProcedures() {
        Log.war("Running testing procedures", false);
    }

    /* getters and setters */

    public CommandHandler getCommandHandler() { return commandHandler; }
    public ConfigHandler getConfigHandler() { return configHandler; }
    public IntegrationHandler getIntegrationHandler() { return integrationHandler; }
    public ListenerHandler getListenerHandler() { return listenerHandler; }
    public LogicHandler getLogicHandler() { return logicHandler; }
    public Definitions getNmsDefinitions() { return nmsDefinitions; }
    public NametagSender getNametagSender() { return nametagSender; }

    /* singleton */

    private static LevelledMobs instance;

    public static @NotNull LevelledMobs getInstance() {
        return Objects.requireNonNull(instance, "instance");
    }

}
