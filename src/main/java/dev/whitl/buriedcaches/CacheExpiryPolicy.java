package dev.whitl.buriedcaches;

/**
 * Decides when an unopened cache should be cleaned up.
 *
 * <p>A cache is only removed automatically if nobody ever opened it. Opening one
 * clears its expiry stamp, so looted-but-not-emptied barrels stay put and behave
 * like any other container.
 */
public record CacheExpiryPolicy(long expiryMillis) {

    public CacheExpiryPolicy {
        if (expiryMillis < 0) {
            throw new IllegalArgumentException("expiryMillis cannot be negative");
        }
    }

    /** Zero disables cleanup and keeps unopened caches in the world forever. */
    public boolean enabled() {
        return expiryMillis > 0;
    }

    public long expiresAt(long spawnedAtMillis) {
        return spawnedAtMillis + expiryMillis;
    }

    public boolean isExpired(long expiresAtMillis, long nowMillis) {
        return expiresAtMillis > 0 && nowMillis >= expiresAtMillis;
    }
}
