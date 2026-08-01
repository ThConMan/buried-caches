package dev.whitl.buriedcaches;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WeightedSelectorTest {

    private final WeightedSelector<String> selector = new WeightedSelector<>(List.of(
            new WeightedEntry<>("common", 70),
            new WeightedEntry<>("rare", 25),
            new WeightedEntry<>("epic", 5)
    ));

    @Test
    void selectsTheExpectedEntryAtEveryBoundary() {
        assertEquals("common", selector.selectByRoll(0));
        assertEquals("common", selector.selectByRoll(69));
        assertEquals("rare", selector.selectByRoll(70));
        assertEquals("rare", selector.selectByRoll(94));
        assertEquals("epic", selector.selectByRoll(95));
        assertEquals("epic", selector.selectByRoll(99));
    }

    @Test
    void rejectsRollsOutsideTheWeightRange() {
        assertThrows(IllegalArgumentException.class, () -> selector.selectByRoll(-1));
        assertThrows(IllegalArgumentException.class, () -> selector.selectByRoll(100));
    }
}
