package dev.whitl.buriedcaches;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TreasureTriggerPolicyTest {

    private final TreasureTriggerPolicy policy = new TreasureTriggerPolicy(10, 100, 60_000L, 0.10);

    @Test
    void waitsUntilTheMinimumBreakCount() {
        assertEquals(TriggerDecision.WAITING,
                policy.evaluate(9, 0L, 100_000L, () -> 0.0));
    }

    @Test
    void cooldownBlocksAWinningRoll() {
        assertEquals(TriggerDecision.COOLDOWN,
                policy.evaluate(50, 90_000L, 100_000L, () -> 0.0));
    }

    @Test
    void chanceBoundaryUsesStrictlyLessThanTheConfiguredChance() {
        assertEquals(TriggerDecision.TRIGGERED,
                policy.evaluate(10, 0L, 100_000L, () -> 0.0999));
        assertEquals(TriggerDecision.ROLL_FAILED,
                policy.evaluate(10, 0L, 100_000L, () -> 0.10));
    }

    @Test
    void pityGuaranteesATreasureAfterCooldown() {
        assertEquals(TriggerDecision.TRIGGERED,
                policy.evaluate(100, 1L, 100_000L, () -> 0.9999));
    }
}
