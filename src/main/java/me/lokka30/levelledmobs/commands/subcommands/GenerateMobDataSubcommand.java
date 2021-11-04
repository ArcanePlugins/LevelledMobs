/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.commands.subcommands;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.misc.Utils;
import me.lokka30.microlib.maths.QuickTimer;
import me.lokka30.microlib.messaging.MessageUtils;
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
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * This subcommand is considered dangerous as it spawns in all types of 'living entities' on the first loaded world at coordinates (0, 512, 0). It also freezes the server for a moment.
 *
 * @author lokka30
 * @since 2.4.0
 */
public class GenerateMobDataSubcommand implements Subcommand {

    // Password for this command: ThisMightDestroyMyWorldIUnderstand

    private int attempts = 3;
    private boolean acknowledged = false;

    public final HashSet<String> excludedEntityTypes = new HashSet<>(Arrays.asList("UNKNOWN", "PLAYER", "NPC", "ARMOR_STAND"));

    @Override
    public void parseSubcommand(final LevelledMobs main, final CommandSender sender, final String label, final String[] args) {
        if (!(sender instanceof ConsoleCommandSender)) {
            sender.sendMessage(MessageUtils.colorizeAll(main.configUtils.getPrefix() + " Only console may use this command."));
            return;
        }

        if (args.length != 2) {
            sender.sendMessage(MessageUtils.colorizeAll(main.configUtils.getPrefix() + " Usage: &b/" + label + " generateMobData <password>"));
            return;
        }

        if (attempts == 0) {
            sender.sendMessage(MessageUtils.colorizeAll(main.configUtils.getPrefix() + " You have ran out of attempts to use the correct password. You will gain another 3 attempts next time you restart the server."));
            return;
        }

        // This is the password I also use for all my accounts. You won't use it.. right?
        if (!args[1].equals("ThisMightDestroyMyWorldIUnderstand")) {
            sender.sendMessage(MessageUtils.colorizeAll(main.configUtils.getPrefix() + " Invalid password '&b%password%&7'! You have &b" + attempts + "&7 more attempt(s) before this command is locked until next restart.").replace("%password%", args[1]));
            attempts--;
            return;
        }

        if (acknowledged) {
            sender.sendMessage(MessageUtils.colorizeAll(main.configUtils.getPrefix() + " Starting generateMobData..."));
            try {
                generateMobData(main);
            } catch (IOException ex) {
                sender.sendMessage(MessageUtils.colorizeAll(main.configUtils.getPrefix() + " Unable to generate mob data! Stack trace:"));
                ex.printStackTrace();
            }
        } else {
            acknowledged = true;
            sender.sendMessage(MessageUtils.colorizeAll(main.configUtils.getPrefix() + " &8&m**********&r &c&lWARNING!&r &8&m**********&r"));
            sender.sendMessage(MessageUtils.colorizeAll(main.configUtils.getPrefix() + " &fThis command can possibly cause significant issues on your server&7, especially by unexpected behaviour from other plugins."));
            sender.sendMessage(MessageUtils.colorizeAll(main.configUtils.getPrefix() + " &fIf you are sure &7you are meant to be running this command, please &frun this command again (with the password too)&7."));
            sender.sendMessage(MessageUtils.colorizeAll(main.configUtils.getPrefix() + " &fDevelopers are NOT responsible for any damages&7 that this plugin could unintentionally cause."));
            sender.sendMessage(MessageUtils.colorizeAll(main.configUtils.getPrefix() + " The files generated will still be&f reset next startup&7, and the files you will generate will &fnot take effect&7. This simply generates new ones which you should copy before you restart the server next."));
            sender.sendMessage(MessageUtils.colorizeAll(main.configUtils.getPrefix() + " &8(This acknowledgement notice will only appear once per restart.)"));
        }
    }

