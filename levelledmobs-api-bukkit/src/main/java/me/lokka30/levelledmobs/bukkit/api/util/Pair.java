package me.lokka30.levelledmobs.bukkit.api.util;

public class Pair<A, B> {

    private final A left;
    private final B right;

    public Pair(A left, B right) {
        this.left = left;
        this.right = right;
    }

    public A getLeft() { return left; }
    public B getRight() { return right; }

}
