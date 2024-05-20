package io.github.arcaneplugins.levelledmobs.nametag

import io.github.arcaneplugins.levelledmobs.LevelledMobs

/**
 * Provides methods for manipulating internal text
 * components
 *
 * @author stumper66
 * @since 3.9.3
 */
object ComponentUtils {
    private val def = LevelledMobs.instance.definitions
    private val ver = LevelledMobs.instance.ver

    fun appendComponents(
        component: Any,
        appendingComponent: Any?
    ) {
        if (appendingComponent == null) {
            return
        }

        try {
            def.methodComponentAppend!!.invoke(component, appendingComponent)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    fun getEmptyComponent(): Any {
        return getTextComponent(null)!!
    }

    fun getTextComponent(
        text: String?
    ): Any? {
        try {
            return def.methodTextComponent!!.invoke(null, text)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun getTranslatableComponent(
        key: String
    ): Any? {
        return getTranslatableComponent(key, null)
    }

    fun getTranslatableComponent(
        key: String,
        args: Array<Any?>?
    ): Any? {
        try {
            if (ver.minecraftVersion >= 1.19) {
                if (args.isNullOrEmpty() || args[0] == null) {
                    return def.methodTranslatable!!.invoke(null, key)
                }

                return def.methodTranslatableWithArgs!!.invoke(null, key, args)
            } else {
                return if (args.isNullOrEmpty()) {
                    def.clazzTranslatableComponent!!
                        .getConstructor(String::class.java)
                        .newInstance(key)
                } else {
                    def.clazzTranslatableComponent!!
                        .getConstructor(String::class.java, Array<Any>::class.java)
                        .newInstance(key, args)
                }
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
            return null
        }
    }
}