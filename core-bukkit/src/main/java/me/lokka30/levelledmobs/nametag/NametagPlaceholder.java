package me.lokka30.levelledmobs.nametag;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.level.LevelledMob;
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

    public String getId() { return "%" + id + "%"; }

    public String getValue(final @NotNull LevelledMobs main, final @NotNull LevelledMob levelledMob) {
        switch(this) {
            case ENTITY_NAME:
                return main.getTranslationHandler().getTranslatedEntityName(levelledMob.getLivingEntity().getType());
            case ENTITY_TYPE:
                return levelledMob.getLivingEntity().getType().toString();
            default:
                throw new IllegalStateException();
        }
    }
}
