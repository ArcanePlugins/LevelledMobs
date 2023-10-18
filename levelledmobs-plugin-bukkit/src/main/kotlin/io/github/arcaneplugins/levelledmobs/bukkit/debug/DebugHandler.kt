package io.github.arcaneplugins.levelledmobs.bukkit.debug

import io.github.arcaneplugins.levelledmobs.bukkit.LevelledMobs
import io.github.arcaneplugins.levelledmobs.bukkit.util.Log.war
import org.spongepowered.configurate.CommentedConfigurationNode
import org.spongepowered.configurate.ConfigurateException
import java.util.*

object DebugHandler {
    val enabledCategories: EnumSet<DebugCategory> = EnumSet.noneOf(DebugCategory::class.java)

    fun isCategoryEnabled(debugCategory: DebugCategory): Boolean{
        return enabledCategories.contains(debugCategory)
    }

    fun load(){
        val parentNode: CommentedConfigurationNode? = LevelledMobs.lmInstance
            .configHandler.settingsCfg.root!!.node(
                "advanced", "debug", "enabled-categories")

        if (parentNode!!.virtual()) return

        try {
            if (parentNode.hasChild("in-list")) {
                val enabledCategoriesStr = parentNode.node("in-list"
                    )!!.getList(String::class.java, LinkedList())
                for (enabledCategoryStr: String in enabledCategoriesStr) {
                    try {
                        val debugCategory = DebugCategory.valueOf(
                            enabledCategoryStr.uppercase()
                        )
                        getEnabledCategories().add(debugCategory)
                    } catch (ignored: IllegalArgumentException) {
                        war(
                            "Unknown debug category '" +
                                    enabledCategoryStr + "'.", true
                        )
                    }
                }
            } else if (parentNode.hasChild("not-in-list")) {
                val disabledCategoriesStr = parentNode.node("in-list")
                    .getList(String::class.java, LinkedList())

                debugCategoryIter@ for (debugCategory: DebugCategory in DebugCategory.entries) {
                    for (disabledCategoryStr: String? in disabledCategoriesStr) {
                        if (debugCategory.name.equals(
                                disabledCategoryStr,
                                ignoreCase = true
                            )
                        ) continue@debugCategoryIter
                    }
                    getEnabledCategories().add(debugCategory)
                }
            }
        } catch (ex: ConfigurateException) {
            throw RuntimeException(ex)
        }
    }

    fun getEnabledCategories(): MutableCollection<DebugCategory>{
        return enabledCategories
    }
}