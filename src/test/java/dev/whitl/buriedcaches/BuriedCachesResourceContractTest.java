package dev.whitl.buriedcaches;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuriedCachesResourceContractTest {

    @Test
    void descriptorTargetsTheExpectedPaperApiAndDeclaresTheAdminCommand() throws IOException {
        String descriptor = resource("plugin.yml");

        assertTrue(descriptor.contains("name: BuriedCaches"));
        assertTrue(descriptor.contains("api-version: '26.1'"));
        assertTrue(descriptor.contains("main: dev.whitl.buriedcaches.BuriedCachesPlugin"));
        assertTrue(descriptor.contains("buriedcaches.admin:"));
        assertTrue(descriptor.contains("buriedcaches.stats:"));
        assertTrue(descriptor.contains("  buriedcaches:"));
        assertFalse(descriptor.toLowerCase(Locale.ROOT).contains("minetreasur"));
    }

    @Test
    void defaultConfigHasCooldownPityAntiFarmAndValidLootTables() throws IOException {
        String config = resource("config.yml");

        assertTrue(config.contains("cooldown-seconds: 240"));
        assertTrue(config.contains("pity-after-blocks: 450"));
        assertTrue(config.contains("placed block never counts"));
        assertTrue(config.contains("minecraft:chests/abandoned_mineshaft"));
        assertTrue(config.contains("empty-fallback-material: GOLD_INGOT"));
        assertTrue(config.contains("celebration: MYTHIC"));
        assertTrue(config.contains("broadcast: true"));
    }

    private String resource(String name) throws IOException {
        try (var stream = getClass().getClassLoader().getResourceAsStream(name)) {
            if (stream == null) {
                throw new IOException("missing test resource " + name);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
