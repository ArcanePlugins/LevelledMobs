package io.github.arcaneplugins.levelledmobs.bukkit.logic.context

import io.github.arcaneplugins.levelledmobs.bukkit.logic.LogicHandler
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.LmFunction
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.Ageable
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.Event
import java.util.function.Supplier

class Context {
    constructor(
        entity: Entity? = null,
        event: Event? = null,
        location: Location? = null,
        player: Player? = null,
        //rule: Rule? = null,
        world: World? = null) {
        this.entity = entity
        this.event = event
        this.location = location
        this.player = player
        //this.rule = rule
        this.world = world
    }

    var entityType: EntityType? = null
    var father: LivingEntity? = null
    var mother: LivingEntity? = null
    val linkedFunctions = mutableListOf<LmFunction>()
    val miscContext = mutableMapOf<String, Supplier<Any>>()
    var event: Event? = null
    var location: Location? = null
    var player: Player? = null
    //var rule: Rule? = null
    var world: World? = null
        get() {
            if (field == null && this.location?.world != null){
                field = this.location!!.world
            }

            return field
        }
    val other: MutableMap<String, Any> = mutableMapOf()
    /* commonly accessed data is stored as a variable */
    //var ruleStack: Stack<Rule> = Stack()
    var entity: Entity? = null
        set(value) {
            field = value
            if (value is LivingEntity) {
                this.livingEntity = value
            }
            if (value != null) {
                this.entityType = entity!!.type
                this.location = value.location
            }
        }
    var livingEntity: LivingEntity? = null
        private set

    val isBaby: Boolean
        get() {
            if (livingEntity == null) return false

            return if (livingEntity is Ageable){
                !(livingEntity as Ageable).isAdult
            } else{
                false
            }
        }

    val nameIfBaby: String?
        get() {
            if (livingEntity == null) return null

            return if (isBaby){
                "BABY_" + livingEntity!!.type
            }
            else{
                livingEntity!!.type.toString()
            }
        }

    /* methods */
    fun replacePlaceholders(from: String): String {
        return LogicHandler.CONTEXT_PLACEHOLDER_HANDLER.replace(from, this)
    }

    fun withEntity(
        entity: Entity
    ): Context{
        this.entity = entity
        return this
    }

    fun withLinkedFunction(
        linkedFunction: LmFunction
    ): Context{
        this.linkedFunctions.add(linkedFunction)
        return this
    }

    fun withFather(father: LivingEntity): Context{
        this.father = father
        return this
    }

    fun withMother(mother: LivingEntity): Context{
        this.mother = mother
        return this
    }

    fun withPlayer(player: Player): Context{
        this.player = player
        return this
    }

    fun withEvent(event: Event): Context{
        this.event
        return this
    }
}