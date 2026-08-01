package dev.whitl.buriedcaches;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

public final class BuriedCachesPlugin extends JavaPlugin {

    private final Set<Material> eligibleMaterials = EnumSet.noneOf(Material.class);
    private final Set<World.Environment> allowedEnvironments = EnumSet.noneOf(World.Environment.class);
    private TreasureLootService lootService;
    private TreasureTriggerPolicy triggerPolicy;
    private int maximumY;

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

        getLogger().info("BuriedCaches enabled with " + eligibleMaterials.size()
                + " eligible block types and placed-block anti-farming.");
    }

    public void reloadAll() {
        reloadConfig();
        loadEligibleMaterials();
        loadAllowedEnvironments();
        maximumY = getConfig().getInt("trigger.maximum-y", 64);
        int minimum = Math.max(1, getConfig().getInt("trigger.minimum-blocks", 64));
        int pity = Math.max(minimum, getConfig().getInt("trigger.pity-after-blocks", 900));
        long cooldown = Math.max(0L, getConfig().getLong("trigger.cooldown-seconds", 600L)) * 1_000L;
        double chance = Math.max(0.0, Math.min(1.0,
                getConfig().getDouble("trigger.chance-per-block", 0.0015)));
        triggerPolicy = new TreasureTriggerPolicy(minimum, pity, cooldown, chance);
        if (lootService != null) {
            lootService.reload();
        }
    }

    public boolean isEligible(Material material) {
        return eligibleMaterials.contains(material);
    }

    public boolean isWorldAllowed(World world) {
        return allowedEnvironments.contains(world.getEnvironment())
                && !getConfig().getStringList("trigger.disabled-worlds").contains(world.getName());
    }

    public int maximumY() {
        return maximumY;
    }

    public TreasureTriggerPolicy triggerPolicy() {
        return triggerPolicy;
    }

    public TreasureLootService lootService() {
        return lootService;
    }

    private void loadEligibleMaterials() {
        eligibleMaterials.clear();
        for (String raw : getConfig().getStringList("trigger.eligible-materials")) {
            Material material = Material.matchMaterial(raw);
            if (material == null || !material.isBlock()) {
                getLogger().warning("Ignoring unknown eligible material: " + raw);
            } else {
                eligibleMaterials.add(material);
            }
        }
        if (eligibleMaterials.isEmpty()) {
            throw new IllegalStateException("trigger.eligible-materials cannot be empty");
        }
    }

    private void loadAllowedEnvironments() {
        allowedEnvironments.clear();
        for (String raw : getConfig().getStringList("trigger.allowed-environments")) {
            try {
                allowedEnvironments.add(World.Environment.valueOf(raw.toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException error) {
                getLogger().warning("Ignoring unknown world environment: " + raw);
            }
        }
        if (allowedEnvironments.isEmpty()) {
            allowedEnvironments.add(World.Environment.NORMAL);
        }
    }
}
