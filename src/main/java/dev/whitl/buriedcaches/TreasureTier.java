package dev.whitl.buriedcaches;

import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.NamespacedKey;
import org.bukkit.loot.LootTable;

public record TreasureTier(
        String id,
        String displayName,
        NamedTextColor color,
        int weight,
        NamespacedKey lootTableKey,
        LootTable lootTable,
        CacheCelebration celebration,
        boolean broadcast
) {
}
