package io.github.arcaneplugins.levelledmobs.bukkit.util

import io.github.arcaneplugins.levelledmobs.bukkit.util.NmsDefinitions as def

object ComponentUtils {
    fun appendComponents(
        component: Any,
        appendingComponent: Any?
    ){
        if (appendingComponent == null) return

        try {
            def.method_ComponentAppend!!.invoke(component, appendingComponent)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    fun getEmptyComponent(): Any{
        return getTextComponent(null)!!
    }

    fun getTextComponent(
        text: String?
    ): Any?{
        return try {
            if (text == null)
                // #empty()
                def.method_EmptyComponent!!.invoke(null)
            else
                // #nullToEmpty(text)
                def.method_TextComponent!!.invoke(null, text)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getTranslatableComponent(
        key: String,
        args: Array<Any>? = null
    ): Any?{
        return try {
            def.method_TranslatableWithArgs!!.invoke(null, key, args)
        } catch (ex: java.lang.Exception) {
            ex.printStackTrace()
            null
        }
    }
}