package me.lokka30.levelledmobs;

import me.lokka30.levelledmobs.customdrops.CustomDropsHandler;
import me.lokka30.levelledmobs.listeners.ChunkLoadListener;
import me.lokka30.levelledmobs.listeners.EntityDamageDebugListener;
import me.lokka30.levelledmobs.managers.ExternalCompatibilityManager;
import me.lokka30.levelledmobs.managers.LevelManager;
import me.lokka30.levelledmobs.managers.MobDataManager;
import me.lokka30.levelledmobs.managers.WorldGuardManager;
import me.lokka30.levelledmobs.misc.ConfigUtils;
import me.lokka30.levelledmobs.misc.Utils;
import me.lokka30.microlib.QuickTimer;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * This is the main class of the plugin. Bukkit will call onLoad and onEnable on startup, and onDisable on shutdown.
 *
 * @author lokka30
 * @contributors stumper66
 */
public class LevelledMobs extends JavaPlugin {

    // Manager classes
    public final LevelInterface levelInterface = new LevelInterface(this);
    public LevelManager levelManager;
    public final MobDataManager mobDataManager = new MobDataManager(this);
    public WorldGuardManager worldGuardManager;
    public CustomDropsHandler customDropsHandler;
    public ChunkLoadListener chunkLoadListener;
    public final Companion companion = new Companion(this);

    // Configuration
    public YamlConfiguration settingsCfg;
    public YamlConfiguration messagesCfg;
    public YamlConfiguration attributesCfg;
    public YamlConfiguration dropsCfg;
    public YamlConfiguration customDropsCfg;
    public final ConfigUtils configUtils = new ConfigUtils(this);

    // Misc
    public EntityDamageDebugListener entityDamageDebugListener;
    public int incompatibilitiesAmount;
    private long loadTime;

    @Override
    public void onLoad() {
        Utils.logger.info("&f~ Initiating start-up procedure ~");
        final QuickTimer timer = new QuickTimer(); // Record how long it takes for the plugin to load.

        companion.checkWorldGuard(); // Do not move this from onLoad. It will not work otherwise.

        loadTime = timer.getTimer(); // combine the load time with enable time.
    }

    @Override
    public void onEnable() {
        final QuickTimer timer = new QuickTimer();

        companion.checkCompatibility();
        if (!companion.loadFiles()) {
            // had fatal error reading required files
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        companion.registerListeners();
        companion.registerCommands();
        companion.loadSpigotConfig();

        Utils.logger.info("&fStart-up: &7Running misc procedures...");
        if (ExternalCompatibilityManager.hasProtocolLibInstalled()) levelManager.startNametagAutoUpdateTask();
        companion.setupMetrics();
        companion.checkUpdates();

        loadTime += timer.getTimer();
        Utils.logger.info("&f~ Start-up complete, took &b" + loadTime + "ms&f ~");
    }

    @Override
    public void onDisable() {
        Utils.logger.info("&f~ Initiating shut-down procedure ~");

        final QuickTimer disableTimer = new QuickTimer();
        disableTimer.start();

        levelManager.stopNametagAutoUpdateTask();
        companion.shutDownAsyncTasks();

        Utils.logger.info("&f~ Shut-down complete, took &b" + disableTimer.getTimer() + "ms&f ~");
    }
}
