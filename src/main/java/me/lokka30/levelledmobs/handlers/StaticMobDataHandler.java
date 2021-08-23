/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.handlers;

import me.lokka30.levelledmobs.LevelledMobs;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.HashSet;

/**
 * @author lokka30
 * @since v4.0.0
 * This class handles how static mob data values
 * (from the internal file `static-mob-data.json`)
 * are accessed.
 */
public class StaticMobDataHandler {

    private final LevelledMobs main;

    public StaticMobDataHandler(final LevelledMobs main) {
        this.main = main;
    }

    /*
    TODO
        lokka30: Example layout of the file (in YAML so it's easier to understand)
                 entities:
                    ARMOR_STAND:
                        unlevellable: true # Tells LM not to level this mob regardless.
                    PLAYER:
                        unlevellable: true
                    CREEPER:
                        unlevellable: false
                        default-attributes:
                            GENERIC_MOVEMENT_SPEED:
                                min: 1.3 # See comment below
                                max: 1.3 # in 'ZOMBIE' section
                        default-drops:
                            - GUNPOWDER
                    ZOMBIE:
                        unlevellable: false
                        default-attributes:
                            GENERIC_MOVEMENT_SPEED:
                                min: 1.3 # Uses min and max since some mobs
                                max: 1.3 # (e.g. donkeys) have dynamic values
                        default-drops:
                            - ROTTEN_FLESH
     */

    public HashMap<EntityType, MobData> mobDataMap = new HashMap<>();

    public void init() {
        loadMobData();
        parseMobData();
    }

    private void loadMobData() {
        /*
        TODO
            lokka30: Load the JSON file (embedded resource)
         */
    }

    private void parseMobData() {
        /*
        TODO
            lokka30: For each entity type existing, check if the mob data file
                     has it in the 'mobs' section. If it does, then create a
                     new MobData object of it and store it in the mobDataMap.
         */

        for (EntityType entityType : EntityType.values()) {
            //TODO
            boolean isLevellable = true;
            final HashSet<Material> defaultMobDrops = new HashSet<>();
            final HashSet<DefaultAttributeValue> defaultMobAttributes = new HashSet<>();

            mobDataMap.put(entityType, new MobData(isLevellable, defaultMobDrops, defaultMobAttributes));
        }
    }



    /* Objects */

    public static class MobData {
        private final boolean isLevellable;
        @NotNull
        private final HashSet<Material> defaultMobDrops;
        @NotNull
        private final HashSet<DefaultAttributeValue> defaultMobAttributes;

        public MobData(final boolean isLevellable, @NotNull final HashSet<Material> defaultMobDrops, @NotNull final HashSet<DefaultAttributeValue> defaultMobAttributes) {
            this.isLevellable = isLevellable;
            this.defaultMobDrops = defaultMobDrops;
            this.defaultMobAttributes = defaultMobAttributes;
        }

        public boolean isLevellable() {
            return isLevellable;
        }

        @NotNull
        public HashSet<Material> getDefaultMobDrops() {
            return defaultMobDrops;
        }

        @NotNull
        public HashSet<DefaultAttributeValue> getDefaultMobAttributes() {
            return defaultMobAttributes;
        }
    }

    public static class DefaultAttributeValue {
        @NotNull
        private final Attribute attribute;
        @NotNull
        private final Object value;

        public DefaultAttributeValue(@NotNull final Attribute attribute, @NotNull final Object value) {
            this.attribute = attribute;
            this.value = value;
        }

        @NotNull
        public Attribute getAttribute() {
            return attribute;
        }

        @NotNull
        public Object getValue() {
            return value;
        }
    }

}
