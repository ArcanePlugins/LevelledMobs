package io.github.lokka30.levelledmobs.commands;

import io.github.lokka30.levelledmobs.LevelledMobs;
import io.github.lokka30.levelledmobs.utils.Utils;
import net.md_5.bungee.api.ChatColor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.jetbrains.annotations.NotNull;

public class LevelledMobsCommand implements CommandExecutor, TabCompleter {

    private LevelledMobs instance;
    private final Random rand;
    private List<String> allMobsList;
    private List<String> zeroThruNine;
    
    public LevelledMobsCommand(final LevelledMobs instance) {
        this.instance = instance;
        rand = new Random();
        
        this.allMobsList = Arrays.asList(
        		"bat",
        		"bee",
        		"blaze",
        		"cat",
        		"cave_spider",
        		"chicken",
        		"cod",
        		"cow",
        		"creeper",
        		"dolphin",
        		"donkey",
        		"drowned",
        		"elder_guardian",
        		"ender_dragon",
        		"enderman",
        		"endermite",
        		"evoker",
        		"evoker_fangs",
        		"fox",
        		"ghast",
        		"giant",
        		"guardian",
        		"hoglin",
        		"horse",
        		"husk",
        		"illusioner",
        		"iron_golem",
        		"llama",
        		"magma_cube",
        		"mule",
        		"mushroom_cow",
        		"ocelot",
        		"panda",
        		"parrot",
        		"phantom",
        		"pig",
        		"piglin",
        		"piglin_brute",
        		"pillager",
        		"polar_bear",
        		"pufferfish",
        		"rabbit",
        		"ravager",
        		"salmon",
        		"sheep",
        		"shulker",
        		"silverfish",
        		"skeleton",
        		"skeleton_horse",
        		"slime",
        		"snowball",
        		"snowman",
        		"spider",
        		"squid",
        		"stray",
        		"strider",
        		"tropical_fish",
        		"turtle",
        		"vex",
        		"villager",
        		"vindicator",
        		"witch",
        		"wither",
        		"wither_skeleton",
        		"wolf",
        		"zoglin",
        		"zombie",
        		"zombie_horse",
        		"zombie_villager",
        		"zombified_piglin"
        		);
        
        this.zeroThruNine = Arrays.asList(
        		"0",
        		"1",
        		"2",
        		"3",
        		"4",
        		"5",
        		"6",
        		"7",
        		"8",
        		"9"
        		);
    }

    public boolean onCommand(@NotNull final CommandSender sender, @NotNull final Command command, @NotNull final String label, final String[] args) {
        if (args.length == 0) {
            sender.sendMessage(" ");
            sender.sendMessage(instance.messageMethods.prefix(instance.PREFIX, "Available commands:"));
            sender.sendMessage(instance.messageMethods.colorize("&8 &m->&3 /levelledMobs &8- &7view plugin commands."));
            sender.sendMessage(instance.messageMethods.colorize("&8 &m->&3 /levelledMobs killall [world] &8- &7butcher levellable mobs."));
            sender.sendMessage(instance.messageMethods.colorize("&8 &m->&3 /levelledMobs summon <...> &8- &7summon a levelled mob."));
            sender.sendMessage(instance.messageMethods.colorize("&8 &m->&3 /levelledMobs reload &8- &7reload the settings file into memory."));
            sender.sendMessage(instance.messageMethods.colorize("&8 &m->&3 /levelledMobs info &8- &7view plugin information."));
            sender.sendMessage(" ");
            return true;
        }
        
        if (args[0].equalsIgnoreCase("kill")) {
            parseKillCmd(sender, args, label);
        } else if (args[0].equalsIgnoreCase("reload")) {
            if (sender.hasPermission("levelledmobs.reload")) {
                sender.sendMessage(instance.messageMethods.prefix(instance.PREFIX, "Reload started..."));
                instance.loadFiles();
                sender.sendMessage(instance.messageMethods.prefix(instance.PREFIX, "...reload complete."));
            } else {
                sender.sendMessage(instance.messageMethods.prefix(instance.PREFIX, "You don't have access to that."));
            }
        } else if (args[0].equalsIgnoreCase("summon")) {
        	parseSummonMobsCmd(sender, args);
        } else if (args[0].equalsIgnoreCase("info")) {
            if (args.length == 1) {
            	for (String line : Utils.getInfoStrings(instance)) {
            		if (line.equals(" "))
            			sender.sendMessage(" ");
            		else
            			sender.sendMessage(instance.messageMethods.colorize(line));
            	}
            } else {
                sender.sendMessage(instance.messageMethods.prefix(instance.PREFIX, "Usage: &b/" + label + " info"));
            }
        } else {
            sender.sendMessage(instance.messageMethods.prefix(instance.PREFIX, "For a list of available commands, please run &b/" + label + "&7."));
        }
        
        return true;
    }
    
