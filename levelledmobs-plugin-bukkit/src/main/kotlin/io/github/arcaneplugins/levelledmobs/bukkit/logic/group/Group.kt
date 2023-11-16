package io.github.arcaneplugins.levelledmobs.bukkit.logic.group

class Group {
    constructor(identifier: String){
        this.identifier = identifier
    }

    constructor(identifier: String, items: MutableList<String>){
        this.identifier = identifier
        this.items.addAll(items)
    }

    val identifier: String
    val items: MutableList<String> = mutableListOf()
}