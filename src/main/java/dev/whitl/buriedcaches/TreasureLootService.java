package dev.whitl.buriedcaches;

import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.loot.LootContext;
import org.bukkit.loot.LootTable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

public final class TreasureLootService {

    private final BuriedCachesPlugin plugin;
    private WeightedSelector<TreasureTier> selector;
    private List<TreasureTier> tiers = List.of();

    public TreasureLootService(BuriedCachesPlugin plugin) {
        this.plugin = plugin;
    }

    public List<TreasureTier> tiers() {
        return tiers;
    }

    public void reload() {
        ConfigurationSection tiers = plugin.getConfig().getConfigurationSection("tiers");
        List<WeightedEntry<TreasureTier>> entries = new ArrayList<>();
        if (tiers != null) {
            for (String id : tiers.getKeys(false)) {
                ConfigurationSection section = tiers.getConfigurationSection(id);
                if (section == null || !section.getBoolean("enabled", true)) {
                    continue;
                }
                int weight = section.getInt("weight", 0);
                String rawKey = section.getString("loot-table", "");
                NamespacedKey key = NamespacedKey.fromString(rawKey);
                LootTable table = key == null ? null : Bukkit.getLootTable(key);
                if (weight <= 0 || table == null) {
                    plugin.getLogger().warning("Skipping treasure tier '" + id
                            + "': weight must be positive and loot-table must exist (" + rawKey + ").");
                    continue;
                }
                NamedTextColor color = parseColor(section.getString("color", "GOLD"));
                String displayName = section.getString("display-name", title(id));
                CacheCelebration celebration =
                        CacheCelebration.fromConfig(section.getString("celebration", "STANDARD"));
                boolean broadcast = section.getBoolean("broadcast", false);
                entries.add(new WeightedEntry<>(
                        new TreasureTier(id, displayName, color, weight, key, table,
                                celebration, broadcast), weight));
            }
        }
        if (entries.isEmpty()) {
            throw new IllegalStateException("BuriedCaches has no valid enabled loot tiers.");
        }
        selector = new WeightedSelector<>(entries);
        this.tiers = entries.stream().map(WeightedEntry::value).toList();
    }

    public GeneratedTreasure generate(Player player, Location location) {
        TreasureTier tier = selector.select(ThreadLocalRandom.current());
        LootContext context = new LootContext.Builder(location)
                .lootedEntity(player)
                .build();
        List<ItemStack> items = new ArrayList<>(
                tier.lootTable().populateLoot(ThreadLocalRandom.current(), context));
        if (items.isEmpty()) {
            Material fallback = Material.matchMaterial(
                    plugin.getConfig().getString("loot.empty-fallback-material", "GOLD_INGOT"));
            if (fallback == null || !fallback.isItem()) {
                fallback = Material.GOLD_INGOT;
            }
            int minimum = Math.max(1, plugin.getConfig().getInt("loot.empty-fallback-minimum", 1));
            int maximum = Math.max(minimum,
                    plugin.getConfig().getInt("loot.empty-fallback-maximum", 3));
            items.add(new ItemStack(fallback,
                    ThreadLocalRandom.current().nextInt(minimum, maximum + 1)));
        }
        return new GeneratedTreasure(tier, items);
    }

    private NamedTextColor parseColor(String raw) {
        NamedTextColor color = NamedTextColor.NAMES.value(raw.toLowerCase(Locale.ROOT));
        return color == null ? NamedTextColor.GOLD : color;
    }

    private String title(String id) {
        if (id.isBlank()) {
            return "Treasure";
        }
        return Character.toUpperCase(id.charAt(0)) + id.substring(1).toLowerCase(Locale.ROOT);
    }
}
