package dev.whitl.buriedcaches;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class BuriedCachesPlugin extends JavaPlugin {

    private volatile Settings settings = Settings.empty();
    private TreasureLootService lootService;

    /**
     * Everything {@link #reloadAll()} derives from the config, swapped in as one
     * object. Values read on the block-break hot path live here rather than being
     * looked up per event, and a rejected reload leaves the previous instance in
     * place instead of half-applying.
     */
    private record Settings(
            Set<Material> eligibleMaterials,
            Set<World.Environment> allowedEnvironments,
            Set<String> disabledWorlds,
            int maximumY,
            boolean requireSurvival,
            boolean removeEmptyBarrels,
            long lockMillis,
            TreasureTriggerPolicy triggerPolicy,
            CacheExpiryPolicy expiryPolicy
    ) {
        static Settings empty() {
            return new Settings(Set.of(), Set.of(), Set.of(), 64, true, true, 30_000L,
                    new TreasureTriggerPolicy(1, 1, 0L, 0.0), new CacheExpiryPolicy(0L));
        }
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();

        lootService = new TreasureLootService(this);
        reloadAll();

        PlayerProgressStore progress = new PlayerProgressStore(this);
        PlacedBlockTracker placedBlocks = new PlacedBlockTracker(this);
        TreasureSpawner spawner = new TreasureSpawner(this, lootService, progress);

        getServer().getPluginManager().registerEvents(
                new TreasureListener(this, progress, placedBlocks, spawner), this);
        getServer().getPluginManager().registerEvents(
                new TreasureProtectionListener(this, spawner), this);

        PluginCommand command = getCommand("buriedcaches");
        if (command == null) {
            throw new IllegalStateException("buriedcaches command is missing from plugin.yml");
        }
        BuriedCachesCommand handler = new BuriedCachesCommand(this, progress);
        command.setExecutor(handler);
        command.setTabCompleter(handler);

        getLogger().info("BuriedCaches enabled with " + settings.eligibleMaterials().size()
                + " eligible block types and placed-block anti-farming.");
    }

    /**
     * Reparses the config. Every value is validated into a new {@link Settings}
     * before any of it is applied, so a broken config leaves the running settings
     * untouched instead of clearing them and leaving the plugin silently inert.
     *
     * @throws IllegalStateException if the config cannot produce a usable setup
     */
    public void reloadAll() {
        reloadConfig();

        Set<Material> materials = parseEligibleMaterials();
        Set<World.Environment> environments = parseAllowedEnvironments();
        Set<String> disabled = new HashSet<>(getConfig().getStringList("trigger.disabled-worlds"));

        int minimum = Math.max(1, getConfig().getInt("trigger.minimum-blocks", 40));
        int pity = Math.max(minimum, getConfig().getInt("trigger.pity-after-blocks", 450));
        long cooldown = Math.max(0L, getConfig().getLong("trigger.cooldown-seconds", 240L)) * 1_000L;
        double chance = Math.max(0.0, Math.min(1.0,
                getConfig().getDouble("trigger.chance-per-block", 0.004)));
        TreasureTriggerPolicy policy = new TreasureTriggerPolicy(minimum, pity, cooldown, chance);

        long expiryMinutes = Math.max(0L, getConfig().getLong("ownership.expire-unopened-minutes", 60L));
        CacheExpiryPolicy expiry = new CacheExpiryPolicy(expiryMinutes * 60_000L);

        Settings parsed = new Settings(
                materials,
                environments,
                disabled,
                getConfig().getInt("trigger.maximum-y", 64),
                getConfig().getBoolean("trigger.require-survival", true),
                getConfig().getBoolean("ownership.remove-empty-barrels", true),
                Math.max(0L, getConfig().getLong("ownership.lock-seconds", 30L)) * 1_000L,
                policy,
                expiry);

        // Tiers are validated last because reload() throws on an unusable tier set;
        // it keeps its own previous selector, so nothing is applied on failure.
        if (lootService != null) {
            lootService.reload();
        }
        settings = parsed;
    }

    public boolean isEligible(Material material) {
        return settings.eligibleMaterials().contains(material);
    }

    public boolean isWorldAllowed(World world) {
        Settings current = settings;
        return current.allowedEnvironments().contains(world.getEnvironment())
                && !current.disabledWorlds().contains(world.getName());
    }

    public int maximumY() {
        return settings.maximumY();
    }

    public boolean requireSurvival() {
        return settings.requireSurvival();
    }

    public boolean removeEmptyBarrels() {
        return settings.removeEmptyBarrels();
    }

    public long lockMillis() {
        return settings.lockMillis();
    }

    public TreasureTriggerPolicy triggerPolicy() {
        return settings.triggerPolicy();
    }

    public CacheExpiryPolicy expiryPolicy() {
        return settings.expiryPolicy();
    }

    public TreasureLootService lootService() {
        return lootService;
    }

    private Set<Material> parseEligibleMaterials() {
        Set<Material> materials = EnumSet.noneOf(Material.class);
        for (String raw : getConfig().getStringList("trigger.eligible-materials")) {
            Material material = Material.matchMaterial(raw);
            if (material == null || !material.isBlock()) {
                getLogger().warning("Ignoring unknown eligible material: " + raw);
            } else {
                materials.add(material);
            }
        }
        if (materials.isEmpty()) {
            throw new IllegalStateException("trigger.eligible-materials cannot be empty");
        }
        return materials;
    }

    private Set<World.Environment> parseAllowedEnvironments() {
        Set<World.Environment> environments = EnumSet.noneOf(World.Environment.class);
        List<String> raw = getConfig().getStringList("trigger.allowed-environments");
        for (String name : raw) {
            try {
                environments.add(World.Environment.valueOf(name.toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException error) {
                getLogger().warning("Ignoring unknown world environment: " + name);
            }
        }
        if (environments.isEmpty()) {
            environments.add(World.Environment.NORMAL);
        }
        return environments;
    }
}
