package io.github.arcaneplugins.levelledmobs.result

import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier

data class AttributePreMod(
    val attributeModifier: AttributeModifier,
    val multiplierResult: MultiplierResult,
    val attribute: Attribute
)