    private void parseKillCmd(CommandSender sender, final String[] args, final String label) {
    	if (!sender.hasPermission("levelledmobs.kill.all") && !sender.hasPermission("levelledmobs.kill.near")) {
            sender.sendMessage(instance.messageMethods.prefix(instance.PREFIX, "You don't have access to that."));
            return;
    	}
    	
    	int killed = 0;
        World world = null;
		// lvlmobs kill all <world>
		// lblmobs kill near <radius>
        
        if (args.length < 2) {
            sender.sendMessage(instance.messageMethods.colorize("Usage: &b/levelledmobs kill all [world]"));
            sender.sendMessage(instance.messageMethods.colorize("Usage: &b/levelledmobs kill near [radius]"));
            return;
        }
        if (args.length == 2) {
        	if (!(sender instanceof Player)) {
        		sender.sendMessage(instance.messageMethods.prefix(instance.PREFIX, "Usage (console): &b/" + label + " kill all [world]"));
        		return;
        	}
        	if (args[1].equalsIgnoreCase("all")) 
        		sender.sendMessage(instance.messageMethods.colorize("Usage: &b/levelledmobs kill all [world]"));	
        	else if (args[1].equalsIgnoreCase("near")) 
        		sender.sendMessage(instance.messageMethods.colorize("Usage: &b/levelledmobs kill near [radius]"));
        	
    		return;
        }
        else if (args.length >= 3) {
        	if (args[1].equalsIgnoreCase("all")) {
            	if (!sender.hasPermission("levelledmobs.kill.all")) {
                    sender.sendMessage(instance.messageMethods.prefix(instance.PREFIX, "You don't have access to that."));
                    return;
            	}
        		
                if (Bukkit.getWorld(args[2]) == null) {
                    sender.sendMessage(instance.messageMethods.prefix(instance.PREFIX, "Invalid world &b" + args[1] + "&7."));
                    return;
                }
                
                world = Bukkit.getWorld(args[2]);
                assert world != null;
                for (Entity e : world.getEntities()) {
                    if (e instanceof LivingEntity) {
                        final LivingEntity livingEntity = (LivingEntity) e;
                        if (instance.levelManager.isLevellable(livingEntity)) {
                            livingEntity.setHealth(0);
                            killed++;
                        }
                    }
                }
                sender.sendMessage(instance.messageMethods.prefix(instance.PREFIX, String.format(
                		"Killed &b%s&7 levellable entities in world '&b%s&7'.", killed, world.getName())));	
        	}
        	else if (args[1].equalsIgnoreCase("near")) {
            	if (!sender.hasPermission("levelledmobs.kill.near")) {
                    sender.sendMessage(instance.messageMethods.prefix(instance.PREFIX, "You don't have access to that."));
                    return;
            	}
        		
            	if (!(sender instanceof Player)) {
            		sender.sendMessage(instance.messageMethods.prefix(instance.PREFIX, "kill near command not available from console"));
            		return;
            	}
            	
            	int radius = 0;
                try {
                    radius = Integer.parseInt(args[2]);
                } catch (NumberFormatException ex) {
                	sender.sendMessage(instance.messageMethods.prefix(instance.PREFIX, "Invalid radius: " + args[2]));
                	return;
                }
                
                Player p = (Player) sender;
                
                for (Entity e : p.getNearbyEntities(radius, radius, radius)) {
                    if (e instanceof LivingEntity) {
                        final LivingEntity livingEntity = (LivingEntity) e;
                        if (instance.levelManager.isLevellable(livingEntity)) {
                        	livingEntity.setHealth(0);
                            killed++;
                        }
                    }
                }
                sender.sendMessage(instance.messageMethods.prefix(instance.PREFIX, String.format(
                	"Killed &b%s&7 levellable entities within radius '&b%s&7'.", killed, radius)));
        	}
        }
    }
    
