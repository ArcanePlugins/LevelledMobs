package io.github.lokka30.levelledmobs.commands.subcommands;

import io.github.lokka30.levelledmobs.LevelledMobs;
import io.github.lokka30.levelledmobs.misc.Utils;
import me.lokka30.microlib.MessageUtils;
import me.lokka30.microlib.QuickTimer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;

// I sincerely apologise to whoever is reading this code. - lokka30

/**
 * This subcommand is considered dangerous as it spawns in all types of 'living entities' on the first loaded world at coordinates (0, 512, 0). It also freezes the server for a moment.
 *
 * @author lokka30
 */
public class GenerateMobDataSubcommand implements Subcommand {

    private final static String PASSWORD = "ThisMightDestroyMyWorldIUnderstand"; // This is the password I also use for all my accounts. You won't use it.. right?

    private int attempts = 3;
    private boolean acknowledged = false;

    @Override
    public void parseSubcommand(LevelledMobs instance, CommandSender sender, String label, String[] args) {
        if (sender instanceof ConsoleCommandSender) {
            if (args.length == 2) {
                if (attempts == 0) {
                    sender.sendMessage(MessageUtils.colorizeAll(instance.configUtils.getPrefix() + " You have ran out of attempts to use the correct password. You will gain another 3 attempts next time you restart the server."));
                } else {
                    if (args[1].equals(PASSWORD)) {
                        if (acknowledged) {
                            sender.sendMessage(MessageUtils.colorizeAll(instance.configUtils.getPrefix() + " Starting generateMobData..."));

                            QuickTimer timer = new QuickTimer();
                            timer.start();

                            try {
                                generateMobData(instance);
                            } catch (IOException ex) {
                                sender.sendMessage(MessageUtils.colorizeAll(instance.configUtils.getPrefix() + " Unable to generate mob data! Stack trace:"));
                                ex.printStackTrace();
                            }

                            sender.sendMessage(MessageUtils.colorizeAll(instance.configUtils.getPrefix() + " Finished generateMobData, took &b" + timer.getTimer() + "ms&7."));
                        } else {
                            acknowledged = true;
                            sender.sendMessage(MessageUtils.colorizeAll(instance.configUtils.getPrefix() + " &8&m**********&r &c&lWARNING!&r &8&m**********&r"));
                            sender.sendMessage(MessageUtils.colorizeAll(instance.configUtils.getPrefix() + " &fThis command can possibly cause significant issues on your server&7, especially by unexpected behaviour from other plugins."));
                            sender.sendMessage(MessageUtils.colorizeAll(instance.configUtils.getPrefix() + " &fIf you are sure &7you are meant to be running this command, please &frun this command again (with the password too)&7."));
                            sender.sendMessage(MessageUtils.colorizeAll(instance.configUtils.getPrefix() + " &fDevelopers are NOT responsible for any damages&7 that this plugin could unintentionally cause."));
                            sender.sendMessage(MessageUtils.colorizeAll(instance.configUtils.getPrefix() + " The files generated will still be&f reset next startup&7, and the files you will generate will &fnot take effect&7. This simply generates new ones which you should copy before you restart the server next."));
                            sender.sendMessage(MessageUtils.colorizeAll(instance.configUtils.getPrefix() + " &8(This acknowledgement notice will only appear once per restart.)"));
                        }
                    } else {
                        sender.sendMessage(MessageUtils.colorizeAll(instance.configUtils.getPrefix() + " Invalid password '&b%password%&7'! You have &b" + attempts + "&7 more attempt(s) before this command is locked until next restart.").replace("%password%", args[1]));
                        attempts--;
                    }
                }
            } else {
                sender.sendMessage(MessageUtils.colorizeAll(instance.configUtils.getPrefix() + " Usage: &b/" + label + " generateMobData <password>"));
            }
        } else {
            sender.sendMessage(MessageUtils.colorizeAll(instance.configUtils.getPrefix() + " Only console may use this command."));
        }
    }

    @Override
    public List<String> parseTabCompletions(LevelledMobs instance, CommandSender sender, String[] args) {
        if (args.length == 2 && sender instanceof ConsoleCommandSender) {
            return Collections.singletonList("(password?)");
        }

        return null;
    }

    YamlConfiguration dropsConfig;

    private void generateMobData(LevelledMobs instance) throws IOException {
        File attribFile = new File(instance.getDataFolder(), "attributes.yml");
        if (attribFile.exists()) {
            attribFile.delete();
        }
        attribFile.createNewFile();
        YamlConfiguration attribConfig = YamlConfiguration.loadConfiguration(attribFile);
        attribConfig.options().header("This is NOT a configuration file! All changes to this file will not take effect and be reset!");
        attribConfig.set("GENERATION_INFO", "[Date: " + new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date()) + "], [ServerVersion: " + Bukkit.getVersion() + "]");

