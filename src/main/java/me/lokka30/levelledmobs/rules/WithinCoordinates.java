package me.lokka30.levelledmobs.rules;

import me.lokka30.levelledmobs.util.Utils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class WithinCoordinates {
    public Integer startX;
    public Integer startY;
    public Integer startZ;
    public Integer endX;
    public Integer endY;
    public Integer endZ;
    public InfinityDirection infinityDirectionX = InfinityDirection.NONE;
    public InfinityDirection infinityDirectionY = InfinityDirection.NONE;
    public InfinityDirection infinityDirectionZ = InfinityDirection.NONE;

    public boolean parseAxis(final @Nullable String number, final @NotNull Axis axis, final boolean isStart){
        if (number == null) return true;

        InfinityDirection infinityDirection = InfinityDirection.NONE;
        if ("-".equals(number)){
            infinityDirection = InfinityDirection.DESCENDING;
        }
        else if ("+".equals(number)){
            infinityDirection = InfinityDirection.ASCENDING;
        }
        else if (Utils.isInteger(number)){
            final int num = Integer.parseInt(number);
            switch (axis){
                case X -> { if (isStart) startX = num; else endX = num; }
                case Y -> { if (isStart) startY = num; else endY = num; }
                case Z -> { if (isStart) startZ = num; else endZ = num; }
            }

            return true;
        }

        if (infinityDirection != InfinityDirection.NONE){
            switch (axis){
                case X -> infinityDirectionX = infinityDirection;
                case Y -> infinityDirectionY = infinityDirection;
                case Z -> infinityDirectionZ = infinityDirection;
            }

            return true;
        }

        return false;
    }

    public boolean isEmpty(){
        for (final Field f : this.getClass().getDeclaredFields()){
            if (!Modifier.isPublic(f.getModifiers())) continue;

            try {
                if (f.get(this) != null){
                    return false;
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        return true;
    }

    public boolean getHasX(){
        return (startX != null || endX != null);
    }

    public boolean getHasY(){
        return (startY != null || endY != null);
    }

    public boolean getHasZ(){
        return (startZ != null || endZ != null);
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean isLocationWithinRange(final int coord, final @NotNull Axis axis){
        Integer range1 = null;
        Integer range2 = null;
        InfinityDirection infinityDirection = InfinityDirection.NONE;
        switch (axis){
            case X -> { range1 = this.startX; range2 = this.endX; infinityDirection = this.infinityDirectionX; }
            case Y -> { range1 = this.startY; range2 = this.endY; infinityDirection = this.infinityDirectionY; }
            case Z -> { range1 = this.startZ; range2 = this.endZ; infinityDirection = this.infinityDirectionZ; }
        }

        if (range1 == null && range2 == null) return false;

        if (range1 != null && range2 != null) {
            if (range1 < range2) {
                return coord >= range1 && coord <= range2;
            } else {
                return coord >= range2 && coord <= range1;
            }
        }

        final int useRange = range1 != null ? range1 : range2;
        if (infinityDirection == InfinityDirection.NONE){
            return coord == useRange;
        }
        else if (infinityDirection == InfinityDirection.ASCENDING){
            return coord >= useRange;
        }
        else {
            return coord <= useRange;
        }
    }

    public enum Axis{
        X,
        Y,
        Z
    }

    private enum InfinityDirection{
        NONE,
        ASCENDING,
        DESCENDING
    }

    public String toString(){
        final StringBuilder sb = new StringBuilder();

        checkNumber(startX, "startX", sb);
        checkNumber(endX, "endX", sb);
        checkInfinityDirection(infinityDirectionX, "X", sb);
        checkNumber(startY, "startY", sb);
        checkNumber(endY, "endY", sb);
        checkInfinityDirection(infinityDirectionY, "Y", sb);
        checkNumber(startZ, "startZ", sb);
        checkNumber(endZ, "endZ", sb);
        checkInfinityDirection(infinityDirectionZ, "Z", sb);

        if (sb.isEmpty())
            return super.toString();
        else
            return sb.toString();
    }

    private void checkNumber(final Integer num, final String name, final StringBuilder sb){
        if (num == null) return;

        if (!sb.isEmpty()) sb.append(", ");
        sb.append(name);
        sb.append(": ");
        sb.append(num);
    }

    private void checkInfinityDirection(final InfinityDirection infinityDirection, final String name, final StringBuilder sb){
        if (infinityDirection == InfinityDirection.NONE) return;

        if (!sb.isEmpty()) sb.append(", ");
        sb.append(name);
        sb.append(": ");
        if (infinityDirection == InfinityDirection.ASCENDING)
            sb.append("+");
        else
            sb.append("-");
    }
}
