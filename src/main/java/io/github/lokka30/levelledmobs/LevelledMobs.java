package io.github.lokka30.levelledmobs;

import io.github.lokka30.levelledmobs.commands.LevelledMobsCommand;
import io.github.lokka30.levelledmobs.listeners.*;
import io.github.lokka30.levelledmobs.utils.ConfigUtils;
import io.github.lokka30.levelledmobs.utils.FileLoader;
import io.github.lokka30.levelledmobs.utils.Utils;
import me.lokka30.microlib.QuickTimer;
import me.lokka30.microlib.UpdateChecker;
import org.bstats.bukkit.Metrics;
import org.bukkit.Material;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;

/**
 * This is the main class of the plugin. Bukkit will call onLoad and onEnable on startup, and onDisable on shutdown.
 */
public class LevelledMobs extends JavaPlugin {

    public YamlConfiguration settingsCfg;
    public YamlConfiguration messagesCfg;
    public YamlConfiguration attributesCfg;
    public YamlConfiguration dropsCfg;
    public YamlConfiguration customDropsCfg;
    public ConfigUtils configUtils;
    public EntityDamageDebugListener entityDamageDebugListener;

    public MobDataManager mobDataManager;
    public LevelManager levelManager;

    public PluginManager pluginManager;

    public boolean hasWorldGuardInstalled;
    public boolean hasProtocolLibInstalled;
    public WorldGuardManager worldGuardManager;

    public boolean debugEntityDamageWasEnabled = false;

    public TreeMap<String, Integer> entityTypesLevelOverride_Min;
    public TreeMap<String, Integer> entityTypesLevelOverride_Max;
    public TreeMap<String, Integer> worldLevelOverride_Min;
    public TreeMap<String, Integer> worldLevelOverride_Max;
    public TreeMap<EntityType, List<CustomItemDrop>> customDropsitems;

    private long loadTime;

    public int incompatibilitiesAmount;

    public void onLoad() {
        Utils.logger.info("&f~ Initiating start-up procedure ~");

        QuickTimer loadTimer = new QuickTimer();
        loadTimer.start(); // Record how long it takes for the plugin to load.

        mobDataManager = new MobDataManager(this);
        levelManager = new LevelManager(this);

        // Hook into WorldGuard, register LM's flags.
        // This cannot be moved to onEnable (stated in WorldGuard's documentation).
        hasWorldGuardInstalled = getServer().getPluginManager().getPlugin("WorldGuard") != null;
        if (hasWorldGuardInstalled) {
            worldGuardManager = new WorldGuardManager(this);
        }

        hasProtocolLibInstalled = getServer().getPluginManager().getPlugin("ProtocolLib") != null;

        loadTime = loadTimer.getTimer(); // combine the load time with enable time.
    }

    public void onEnable() {
        QuickTimer enableTimer = new QuickTimer();
        enableTimer.start(); // Record how long it takes for the plugin to enable.

        checkCompatibility();
        loadFiles();
        registerListeners();
        registerCommands();
        if (hasProtocolLibInstalled) {
            levelManager.startNametagAutoUpdateTask();
        }

        Utils.logger.info("&fStart-up: &7Running misc procedures...");
        setupMetrics();
        checkUpdates();

        Utils.logger.info("&f~ Start-up complete, took &b" + (enableTimer.getTimer() + loadTime) + "ms&f ~");
    }

    public void onDisable() {
        Utils.logger.info("&f~ Initiating shut-down procedure ~");

        QuickTimer disableTimer = new QuickTimer();
        disableTimer.start();

        levelManager.stopNametagAutoUpdateTask();

        Utils.logger.info("&f~ Shut-down complete, took &b" + disableTimer.getTimer() + "ms&f ~");
    }

