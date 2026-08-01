package dev.whitl.buriedcaches;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CacheExpiryPolicyTest {

    private static final long HOUR = 3_600_000L;

    @Test
    void zeroDisablesCleanupEntirely() {
        CacheExpiryPolicy policy = new CacheExpiryPolicy(0L);

        assertFalse(policy.enabled());
        assertFalse(policy.isExpired(0L, Long.MAX_VALUE));
    }

    @Test
    void aCacheExpiresOnceItsDueTimePasses() {
        CacheExpiryPolicy policy = new CacheExpiryPolicy(HOUR);
        long spawned = 1_000L;
        long due = policy.expiresAt(spawned);

        assertTrue(policy.enabled());
        assertEquals(spawned + HOUR, due);
        assertFalse(policy.isExpired(due, due - 1));
        assertTrue(policy.isExpired(due, due));
        assertTrue(policy.isExpired(due, due + 1));
    }

    @Test
    void anOpenedCacheHasNoStampAndNeverExpires() {
        CacheExpiryPolicy policy = new CacheExpiryPolicy(HOUR);

        // markOpened removes the key, so the barrel reports the 0 default.
        assertFalse(policy.isExpired(0L, Long.MAX_VALUE));
    }

    @Test
    void negativeExpiryIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new CacheExpiryPolicy(-1L));
    }
}
