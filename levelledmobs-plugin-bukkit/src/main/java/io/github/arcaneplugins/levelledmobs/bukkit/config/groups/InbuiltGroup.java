package io.github.arcaneplugins.levelledmobs.bukkit.config.groups;

import io.github.arcaneplugins.levelledmobs.bukkit.logic.group.Group;
import java.util.List;

public class InbuiltGroup {

    public static final Group PASSIVE_MOBS = new Group(
        "passive-mobs",
        List.of(
            "ALLAY",
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
            "ZOMBIE_HORSE"
        )
    );

    public static final Group NEUTRAL_MOBS = new Group(
        "neutral-mobs",
        List.of(
            "BEE",
            "DOLPHIN",
            "GOAT",
            "IRON_GOLEM",
            "LLAMA",
            "PANDA",
            "POLAR_BEAR",
            "TRADER_LLAMA",
            "WOLF"
        )
    );

    public static final Group HOSTILE_MOBS = new Group(
        "hostile-mobs",
        List.of(
            "BLAZE",
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
            "ZOMBIFIED_PIGLIN"
        )
    );

    public static final Group BOSS_MOBS = new Group(
        "boss-mobs",
        List.of(
            "ENDER_DRAGON",
            "WITHER"
        )
    );

}