        File dropsFile = new File(instance.getDataFolder(), "drops.yml");
        if (dropsFile.exists()) {
            dropsFile.delete();
        }
        dropsFile.createNewFile();
        dropsConfig = YamlConfiguration.loadConfiguration(dropsFile);
        dropsConfig.options().header("This is NOT a configuration file! All changes to this file will not take effect and be reset!");
        dropsConfig.set("GENERATION_INFO", "[Date: " + new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date()) + "], [ServerVersion: " + Bukkit.getVersion() + "]");

        DeathDropListener deathDropListener = new DeathDropListener();
        instance.pluginManager.registerEvents(deathDropListener, instance);

        World world = Bukkit.getWorlds().get(0);

        for (EntityType entityType : EntityType.values()) {

            // Don't spawn these in.
            if (entityType == EntityType.UNKNOWN || entityType == EntityType.PLAYER) {
                Utils.logger.info("&f&lGenerateMobData: &7Skipping &b" + entityType.toString() + ".");
                continue;
            }

            Utils.logger.info("&f&lGenerateMobData: &7Processing &b" + entityType.toString() + ":");

            if (entityType.getEntityClass() == null) {
                Utils.logger.info("&f&lGenerateMobData: &8[" + entityType.toString() + "&8] &7Entity Class is null! Skipping...");
                continue;
            }

            Entity entity;
            try {
                entity = world.spawnEntity(new Location(world, 0, 512, 0), entityType);
            } catch (IllegalArgumentException ex) {
                Utils.logger.info("&f&lGenerateMobData: &8[" + entityType.toString() + "&8] &7Unable to spawn entity! Skipping...");
                continue;
            }

            if (entity instanceof LivingEntity) {
                Utils.logger.info("&f&lGenerateMobData: &8[" + entityType.toString() + "&8] &7Entity is a LivingEntity. Proceeding...");

                LivingEntity livingEntity = (LivingEntity) entity;

                for (Attribute attribute : Attribute.values()) {
                    if (livingEntity.getAttribute(attribute) != null) {
                        Utils.logger.info("&f&lGenerateMobData: &8[" + entityType.toString() + "&8] &7Saving attribute &b" + attribute.toString() + "&7...");
                        attribConfig.set(entityType.toString() + "." + attribute.toString(), Objects.requireNonNull(livingEntity.getAttribute(attribute)).getBaseValue());
                    }
                }

                Utils.logger.info("&f&lGenerateMobData: &8[" + entityType.toString() + "&8] &7Finished with entity.");
            } else {
                Utils.logger.info("&f&lGenerateMobData: &8[" + entityType.toString() + "&8] &7Entity is not a LivingEntity, skipping...");
            }

            entity.remove();
            Utils.logger.info("&f&lGenerateMobData: &8[" + entityType.toString() + "&8] &7Done. Proceeding with next entity if it exists.");
        }
        attribConfig.save(attribFile);

        Utils.logger.info("&f&lGenerateMobData: &7Finished attributes. doing drops now");
        for (EntityType entityType : EntityType.values()) {

            // Don't spawn these in.
            if (entityType == EntityType.UNKNOWN || entityType == EntityType.PLAYER || entityType == EntityType.FISHING_HOOK || entityType == EntityType.LIGHTNING) {
                Utils.logger.info("&f&lGenerateMobData: &7Skipping &b" + entityType.toString() + ".");
                continue;
            }

            Utils.logger.info("&f&lGenerateMobData: &7Processing &b" + entityType.toString() + ":");

            if (entityType.getEntityClass() == null) {
                Utils.logger.info("&f&lGenerateMobData: &8[" + entityType.toString() + "&8] &7Entity Class is null! Skipping...");
                continue;
            }

            Entity entity;
            try {
                entity = world.spawnEntity(new Location(world, 0, 512, 0), entityType);
            } catch (IllegalArgumentException ex) {
                Utils.logger.info("&f&lGenerateMobData: &8[" + entityType.toString() + "&8] &7Unable to spawn entity! Skipping...");
                continue;
            }

            for (int i = 0; i < 25; i++) {
                if (entity instanceof LivingEntity) {
                    Utils.logger.info("&f&lGenerateMobData: &8[" + entityType.toString() + "&8] &7Processing mob " + entity.getType().toString() + "...");

                    LivingEntity livingEntity = (LivingEntity) entity;

                    if (livingEntity.getEquipment() != null) livingEntity.getEquipment().clear();

                    livingEntity.setHealth(0.0);

                    Utils.logger.info("&f&lGenerateMobData: &8[" + entityType.toString() + "&8] &7Drops saved. Proceeding with next entity if it exists.");
                } else {
                    entity.remove();
                }
            }

            Utils.logger.info("&f&lGenerateMobData: &8[" + entityType.toString() + "&8] &7Done. Proceeding with next entity if it exists.");
        }
        dropsConfig.save(dropsFile);
        Utils.logger.info("&f&lGenerateMobData: &7Complete!");

        HandlerList.unregisterAll(deathDropListener);
    }

    private class DeathDropListener implements Listener {
        @EventHandler
        public void onDeath(final EntityDeathEvent event) {
            Utils.logger.info("&f&lGenerateMobData: &8[Death] &7" + event.getEntityType().toString() + " died");
            for (ItemStack drop : event.getDrops()) {
                final String path = event.getEntityType().toString();
                final String dropType = drop.getType().toString();

                List<String> dropsList = dropsConfig.getStringList(path);
                if (!dropsList.contains(dropType)) {
                    dropsList.add(dropType);
                    dropsConfig.set(path, dropsList);
                }
            }
        }
    }
}