    @Override
    public List<String> parseTabCompletions(final LevelledMobs main, final CommandSender sender, @NotNull final String[] args) {
        if (args.length == 2 && sender instanceof ConsoleCommandSender)
            return Collections.singletonList("(password?)");

        return Collections.emptyList();
    }

    YamlConfiguration dropsConfig;

    private void generateMobData(@NotNull final LevelledMobs main) throws IOException {
        final QuickTimer quickTimer = new QuickTimer();

        final File attribFile = new File(main.getDataFolder(), "defaultAttributes.yml");
        if (attribFile.exists()) attribFile.delete();
        attribFile.createNewFile();
        YamlConfiguration attribConfig = YamlConfiguration.loadConfiguration(attribFile);
        attribConfig.options().header("This is NOT a configuration file! All changes to this file will not take effect and be reset!");

        attribConfig.set("GENERATION_INFO.date", new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date()) + ", Time" +
                "zone: " + Calendar.getInstance().getTimeZone().getDisplayName());
        attribConfig.set("GENERATION_INFO.server-version", Bukkit.getVersion());

        final World world = Bukkit.getWorlds().get(0);

        int entitiesSkipped = 0;
        int entitiesCompleted = 0;

        for (final EntityType entityType : EntityType.values()) {

            Utils.logger.info("&f&lGenerateMobData: &7Processing &b" + entityType + "&7:");

            /* Don't spawn certain entities. */
            if (excludedEntityTypes.contains(entityType.toString())) {
                Utils.logger.info("&f&lGenerateMobData: &7Skipping &b" + entityType + "&7.");
                entitiesSkipped++;
                continue;
            }

            /* Check if the mob is a LivingEntity, otherwise it isn't levellable at all, so no point in saving its attributes ;) */
            if (entityType.getEntityClass() == null) {
                Utils.logger.info("&f&lGenerateMobData: &8[&3" + entityType + "&8] &7Entity class is null, skipping.");
                entitiesSkipped++;
                continue;
            }

            Entity entity;
            try {
                entity = world.spawnEntity(new Location(world, 0, 512, 0), entityType);
            } catch (IllegalArgumentException ex) {
                Utils.logger.info("&f&lGenerateMobData: &8[&3" + entityType + "&8] &7This entity is not spawnable, skipping.");
                entitiesSkipped++;
                continue;
            }

            if (!(entity instanceof LivingEntity)) {
                Utils.logger.info("&f&lGenerateMobData: &8[&3" + entityType + "&8] &7Entity is not a LivingEntity, skipping...");
                entitiesSkipped++;
                continue;
            }

            Utils.logger.info("&f&lGenerateMobData: &8[&3" + entityType + "&8] &7Entity is a LivingEntity. Saving...");

            final LivingEntity livingEntity = (LivingEntity) entity;

            for (final Attribute attribute : Attribute.values()) {
                if (livingEntity.getAttribute(attribute) != null) {
                    Utils.logger.info("&f&lGenerateMobData: &8[&3" + entityType + "&8] &7Saving attribute &b" + attribute + "&7...");
                    attribConfig.set(entityType + "." + attribute, Objects.requireNonNull(livingEntity.getAttribute(attribute)).getBaseValue());
                }
            }
            Utils.logger.info("&f&lGenerateMobData: &8[&3" + entityType + "&8] &7Entity's attributes have been gathered.");

            entity.remove();
            entitiesCompleted++;
            Utils.logger.info("&f&lGenerateMobData: &8[&3" + entityType + "&8] &7Entity removed. Proceeding with next entity if it exists.");
        }

        attribConfig.save(attribFile);
        Utils.logger.info("&f&lGenerateMobData: &7Process completed. &8//&7 Processed &b" + (entitiesCompleted + entitiesSkipped) + "&7 entit" +
                "ies in &b" + quickTimer.getTimer() + "ms&7, where &b" + entitiesCompleted + "&7 entities had their attributes saved, a" +
                "nd &b" + entitiesSkipped + "&7 entities were skipped.");
    }
}
