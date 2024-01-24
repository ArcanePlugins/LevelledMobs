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
            def.method_ComponentAppend!!.invoke(component, appendingComponent)
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
            return if (text == null && ver.minecraftVersion >= 1.19) {
                // #empty()
                def.method_EmptyComponent!!.invoke(null)
            } else {
                // #nullToEmpty(text)
                def.method_TextComponent!!.invoke(null, text)
            }
        } catch (e: java.lang.Exception) {
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
                    return def.method_Translatable!!.invoke(null, key)
                }

                return def.method_TranslatableWithArgs!!.invoke(null, key, args)
            } else {
                return if (args.isNullOrEmpty()) {
                    def.clazz_TranslatableComponent!!
                        .getConstructor(String::class.java)
                        .newInstance(key)
                } else {
                    def.clazz_TranslatableComponent!!
                        .getConstructor(String::class.java, Array<Any>::class.java)
                        .newInstance(key, args)
                }
            }
        } catch (ex: java.lang.Exception) {
            ex.printStackTrace()
            return null
        }
    }
}