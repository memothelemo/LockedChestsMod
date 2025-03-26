package xyz.memothelemo.lockedchests.events;

public interface ModifableImmuneToExplosion {
    default boolean isImmuneToExplosion() {
        return false;
    }
}
