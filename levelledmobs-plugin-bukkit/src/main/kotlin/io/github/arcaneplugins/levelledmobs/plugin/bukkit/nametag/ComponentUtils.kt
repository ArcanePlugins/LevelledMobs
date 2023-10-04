package io.github.arcaneplugins.levelledmobs.plugin.bukkit.nametag

import io.github.arcaneplugins.levelledmobs.plugin.bukkit.LevelledMobs

class ComponentUtils {
    companion object{
        fun appendComponents(component: Any, appendingComponent: Any?){
            if (appendingComponent == null) return

            LevelledMobs.lmInstance.definitions.method_ComponentAppend!!
                .invoke(component, appendingComponent)
        }

        fun getEmptyComponent() : Any{
            return getTextComponent(null)!!
        }

        fun getTextComponent(text: String?) : Any?{
            val def = LevelledMobs.lmInstance.definitions

            return if (text == null)
                def.method_EmptyComponent!!.invoke(null)
            else
                def.method_TextComponent!!.invoke(null, text);
        }

        fun getTranslatableComponent(key: String, args: Array<Any>? = null) : Any?{
            val def = LevelledMobs.lmInstance.definitions

            return if (args.isNullOrEmpty()) {
                def.method_Translatable!!.invoke(null, key)
            } else {
                def.method_TranslatableWithArgs!!.invoke(null, key, args)
            }
        }
    }
}