    private void parseSummonMobsCmd(CommandSender sender, final String[] args) {
    	if (!sender.hasPermission("levelledmobs.summon")) {
    		sender.sendMessage(instance.messageMethods.prefix(instance.PREFIX, "You don't have access to that."));
    		return;
    	}
        
    	boolean isSenderPlayer = (sender instanceof Player); // as opposed to console
        Player player = null;
        if (isSenderPlayer) player = (Player)sender;
    	
		// len:    1      2        3        4       5          6            7   8
		// arg:    0      1        2        3       4          5            6   9
		// lvlmobs summon <amount> <entity> <level> here
		// lvlmobs summon <amount> <entity> <level> atPlayer   <playername>
    	// lvlmobs summon <amount> <entity> <level> atLocation <x>          <y> <z>

        if (args.length < 4) {
        	sender.sendMessage(ChatColor.GRAY + "Summon command syntax:");
        	sender.sendMessage("/lvlmobs summon <amount> <entity> <level> here");
        	sender.sendMessage("/lvlmobs summon <amount> <entity> <level> atPlayer <playername>");
        	sender.sendMessage("/lvlmobs summon <amount> <entity> <level> atLocation <x> <y> <z>");
        	return;
        }

        EntityType entityType;
        int amount = 0;
        int level = 1;

        try {
            amount = Integer.parseInt(args[1]);
        } catch (NumberFormatException ex) {
            sender.sendMessage(instance.messageMethods.prefix(instance.PREFIX, "Invalid amount: " + args[1]));
            return;
        }
        
        try {
            entityType = EntityType.valueOf(args[2].toUpperCase());
        } catch (Exception ex) {
            sender.sendMessage(instance.messageMethods.prefix(instance.PREFIX, "Invalid mob type: " + args[2]));
            return;
        }
        
        try {
            level = Integer.parseInt(args[3]);
        } catch (NumberFormatException ex) {
            sender.sendMessage(instance.messageMethods.prefix(instance.PREFIX, "Invalid level: " + args[3]));
            return;
        }
        
        SummonType summonType = SummonType.Here;
        
        if (args.length > 4) {
	        if (args[4].equalsIgnoreCase("atplayer")) summonType = SummonType.At_Player;
	        else if (args[4].equalsIgnoreCase("atlocation")) summonType = SummonType.At_Location;
	        else if (!args[4].equalsIgnoreCase("here")) {
	        	sender.sendMessage(ChatColor.RED + "invalid summon type: " + args[4]);
	        	return;
	        }
        }
        
        if (summonType == SummonType.Here) {
        	if (!isSenderPlayer) {
        		sender.sendMessage(ChatColor.RED + "You can't use this option from console");
            	return;	
        	}

        	// payload 1:
            createMobs(entityType, amount, player, sender, level);
            return;
        }
        
        if (args.length < 6 || summonType == SummonType.At_Location && args.length < 8) {
        	sender.sendMessage(ChatColor.RED + "incomplete command");
        	return;
        }
        
        if (summonType == SummonType.At_Player) {
        	Player p = Bukkit.getPlayer(args[5]);
        	if (p == null) {
        		sender.sendMessage(ChatColor.RED + "player " + args[5] + " is invalid or offline");
            	return;
        	}
        	// payload 2:
        	createMobs(entityType, amount, p, sender, level);
        	return;
        }
        
        final boolean xIsRelative = args[5].charAt(0) == '~';
        final boolean yIsRelative = args[6].charAt(0) == '~';
        final boolean zIsRelative = args[7].charAt(0) == '~';
        
        if (!isSenderPlayer && (xIsRelative || yIsRelative || zIsRelative)) {
        	sender.sendMessage(ChatColor.RED + "You can't use this option from console");
        	return;
        }
        
        final int useX = getLocationFromArg(sender, player, args[5], CoordiateLetter.X, xIsRelative);
        if (useX == -1) return;
        final int useY = getLocationFromArg(sender, player, args[6], CoordiateLetter.Y, yIsRelative);
        if (useY == -1) return;
        final int useZ = getLocationFromArg(sender, player, args[7], CoordiateLetter.Z, zIsRelative);
        if (useZ == -1) return;

        // payload 3:
        createMobs(entityType, amount, player, sender, level, useX, useY, useZ);
    }
        
    private enum SummonType{
    	At_Player,
    	At_Location,
    	Here
    }
    
    private enum CoordiateLetter{
    	X,
    	Y,
    	Z
    }
    
