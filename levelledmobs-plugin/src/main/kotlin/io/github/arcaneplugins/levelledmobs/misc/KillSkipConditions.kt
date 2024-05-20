package io.github.arcaneplugins.levelledmobs.misc

import io.github.arcaneplugins.levelledmobs.rules.RulesParser

class KillSkipConditions {
    var isNametagged = false
    var isTamed = false
    var isLeashed = false
    var isTransforming = false
    var isVillager = false
    var entityTypes: CachedModalList<String>? = null

    companion object{
        fun parseConditions(
            helper: YmlParsingHelper
        ): KillSkipConditions{
            val result = KillSkipConditions()
            val cs = YmlParsingHelper.objToCS(helper.cs, "kill-skip-conditions") ?: return result

            for (key in cs.getKeys(false)){
                when (key.lowercase()){
                    "is-nametagged" -> result.isNametagged = cs.getBoolean(key)
                    "is-tamed" -> result.isTamed = cs.getBoolean(key)
                    "is-leashed" -> result.isLeashed = cs.getBoolean(key)
                    "is-transforming" -> result.isTransforming = cs.getBoolean(key)
                    "is-villager" -> result.isVillager = cs.getBoolean(key)
                    "entitytype" -> {
                        result.entityTypes = RulesParser.buildCachedModalListOfString(
                            cs, key, null)
                    }
                }
            }

            return result
        }
    }

    override fun toString(): String {
        val sb = StringBuilder()
        if (isNametagged) appendValue(sb, "nametagged")
        if (isTamed) appendValue(sb, "tamed")
        if (isLeashed) appendValue(sb, "leashed")
        if (isTransforming) appendValue(sb, "transforming")
        if (isVillager) appendValue(sb, "villager")
        if (entityTypes != null) appendValue(sb, entityTypes.toString())

        return sb.toString()
    }

    private fun appendValue(sb: StringBuilder, value: String){
        if (sb.isNotEmpty())
            sb.append(", ")

        sb.append(value)
    }
}