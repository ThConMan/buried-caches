package dev.whitl.buriedcaches;

public record TreasureProgress(int blocksSinceTreasure, long lastTreasureMillis, boolean forced) {
}
