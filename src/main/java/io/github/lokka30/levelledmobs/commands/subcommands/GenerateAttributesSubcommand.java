package io.github.lokka30.levelledmobs.commands.subcommands;

import io.github.lokka30.levelledmobs.LevelledMobs;
import io.github.lokka30.levelledmobs.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
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
            if (args.length != 2) {
                if (attempts == 0) {
                    sender.sendMessage("You have ran out of attempts to use the correct password. You will gain another 3 attempts next time you restart the server.");
                } else {
                    if (args[0].equals(PASSWORD)) {
                        if (acknowledged) {
                            sender.sendMessage("Starting generateAttributes. The server will most likely freeze until this is completed.");
                            try {
                                generateAttributes(instance);
                            } catch (IOException ex) {
                                sender.sendMessage("Unable to generate attributes! Stack trace:");
                                ex.printStackTrace();
                            }

                            sender.sendMessage("Finished generateAttributes.");
                        } else {
                            acknowledged = true;
                            sender.sendMessage("********* WARNING! **********");
                            sender.sendMessage("This command can possibly cause significant issues on your server, especially by unexpected behaviour from other plugins.");
                            sender.sendMessage("If you are sure you are meant to be running this command, please run this command again (with the password too).");
                            sender.sendMessage("Developers are NOT responsible for any damages that this plugin could unintentionally cause.");
                            sender.sendMessage("(This acknowledgement notice will only appear once per restart.)");
                        }
                    } else {
                        sender.sendMessage("Invalid password! You have " + attempts + " more attempt(s) before this command is locked until next restart.");
                        attempts--;
                    }
                }
            } else {
                sender.sendMessage("Usage: /" + label + " generateAttributes <password>");
            }
        } else {
            sender.sendMessage("Only console may use this command.");
        }
    }

    @Override
    public List<String> parseTabCompletions(LevelledMobs instance, CommandSender sender, String[] args) {
        if (args.length == 0) {
            return Collections.singletonList("password?");
        } else {
            return null;
        }
    }

    private void generateAttributes(LevelledMobs instance) throws IOException {
        File attribFile = new File(instance.getDataFolder(), "attributes.yml");
        if (attribFile.exists()) {
            attribFile.delete();
        }
        attribFile.createNewFile();

        YamlConfiguration attribConfig = YamlConfiguration.loadConfiguration(attribFile);
        attribConfig.options().header("DO NOT modify this file unless you know exactly what you are doing!");

        World world = Bukkit.getWorlds().get(0);

        for (EntityType entityType : EntityType.values()) {

            if (entityType.getEntityClass() == null) {
                continue;
            }

            if (entityType.getEntityClass().isAssignableFrom(LivingEntity.class)) { // Must be a LivingEntity to have attributes anyways.

                Utils.logger.info("&f&lGenerateAttributes: &7Saving attributes of entity " + entityType.toString() + "...");

                // Spawn the entity in.
                // Does it high up in the sky in case it breaks things around it when it spawns in.
                LivingEntity entity = (LivingEntity) world.spawnEntity(new Location(world, 0, 512, 0), entityType);

                // A few safety measures.
                entity.setInvulnerable(true);
                entity.setPersistent(false);
                entity.setAI(false);
                entity.setCollidable(false);

                //Loop through all possible attributes.
                for (Attribute attribute : Attribute.values()) {
                    if (entity.getAttribute(attribute) != null) {
                        attribConfig.set(entityType.toString() + "." + attribute.toString(), Objects.requireNonNull(entity.getAttribute(attribute)).getBaseValue());
                    }
                }

                entity.remove();
            }
        }

        attribConfig.save(attribFile);
    }
}
