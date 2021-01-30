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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    public boolean hasMythicMobsInstalled;
    public WorldGuardManager worldGuardManager;
    public MythicMobsHelper mythicMobsHelper;

    public boolean debugEntityDamageWasEnabled = false;

    public TreeMap<String, Integer> entityTypesLevelOverride_Min;
    public TreeMap<String, Integer> entityTypesLevelOverride_Max;
    public TreeMap<String, Integer> worldLevelOverride_Min;
    public TreeMap<String, Integer> worldLevelOverride_Max;
    public TreeMap<EntityType, List<CustomItemDrop>> customDropsitems;
    public TreeMap<CustomDropsUniversalGroups, List<CustomItemDrop>> customDropsitems_groups;
    public HashSet<EntityType> groups_HostileMobs;
    public HashSet<EntityType> groups_AquaticMobs;
    public HashSet<EntityType> groups_PassiveMobs;
    public HashSet<EntityType> groups_NetherMobs;

    private long loadTime;

    public int incompatibilitiesAmount;

    public void onLoad() {
        Utils.logger.info("&f~ Initiating start-up procedure ~");

        final QuickTimer loadTimer = new QuickTimer();
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
        final QuickTimer enableTimer = new QuickTimer();
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
        buildUniversalGroups();
        hasMythicMobsInstalled = pluginManager.getPlugin("MythicMobs") != null;
        if (hasMythicMobsInstalled) {
            this.mythicMobsHelper = new MythicMobsHelper(this);
        }

        Utils.logger.info("&f~ Start-up complete, took &b" + (enableTimer.getTimer() + loadTime) + "ms&f ~");
    }

    public void onDisable() {
        Utils.logger.info("&f~ Initiating shut-down procedure ~");

        final QuickTimer disableTimer = new QuickTimer();
        disableTimer.start();

        levelManager.stopNametagAutoUpdateTask();

        Utils.logger.info("&f~ Shut-down complete, took &b" + disableTimer.getTimer() + "ms&f ~");
    }

    //Checks if the server version is supported
    public void checkCompatibility() {
        Utils.logger.info("&fCompatibility Checker: &7Checking compatibility with your server...");

        // Using a List system in case more compatibility checks are added.
        final List<String> incompatibilities = new ArrayList<>();

        // Check the MC version of the server.
        final String currentServerVersion = getServer().getVersion();
        boolean isRunningSupportedVersion = false;
        for (final String supportedServerVersion : Utils.getSupportedServerVersions()) {
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
        settingsCfg = FileLoader.loadFile(this, "settings", FileLoader.SETTINGS_FILE_VERSION, true);
        messagesCfg = FileLoader.loadFile(this, "messages", FileLoader.MESSAGES_FILE_VERSION, false);

        this.entityTypesLevelOverride_Min = getMapFromConfigSection("entitytype-level-override.min-level");
        this.entityTypesLevelOverride_Max = getMapFromConfigSection("entitytype-level-override.max-level");
        this.worldLevelOverride_Min = getMapFromConfigSection("world-level-override.min-level");
        this.worldLevelOverride_Max = getMapFromConfigSection("world-level-override.max-level");
        this.customDropsitems = new TreeMap<>();
        this.customDropsitems_groups = new TreeMap<>();

        // Replace/copy attributes file
        saveResource("attributes.yml", true);
        attributesCfg = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "attributes.yml"));

        // Replace/copy drops file
        saveResource("drops.yml", true);
        dropsCfg = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "drops.yml"));

        final File customDropsFile = new File(getDataFolder(), "customdrops.yml");
        if (!customDropsFile.exists()) saveResource("customdrops.yml", false);
        customDropsCfg = YamlConfiguration.loadConfiguration(customDropsFile);
        if (settingsCfg.getBoolean("use-custom-item-drops-for-mobs")) processCustomDropsConfig();

        // load configutils
        configUtils = new ConfigUtils(this);
    }

    private void processCustomDropsConfig(){

        for (final Map.Entry<String, Object> map: customDropsCfg.getValues(true).entrySet()){
            final String mobTypeTemp = map.getKey();
            String[] mobTypes;
            EntityType entityType = null;
            if (mobTypeTemp.contains(";"))  mobTypes = mobTypeTemp.split(";");
            else mobTypes = new String[]{ mobTypeTemp };

            for (final String type : mobTypes) {
                final String mobType = type.trim();
                if (mobType.equals("")) continue;

                CustomDropsUniversalGroups universalGroup = null;
                final boolean isUniversalGroup = mobType.toLowerCase().startsWith("all_");

                if (isUniversalGroup) {
                    try {
                        universalGroup = CustomDropsUniversalGroups.valueOf(mobType.toUpperCase());
                    } catch (Exception e) {
                        Utils.logger.warning("invalid universal group in customdrops.yml: " + mobType);
                        continue;
                    }
                } else {
                    try {
                        entityType = EntityType.valueOf(mobType.toUpperCase());
                    } catch (Exception e) {
                        Utils.logger.warning("invalid mob type in customdrops.yml: " + mobType);
                        continue;
                    }
                }

                List<CustomItemDrop> dropList;

                if (isUniversalGroup) {
                    dropList = customDropsitems_groups.containsKey(universalGroup) ? customDropsitems_groups.get(universalGroup) : new ArrayList<>();
                    if (processCustomDropsConfig2(universalGroup, null, map.getValue(), dropList))
                        customDropsitems_groups.put(universalGroup, dropList);
                } else {
                    dropList = customDropsitems.containsKey(entityType) ? customDropsitems.get(entityType) : new ArrayList<>();
                    if (processCustomDropsConfig2(null, entityType, map.getValue(), dropList))
                        customDropsitems.put(entityType, dropList);
                }
            }
        } // next mob

        if (settingsCfg.getStringList("debug-misc").contains("custom-drops")) {
            Utils.logger.info(String.format("custom drops count: %s, custom groups drops counts: %s",
                    customDropsitems.size(), customDropsitems_groups.size()));

            showCustomDropsDebugInfo();
        }
    }

    private boolean processCustomDropsConfig2(final CustomDropsUniversalGroups entityGroup, final EntityType entityType, final Object configValue, final List<CustomItemDrop> dropList){
        ArrayList<Map<String, Object>> map2 = null;
        ArrayList<String> materialsStrings = null;
        boolean isMap = false;
        boolean addedDrop = false;
        final String mobTypeOrGroupName = entityType == null ? entityGroup.name() : entityType.name();

        // the below try block is to determine which generics class is returned since this is impossible to check at runtime
        // note if it's the wrong cast it will be thrown when attempting to enumerate the collection

        try{
            // I can't get the IDE to shutup about the potential cast exception!
            map2 = (ArrayList<Map<String, Object>>) configValue;
            for (Map<String, Object> ignored : map2) break;
            isMap = true;
        }
        catch (ClassCastException e){
            try{
                // I can't get the IDE to shutup about the potential cast exception!
                materialsStrings = (ArrayList<String>) configValue;
            }
            catch (ClassCastException ex){
                Utils.logger.warning("Unable to parse values (cast exception) for " + mobTypeOrGroupName);
                return false;
            }
        }

        // the IDE complains isMap will always be true, but it's a lie!
        if (!isMap){
            // was just a simple assignment without any specifics
            CustomItemDrop item;
            if (entityType == null) item = new CustomItemDrop(entityGroup);
            else item = new CustomItemDrop(entityType);

            for (final String str : materialsStrings){
                Material material;
                try {
                    material = Material.valueOf(str.toUpperCase());
                } catch (Exception e) {
                    Utils.logger.warning(String.format("Invalid material type specified in customdrops.yml for: %s, %s", mobTypeOrGroupName, str));
                    continue;
                }
                item.setMaterial(material);
                dropList.add(item);
                addedDrop = true;
            }
            return addedDrop;
        }

        // here we'll start parsing attributes, etc
        for (final Map<String, Object> materialsMap : map2){
            for (final String materialName : materialsMap.keySet()) {
                Material material;
                try {
                    material = Material.valueOf(materialName.toUpperCase());
                } catch (Exception e) {
                    Utils.logger.warning(String.format("Invalid material type specified in customdrops.yml for: %s, %s", mobTypeOrGroupName, materialName));
                    continue;
                }

                CustomItemDrop item;
                if (entityType == null) item = new CustomItemDrop(entityGroup);
                else item = new CustomItemDrop(entityType);

                item.setMaterial(material);
                Map<String, Object> materialAttributes;

                try {
                    materialAttributes = (Map<String, Object>) materialsMap.get(materialName);
                }
                catch (ClassCastException e){
                    Utils.logger.warning("Unable to parse values (cast exception2) for " + mobTypeOrGroupName);
                    return false;
                }

                if (materialAttributes == null) {
                    // there is a colon after the material name but no attributes were specified
                    dropList.add(item);
                    addedDrop = true;
                    continue;
                }

                for (final String attribute : materialAttributes.keySet()) {
                    // example: amount

                    final Object valueOrEnchant = materialAttributes.get(attribute);
                    if (attribute.equalsIgnoreCase("enchantments") && valueOrEnchant.getClass().equals(LinkedHashMap.class)) {
                        // enchantments here
                        Map<String, Object> enchantments;
                        try {
                            enchantments = (Map<String, Object>) valueOrEnchant;
                        }
                        catch (ClassCastException e){
                            Utils.logger.warning("Unable to parse values (cast exception3) for " + mobTypeOrGroupName);
                            return false;
                        }

                        for (final String enchantmentName : enchantments.keySet()) {
                            Object enchantLevelObj = enchantments.get(enchantmentName);
                            int enchantLevel = 1;
                            if (enchantLevelObj != null && Utils.isInteger(enchantLevelObj.toString()))
                                enchantLevel = Integer.parseInt(enchantLevelObj.toString());

                            final Enchantment en = getEnchantmentFromName(enchantmentName);
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

                            final ItemMeta meta = item.getItemStack().getItemMeta();
                            if (meta != null) {
                                // true is for ignoring level restriction
                                meta.addEnchant(en, enchantLevel, true);
                                item.getItemStack().setItemMeta(meta);
                            }
                        }
                    } else if (!attribute.equalsIgnoreCase("enchantments")) {
                        // non-enchantments here
                        // example 0.1
                        if (valueOrEnchant != null && Utils.isDouble(valueOrEnchant.toString())) {
                            final double dValue = Double.parseDouble(valueOrEnchant.toString());
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
                                    Utils.logger.warning("invalid attribute for " + mobTypeOrGroupName + " in customdrops.yml: " + attribute);
                            }
                        }
                        else if (valueOrEnchant != null && valueOrEnchant.toString().contains("-")){
                            if (!item.setAmountRangeFromString(valueOrEnchant.toString()))
                                Utils.logger.warning(String.format("Invalid number range for %s, %s", mobTypeOrGroupName, valueOrEnchant));
                        }
                        else if ("nomultiplier".equalsIgnoreCase(attribute)) {
                            item.noMultiplier = true;
                        }
                        else if ("nospawner".equalsIgnoreCase(attribute)) {
                            item.noSpawner = true;
                        }
                    }
                } // next attribute
                dropList.add(item);
                addedDrop = true;
            } // next material
        } // next material array

        return addedDrop;
    }

    private void showCustomDropsDebugInfo(){
        for (final EntityType ent : customDropsitems.keySet()) {
            Utils.logger.info("mob: " + ent.name());
            for (final CustomItemDrop item : customDropsitems.get(ent)) {
                final String msg = String.format("    %s, amount: %s, chance: %s, minL: %s, maxL: %s, noMulp: %s",
                        item.getMaterial(), item.getAmountAsString(), item.dropChance, item.minLevel, item.maxLevel, item.noMultiplier);
                final StringBuilder sb = new StringBuilder();
                final ItemMeta meta = item.getItemStack().getItemMeta();
                if (meta != null) {
                    for (final Enchantment enchant : meta.getEnchants().keySet()) {
                        if (sb.length() > 0) sb.append(", ");
                        sb.append(String.format("%s (%s)", enchant.getKey().getKey(), item.getItemStack().getItemMeta().getEnchants().get(enchant)));
                    }
                }
                Utils.logger.info(msg);
                if (sb.length() > 0) Utils.logger.info("         " + sb.toString());
            }
        }

        for (final CustomDropsUniversalGroups group : customDropsitems_groups.keySet()) {
            Utils.logger.info("group: " + group.name());
            for (final CustomItemDrop item : customDropsitems_groups.get(group)) {
                final String msg = String.format("    %s, amount: %s, chance: %s, minL: %s, maxL: %s, noMulp: %s, noSpawn: %s",
                        item.getMaterial(), item.getAmountAsString(), item.dropChance, item.minLevel, item.maxLevel, item.noMultiplier, item.noSpawner);
                final StringBuilder sb = new StringBuilder();
                final ItemMeta meta = item.getItemStack().getItemMeta();
                if (meta != null) {
                    for (final Enchantment enchant : meta.getEnchants().keySet()) {
                        if (sb.length() > 0) sb.append(", ");
                        sb.append(String.format("%s (%s)", enchant.getKey().getKey(), item.getItemStack().getItemMeta().getEnchants().get(enchant)));
                    }
                }
                Utils.logger.info(msg);
                if (sb.length() > 0) Utils.logger.info("         " + sb.toString());
            }
        }
    }

    private void buildUniversalGroups(){

        // include interfaces: Monster, Boss
        groups_HostileMobs = Stream.of(
                EntityType.ENDER_DRAGON,
                EntityType.GHAST,
                EntityType.HOGLIN,
                EntityType.MAGMA_CUBE,
                EntityType.PHANTOM,
                EntityType.SHULKER,
                EntityType.SLIME
        ).collect(Collectors.toCollection(HashSet::new));

        // include interfaces: Animals, WaterMob
        groups_PassiveMobs = Stream.of(
                EntityType.IRON_GOLEM,
                EntityType.SNOWMAN,
                EntityType.ZOMBIFIED_PIGLIN,
                EntityType.STRIDER
        ).collect(Collectors.toCollection(HashSet::new));

        // include interfaces: WaterMob
        groups_AquaticMobs = Stream.of(
                EntityType.DROWNED,
                EntityType.ELDER_GUARDIAN,
                EntityType.GUARDIAN,
                EntityType.TURTLE
        ).collect(Collectors.toCollection(HashSet::new));
    }

    @Nullable
    private static Enchantment getEnchantmentFromName(final String name){

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
            case "damage_all": case "sharpness":
                return Enchantment.DAMAGE_ALL;
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

    @Nonnull
    private TreeMap<String, Integer> getMapFromConfigSection(final String configPath){
        final TreeMap<String, Integer> result = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        final ConfigurationSection cs = settingsCfg.getConfigurationSection(configPath);
        if (cs == null) return result;

        final Set<String> set = cs.getKeys(false);

        for (final String item : set) {
            final Object value = cs.get(item);
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

        final PluginCommand levelledMobsCommand = getCommand("levelledmobs");
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
            final UpdateChecker updateChecker = new UpdateChecker(this, 74304);
            updateChecker.getLatestVersion(latestVersion -> {
                if (!updateChecker.getCurrentVersion().equals(latestVersion)) {
                    Utils.logger.warning("&fUpdate Checker: &7The plugin has an update available! You're running &bv" + updateChecker.getCurrentVersion() + "&7, latest version is &bv" + latestVersion + "&7.");
                }
            });
        }
    }


}
