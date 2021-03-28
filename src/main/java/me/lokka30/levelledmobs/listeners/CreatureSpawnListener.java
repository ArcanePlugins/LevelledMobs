package me.lokka30.levelledmobs.listeners;

import me.lokka30.levelledmobs.LevelInterface;
import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.managers.LevelManager;
import me.lokka30.levelledmobs.misc.*;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.enchantments.EnchantmentTarget;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

/**
 * @author lokka30
 * @contributors stumper66
 */
public class CreatureSpawnListener implements Listener {

    private final LevelledMobs main;
    private final HashSet<String> forcedTypes = new HashSet<>(Arrays.asList("ENDER_DRAGON", "PHANTOM"));

    public CreatureSpawnListener(final LevelledMobs main) {
        this.main = main;
    }

    /**
     * This listener handles death nametags
     *
     * @param event PlayerDeathEvent
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerDeath(final PlayerDeathEvent event) {
        String deathNametag = main.settingsCfg.getString("creature-death-nametag", "&8[&7Level %level%&8 | &f%displayname%&8]");
        if (Utils.isNullOrEmpty(deathNametag))
            return; // if they want retain the stock message they are configure it with an empty string

        final EntityDamageEvent entityDamageEvent = event.getEntity().getLastDamageCause();
        if (entityDamageEvent == null || entityDamageEvent.isCancelled() || !(entityDamageEvent instanceof EntityDamageByEntityEvent)) {
            return;
        }

        Entity damager = ((EntityDamageByEntityEvent) entityDamageEvent).getDamager();
        LivingEntity killer;

        if (damager instanceof Projectile) {
            killer = (LivingEntity) ((Projectile) damager).getShooter();
        } else if (!(damager instanceof LivingEntity)) {
            return;
        } else {
            killer = (LivingEntity) damager;
        }

        if (killer == null) return;
        if (!main.levelInterface.isLevelled(killer)) return;

        event.setDeathMessage(Utils.replaceEx(event.getDeathMessage(), killer.getName(), main.levelManager.getNametag(killer, true)));
    }

    /**
     * This listens for entities that are not passed thrrough CreatureSpawnEvent,
     * such as Phantoms and Ender Dragons, which server owners may want to have levelled.
     *
     * @param event EntitySpawnEvent
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onEntitySpawn(final EntitySpawnEvent event) {
        if (!forcedTypes.contains(event.getEntityType().toString())) return;

        if (!(event.getEntity() instanceof LivingEntity)) return;
        final LivingEntity livingEntity = (LivingEntity) event.getEntity();

        //TODO uncomment following code when ready
        /*
        if(main.levelInterface.getLevellableState(livingEntity) == LevelInterface.LevellableState.ALLOWED) {
            //delay this so it can skip default levelling if the mob was already levelled (e.g. summoned)
            //ran 1 tick later (20th of a second), asynchronously (on different thread).
            new BukkitRunnable() {
                @Override
                public void run() {
                    if(!main.levelInterface.isLevelled(livingEntity)) {
                        final int level = main.levelInterface.generateLevelForMob(livingEntity);
                        main.levelInterface.applyLevelToMob(livingEntity, level, false, false);
                    }
                }
            }.runTaskLaterAsynchronously(main, 1);
        }
         */

