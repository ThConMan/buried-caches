package dev.whitl.buriedcaches;

public record WeightedEntry<T>(T value, int weight) {

    public WeightedEntry {
        if (value == null) {
            throw new IllegalArgumentException("value cannot be null");
        }
        if (weight <= 0) {
            throw new IllegalArgumentException("weight must be positive");
        }
    }
}