    private int getLocationFromArg(final CommandSender sender, Player player, final String arg, CoordiateLetter letter, boolean isRelative) {
    	
    	String temp = null;
    	int result = -1;
    	int toAdd = 0;
    	
    	if (isRelative) {
	    	switch (letter) {
	    		case X: result = player.getLocation().getBlockX(); break;
	    		case Y: result = player.getLocation().getBlockY(); break;
	    		case Z: result = player.getLocation().getBlockZ(); break;
	    	}
	    	
	    	if (arg.length() == 1) return result;
	    		
	    	if (arg.charAt(0) == '+') {
	    		if (arg.length() == 2) {
	    			sender.sendMessage(instance.messageMethods.prefix(instance.PREFIX, "Invalid " + letter.toString() + " coordinate: " + arg));
	    			return -1;
	    		}
	    		temp = arg.substring(2);
	    	}
	    	else
	    		temp = arg.substring(1);
    	}
    	
        try {
            toAdd = Integer.parseInt(isRelative ? temp : arg);
            result += toAdd;
        } catch (NumberFormatException ex) {
        	sender.sendMessage(instance.messageMethods.prefix(instance.PREFIX, "Invalid " + letter.toString() + " coordinate: " + arg));
        	return -1;
        }	
        
        return result;
    }
    
    private void createMobs(EntityType entity, int amount, Player player, CommandSender sender, int level, int useX, int useY, int useZ) {
    	Location location = new Location(player.getWorld(), useX, useY, useZ);
    	MobSpawnResult result = createMobs(entity, amount, player, sender, level, location, false);
    	
    	if (result.MobsSpawned == 0) return;
		sender.sendMessage(String.format("Spawned (%s) of [Level %s | %s] at %s,%s,%s",
			result.MobsSpawned, level, entity, result.location.getBlockX(), result.location.getBlockY(), result.location.getBlockZ()));
    }
    
    private void createMobs(EntityType entity, int amount, Player player, CommandSender sender, int level) {
    	MobSpawnResult result = createMobs(entity, amount, player, sender, level, player.getLocation(), true);
    	
    	if (result.MobsSpawned == 0) return;
    	
		if (sender == player) {
			sender.sendMessage(String.format("Spawned (%s) of [Level %s | %s]",
				result.MobsSpawned, level, entity.toString().toLowerCase()));
		}
		else {
			sender.sendMessage(String.format("Spawned (%s) of [Level %s | %s] at player %s",
				result.MobsSpawned, level, entity.toString().toLowerCase(), player.getName()));
		}
    }
    
    private MobSpawnResult createMobs(EntityType entity, int amount, Player player, CommandSender sender, int level, 
    		Location location, boolean useDistFromPlayer) {
    	
	    MobSpawnResult mobResult = new MobSpawnResult();
        
        if (amount > 100) {
        	sender.sendMessage("max mobs allowed is 100");
        	amount = 100;
        }
    	
	    int distanceX = 0;
	    int distanceZ = 0;
	    int distFromPlayer = useDistFromPlayer ? instance.fileCache.SETTINGS_SPAWN_DISTANCE_FROM_PLAYER : 0;

	    World world = player.getWorld();
	    
	    double direction = player.getLocation().getYaw();
	    if (direction >= 225.0D && direction <= 314.9D) distanceX += distFromPlayer; 
	    if (direction >= 45.0D && direction <= 134.9D)  distanceX -= distFromPlayer; 
	    if (direction >= 135.0D && direction <= 224.9D) distanceZ -= distFromPlayer; 
	    if (direction >= 315.0D || direction <= 44.9D)  distanceZ += distFromPlayer;
	    
	    Location newLocation1 = new Location(world, location.getX() + distanceX, location.getY(), location.getZ() + distanceZ);
	    Location newLocation2 = new Location(world, location.getX() + distanceX, location.getY() + 1.0D, location.getZ() + distanceZ);
	    
	    if (newLocation1.getBlock().getType().isSolid() || newLocation2.getBlock().getType().isSolid()) {
	    	sender.sendMessage(instance.messageMethods.prefix(instance.PREFIX, "&7Not enough room, try somewhere else"));
	    	return mobResult;
	    }
	    
	    SpawnReason spawnReason = SpawnReason.CUSTOM;
	    mobResult.location = newLocation1;

		for (int i = 0; i < amount; i++) {
			if (i > 0) {
				// offset by a random number otherwise it'll look just like only one entity is there
				double randomX = (double)rand.ints(0, 50).findFirst().getAsInt() * 0.01;
				double randomZ = (double)rand.ints(0, 50).findFirst().getAsInt() * 0.01;
				newLocation1 = new Location(world, newLocation2.getX() + randomX, newLocation1.getY(), newLocation2.getZ() + randomZ);
			}
			
			Entity spawnedEntity = world.spawnEntity(newLocation1, entity);
						
			if (spawnedEntity != null) {
				mobResult.MobsSpawned++;
				instance.creatureSpawn.processMobSpawn((LivingEntity)spawnedEntity, spawnReason, level);
			}
		}
		
		return mobResult;
    }
    
