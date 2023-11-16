package io.github.arcaneplugins.levelledmobs.bukkit.integration.type.nbt

import org.bukkit.entity.Entity
import org.bukkit.inventory.ItemStack
import java.lang.Exception

class NbtModificationResult() {
    constructor(itemStack: ItemStack) : this(){
        this.itemStack = itemStack
    }

    constructor(entity: Entity) : this(){
        this.entity = entity
    }

    var itemStack: ItemStack? = null
        private set
    var entity: Entity? = null
        private set
    var exception: Exception? = null
        private set
    private val objectsAdded: MutableList<String> = mutableListOf()
    private val objectsUpdated: MutableList<String> = mutableListOf()
    private val objectsRemoved: MutableList<String> = mutableListOf()

    fun withException(exception: Exception):  NbtModificationResult{
        this.exception = exception
        return this
    }

    fun withObjectsAdded(objects: MutableList<Any>): NbtModificationResult{
        for (obj in objects){
            objectsAdded.add(obj.toString())
        }
        return this
    }

    fun withObjectsUpdated(objects: MutableList<Any>): NbtModificationResult{
        for (obj in objects){
            objectsUpdated.add(obj.toString())
        }
        return this
    }

    fun withObjectsRemoved(objects: MutableList<Any>): NbtModificationResult{
        for (obj in objects){
            objectsRemoved.add(obj.toString())
        }
        return this
    }

    val hasEntity: Boolean
        get() {
            return this.entity != null
        }

    val hasItemStack: Boolean
        get() {
            return this.itemStack != null
        }

    val hasException: Boolean
        get() {
            return this.exception != null
        }
}