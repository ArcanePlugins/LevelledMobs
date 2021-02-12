package io.github.lokka30.levelledmobs;

import io.github.lokka30.levelledmobs.commands.LevelledMobsCommand;
import io.github.lokka30.levelledmobs.listeners.*;
import io.github.lokka30.levelledmobs.utils.ConfigUtils;
import io.github.lokka30.levelledmobs.utils.FileLoader;
import io.github.lokka30.levelledmobs.utils.MC1_16_Compat;
import io.github.lokka30.levelledmobs.utils.Utils;
import me.lokka30.microlib.MicroUtils;
import me.lokka30.microlib.QuickTimer;
import me.lokka30.microlib.UpdateChecker;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
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
    public boolean useProtocolLib;
    public boolean hasProtocolLibInstalled;
    public boolean hasMythicMobsInstalled;
    public WorldGuardManager worldGuardManager;
    public MythicMobsHelper mythicMobsHelper;
    public boolean isMCVersion_16_OrHigher;
    public MC1_16_Compat mc1_16_Compat;

    // need to move these
    public boolean debugEntityDamageWasEnabled = false;
    public TreeMap<String, Integer> entityTypesLevelOverride_Min;
    public TreeMap<String, Integer> entityTypesLevelOverride_Max;
    public TreeMap<String, Integer> worldLevelOverride_Min;
    public TreeMap<String, Integer> worldLevelOverride_Max;
    public Set<String> noDropMultiplierEntities;
    public TreeMap<EntityType, CustomDropInstance> customDropsitems;
    public TreeMap<CustomDropsUniversalGroups, CustomDropInstance> customDropsitems_groups;
    public HashSet<EntityType> groups_HostileMobs;
    public HashSet<EntityType> groups_AquaticMobs;
    public HashSet<EntityType> groups_PassiveMobs;
    public HashSet<EntityType> groups_NetherMobs;

    private long loadTime;

    public int incompatibilitiesAmount;

    @Override
    public void onLoad() {
        Utils.logger.info("&f~ Initiating start-up procedure ~");

        final QuickTimer loadTimer = new QuickTimer();
        loadTimer.start(); // Record how long it takes for the plugin to load.

        mobDataManager = new MobDataManager(this);
        levelManager = new LevelManager(this);

        pluginManager = getServer().getPluginManager();

        // Hook into WorldGuard, register LM's flags.
        // This cannot be moved to onEnable (stated in WorldGuard's documentation).
        hasWorldGuardInstalled = pluginManager.getPlugin("WorldGuard") != null;
        if (hasWorldGuardInstalled) {
            worldGuardManager = new WorldGuardManager(this);
        }

        hasProtocolLibInstalled = pluginManager.getPlugin("ProtocolLib") != null;
        useProtocolLib = hasProtocolLibInstalled;

        loadTime = loadTimer.getTimer(); // combine the load time with enable time.
    }

    @Override
    public void onEnable() {
        final QuickTimer enableTimer = new QuickTimer();
        enableTimer.start(); // Record how long it takes for the plugin to enable.

        checkCompatibility();
        if (!loadFiles()){
            // had fatal error reading required files
            this.setEnabled(false);
            return;
        }
        buildUniversalGroups();
        hasMythicMobsInstalled = pluginManager.getPlugin("MythicMobs") != null;
        if (hasMythicMobsInstalled) {
            this.mythicMobsHelper = new MythicMobsHelper(this);
        }

        if ("disabled".equalsIgnoreCase(settingsCfg.getString("creature-nametag"))){
            // we're resort to same behavior as protocollib not installed to disable nametags
            this.useProtocolLib = false;
        }

        registerListeners();
        registerCommands();
        if (useProtocolLib) levelManager.startNametagAutoUpdateTask();

        Utils.logger.info("&fStart-up: &7Running misc procedures...");
        setupMetrics();
        checkUpdates();

        Utils.logger.info("&f~ Start-up complete, took &b" + (enableTimer.getTimer() + loadTime) + "ms&f ~");
    }

    @Override
    public void onDisable() {
        Utils.logger.info("&f~ Initiating shut-down procedure ~");

        final QuickTimer disableTimer = new QuickTimer();
        disableTimer.start();

        levelManager.stopNametagAutoUpdateTask();
        shutDownAsyncTasks();

        Utils.logger.info("&f~ Shut-down complete, took &b" + disableTimer.getTimer() + "ms&f ~");
    }

    //Checks if the server version is supported
    public void checkCompatibility() {
        Utils.logger.info("&fCompatibility Checker: &7Checking compatibility with your server...");

        final String[] bukkitVer = Bukkit.getBukkitVersion().split("\\.");
        final int middleVer = Integer.parseInt(bukkitVer[1]);
        if (middleVer >= 16) {
            this.isMCVersion_16_OrHigher = true;
            this.mc1_16_Compat = new MC1_16_Compat();
        }

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
    public boolean loadFiles() {
        Utils.logger.info("&fFile Loader: &7Loading files...");

        // save license.txt
        FileLoader.saveResourceIfNotExists(this, new File(getDataFolder(), "license.txt"));

        // load configurations
        settingsCfg = FileLoader.loadFile(this, "settings", FileLoader.SETTINGS_FILE_VERSION, true);

        if (settingsCfg != null) // only load if settings were loaded successfully
            messagesCfg = FileLoader.loadFile(this, "messages", FileLoader.MESSAGES_FILE_VERSION, true);
        else {
            // had an issue reading the file.  Disable the plugin now
            return false;
        }

        customDropsCfg = FileLoader.loadFile(this, "customdrops", FileLoader.CUSTOMDROPS_FILE_VERSION, true);

        this.entityTypesLevelOverride_Min = getMapFromConfigSection("entitytype-level-override.min-level");
        this.entityTypesLevelOverride_Max = getMapFromConfigSection("entitytype-level-override.max-level");
        this.worldLevelOverride_Min = getMapFromConfigSection("world-level-override.min-level");
        this.worldLevelOverride_Max = getMapFromConfigSection("world-level-override.max-level");
        this.noDropMultiplierEntities = getSetFromConfigSection("no-drop-multipler-entities");
        this.customDropsitems = new TreeMap<>();
        this.customDropsitems_groups = new TreeMap<>();

        // Replace/copy attributes file
        saveResource("attributes.yml", true);
        attributesCfg = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "attributes.yml"));

        // Replace/copy drops file
        saveResource("drops.yml", true);
        dropsCfg = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "drops.yml"));

        if (settingsCfg.getBoolean("use-custom-item-drops-for-mobs") && customDropsCfg != null)
            parseCustomDrops(customDropsCfg);

        // load configutils
        configUtils = new ConfigUtils(this);

        return true;
    }

    private void parseCustomDrops(ConfigurationSection config){
        for (String item : config.getKeys(false)) {
            String[] mobTypeOrGroups;
            EntityType entityType = null;
            mobTypeOrGroups = item.split(";");

            for (String mobTypeOrGroup : mobTypeOrGroups) {
                mobTypeOrGroup = mobTypeOrGroup.trim();
                if ("".equals(mobTypeOrGroup)) continue;
                if (mobTypeOrGroup.startsWith("file-version")) continue;

                CustomDropsUniversalGroups universalGroup = null;
                final boolean isUniversalGroup = mobTypeOrGroup.toLowerCase().startsWith("all_");
                CustomDropInstance dropInstance;

                if (isUniversalGroup) {
                    try {
                        universalGroup = CustomDropsUniversalGroups.valueOf(mobTypeOrGroup.toUpperCase());
                    } catch (Exception e) {
                        Utils.logger.warning("invalid universal group in customdrops.yml: " + mobTypeOrGroup);
                        continue;
                    }
                    dropInstance = new CustomDropInstance(universalGroup);
                } else {
                    try {
                        entityType = EntityType.valueOf(mobTypeOrGroup.toUpperCase());
                    } catch (Exception e) {
                        Utils.logger.warning("invalid mob type in customdrops.yml: " + mobTypeOrGroup);
                        continue;
                    }
                    dropInstance = new CustomDropInstance(entityType);
                }

                parseCustomDrops2(config.getList(item), dropInstance);

                if (!dropInstance.customItems.isEmpty()) {
                    if (isUniversalGroup)
                        customDropsitems_groups.put(universalGroup, dropInstance);
                    else
                        customDropsitems.put(entityType, dropInstance);
                }
            } // next mob or group
        } // next root item from file

        if (settingsCfg.getStringList("debug-misc").contains("custom-drops")) {
            Utils.logger.info(String.format("custom drops count: %s, custom groups drops counts: %s",
                    customDropsitems.size(), customDropsitems_groups.size()));

            showCustomDropsDebugInfo();
        }
    }

    private void parseCustomDrops2(final List<?> itemConfigurations, final CustomDropInstance dropInstance){

        if (itemConfigurations == null) {
            Utils.logger.info("itemconfigs was null");
            return;
        }

        for (final Object itemObject : itemConfigurations) {

            if (itemObject instanceof String) {
                // just the string was given
                final CustomItemDrop item = new CustomItemDrop();
                final String materialName = (String) itemObject;

                if ("override".equalsIgnoreCase(materialName)){
                    dropInstance.overrideStockDrops = true;
                    continue;
                }

                Material material;
                try {
                    material = Material.valueOf(materialName.toUpperCase());
                } catch (Exception e) {
                    Utils.logger.warning(String.format("Invalid material type specified in customdrops.yml for: %s, %s", dropInstance.getMobOrGroupName(), materialName));
                    continue;
                }
                item.setMaterial(material);
                dropInstance.customItems.add(item);
                continue;
            }
            final ConfigurationSection itemConfiguration = objectToConfigurationSection(itemObject);
            if (itemConfiguration == null) continue;

            for (final Map.Entry<String,Object> itemEntry : itemConfiguration.getValues(false).entrySet()) {

                final String materialName = itemEntry.getKey();
                final ConfigurationSection itemInfoConfiguration = objectToConfigurationSection(itemEntry.getValue());
                if (itemInfoConfiguration == null) continue;

                final CustomItemDrop item = new CustomItemDrop();

                Material material;
                try {
                    material = Material.valueOf(materialName.toUpperCase());
                } catch (Exception e) {
                    Utils.logger.warning(String.format("Invalid material type specified in customdrops.yml for: %s, %s", dropInstance.getMobOrGroupName(), materialName));
                    continue;
                }
                item.setMaterial(material);
                dropInstance.customItems.add(item);

                item.setAmount(itemInfoConfiguration.getInt("amount", 1));
                item.dropChance = itemInfoConfiguration.getDouble("chance", 0.2);
                item.minLevel = itemInfoConfiguration.getInt("minlevel", -1);
                item.maxLevel = itemInfoConfiguration.getInt("maxlevel", -1);
                item.groupId = itemInfoConfiguration.getInt("groupid", -1);
                item.setDamage(itemInfoConfiguration.getInt("damage", 0));
                item.lore = itemInfoConfiguration.getStringList("lore");
                item.noMultiplier = itemInfoConfiguration.getBoolean("nomultipler");
                item.noSpawner = itemInfoConfiguration.getBoolean("nospawner");
                item.customName = itemInfoConfiguration.getString("name");
                item.isEquipped = itemInfoConfiguration.getBoolean("equipped");

                String numberRange = itemInfoConfiguration.getString("amount");
                if (numberRange != null && numberRange.contains("-") && !item.setAmountRangeFromString(numberRange)){
                    Utils.logger.warning(String.format("Invalid number range for amount on %s, %s", dropInstance.getMobOrGroupName(), numberRange));
                }

                numberRange = itemInfoConfiguration.getString("damage");
                if (numberRange != null && numberRange.contains("-") && !item.setDamageRangeFromString(numberRange)){
                    Utils.logger.warning(String.format("Invalid number range for damage on %s, %s", dropInstance.getMobOrGroupName(), numberRange));
                }

                final Object enchantmentsSection = itemInfoConfiguration.get("enchantments");
                if (enchantmentsSection != null){
                    final ConfigurationSection enchantments = objectToConfigurationSection(enchantmentsSection);
                    if (enchantments != null) {
                        final Map<String, Object> enchantMap = enchantments.getValues(false);
                        for (final String enchantName : enchantMap.keySet()) {
                            final Object value = enchantMap.get(enchantName);

                            int enchantLevel = 1;
                            if (value != null && Utils.isInteger(value.toString()))
                                enchantLevel = Integer.parseInt(value.toString());

                            final Enchantment en = getEnchantmentFromName(enchantName);
                            if (en != null)
                                item.getItemStack().addUnsafeEnchantment(en, enchantLevel);
                        }
                    }
                } // end enchantments

                // set item attributes, etc here:

                if (item.getDamage() != 0 || item.getHasDamageRange()){
                    ItemMeta meta = item.getItemStack().getItemMeta();
                    if (meta instanceof Damageable){
                        int damageAmount = item.getDamage();
                        if (item.getHasDamageRange())
                            damageAmount = ThreadLocalRandom.current().nextInt(item.getDamageRangeMin(), item.getDamageRangeMax() + 1);

                        ((Damageable) meta).setDamage(damageAmount);
                        item.getItemStack().setItemMeta(meta);
                    }
                }
                if (item.lore != null && !item.lore.isEmpty()){
                    ItemMeta meta = item.getItemStack().getItemMeta();
                    if (meta != null) {
                        meta.setLore(Utils.colorizeAllInList(item.lore));
                        item.getItemStack().setItemMeta(meta);
                    }
                }

                if (item.customName != null && !"".equals(item.customName)){
                    ItemMeta meta = item.getItemStack().getItemMeta();
                    if (meta != null) {
                        meta.setDisplayName(MicroUtils.colorize(item.customName));
                        item.getItemStack().setItemMeta(meta);
                    }
                }
            }
        } // next item
    }

    private  ConfigurationSection objectToConfigurationSection(Object object){
        if (object instanceof ConfigurationSection) {
            return (ConfigurationSection) object;
        } else if (object instanceof Map){
            final MemoryConfiguration result = new MemoryConfiguration();
            result.addDefaults((Map<String, Object>) object);
            return result.getDefaultSection();
        } else {
            Utils.logger.warning("couldn't parse Config of type: " + object.getClass().getSimpleName());
            return null;
        }
    }

    private void showCustomDropsDebugInfo(){
        for (final EntityType ent : customDropsitems.keySet()) {
            final String override = customDropsitems.get(ent).overrideStockDrops ? " (override)" : "";
            Utils.logger.info("mob: " + ent.name() + override);
            for (final CustomItemDrop item : customDropsitems.get(ent).customItems) {
                showCustomDropsDebugInfo2(item);
            }
        }

        for (final CustomDropsUniversalGroups group : customDropsitems_groups.keySet()) {
            final String override = customDropsitems_groups.get(group).overrideStockDrops ? " (override)" : "";
            Utils.logger.info("group: " + group.name() + override);
            for (final CustomItemDrop item : customDropsitems_groups.get(group).customItems) {
                showCustomDropsDebugInfo2(item);
            }
        }
    }

    private void showCustomDropsDebugInfo2(final CustomItemDrop item){
        String msg = String.format("    %s, amount: %s, chance: %s, minL: %s, maxL: %s",
                item.getMaterial(), item.getAmountAsString(), item.dropChance, item.minLevel, item.maxLevel);

        if (item.noMultiplier) msg += ", nomultp";
        if (item.noSpawner) msg += ", nospn";
        if (item.lore != null && !item.lore.isEmpty()) msg += ", hasLore";
        if (item.customName != null && !"".equals(item.customName)) msg += ", hasName";
        if (item.getDamage() != 0 || item.getHasDamageRange())
            msg += ", dmg: " + item.getDamageAsString();

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

    private void buildUniversalGroups(){

        // include interfaces: Monster, Boss
        groups_HostileMobs = Stream.of(
                EntityType.ENDER_DRAGON,
                EntityType.GHAST,
                EntityType.MAGMA_CUBE,
                EntityType.PHANTOM,
                EntityType.SHULKER,
                EntityType.SLIME
        ).collect(Collectors.toCollection(HashSet::new));

        if (this.isMCVersion_16_OrHigher)
            groups_HostileMobs.addAll(this.mc1_16_Compat.getHostileMobs());

        // include interfaces: Animals, WaterMob
        groups_PassiveMobs = Stream.of(
                EntityType.IRON_GOLEM,
                EntityType.SNOWMAN
        ).collect(Collectors.toCollection(HashSet::new));

        if (this.isMCVersion_16_OrHigher)
            groups_HostileMobs.addAll(this.mc1_16_Compat.getPassiveMobs());

        // include interfaces: WaterMob
        groups_AquaticMobs = Stream.of(
                EntityType.DROWNED,
                EntityType.ELDER_GUARDIAN,
                EntityType.GUARDIAN,
                EntityType.TURTLE
        ).collect(Collectors.toCollection(HashSet::new));
    }

    @Nullable
    private Enchantment getEnchantmentFromName(final String name){

        switch (name.replace(" ", "_").toLowerCase()){
            case "arrow_damage": case "power":
                return Enchantment.ARROW_DAMAGE;
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
            case "damage_arthropods": case "bane_of_arthropods":
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
            case "projectile_protection":
                return Enchantment.PROTECTION_PROJECTILE;
            case "quick_charge": return Enchantment.QUICK_CHARGE;
            case "riptide": return Enchantment.RIPTIDE;
            case "silk_touch": return Enchantment.SILK_TOUCH;
            case "soul_speed": return Enchantment.SOUL_SPEED;
            case "sweeping_edge": return Enchantment.SWEEPING_EDGE;
            case "thorns": return Enchantment.THORNS;
            case "vanishing_curse": case "curse_of_vanishing":
                return Enchantment.VANISHING_CURSE;
            case "water_worker": case "respiration":
                return Enchantment.WATER_WORKER;
            default:
                try{
                    final NamespacedKey namespacedKey = new NamespacedKey(this, name);
                    return Enchantment.getByKey(namespacedKey);
                }
                catch (Exception e){
                    Utils.logger.warning("Invalid enchantment: " + name);
                }
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

    @Nonnull
    @SuppressWarnings("SameParameterValue")
    private Set<String> getSetFromConfigSection(final String configPath){
        final Set<String> result = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        final List<String> set = settingsCfg.getStringList(configPath);

        result.addAll(set);

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

    private void shutDownAsyncTasks() {
        Utils.logger.info("&fTasks: &7Shutting down other async tasks...");
        Bukkit.getScheduler().cancelTasks(this);
    }
}
