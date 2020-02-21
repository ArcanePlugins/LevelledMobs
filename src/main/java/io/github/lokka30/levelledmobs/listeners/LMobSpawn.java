package io.github.lokka30.levelledmobs.listeners;

import io.github.lokka30.levelledmobs.LevelledMobs;

import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.persistence.PersistentDataType;

import java.util.Collections;
import java.util.Objects;
import java.util.Random;

public class LMobSpawn implements Listener {

    private LevelledMobs instance = LevelledMobs.getInstance();

    /*
    This class assigns mob levels to each entity spawned.
    Attribute determined by: setBaseValue(default + elevated? + (increase-per-level * level)
     */
    @EventHandler
    public void onMobSpawn(final CreatureSpawnEvent e) {
        if (!e.isCancelled()) {
        	final int level;
            LivingEntity ent = e.getEntity();
            
            //Check settings for spawn distance levelling and choose accordingly
            if(instance.settings.get("spawn-distance-levelling.active", false))
            	level = generateLevelByDistance(ent);
            else
            	level = generateLevel();
            
            if (instance.isLevellable(ent)) {
                for (String blacklistedWorld : instance.settings.get("blacklisted-worlds", Collections.singletonList("BLACKLISTED_WORLD"))) {
                    if (e.getEntity().getWorld().getName().equalsIgnoreCase(blacklistedWorld)) {
                        return;
                    }
                }

                for (String blacklistedReason : instance.settings.get("blacklisted-reasons", Collections.singletonList("NONE"))) {
                    if (e.getSpawnReason().toString().equalsIgnoreCase(blacklistedReason)) {
                        return;
                    }
                }

                final double baseMaxHealth = Objects.requireNonNull(e.getEntity().getAttribute(Attribute.GENERIC_MAX_HEALTH)).getBaseValue();
                final double newMaxHealth = baseMaxHealth + (baseMaxHealth * (instance.settings.get("fine-tuning.multipliers.max-health", 0.2F)) * level);
                Objects.requireNonNull(ent.getAttribute(Attribute.GENERIC_MAX_HEALTH)).setBaseValue(newMaxHealth);
                ent.setHealth(newMaxHealth);

                //Only monsters should have their movement speed changed. Otherwise you would have a very fast level 10 race horse, or an untouchable bat.
                if (ent instanceof Monster) {
                    final double baseMovementSpeed = Objects.requireNonNull(e.getEntity().getAttribute(Attribute.GENERIC_MOVEMENT_SPEED)).getBaseValue();
                    final double newMovementSpeed = baseMovementSpeed + (baseMovementSpeed * instance.settings.get("fine-tuning.multipliers.movement-speed", 0.065F) * level);
                    Objects.requireNonNull(ent.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED)).setBaseValue(newMovementSpeed);
                }

                //These are melee mobs - their attack damage can be defined. Don't touch ranged mobs, else a NPE will occur.
                switch (ent.getType()) {
                    case ZOMBIE:
                    case HUSK:
                    case DROWNED:
                    case ZOMBIE_VILLAGER:
                    case WITHER_SKELETON:
                    case PIG_ZOMBIE:
                    case CAVE_SPIDER:
                    case SILVERFISH:
                    case SPIDER:
                    case ENDERMAN:
                    case ENDERMITE:
                    case SLIME:
                    case VINDICATOR:
                    case RAVAGER:
                    case EVOKER:
                    case IRON_GOLEM:
                        final double baseAttackDamage = Objects.requireNonNull(e.getEntity().getAttribute(Attribute.GENERIC_ATTACK_DAMAGE)).getBaseValue();
                        final double defaultAttackDamageAddition = instance.settings.get("fine-tuning.default-attack-damage-increase", 1.0F);
                        final double attackDamageMultiplier = instance.settings.get("fine-tuning.multipliers.attack-damage", 1.5F);
                        final double newAttackDamage = baseAttackDamage + defaultAttackDamageAddition + (attackDamageMultiplier * level);

                        Objects.requireNonNull(ent.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE)).setBaseValue(newAttackDamage);
                        break;
                    default:
                        break;
                }

                //Set the level.
                e.getEntity().getPersistentDataContainer().set(instance.key, PersistentDataType.INTEGER, level);

                //Update their tag.
                instance.updateTag(e.getEntity());
            }
        }
    }

    //Generates a level.
    public Integer generateLevel() {
        return new Random().nextInt(instance.settings.get("fine-tuning.max-level", 10) + 1) + instance.settings.get("fine-tuning.min-level", 0);
    }
    
    //Generates a level based on distance to spawn and, if active, variance
    private Integer generateLevelByDistance(LivingEntity lEnt) {
    	int minlevel, maxlevel, defaultlevel, finallevel, levelspan, distance;
    	
    	minlevel = instance.settings.get("fine-tuning.min-level", 0);
    	maxlevel = instance.settings.get("fine-tuning.max-level", 10);
    	finallevel = -1;
    	
    	//Calculate amount of available levels
    	levelspan = (maxlevel + 1) - minlevel;
    	
    	//Get distance between entity spawn point and world spawn
    	distance = (int) lEnt.getWorld().getSpawnLocation().distance(lEnt.getLocation());
    	
    	//Get the level thats meant to be at a given distance
    	defaultlevel = (distance / instance.settings.get("spawn-distance-levelling.increase-level-distance", 200)) + minlevel;
    	if(defaultlevel > maxlevel)
    		defaultlevel = maxlevel;
    	
    	//Check if there should be a variance in level
    	if(instance.settings.get("spawn-distance-levelling.variance", true)){
    		double binomialp, randomnumber;
    		double[] levelarray, weightedlevelarray;
    		
    		
    		//Create array with chances for each level
    		levelarray = new double[levelspan];
    		binomialp = (1.0D / levelspan / 2.0D) + ((1.0D - (1.0D / levelspan)) / levelspan * (defaultlevel - minlevel));
    		for(int i = 0; i < levelspan; i++) {
    			levelarray[i] = binomialDistribution(levelspan, i, binomialp);
    		}
    		
    		//Create weighted array for choosing a level
    		weightedlevelarray = createWeightedArray(levelarray);
    		
    		//Choose a level based on the weight of a level
    		randomnumber = new Random().nextDouble() * weightedlevelarray[weightedlevelarray.length - 1];
    		for(int i = 0; i < weightedlevelarray.length; i++)
    			if(randomnumber <= weightedlevelarray[i]) {
    				finallevel = i + minlevel;
    				break;
    			}
    		
    	}
    	else
    		finallevel = defaultlevel;
    	
    	finallevel = finallevel == -1 ? 0 : finallevel;
    	
    	return finallevel;
    }
    
    //Creates a weighted array where the values contain the sum of itself and all preceding values
    private double[] createWeightedArray(double[] inputarray) {
    	double[] outputarray = new double[inputarray.length];
    	
    	outputarray[0] = inputarray[0];
    	for(int i = 1; i < inputarray.length; i++) {
    		outputarray[i] = inputarray[i] + outputarray[i - 1];
    	}
    	
    	return outputarray;
    }
    
    //Binomial distribution function
    private double binomialDistribution(int n, int k, double p) {
    	double chance;
    		chance = factorial(n) / (factorial(k)*factorial(n-k)) * Math.pow(p, k) * Math.pow(1 - p, n - k);
    	return chance;
    }
    
    //Factorial function
    private long factorial(int num) {
    	long result = 1;
    	for(int i = num; i > 1; i--)
    			result *= i;
    	return result;
    }
}
