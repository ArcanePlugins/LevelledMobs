package io.github.arcaneplugins.levelledmobs.bukkit.config.groups

import io.github.arcaneplugins.levelledmobs.bukkit.logic.group.Group

object InbuiltGroup {
    val PASSIVE_MOBS = Group(
        "passive-mobs",
        mutableListOf("ALLAY",
            "AXOLOTL",
            "BAT",
            "CAT",
            "CHICKEN",
            "COD",
            "COW",
            "DONKEY",
            "FOX",
            "FROG",
            "GLOW_SQUID",
            "HORSE",
            "MUSHROOM_COW",
            "MULE",
            "OCELOT",
            "PARROT",
            "PIG",
            "PUFFERFISH",
            "RABBIT",
            "SALMON",
            "SHEEP",
            "SKELETON_HORSE",
            "SNOWMAN",
            "SQUID",
            "STRIDER",
            "TADPOLE",
            "TROPICAL_FISH",
            "TURTLE",
            "VILLAGER",
            "WANDERING_TRADER",
            "ZOMBIE_HORSE")
        )

    val NEUTRAL_MOBS = Group(
        "neutral-mobs",
        mutableListOf("BEE",
            "BEE",
            "DOLPHIN",
            "GOAT",
            "IRON_GOLEM",
            "LLAMA",
            "PANDA",
            "POLAR_BEAR",
            "TRADER_LLAMA",
            "WOLF")
        )

    val HOSTILE_MOBS = Group(
        "hostile-mobs",
        mutableListOf("BLAZE",
            "CAVE_SPIDER",
            "CREEPER",
            "DROWNED",
            "ELDER_GUARDIAN",
            "ENDERMAN",
            "ENDERMITE",
            "EVOKER",
            "GHAST",
            "GIANT",
            "GUARDIAN",
            "HOGLIN",
            "HUSK",
            "ILLUSIONER",
            "MAGMA_CUBE",
            "PHANTOM",
            "PIGLIN",
            "PIGLIN_BRUTE",
            "PILLAGER",
            "RAVAGER",
            "SHULKER",
            "SKELETON",
            "SLIME",
            "SPIDER",
            "STRAY",
            "VEX",
            "VINDICATOR",
            "WARDEN",
            "WITCH",
            "WITHER_SKELETON",
            "ZOGLIN",
            "ZOMBIE",
            "ZOMBIFIED_PIGLIN")
    )

    val BOSS_MOBS = Group(
        "boss-mobs",
        mutableListOf("ENDER_DRAGON",
            "WITHER")
    )


}