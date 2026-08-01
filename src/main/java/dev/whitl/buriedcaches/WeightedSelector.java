package dev.whitl.buriedcaches;

import java.util.List;
import java.util.random.RandomGenerator;

public final class WeightedSelector<T> {

    private final List<WeightedEntry<T>> entries;
    private final int totalWeight;

    public WeightedSelector(List<WeightedEntry<T>> entries) {
        if (entries == null || entries.isEmpty()) {
            throw new IllegalArgumentException("at least one weighted entry is required");
        }
        this.entries = List.copyOf(entries);
        this.totalWeight = entries.stream().mapToInt(WeightedEntry::weight).sum();
    }

    public T select(RandomGenerator random) {
        return selectByRoll(random.nextInt(totalWeight));
    }

    public T selectByRoll(int roll) {
        if (roll < 0 || roll >= totalWeight) {
            throw new IllegalArgumentException("roll must be from 0 to " + (totalWeight - 1));
        }
        int cursor = 0;
        for (WeightedEntry<T> entry : entries) {
            cursor += entry.weight();
            if (roll < cursor) {
                return entry.value();
            }
        }
        throw new IllegalStateException("weighted selection fell through");
    }

    public int totalWeight() {
        return totalWeight;
    }
}
