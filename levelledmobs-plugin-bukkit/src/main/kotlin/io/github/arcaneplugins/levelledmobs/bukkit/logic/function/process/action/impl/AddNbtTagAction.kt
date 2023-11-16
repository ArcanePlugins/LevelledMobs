package io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.action.impl

import io.github.arcaneplugins.levelledmobs.bukkit.integration.IntegrationHandler.getPrimaryNbtProvider
import io.github.arcaneplugins.levelledmobs.bukkit.integration.type.nbt.NbtModificationResult
import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.Context
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.Process
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.action.Action
import org.bukkit.entity.LivingEntity
import org.spongepowered.configurate.CommentedConfigurationNode
import org.spongepowered.configurate.ConfigurateException

class AddNbtTagAction(
    parentProcess: Process,
    actionNode: CommentedConfigurationNode
): Action(parentProcess, actionNode) {
    val tags = mutableSetOf<String>()
    init {
        try {
            if (actionNode.hasChild("tags")) {
                tags.addAll(actionNode.node("tags").getList(String::class.java, emptyList()))
            } else if (actionNode.hasChild("tag")) {
                tags.add(actionNode.node("tag").getString(""))
            } else {
                throw IllegalStateException("No valid NBT tag(s) were specified.")
            }
        } catch (ex: ConfigurateException) {
            throw IllegalStateException(
                "Parsing error - likely caused by a user " +
                        "syntax error.", ex
            )
        } catch (ex: NullPointerException) {
            throw IllegalStateException(
                "Parsing error - likely caused by a user " +
                        "syntax error.", ex
            )
        }
    }

    override fun run(context: Context) {
        if (context.entity == null) {
            throw IllegalStateException("No Entity context available")
        }

        if (context.livingEntity != null) {
            val nbtProvider = getPrimaryNbtProvider()
                ?: throw java.lang.IllegalStateException(
                    "Can't run AddNbtTagAction: no NBT " +
                            "provider available."
                )
            for (tag: String? in tags) {
                val result: NbtModificationResult = nbtProvider.addNbtTag(context.livingEntity!!, tag)
                //TODO should probably make the NbtProvider accept a collection of tags instead of
                //     calling the method for each tag
                if (result.hasException) {
                    throw RuntimeException(result.exception)
                }
            }
        } else {
            throw java.lang.IllegalStateException("Context's entity is not a LivingEntity")
        }
    }
}