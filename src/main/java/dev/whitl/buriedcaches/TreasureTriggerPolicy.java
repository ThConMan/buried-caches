package dev.whitl.buriedcaches;

import java.util.function.DoubleSupplier;

public record TreasureTriggerPolicy(
        int minimumBlocks,
        int pityBlocks,
        long cooldownMillis,
        double chancePerBlock
) {

    public TreasureTriggerPolicy {
        if (minimumBlocks < 1) {
            throw new IllegalArgumentException("minimumBlocks must be positive");
        }
        if (pityBlocks < minimumBlocks) {
            throw new IllegalArgumentException("pityBlocks must be at least minimumBlocks");
        }
        if (cooldownMillis < 0) {
            throw new IllegalArgumentException("cooldownMillis cannot be negative");
        }
        if (chancePerBlock < 0 || chancePerBlock > 1) {
            throw new IllegalArgumentException("chancePerBlock must be from 0 to 1");
        }
    }

    public TriggerDecision evaluate(
            int blocksAfterBreak,
            long lastTreasureMillis,
            long nowMillis,
            DoubleSupplier random
    ) {
        if (blocksAfterBreak < minimumBlocks) {
            return TriggerDecision.WAITING;
        }
        if (lastTreasureMillis > 0 && nowMillis - lastTreasureMillis < cooldownMillis) {
            return TriggerDecision.COOLDOWN;
        }
        if (blocksAfterBreak >= pityBlocks) {
            return TriggerDecision.TRIGGERED;
        }
        return random.getAsDouble() < chancePerBlock
                ? TriggerDecision.TRIGGERED
                : TriggerDecision.ROLL_FAILED;
    }
}
