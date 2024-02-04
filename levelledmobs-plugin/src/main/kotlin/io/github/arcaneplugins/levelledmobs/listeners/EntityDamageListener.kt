package io.github.arcaneplugins.levelledmobs.listeners

import java.time.Instant
import io.github.arcaneplugins.levelledmobs.LevelledMobs
import io.github.arcaneplugins.levelledmobs.debug.DebugManager
import io.github.arcaneplugins.levelledmobs.enums.Addition
import io.github.arcaneplugins.levelledmobs.debug.DebugType
import io.github.arcaneplugins.levelledmobs.misc.NamespacedKeys
import io.github.arcaneplugins.levelledmobs.misc.QueueItem
import io.github.arcaneplugins.levelledmobs.enums.NametagVisibilityEnum
import io.github.arcaneplugins.levelledmobs.wrappers.LivingEntityWrapper
import org.bukkit.entity.AreaEffectCloud
import org.bukkit.entity.EnderDragon
import org.bukkit.entity.EntityType
import org.bukkit.entity.Ghast
import org.bukkit.entity.Guardian
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.entity.Wither
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.persistence.PersistentDataType

/**
 * Listens for when an entity is damaged so LevelledMobs can apply a multiplier to the damage
 * amount
 *
 * @author lokka30
 * @since 2.4.0
 */
class EntityDamageListener : Listener {
    // When the mob is damaged, update their nametag.
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    fun onEntityDamageEvent(event: EntityDamageEvent) {
        if (event.entity !is LivingEntity) {
            return
        }

        val isCritical = event.finalDamage == 0.0

        if (event is EntityDamageByEntityEvent && isCritical &&
            event.damager is Player
        ) {
            // this is so custom drops can associate the killer if the mob was
            // killed via a custom projectile such as magic
            LevelledMobs.instance.entityDeathListener.damageMappings[event.getEntity().uniqueId] = (event.damager as Player)
            return
        }

        if (isCritical) {
            return
        }

        if (event.entity is Player) {
            if (event !is EntityDamageByEntityEvent) {
                return
            }

            // if a mob hit a player then show the mob's nametag
            if (event.damager !is LivingEntity || event.damager is Player) {
                return
            }

            val theHitter = LivingEntityWrapper.getInstance(event.damager as LivingEntity)
            val nametagVisibilityEnums = LevelledMobs.instance.rulesManager.getRuleCreatureNametagVisbility(
                theHitter
            )
            val nametagVisibleTime = theHitter.getNametagCooldownTime()

            if (nametagVisibleTime > 0L &&
                nametagVisibilityEnums.contains(NametagVisibilityEnum.ATTACKED)
            ) {
                if (theHitter.playersNeedingNametagCooldownUpdate == null) {
                    theHitter.playersNeedingNametagCooldownUpdate = HashSet()
                }
                theHitter.playersNeedingNametagCooldownUpdate!!.add((event.getEntity() as Player))
                LevelledMobs.instance.levelManager.updateNametagWithDelay(theHitter)
            }
            theHitter.free()
            return
        }

        val lmEntity = LivingEntityWrapper.getInstance(event.entity as LivingEntity)

        //Make sure the mob is levelled
        if (!lmEntity.isLevelled) {
            if (lmEntity.main.levelManager.entitySpawnListener.processMobSpawns) {
                lmEntity.free()
                return
            }

            if (lmEntity.getMobLevel < 0) {
                lmEntity.reEvaluateLevel = true
            }
        }

        var wasDamagedByEntity = false
        if (event is EntityDamageByEntityEvent) {
            wasDamagedByEntity = true
            if (event.damager is Player) {
                lmEntity.associatedPlayer = (event.damager as Player)
            }
        }
        val nametagVisibilityEnums = lmEntity.main.rulesManager.getRuleCreatureNametagVisbility(
            lmEntity
        )
        val nametagVisibleTime = lmEntity.getNametagCooldownTime()

        if (nametagVisibleTime > 0L && wasDamagedByEntity &&
            nametagVisibilityEnums.contains(NametagVisibilityEnum.ATTACKED)
        ) {
            if (lmEntity.associatedPlayer != null) {
                if (lmEntity.playersNeedingNametagCooldownUpdate == null) {
                    lmEntity.playersNeedingNametagCooldownUpdate = HashSet()
                }

                lmEntity.playersNeedingNametagCooldownUpdate!!.add(lmEntity.associatedPlayer!!)
            }
        }

        lmEntity.pdc.set(NamespacedKeys.lastDamageTime, PersistentDataType.LONG, Instant.now().toEpochMilli())

        // Update their nametag with a 1 tick delay so that their health after the damage is shown
        lmEntity.main.levelManager.updateNametagWithDelay(lmEntity)
        lmEntity.free()
    }

