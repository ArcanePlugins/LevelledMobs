package io.github.lokka30.levelledmobs.listeners;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import io.github.lokka30.levelledmobs.LevelledMobs;
import io.github.lokka30.levelledmobs.utils.Utils;
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
import java.util.concurrent.ThreadLocalRandom;

import static io.github.lokka30.levelledmobs.utils.ManageWorldGuard.*;
import static io.github.lokka30.levelledmobs.utils.Utils.*;

public class LMobSpawn implements Listener {

    private LevelledMobs instance = LevelledMobs.getInstance();

    /*
    This class assigns mob levels to each entity spawned.
    Attribute determined by: setBaseValue(default + elevated? + (increase-per-level * level)
     */
    @EventHandler
    public void onMobSpawn(final CreatureSpawnEvent e) {
        if (!e.isCancelled()) {
            final int level; //The mob's level.
            LivingEntity ent = e.getEntity(); //The entity that was just spawned.

            //Check settings for spawn distance levelling and choose levelling method accordingly.
            if(instance.worldguard && checkRegionFlags(ent)) {
                level = generateRegionLevel(ent);
            } else if (instance.settings.get("spawn-distance-levelling.active", false)) {
                level = generateLevelByDistance(ent);
            } else {
                level = generateLevel();
            }

            if (instance.levelManager.isLevellable(ent)) { //Is the mob allowed to be levelled?

                //Check the list of blacklisted worlds. If the entity's world is in here, then we don't continue.
                for (String blacklistedWorld : instance.settings.get("blacklisted-worlds", Collections.singletonList("BLACKLISTED_WORLD"))) {
                    if (e.getEntity().getWorld().getName().equalsIgnoreCase(blacklistedWorld)) {
                        return;
                    }
                }

                //Check the list of blacklisted spawn reasons. If the entity's spawn reason is in there, then we don't continue.
                //Uses a default as "NONE" as there are no blocked spawn reasons in the default config.
                for (String blacklistedReason : instance.settings.get("blacklisted-reasons", Collections.singletonList("NONE"))) {
                    if (e.getSpawnReason().toString().equalsIgnoreCase(blacklistedReason)) {
                        return;
                    }
                }

                //Set the entity's max health.
                final double baseMaxHealth = Objects.requireNonNull(e.getEntity().getAttribute(Attribute.GENERIC_MAX_HEALTH)).getBaseValue();
                final double newMaxHealth = baseMaxHealth + (baseMaxHealth * (instance.settings.get("fine-tuning.multipliers.max-health", 0.2F)) * level);
                Objects.requireNonNull(ent.getAttribute(Attribute.GENERIC_MAX_HEALTH)).setBaseValue(newMaxHealth);
                ent.setHealth(newMaxHealth); //Set the entity's health to their max health, otherwise their health is still the default of 20, so they'll be just as easy to kill.

                //Set the entity's movement speed.
                //Only monsters should have their movement speed changed. Otherwise you would have a very fast level 10 race horse, or an untouchable bat.
                if (ent instanceof Monster) {
                    final double baseMovementSpeed = Objects.requireNonNull(e.getEntity().getAttribute(Attribute.GENERIC_MOVEMENT_SPEED)).getBaseValue();
                    final double newMovementSpeed = baseMovementSpeed + (baseMovementSpeed * instance.settings.get("fine-tuning.multipliers.movement-speed", 0.065F) * level);
                    Objects.requireNonNull(ent.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED)).setBaseValue(newMovementSpeed);
                }

                //These are melee mobs - their attack damage can be defined.
                // Don't touch ranged mobs, else a NPE will occur.
                // Ranged mobs' damage is planned to be added in a later date.
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
                    default: //The mob isn't a melee mob defined above. Don't set their movement speed.
                        break;
                }

                //Define the mob's level so it can be accessed elsewhere.
                e.getEntity().getPersistentDataContainer().set(instance.key, PersistentDataType.INTEGER, level);

                //Update their tag.
                instance.levelManager.updateTag(e.getEntity());

            } else if (e.getSpawnReason() == CreatureSpawnEvent.SpawnReason.CURED) {
                //Check if a zombie villager was cured. If villagers aren't levellable, then their name will be cleared,
                // otherwise their nametag is still 'Zombie Villager'. Imposter!
                e.getEntity().setCustomName("");
            }
        }
    }

    //Generates a level.
    //Uses ThreadLocalRandom.current().nextInt(min, max + 1). + 1 is because ThreadLocalRandom is usually exclusive of the uppermost value.
    public Integer generateLevel() {
        return ThreadLocalRandom.current().nextInt(instance.settings.get("fine-tuning.min-level", 1), instance.settings.get("fine-tuning.max-level", 10) + 1);
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
            for (int i = 0; i < levelspan; i++) {
                levelarray[i] = Utils.binomialDistribution(levelspan, i, binomialp);
            }

            //Create weighted array for choosing a level
            weightedlevelarray = Utils.createWeightedArray(levelarray);

            //Choose a level based on the weight of a level
            randomnumber = new Random().nextDouble() * weightedlevelarray[weightedlevelarray.length - 1];
            for (int i = 0; i < weightedlevelarray.length; i++)
                if (randomnumber <= weightedlevelarray[i]) {
                    finallevel = i + minlevel;
                    break;
                }

        }
    	else
    		finallevel = defaultlevel;
    	
    	finallevel = finallevel == -1 ? 0 : finallevel;
    	
    	return finallevel;
    }

    private int generateRegionLevel(LivingEntity ent){
        //Fallback values
        int minlevel = instance.settings.get("fine-tuning.min-level", 1);
        int maxlevel = instance.settings.get("fine-tuning.max-level", 10);

        int[] levels = getRegionLevel(ent, minlevel, maxlevel);

        return levels[0] + Math.round(new Random().nextFloat() * (levels[1] - levels[0]));
    }
}
