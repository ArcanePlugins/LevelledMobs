package io.github.lokka30.levelledmobs.commands.subcommands;

import io.github.lokka30.levelledmobs.LevelledMobs;
import io.github.lokka30.levelledmobs.utils.Utils;
import me.lokka30.microlib.MicroUtils;
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

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * This subcommand is considered dangerous as it spawns in all types of 'living entities' on the first loaded world at coordinates (0, 512, 0). It also freezes the server for a moment.
 */
public class GenerateAttributesSubcommand implements Subcommand {

    private final static String PASSWORD = "ThisMightDestroyMyWorldIUnderstand"; // This is the password I also use for all my accounts. You won't use it.. right?

    private int attempts = 3;
    private boolean acknowledged = false;

    @Override
    public void parseSubcommand(LevelledMobs instance, CommandSender sender, String label, String[] args) {
        if (sender instanceof ConsoleCommandSender) {
            if (args.length == 2) {
                if (attempts == 0) {
                    sender.sendMessage(MicroUtils.colorize(instance.configUtils.getPrefix() + " You have ran out of attempts to use the correct password. You will gain another 3 attempts next time you restart the server."));
                } else {
                    if (args[1].equals(PASSWORD)) {
                        if (acknowledged) {
                            sender.sendMessage(MicroUtils.colorize(instance.configUtils.getPrefix() + " Starting generateAttributes..."));

                            QuickTimer timer = new QuickTimer();
                            timer.start();

                            try {
                                generateAttributes(instance);
                            } catch (IOException ex) {
                                sender.sendMessage(MicroUtils.colorize(instance.configUtils.getPrefix() + " Unable to generate attributes! Stack trace:"));
                                ex.printStackTrace();
                            }

                            sender.sendMessage(MicroUtils.colorize(instance.configUtils.getPrefix() + " Finished generateAttributes, took &b" + timer.getTimer() + "ms&7."));
                        } else {
                            acknowledged = true;
                            sender.sendMessage(MicroUtils.colorize(instance.configUtils.getPrefix() + " &8&m**********&r &c&lWARNING!&r &8&m**********&r"));
                            sender.sendMessage(MicroUtils.colorize(instance.configUtils.getPrefix() + " &fThis command can possibly cause significant issues on your server&7, especially by unexpected behaviour from other plugins."));
                            sender.sendMessage(MicroUtils.colorize(instance.configUtils.getPrefix() + " &fIf you are sure &7you are meant to be running this command, please &frun this command again (with the password too)&7."));
                            sender.sendMessage(MicroUtils.colorize(instance.configUtils.getPrefix() + " &fDevelopers are NOT responsible for any damages&7 that this plugin could unintentionally cause."));
                            sender.sendMessage(MicroUtils.colorize(instance.configUtils.getPrefix() + " The file generated will still be&f reset next startup&7, and the file you will generate will &fnot take effect&7. This simply generates a new one which you should copy before you restart the server next."));
                            sender.sendMessage(MicroUtils.colorize(instance.configUtils.getPrefix() + " &8(This acknowledgement notice will only appear once per restart.)"));
                        }
                    } else {
                        sender.sendMessage(MicroUtils.colorize(instance.configUtils.getPrefix() + " Invalid password '&b%password%&7'! You have &b" + attempts + "&7 more attempt(s) before this command is locked until next restart.").replace("%password%", args[1]));
                        attempts--;
                    }
                }
            } else {
                sender.sendMessage(MicroUtils.colorize(instance.configUtils.getPrefix() + " Usage: &b/" + label + " generateAttributes <password>"));
            }
        } else {
            sender.sendMessage(MicroUtils.colorize(instance.configUtils.getPrefix() + " Only console may use this command."));
        }
    }

    @Override
    public List<String> parseTabCompletions(LevelledMobs instance, CommandSender sender, String[] args) {
        if (args.length == 2 && sender instanceof ConsoleCommandSender) {
            return Collections.singletonList("(password?)");
        }

        return null;
    }

    private void generateAttributes(LevelledMobs instance) throws IOException {
        File attribFile = new File(instance.getDataFolder(), "attributes.yml");
        if (attribFile.exists()) {
            attribFile.delete();
        }
        attribFile.createNewFile();

        YamlConfiguration attribConfig = YamlConfiguration.loadConfiguration(attribFile);
        attribConfig.options().header("This is NOT a configuration file! All changes to this file will not take effect and be reset!");
        attribConfig.set("GENERATION_INFO", "[Date: " + new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date()) + "], [ServerVersion: " + Bukkit.getVersion() + "]");

        World world = Bukkit.getWorlds().get(0);

        for (EntityType entityType : EntityType.values()) {

            // Don't spawn these in.
            if (entityType == EntityType.UNKNOWN || entityType == EntityType.PLAYER) {
                Utils.logger.info("&f&lGenerateAttributes: &7Skipping &b" + entityType.toString() + ".");
                continue;
            }

            Utils.logger.info("&f&lGenerateAttributes: &7Processing &b" + entityType.toString() + ":");

            if (entityType.getEntityClass() == null) {
                Utils.logger.info("&f&lGenerateAttributes: &8[" + entityType.toString() + "&8] &7Entity Class is null! Skipping...");
                continue;
            }

            Entity entity;
            try {
                entity = world.spawnEntity(new Location(world, 0, 512, 0), entityType);
            } catch (IllegalArgumentException ex) {
                Utils.logger.info("&f&lGenerateAttributes: &8[" + entityType.toString() + "&8] &7Unable to spawn entity! Skipping...");
                continue;
            }

            if (entity instanceof LivingEntity) {
                Utils.logger.info("&f&lGenerateAttributes: &8[" + entityType.toString() + "&8] &7Entity is a LivingEntity. Proceeding...");

                LivingEntity livingEntity = (LivingEntity) entity;

                for (Attribute attribute : Attribute.values()) {
                    if (livingEntity.getAttribute(attribute) != null) {
                        Utils.logger.info("&f&lGenerateAttributes: &8[" + entityType.toString() + "&8] &7Saving attribute &b" + attribute.toString() + "&7...");
                        attribConfig.set(entityType.toString() + "." + attribute.toString(), Objects.requireNonNull(livingEntity.getAttribute(attribute)).getBaseValue());
                    }
                }
                Utils.logger.info("&f&lGenerateAttributes: &8[" + entityType.toString() + "&8] &7Attributes saved.");
            } else {
                Utils.logger.info("&f&lGenerateAttributes: &8[" + entityType.toString() + "&8] &7Entity is not a LivingEntity, skipping...");
            }

            entity.remove();
            Utils.logger.info("&f&lGenerateAttributes: &8[" + entityType.toString() + "&8] &7Done. Proceeding with next entity if it exists.");
        }

        attribConfig.save(attribFile);
        Utils.logger.info("&f&lGenerateAttributes: &7Complete!");
    }
}