    //Checks if the server version is supported
    public void checkCompatibility() {
        Utils.logger.info("&fCompatibility Checker: &7Checking compatibility with your server...");

        // Using a List system in case more compatibility checks are added.
        List<String> incompatibilities = new ArrayList<>();

        // Check the MC version of the server.
        final String currentServerVersion = getServer().getVersion();
        boolean isRunningSupportedVersion = false;
        for (String supportedServerVersion : Utils.getSupportedServerVersions()) {
            if (currentServerVersion.contains(supportedServerVersion)) {
                isRunningSupportedVersion = true;
                break;
            }
        }
        if (!isRunningSupportedVersion) {
            incompatibilities.add("Your server version &8(&b" + currentServerVersion + "&8)&7 is unsupported by &bLevelledMobs v" + getDescription().getVersion() + "&7!" +
                    "Compatible MC versions: &b" + String.join(", ", Utils.getSupportedServerVersions()) + "&7.");
        }

        if (!hasProtocolLibInstalled) {
            incompatibilities.add("Your server does not have &bProtocolLib&7 installed! This means that no levelled nametags will appear on the mobs. If you wish to see custom nametags above levelled mobs, then you must install ProtocolLib.");
        }

        incompatibilitiesAmount = incompatibilities.size();
        if (incompatibilities.isEmpty()) {
            Utils.logger.info("&fCompatibility Checker: &7No incompatibilities found.");
        } else {
            Utils.logger.warning("&fCompatibility Checker: &7Found the following possible incompatibilities:");
            incompatibilities.forEach(incompatibility -> Utils.logger.info("&8 - &7" + incompatibility));
        }
    }

