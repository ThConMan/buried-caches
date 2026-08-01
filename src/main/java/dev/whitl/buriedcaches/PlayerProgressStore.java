package dev.whitl.buriedcaches;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Locale;

public final class PlayerProgressStore {

    private final BuriedCachesPlugin plugin;
    private final NamespacedKey blocksKey;
    private final NamespacedKey lastTreasureKey;
    private final NamespacedKey forceNextKey;
    private final NamespacedKey totalFindsKey;

    public PlayerProgressStore(BuriedCachesPlugin plugin) {
        this.plugin = plugin;
        this.blocksKey = new NamespacedKey(plugin, "blocks_since_treasure");
        this.lastTreasureKey = new NamespacedKey(plugin, "last_treasure_millis");
        this.forceNextKey = new NamespacedKey(plugin, "force_next");
        this.totalFindsKey = new NamespacedKey(plugin, "total_finds");
    }

    public TreasureProgress increment(Player player, int cap) {
        PersistentDataContainer data = player.getPersistentDataContainer();
        int current = data.getOrDefault(blocksKey, PersistentDataType.INTEGER, 0);
        int next = Math.min(cap, current + 1);
        data.set(blocksKey, PersistentDataType.INTEGER, next);
        return new TreasureProgress(
                next,
                data.getOrDefault(lastTreasureKey, PersistentDataType.LONG, 0L),
                data.has(forceNextKey, PersistentDataType.BYTE)
        );
    }

    public TreasureProgress get(Player player) {
        PersistentDataContainer data = player.getPersistentDataContainer();
        return new TreasureProgress(
                data.getOrDefault(blocksKey, PersistentDataType.INTEGER, 0),
                data.getOrDefault(lastTreasureKey, PersistentDataType.LONG, 0L),
                data.has(forceNextKey, PersistentDataType.BYTE)
        );
    }

    public void recordTreasure(Player player, long nowMillis) {
        PersistentDataContainer data = player.getPersistentDataContainer();
        data.set(blocksKey, PersistentDataType.INTEGER, 0);
        data.set(lastTreasureKey, PersistentDataType.LONG, nowMillis);
        data.remove(forceNextKey);
    }

    public void forceNext(Player player) {
        player.getPersistentDataContainer().set(forceNextKey, PersistentDataType.BYTE, (byte) 1);
    }

    public void recordFind(Player player, TreasureTier tier) {
        PersistentDataContainer data = player.getPersistentDataContainer();
        data.set(totalFindsKey, PersistentDataType.INTEGER,
                data.getOrDefault(totalFindsKey, PersistentDataType.INTEGER, 0) + 1);
        NamespacedKey key = tierFindsKey(tier.id());
        data.set(key, PersistentDataType.INTEGER,
                data.getOrDefault(key, PersistentDataType.INTEGER, 0) + 1);
    }

    public int totalFinds(Player player) {
        return player.getPersistentDataContainer()
                .getOrDefault(totalFindsKey, PersistentDataType.INTEGER, 0);
    }

    public int tierFinds(Player player, String tierId) {
        return player.getPersistentDataContainer()
                .getOrDefault(tierFindsKey(tierId), PersistentDataType.INTEGER, 0);
    }

    // Tier ids come from config keys, so they are sanitized to the character
    // set NamespacedKey accepts before becoming part of a key.
    private NamespacedKey tierFindsKey(String tierId) {
        String safe = tierId.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]", "_");
        return new NamespacedKey(plugin, "finds_" + safe);
    }

    public void reset(Player player) {
        PersistentDataContainer data = player.getPersistentDataContainer();
        data.remove(blocksKey);
        data.remove(lastTreasureKey);
        data.remove(forceNextKey);
    }
}
