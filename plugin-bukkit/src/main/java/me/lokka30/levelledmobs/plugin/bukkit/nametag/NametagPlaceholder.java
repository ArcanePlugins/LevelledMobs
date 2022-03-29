package me.lokka30.levelledmobs.plugin.bukkit.nametag;

import me.lokka30.levelledmobs.plugin.bukkit.LevelledMobs;
import me.lokka30.levelledmobs.plugin.bukkit.level.LevelledMob;
import org.jetbrains.annotations.NotNull;

/*
TODO Javadocs
 */
public enum NametagPlaceholder {

    ENTITY_NAME("entity-name"),
    ENTITY_TYPE("entity-type");

    private final String id;

    NametagPlaceholder(final String id) {
        this.id = id;
    }

    public String getId() {
        return "%" + id + "%";
    }

    public String getValue(final @NotNull LevelledMob levelledMob) {
        switch (this) {
            case ENTITY_NAME:
                return LevelledMobs.getInstance()
                    .translationHandler
                    .getTranslatedEntityName(levelledMob.livingEntity.getType());
            case ENTITY_TYPE:
                return levelledMob.livingEntity.getType().toString();
            default:
                throw new IllegalStateException();
        }
    }
}