    // Note: also called by the reload subcommand.
    public void loadFiles() {
        Utils.logger.info("&fFile Loader: &7Loading files...");

        // save license.txt
        FileLoader.saveResourceIfNotExists(this, new File(getDataFolder(), "license.txt"));

        // load configurations
        settingsCfg = FileLoader.loadFile(this, "settings", FileLoader.SETTINGS_FILE_VERSION);
        messagesCfg = FileLoader.loadFile(this, "messages", FileLoader.MESSAGES_FILE_VERSION);

        this.entityTypesLevelOverride_Min = getMapFromConfigSection("entitytype-level-override.min-level");
        this.entityTypesLevelOverride_Max = getMapFromConfigSection("entitytype-level-override.max-level");
        this.worldLevelOverride_Min = getMapFromConfigSection("world-level-override.min-level");
        this.worldLevelOverride_Max = getMapFromConfigSection("world-level-override.max-level");
        this.customDropsitems = new TreeMap<>();

        // Replace/copy attributes file
        saveResource("attributes.yml", true);
        attributesCfg = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "attributes.yml"));

        // Replace/copy drops file
        saveResource("drops.yml", true);
        dropsCfg = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "drops.yml"));

        saveResource("customdrops.yml", false);
        customDropsCfg = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "customdrops.yml"));
        if (settingsCfg.getBoolean("use-custom-item-drops-for-mobs")) processCustomDropsConfig();

        // load configutils
        configUtils = new ConfigUtils(this);
    }

    private void processCustomDropsConfig(){
        //TODO: stumper66 is actively working on this section
        // this code currently works but is ugly and does no kind of data validation

        for (Map.Entry<String, Object> map: customDropsCfg.getValues(true).entrySet()){
            String mobType = map.getKey();
            EntityType entityType;
            try{
                entityType = EntityType.valueOf(mobType.toUpperCase()); }
            catch (Exception e){
                Utils.logger.warning("invalid mob type in customdrops.yml: " + mobType);
                continue;
            }

            List<CustomItemDrop> dropList = new ArrayList<>();

            // now we have the mob type start parsing the materials next
            ArrayList<Map<String, Object>> materials = (ArrayList<Map<String, Object>>) map.getValue();

            for (Map<String, Object> materialsMap : materials){
                for (String materialName : materialsMap.keySet()) {
                    Material material;
                    try {
                        material = Material.valueOf(materialName.toUpperCase());
                    } catch (Exception e) {
                        Utils.logger.warning(String.format("Invalid material type specified in customdrops.yml for mob: %s, %s", entityType, mobType));
                        continue;
                    }

                    CustomItemDrop item = new CustomItemDrop(entityType);
                    item.setMaterial(material);
                    Map<String, Object> materialAttributes = (Map<String, Object>) materialsMap.get(materialName);

                    for (String attribute : materialAttributes.keySet()) {
                        // example: amount

                        Object valueOrEnchant = materialAttributes.get(attribute);
                        if (attribute.equalsIgnoreCase("enchantments") && valueOrEnchant.getClass().equals(LinkedHashMap.class)) {
                            // enchantments here
                            Map<String, Object> enchantments = (Map<String, Object>) valueOrEnchant;
                            for (String enchantmentName : enchantments.keySet()) {
                                Object enchantLevelObj = enchantments.get(enchantmentName);
                                int enchantLevel = 1;
                                if (enchantLevelObj != null && Utils.isInteger(enchantLevelObj.toString()))
                                    enchantLevel = Integer.parseInt(enchantLevelObj.toString());

                                Enchantment en = getEnchantmentFromName(enchantmentName);
                                if (en == null) {
                                    Utils.logger.warning("invalid enchantment in customdrops.yml: " + enchantmentName);
                                    continue;
                                }
                                if (!en.canEnchantItem(item.getItemStack())) {
                                    Utils.logger.warning(String.format(
                                            "Enchantment %s in customdrops.yml: is not valid for item %s",
                                            enchantmentName, materialName));
                                    continue;
                                }

                                ItemMeta meta = item.getItemStack().getItemMeta();
                                // true is for ignoring level restriction
                                meta.addEnchant(en, enchantLevel, true);
                                item.getItemStack().setItemMeta(meta);
                            }
                        } else if (!attribute.equalsIgnoreCase("enchantments")) {
                            // non-enchantments here
                            // example 0.1
                            if (valueOrEnchant != null && Utils.isDouble(valueOrEnchant.toString())) {
                                double dValue = Double.parseDouble(valueOrEnchant.toString());
                                switch (attribute.toLowerCase()) {
                                    case "minlevel":
                                        item.minLevel = (int) dValue;
                                        break;
                                    case "maxlevel":
                                        item.maxLevel = (int) dValue;
                                        break;
                                    case "chance":
                                        item.dropChance = dValue;
                                        break;
                                    case "amount":
                                        item.amount = (int) dValue;
                                        if (item.amount > 64) item.amount = 64;
                                        else if (item.amount < 1) item.amount = 1;
                                        break;
                                    default:
                                        Utils.logger.warning("invalid attribute for " + mobType + " in customdrops.yml: " + attribute);
                                }
                            } else if (valueOrEnchant != null && "nomultiplier".equalsIgnoreCase(valueOrEnchant.toString())) {
                                item.noMultiplier = true;
                            }
                        }
                    } // next attribute
                    dropList.add(item);
                } // next material
            } // next material array
            if (dropList.size() > 0) customDropsitems.put(entityType, dropList);
        } // next mob

        if (settingsCfg.getStringList("debug-misc").contains("custom-drops")) {
            Utils.logger.info("custom drops count: " + customDropsitems.size());
            for (EntityType ent : customDropsitems.keySet()) {
                Utils.logger.info("mob: " + ent.name());
                for (CustomItemDrop item : customDropsitems.get(ent)) {
                    String msg = String.format("    %s, amount: %s, chance: %s, minL: %s, maxL: %s, noMulp: %s",
                            item.getMaterial(), item.amount, item.dropChance, item.minLevel, item.maxLevel, item.noMultiplier);
                    StringBuilder sb = new StringBuilder();
                    for (Enchantment enchant : item.getItemStack().getItemMeta().getEnchants().keySet()) {
                        if (sb.length() > 0) sb.append(", ");
                        sb.append(String.format("%s (%s)", enchant.getKey().getKey(), item.getItemStack().getItemMeta().getEnchants().get(enchant)));
                    }
                    Utils.logger.info(msg);
                    if (sb.length() > 0) Utils.logger.info("         " + sb.toString());
                }
            }
        }
    }

    private static Enchantment getEnchantmentFromName(String name){

        switch (name.replace(" ", "_").toLowerCase()){
            case "arrow_damage": return Enchantment.ARROW_DAMAGE;
            case "arrow_fire": return Enchantment.ARROW_FIRE;
            case "arrow_infinity": case "infinity":
                return Enchantment.ARROW_INFINITE;
            case "binding": case "binding_curse":
                return Enchantment.BINDING_CURSE;
            case "arrow_knockback": case "punch":
                return Enchantment.ARROW_KNOCKBACK;
            case "channeling": return Enchantment.CHANNELING;
            case "damage_all": return Enchantment.DAMAGE_ALL;
            case "damage_arthropods": case "bane_of_arthopods":
                return Enchantment.DAMAGE_ARTHROPODS;
            case "damage_undead": case "smite":
                return Enchantment.DAMAGE_UNDEAD;
            case "depth_strider": return Enchantment.DEPTH_STRIDER;
            case "dig_speed": case "efficiency":
                return Enchantment.DIG_SPEED;
            case "durability": case "unbreaking":
                return Enchantment.DURABILITY;
            case "fire_aspect": return Enchantment.FIRE_ASPECT;
            case "frost_walker": return Enchantment.FROST_WALKER;
            case "impaling": return Enchantment.IMPALING;
            case "knockback": return Enchantment.KNOCKBACK;
            case "loot_bonus_blocks": case "looting":
                return Enchantment.LOOT_BONUS_BLOCKS;
            case "loyalty": return Enchantment.LOYALTY;
            case "luck": case "luck_of_the_sea":
                return Enchantment.LUCK;
            case "lure": return Enchantment.LURE;
            case "mending": return Enchantment.MENDING;
            case "multishot": return Enchantment.MULTISHOT;
            case "piercing": return Enchantment.PIERCING;
            case "protection_environmental": case "protection":
                return Enchantment.PROTECTION_ENVIRONMENTAL;
            case "protection_explosions": case "blast_protection":
                return Enchantment.PROTECTION_EXPLOSIONS;
            case "protection_fall": case "feather_falling":
                return Enchantment.PROTECTION_FALL;
            case "quick_charge": return Enchantment.QUICK_CHARGE;
            case "riptide": return Enchantment.RIPTIDE;
            case "silk_touch": return Enchantment.SILK_TOUCH;
            case "soul_speed": return Enchantment.SOUL_SPEED;
            case "sweeping_edge": return Enchantment.SWEEPING_EDGE;
            case "thorns": return Enchantment.THORNS;
            case "vanishing_curse": case "curse of vanishing":
                return Enchantment.VANISHING_CURSE;
            case "water_worker": case "respiration":
                return Enchantment.WATER_WORKER;
            default:
                return null;
        }
    }

    private TreeMap<String, Integer> getMapFromConfigSection(String configPath){
        TreeMap<String, Integer> result = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        ConfigurationSection cs = settingsCfg.getConfigurationSection(configPath);
        if (cs == null){ return result; }

        Set<String> set = cs.getKeys(false);

        for (String item : set) {
            Object value = cs.get(item);
            if (value != null && Utils.isInteger(value.toString())) {
                result.put(item, Integer.parseInt(value.toString()));
            }
        }

        return result;
    }

    private void registerListeners() {
        Utils.logger.info("&fListeners: &7Registering event listeners...");

        pluginManager = getServer().getPluginManager();

        levelManager.creatureSpawnListener = new CreatureSpawnListener(this); // we're saving this reference so the summon command has access to it
        entityDamageDebugListener = new EntityDamageDebugListener(this);

        if (settingsCfg.getBoolean("debug-entity-damage")) {
            // we'll load and unload this listener based on the above setting when reloading
            debugEntityDamageWasEnabled = true;
            pluginManager.registerEvents(this.entityDamageDebugListener, this);
        }

        pluginManager.registerEvents(levelManager.creatureSpawnListener, this);
        pluginManager.registerEvents(new EntityDamageListener(this), this);
        pluginManager.registerEvents(new EntityDeathListener(this), this);
        pluginManager.registerEvents(new EntityRegainHealthListener(this), this);
        pluginManager.registerEvents(new PlayerJoinWorldNametagListener(this), this);
        pluginManager.registerEvents(new EntityTransformListener(this), this);
        pluginManager.registerEvents(new EntityNametagListener(this), this);
        pluginManager.registerEvents(new EntityTargetListener(this), this);
        pluginManager.registerEvents(new PlayerJoinListener(this), this);
    }

    private void registerCommands() {
        Utils.logger.info("&fCommands: &7Registering commands...");

        PluginCommand levelledMobsCommand = getCommand("levelledmobs");
        if (levelledMobsCommand == null) {
            Utils.logger.error("Command &b/levelledmobs&7 is unavailable, is it not registered in plugin.yml?");
        } else {
            levelledMobsCommand.setExecutor(new LevelledMobsCommand(this));
        }
    }

    private void setupMetrics() {
        new Metrics(this, 6269);
    }

    //Check for updates on the Spigot page.
    private void checkUpdates() {
        if (settingsCfg.getBoolean("use-update-checker")) {
            UpdateChecker updateChecker = new UpdateChecker(this, 74304);
            updateChecker.getLatestVersion(latestVersion -> {
                if (!updateChecker.getCurrentVersion().equals(latestVersion)) {
                    Utils.logger.warning("&fUpdate Checker: &7The plugin has an update available! You're running &bv" + updateChecker.getCurrentVersion() + "&7, latest version is &bv" + latestVersion + "&7.");
                }
            });
        }
    }


}