    private class MobSpawnResult{
    	public int MobsSpawned;
    	public Location location;
    }
    
	@Override
	public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
				
		if (cmd.getName().equalsIgnoreCase("levelledmobs")) {
			List<String> cmds = new ArrayList<String>();
			
			if (args.length == 1) { 
				cmds.add("info");
				
				if (sender.hasPermission("levelledmobs.kill.all") || sender.hasPermission("levelledmobs.kill.near"))
					cmds.add("kill");
				if (sender.hasPermission("levelledmobs.reload"))
					cmds.add("reload");
				if (sender.hasPermission("levelledmobs.summon"))
					cmds.add("summon");
			}
			else if (args.length >= 2) {
				// lvlmobs kill all <world>
				// lblmobs kill near <radius>
				
				if (args[0].equalsIgnoreCase("kill")) {
					if (args.length == 2) {
						cmds.add("all");
						cmds.add("near");
					}
					else if (args.length == 3) {
						if (args[1].equalsIgnoreCase("all") && sender.hasPermission("levelledmobs.kill.all")){
							List<String> enabledWorlds = getEnabledWorldsList();
							if (enabledWorlds != null && !enabledWorlds.isEmpty()) {
								for (String world : enabledWorlds) {
									cmds.add(world);
								}
							}
						}
						else if (args[1].equalsIgnoreCase("near")){
							for (String num : this.zeroThruNine) cmds.add(num);
						}
					}
					else if (args.length >= 4) {
						return cmds;
					}
				}
				else if (args[0].equalsIgnoreCase("summon")) {
					// len:    1      2        3        4       5          6            7   8
					// arg:    0      1        2        3       4          5            6   9
					// lvlmobs summon <amount> <entity> <level> here
					// lvlmobs summon <amount> <entity> <level> atPlayer   <playername>
			    	// lvlmobs summon <amount> <entity> <level> atLocation <x>          <y> <z>
			    	
					
					boolean isAtLocation = false;
					boolean isAtPlayer = false;
					if (args.length > 4) {
						if (args[4].equalsIgnoreCase("atlocation")) isAtLocation = true;
						if (args[4].equalsIgnoreCase("atplayer")) isAtPlayer = true;
					}
					
					if (args.length == 3) {
						for (String mob : this.allMobsList) cmds.add(mob);
					}
					else if (args.length == 2 || args.length == 4) {
						for (String num : this.zeroThruNine) cmds.add(num);
					}
					else if (args.length == 5) {
						cmds.add("atLocation");
						cmds.add("atPlayer");
						cmds.add("here");
					}
					else if (args.length == 6) {
						if (isAtLocation) {
							cmds.add("~ ~ ~");
							cmds.add("~ ~");
							cmds.add("~");
						}
						else if (isAtPlayer) return null; // causes player list to show
					}
					else if (args.length == 7 && !isAtPlayer) {
						cmds.add("~ ~");
						cmds.add("~");
					}
					else if (args.length == 8 && !isAtPlayer) {
						cmds.add("~");
					}
				}
			} // end if args >= 2
			
			return cmds;
		} // end if cmd == leveledmobs
		
		return null;
	}
	
	private List<String> getEnabledWorldsList() {
		String mode = "";
		final List<String> worlds = new ArrayList<String>();
		
		if (instance.fileCache.SETTINGS_WORLDS_LIST_ENABLED) {
            mode = instance.fileCache.SETTINGS_WORLDS_LIST_MODE;
		}
		
		for (World world : Bukkit.getWorlds()) {
			String worldName = world.getName();
			
	        if (!Utils.isNullOrEmpty(mode)) {
	            switch (mode) {
	                case "BLACKLIST":
	                    if (instance.fileCache.SETTINGS_WORLDS_LIST_LIST.contains(worldName)) continue;
	                    break;
	                case "WHITELIST":
	                    if (!instance.fileCache.SETTINGS_WORLDS_LIST_LIST.contains(worldName)) continue;
	                    break;
	            }
	        }
	        else {
	        	worlds.add(worldName);
	        }
		} // next world
		
		return worlds;
	}
}
