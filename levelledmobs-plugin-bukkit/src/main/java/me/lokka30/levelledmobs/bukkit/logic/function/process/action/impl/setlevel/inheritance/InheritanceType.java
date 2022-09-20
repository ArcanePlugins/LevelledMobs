package me.lokka30.levelledmobs.bukkit.logic.function.process.action.impl.setlevel.inheritance;

import org.jetbrains.annotations.Nullable;

enum InheritanceType {

    TRANSFORMATION(1),

    BREEDING(2);

    private final int parentsSize;

    InheritanceType(
        final int parentsSize
    ) {
        this.parentsSize = parentsSize;
    }

    public int getParentsSize() {
        return parentsSize;
    }

    @Nullable
    public static InheritanceType fromParentsSize(final int parentsSize) {
        for(final InheritanceType it : values()) {
            if(it.getParentsSize() == parentsSize) {
                return it;
            }
        }
        return null;
    }
}