        //TODO move the following code
        final int mobLevel = processMobSpawn(livingEntity, SpawnReason.DEFAULT, -1, MobProcessReason.NONE, false);
        if (mobLevel >= 0 && main.settingsCfg.getBoolean("use-custom-item-drops-for-mobs"))
            processMobEquipment(livingEntity, mobLevel);
    }

    /**
     * This listens to most mobs that spawn in.
     *
     * @param event CreatureSpawnEvent, the event to listen to
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onCreatureSpawn(final CreatureSpawnEvent event) {
        // spawned using summon command.  It will get processed directly.  However 3rd party plugins like MM use custom as well
        if (event.getSpawnReason() == SpawnReason.CUSTOM) return;

        // process the spawns after 1 tick so other plugins have a chance to process them, such as mythic mobs

        new BukkitRunnable() {
            public void run() {
                final int mobLevel = processMobSpawn(event.getEntity(), event.getSpawnReason(), -1, MobProcessReason.NONE, false);
                if (mobLevel >= 0 && main.settingsCfg.getBoolean("use-custom-item-drops-for-mobs"))
                    processMobEquipment(event.getEntity(), mobLevel);
            }
        }.runTaskLater(main, 1L);
    }

    public int processMobSpawn(final LivingEntity livingEntity, final SpawnReason spawnReason, int level, final MobProcessReason processReason, final boolean override) {

        //Check if the mob is already levelled
        if (main.levelInterface.isLevelled(livingEntity)) return -1;

        //Check the 'worlds list' to see if the mob is allowed to be levelled in the world it spawned in
        if (!override && !ModalList.isEnabledInList(main.settingsCfg, "allowed-worlds-list", livingEntity.getWorld().getName())) {
            if (main.settingsCfg.getBoolean("debug-show-mobs-not-levellable")) {
                Utils.logger.info("&b" + livingEntity.getName() + "&7 spawned but is not levellable - not in allowed-worlds-list");
            }
            return -1;
        }

        boolean isSpawner = false;
        if (spawnReason == SpawnReason.SPAWNER) {
            livingEntity.getPersistentDataContainer().set(main.levelManager.isSpawnerKey, PersistentDataType.STRING, "true");
            isSpawner = true;
        }

        //Check the list of blacklisted spawn reasons. If the entity's spawn reason is in there, then we don't continue.
        //Uses a default as "NONE" as there are no blocked spawn reasons in the default config.
        if (!override && !ModalList.isEnabledInList(main.settingsCfg, "allowed-spawn-reasons-list", spawnReason.toString()) ||
                isSpawner && !ModalList.isEnabledInList(main.settingsCfg, "allowed-spawn-reasons-list", SpawnReason.SPAWNER.toString())) {
            if (main.settingsCfg.getBoolean("debug-show-mobs-not-levellable")) {
                Utils.logger.info("&b" + livingEntity.getName() + "&7 spawned but is not levellable - not in allowed-spawn-reasons-list");
            }
            return -1;
        }

        final DebugInfo debugInfo = main.settingsCfg.getBoolean("debug-show-spawned-mobs") ?
                new DebugInfo() : null;

        if (override || main.levelManager.isLevellable(livingEntity)) {

            // if spawned naturally it will be -1.  If used summon with specific level specified or if using the slime child system then it will be >= 0
            // all level logic should now be in LevelManager.java
            if (level == -1)
                level = main.levelManager.generateLevel(livingEntity, debugInfo, spawnReason);

            //Define the mob's level so it can be accessed elsewhere.
            livingEntity.getPersistentDataContainer().set(main.levelManager.levelKey, PersistentDataType.INTEGER, level);

            // Modify their maximum health attribute. This changes the maximum health the levelled mob has.
            // Makes sure the levelled mob has this attribute. If not, skip setting it.
            if (livingEntity.getAttribute(Attribute.GENERIC_MAX_HEALTH) != null) {

                boolean useBaseValue = (livingEntity instanceof Slime);

                // This sets the max health value.
                main.mobDataManager.setAdditionsForLevel(livingEntity, Attribute.GENERIC_MAX_HEALTH, Addition.ATTRIBUTE_MAX_HEALTH, level, useBaseValue);

                // Need to set their actual health otherwise their current health != max health.
                livingEntity.setHealth(Objects.requireNonNull(livingEntity.getAttribute(Attribute.GENERIC_MAX_HEALTH)).getBaseValue());
            }

            // Modify their movement speed attribute. This changes the movement speed the levelled mob has.
            // Makes sure the levelled mob has this attribute. If not, skip setting it.
            if (livingEntity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED) != null) {
                //This sets the movement speed value.
                main.mobDataManager.setAdditionsForLevel(livingEntity, Attribute.GENERIC_MOVEMENT_SPEED, Addition.ATTRIBUTE_MOVEMENT_SPEED, level, false);
            }

            // Modify their attack damage attribute. This changes the attack damage the levelled mob has.
            // Makes sure the levelled mob has this attribute. If not, skip setting it.
            if (livingEntity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE) != null) {
                //This sets the movement speed value.
                main.mobDataManager.setAdditionsForLevel(livingEntity, Attribute.GENERIC_ATTACK_DAMAGE, Addition.ATTRIBUTE_ATTACK_DAMAGE, level, false);
            }

            if (livingEntity instanceof Creeper && main.settingsCfg.getInt("creeper-max-damage-radius", 3) != 3) {

                // level 1 ends up with 3 (base) and anything higher becomes a percent of the max radius as specified in the config
                //int blastRadius = (int) Math.floor(level / 10.0 * ((double) instance.settingsCfg.getInt("creeper-max-damage-radius")) - 3) + 3;

                final double levelDiff = main.settingsCfg.getInt("fine-tuning.max-level", 10) - main.settingsCfg.getInt("fine-tuning.min-level", 1);
                final double maxBlastDiff = main.settingsCfg.getInt("creeper-max-damage-radius", 3) - 3;
                final double useLevel = level - main.settingsCfg.getInt("fine-tuning.min-level", 1);
                final double percent = useLevel / levelDiff;
                int blastRadius = (int) Math.round(maxBlastDiff * percent) + 3;

                // even at 100 creepers are atomic bombs but at least won't blow up the entire world
                if (blastRadius > LevelManager.maxCreeperBlastRadius) {
                    blastRadius = LevelManager.maxCreeperBlastRadius;
                }
                if (level == 1) {
                    blastRadius = 3;
                } // level 1 creepers will always have default radius
                else if (level == 0 && blastRadius > 2) {
                    blastRadius = 2;
                } // level 0 will always be less than default

                ((Creeper) livingEntity).setExplosionRadius(blastRadius);
            }

            //Update their tag.
            final String nameTag;
            if (level != 1 || main.settingsCfg.getBoolean("show-label-for-default-levelled-mobs")) {
                nameTag = main.levelManager.getNametag(livingEntity, level, false);
                main.levelManager.updateNametagWithDelay(livingEntity, nameTag, livingEntity.getWorld().getPlayers(), 1);
            }

            // Debug Info
            if (debugInfo != null) {
                boolean isBabyEntity = Utils.isBabyMob(livingEntity);

                if (spawnReason == SpawnReason.CUSTOM) {
                    debugInfo.minLevel = level;
                    debugInfo.maxLevel = level;
                }

                if (processReason == MobProcessReason.SUMMON) {
                    debugInfo.rule = MobProcessReason.SUMMON;
                }

                String babyOrAdult = isBabyEntity ? "Baby" : "Adult";
                String rule;
                switch (debugInfo.rule) {
                    case WORLD:
                        rule = " - World rule";
                        break;
                    case ENTITY:
                        rule = " - Entity rule";
                        break;
                    case WORLD_GUARD:
                        rule = " - WG rule";
                        break;
                    case SUMMON:
                        rule = " - summon rule";
                        break;
                    case TRANSFORM:
                        rule = " - transform rule";
                        break;
                    default:
                        rule = "";
                }

                Utils.logger.info(String.format("Spawned a &fLvl.%s &b%s &8(&7%s&8) min: %s, max: %s%s, reason: %s",
                        level, livingEntity.getName(), babyOrAdult, debugInfo.minLevel, debugInfo.maxLevel, rule, spawnReason));
            }

        } else if (spawnReason == CreatureSpawnEvent.SpawnReason.CURED) {
            //Check if a zombie villager was cured. If villagers aren't levellable, then their name will be cleared,
            //otherwise their nametag is still 'Zombie Villager'.
            main.levelManager.updateNametagWithDelay(livingEntity, null, livingEntity.getWorld().getPlayers(), 1);
        } else {
            if (main.settingsCfg.getBoolean("debug-show-mobs-not-levellable")) {
                Utils.logger.info("&b" + livingEntity.getName() + "&7 spawned but is not levellable");
            }
        }

        return level;
    }

    public void processMobEquipment(final LivingEntity livingEntity, final int level){
        List<ItemStack> items = new ArrayList<>();
        main.customDropsHandler.getCustomItemDrops(livingEntity, level, items, true, true);
        if (items.isEmpty()) return;

        EntityEquipment ee = livingEntity.getEquipment();
        if (ee == null) return;

        boolean hadMainItem = false;

        for (ItemStack itemStack : items) {
            Material material = itemStack.getType();
            if (EnchantmentTarget.ARMOR_FEET.includes(material)) {
                ee.setBoots(itemStack, true);
            } else if (EnchantmentTarget.ARMOR_LEGS.includes(material)) {
                ee.setLeggings(itemStack, true);
            } else if (EnchantmentTarget.ARMOR_TORSO.includes(material)) {
                ee.setChestplate(itemStack, true);
            } else if (EnchantmentTarget.ARMOR_HEAD.includes(material)) {
                ee.setHelmet(itemStack, true);
            } else {
                if (!hadMainItem) {
                    ee.setItemInMainHand(itemStack);
                    hadMainItem = true;
                } else {
                    ee.setItemInOffHand(itemStack);
                }
            }
        }
    }
}