    // Check for levelled ranged damage.
    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
    fun onEntityDamageByEntityEvent(event: EntityDamageByEntityEvent) {
        if (event.finalDamage == 0.0) {
            return
        }

        processRangedDamage(event)
        processOtherRangedDamage(event)
    }

    private fun processRangedDamage(event: EntityDamageByEntityEvent) {
        if (event.damager.type == EntityType.AREA_EFFECT_CLOUD) {
            // ender dragon breath
            val aec = event.damager as AreaEffectCloud
            if (aec.source !is EnderDragon) {
                return
            }
            val lmEntity = LivingEntityWrapper.getInstance(aec.source as LivingEntity)

            processRangedDamage2(lmEntity, event)
            lmEntity.free()
            return
        }

        if (event.damager !is Projectile) {
            return
        }

        val projectile = event.damager as Projectile
        if (projectile.shooter == null) {
            return
        }

        if (projectile.shooter is Player
            && event.entity is LivingEntity
        ) {
            val lmEntity = LivingEntityWrapper.getInstance(event.entity as LivingEntity)

            if (lmEntity.isLevelled && lmEntity.main.rulesManager.getRuleCreatureNametagVisbility(
                    lmEntity
                ).contains(NametagVisibilityEnum.ATTACKED)
            ) {
                if (lmEntity.playersNeedingNametagCooldownUpdate == null) {
                    lmEntity.playersNeedingNametagCooldownUpdate = java.util.HashSet()
                }
                lmEntity.playersNeedingNametagCooldownUpdate!!.add(projectile.shooter as Player)
                lmEntity.main.levelManager.updateNametagWithDelay(lmEntity)
            }
            lmEntity.free()
            return
        }

        if (projectile.shooter !is LivingEntity) {
            return
        }

        val shooter = LivingEntityWrapper.getInstance(projectile.shooter as LivingEntity)
        processRangedDamage2(shooter, event)

        shooter.free()
    }

    private fun processRangedDamage2(
        shooter: LivingEntityWrapper,
        event: EntityDamageByEntityEvent
    ) {
        if (!shooter.livingEntity.isValid) {
            return
        }
        if (!shooter.isLevelled) {
            if (shooter.main.levelManager.entitySpawnListener.processMobSpawns) {
                return
            }

            shooter.main.mobsQueueManager.addToQueue(QueueItem(shooter, event))
        }

        val newDamage: Float =
            event.damage.toFloat() + shooter.main.mobDataManager.getAdditionsForLevel(
                shooter,
                Addition.CUSTOM_RANGED_ATTACK_DAMAGE, event.damage.toFloat()
            ).amount
        DebugManager.log(DebugType.RANGED_DAMAGE_MODIFICATION, shooter) {
            String.format(
                "&7Source: &b%s&7 (lvl &b%s&7), damage: &b%s&7, new damage: &b%s&7",
                shooter.nameIfBaby, shooter.getMobLevel, event.damage, newDamage
            )
        }
        event.damage = newDamage.toDouble()
    }

    private fun processOtherRangedDamage(event: EntityDamageByEntityEvent) {
        if (event.damager !is LivingEntity) {
            return
        }
        val livingEntity = event.damager as LivingEntity
        if (livingEntity !is Guardian &&
            livingEntity !is Ghast &&
            livingEntity !is Wither
        ) {
            return
        }

        if (!livingEntity.isValid) {
            return
        }
        if (!LevelledMobs.instance.levelInterface.isLevelled(livingEntity)) {
            return
        }

        DebugManager.log(
            DebugType.RANGED_DAMAGE_MODIFICATION,
            livingEntity
        ) { "Range attack damage modified for &b" + livingEntity.name + "&7:" }
        DebugManager.log(
            DebugType.RANGED_DAMAGE_MODIFICATION,
            livingEntity
        ) { "Previous guardianDamage: &b" + event.damage }

        val lmEntity = LivingEntityWrapper.getInstance(livingEntity)
        event.damage = lmEntity.main.mobDataManager.getAdditionsForLevel(
            lmEntity, Addition.CUSTOM_RANGED_ATTACK_DAMAGE,
            event.damage.toFloat()
        ).amount.toDouble() // use ranged attack damage value
        DebugManager.log(DebugType.RANGED_DAMAGE_MODIFICATION, livingEntity) { "New guardianDamage: &b" + event.damage }
        lmEntity.free()
    }
}