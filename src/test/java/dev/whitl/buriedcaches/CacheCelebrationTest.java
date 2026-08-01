package dev.whitl.buriedcaches;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CacheCelebrationTest {

    @Test
    void parsesConfiguredValuesCaseInsensitivelyAndTrimmed() {
        assertEquals(CacheCelebration.STANDARD, CacheCelebration.fromConfig("STANDARD"));
        assertEquals(CacheCelebration.GRAND, CacheCelebration.fromConfig("grand"));
        assertEquals(CacheCelebration.MYTHIC, CacheCelebration.fromConfig(" Mythic "));
    }

    @Test
    void fallsBackToStandardForMissingOrUnknownValues() {
        assertEquals(CacheCelebration.STANDARD, CacheCelebration.fromConfig(null));
        assertEquals(CacheCelebration.STANDARD, CacheCelebration.fromConfig(""));
        assertEquals(CacheCelebration.STANDARD, CacheCelebration.fromConfig("spectacular"));
    }
}
