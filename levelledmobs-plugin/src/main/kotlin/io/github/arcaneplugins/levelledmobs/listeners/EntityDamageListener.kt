package io.github.arcaneplugins.levelledmobs.listeners

import java.time.Instant
import io.github.arcaneplugins.levelledmobs.LevelledMobs
import io.github.arcaneplugins.levelledmobs.debug.DebugManager
import io.github.arcaneplugins.levelledmobs.enums.Addition
import io.github.arcaneplugins.levelledmobs.debug.DebugType
import io.github.arcaneplugins.levelledmobs.misc.NamespacedKeys
import io.github.arcaneplugins.levelledmobs.misc.QueueItem
import io.github.arcaneplugins.levelledmobs.enums.NametagVisibilityEnum
import io.github.arcaneplugins.levelledmobs.managers.MobDataManager
import io.github.arcaneplugins.levelledmobs.util.Log
import io.github.arcaneplugins.levelledmobs.wrappers.LivingEntityWrapper
import org.bukkit.Bukkit
import org.bukkit.entity.AreaEffectCloud
import org.bukkit.entity.EnderDragon
import org.bukkit.entity.EntityType
import org.bukkit.entity.Ghast
import org.bukkit.entity.Guardian
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.entity.Wither
import org.bukkit.event.EventPriority
import org.bukkit.event.HandlerList
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
    var updateMobsOnNonPlayerdamage = true
    private var lastPriority: EventPriority? = null
    private val settingName = "entity-damage-event"

    companion object {
        @JvmStatic
        lateinit var instance: EntityDamageListener
            private set
    }

    init {
        instance = this
    }

    fun load() {
        val priority = LevelledMobs.instance.mainCompanion.getEventPriority(settingName, EventPriority.MONITOR)
        if (lastPriority != null) {
            if (priority == lastPriority) return

            HandlerList.unregisterAll(this)
            Log.inf("Changing event priority for $settingName from $lastPriority to $priority")
        }

        Bukkit.getPluginManager().registerEvent(
            EntityDamageEvent::class.java,
            this,
            priority,
            { _, event -> if (event is EntityDamageEvent) onEntityDamageEvent(event) },
            LevelledMobs.instance,
            false
        )

        Bukkit.getPluginManager().registerEvent(
            EntityDamageByEntityEvent::class.java,
            this,
            priority,
            { _, event -> if (event is EntityDamageByEntityEvent) onEntityDamageByEntityEvent(event) },
            LevelledMobs.instance,
            false
        )

        lastPriority = priority
    }

    // When the mob is damaged, update their nametag.
    private fun onEntityDamageEvent(event: EntityDamageEvent) {
        if (event.entity !is LivingEntity) return

        val isCritical = event.finalDamage == 0.0

        if (event is EntityDamageByEntityEvent
        ) {
            if (isCritical && event.damager is Player) {
                // this is so custom drops can associate the killer if the mob was
                // killed via a custom projectile such as magic
                LevelledMobs.instance.entityDeathListener.damageMappings[event.getEntity().uniqueId] =
                    (event.damager as Player)
                return
            }
            if (!updateMobsOnNonPlayerdamage && !isCritical && event.entity !is Player && event.damager !is Player) {
                // we only care about player caused damage
                return
            }
        } else if (!updateMobsOnNonPlayerdamage) {
            // we only care about player caused damage
            return
        }

        if (isCritical) return

        if (event.entity is Player) {
            if (event !is EntityDamageByEntityEvent) return

            // if a mob hit a player then show the mob's nametag
            if (event.damager !is LivingEntity || event.damager is Player)
                return

            val theHitter = LivingEntityWrapper.getInstance(event.damager as LivingEntity)
            val nametagVisibilityEnums = theHitter.nametagVisibilityEnum
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
        val livingEntity = event.entity as LivingEntity
        val lmEntity = LivingEntityWrapper.getInstance(event.entity as LivingEntity)

        //Make sure the mob is levelled
        if (!lmEntity.isLevelled) {
            if (EntitySpawnListener.instance.processMobSpawns) {
                lmEntity.free()
                return
            }

            if (lmEntity.getMobLevel < 0)
                lmEntity.reEvaluateLevel = true
        }

        var wasDamagedByEntity = false
        if (event is EntityDamageByEntityEvent) {
            wasDamagedByEntity = true
            if (event.damager is Player)
                lmEntity.associatedPlayer = (event.damager as Player)
        }
        livingEntity.scheduler.run(LevelledMobs.instance, { task ->
            val nametagVisibilityEnums = lmEntity.nametagVisibilityEnum
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
        }, null)
    }

    // Check for levelled ranged damage.
    private fun onEntityDamageByEntityEvent(event: EntityDamageByEntityEvent) {
        if (event.finalDamage == 0.0) return

        event.entity.scheduler.run(LevelledMobs.instance, { task ->
            processRangedDamage(event)
            processOtherRangedDamage(event)
        }, null)
    }

    private fun processRangedDamage(event: EntityDamageByEntityEvent) {
        if (event.damager.type == EntityType.AREA_EFFECT_CLOUD) {
            // ender dragon breath
            val aec = event.damager as AreaEffectCloud
            if (aec.source !is EnderDragon) return

            val lmEntity = LivingEntityWrapper.getInstance(aec.source as LivingEntity)
            MobDataManager.populateAttributeCache(lmEntity)

            processRangedDamage2(lmEntity, event)
            lmEntity.free()
            return
        }

        if (event.damager !is Projectile) return

        val projectile = event.damager as Projectile
        if (projectile.shooter == null) return

        if (projectile.shooter is Player && event.entity is LivingEntity) {
            val lmEntity = LivingEntityWrapper.getInstance(event.entity as LivingEntity)
            MobDataManager.populateAttributeCache(lmEntity)

            if (lmEntity.isLevelled && (lmEntity.nametagVisibilityEnum).contains(NametagVisibilityEnum.ATTACKED)) {
                if (lmEntity.playersNeedingNametagCooldownUpdate == null) {
                    lmEntity.playersNeedingNametagCooldownUpdate = java.util.HashSet()
                }
                lmEntity.playersNeedingNametagCooldownUpdate!!.add(projectile.shooter as Player)
                lmEntity.main.levelManager.updateNametagWithDelay(lmEntity)
            }
            lmEntity.free()
            return
        }

        if (projectile.shooter !is LivingEntity) return

        val shooter = LivingEntityWrapper.getInstance(projectile.shooter as LivingEntity)
        MobDataManager.populateAttributeCache(shooter)
        processRangedDamage2(shooter, event)

        shooter.free()
    }

    private fun processRangedDamage2(
        shooter: LivingEntityWrapper,
        event: EntityDamageByEntityEvent
    ) {
        if (!shooter.livingEntity.isValid) return

        if (!shooter.isLevelled) {
            if (EntitySpawnListener.instance.processMobSpawns) return

            shooter.main.mobsQueueManager.addToQueue(QueueItem(shooter, event))
        }

        shooter.rangedDamage = event.damage.toFloat()
        val newDamage: Float =
            event.damage.toFloat() + shooter.main.mobDataManager.getAdditionsForLevel(
                shooter,
                Addition.CUSTOM_RANGED_ATTACK_DAMAGE, event.damage.toFloat()
            ).amount
        DebugManager.log(DebugType.RANGED_DAMAGE_MODIFICATION, shooter) {
            "damage: &b${event.damage}&7, new damage: &b$newDamage&7"
        }
        event.damage = newDamage.toDouble()
    }

    private fun processOtherRangedDamage(event: EntityDamageByEntityEvent) {
        if (event.damager !is LivingEntity) return

        val livingEntity = event.damager as LivingEntity
        if (livingEntity !is Guardian &&
            livingEntity !is Ghast &&
            livingEntity !is Wither
        ) {
            return
        }

        if (!livingEntity.isValid) return
        if (!LevelledMobs.instance.levelInterface.isLevelled(livingEntity)) return

        val oldDamage = event.damage
        val lmEntity = LivingEntityWrapper.getInstance(livingEntity)
        event.damage = lmEntity.main.mobDataManager.getAdditionsForLevel(
            lmEntity, Addition.CUSTOM_RANGED_ATTACK_DAMAGE,
            event.damage.toFloat()
        ).amount.toDouble() // use ranged attack damage value
        DebugManager.log(DebugType.RANGED_DAMAGE_MODIFICATION, livingEntity)
        { "old damage: &b: $oldDamage&r, new damage: &b${event.damage}&r" }
        lmEntity.free()
    }
}