package dev.whitl.buriedcaches;

import org.bukkit.GameMode;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

import java.util.concurrent.ThreadLocalRandom;
import java.util.List;

public final class TreasureListener implements Listener {

    private final BuriedCachesPlugin plugin;
    private final PlayerProgressStore progress;
    private final PlacedBlockTracker placedBlocks;
    private final TreasureSpawner spawner;

    public TreasureListener(
            BuriedCachesPlugin plugin,
            PlayerProgressStore progress,
            PlacedBlockTracker placedBlocks,
            TreasureSpawner spawner
    ) {
        this.plugin = plugin;
        this.progress = progress;
        this.placedBlocks = placedBlocks;
        this.spawner = spawner;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void rememberPlacedEligibleBlocks(BlockPlaceEvent event) {
        if (plugin.isEligible(event.getBlockPlaced().getType())) {
            placedBlocks.mark(event.getBlockPlaced());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void considerTreasure(BlockBreakEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();
        if (!plugin.isEligible(block.getType())) {
            return;
        }
        if (placedBlocks.consume(block)) {
            return;
        }
        if (!plugin.isWorldAllowed(block.getWorld()) || block.getY() > plugin.maximumY()) {
            return;
        }
        if (plugin.requireSurvival() && player.getGameMode() != GameMode.SURVIVAL) {
            return;
        }

        TreasureTriggerPolicy policy = plugin.triggerPolicy();
        TreasureProgress state = progress.increment(player, policy.pityBlocks());
        long now = System.currentTimeMillis();
        TriggerDecision decision = state.forced()
                ? TriggerDecision.TRIGGERED
                : policy.evaluate(state.blocksSinceTreasure(), state.lastTreasureMillis(), now,
                        ThreadLocalRandom.current()::nextDouble);
        if (decision != TriggerDecision.TRIGGERED) {
            return;
        }

        // Record immediately so a hammer's remaining block events cannot queue
        // more barrels in the same tick.
        progress.recordTreasure(player, now);
        var location = block.getLocation();
        plugin.getServer().getScheduler().runTask(plugin, () -> spawner.spawn(player, location));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void cleanPlacedMarkers(BlockExplodeEvent event) {
        event.blockList().forEach(placedBlocks::remove);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void cleanPlacedMarkers(EntityExplodeEvent event) {
        event.blockList().forEach(placedBlocks::remove);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void movePlacedMarkers(BlockPistonExtendEvent event) {
        transferMovedMarkers(event.getBlocks(), event.getDirection());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void movePlacedMarkers(BlockPistonRetractEvent event) {
        transferMovedMarkers(event.getBlocks(), event.getDirection().getOppositeFace());
    }

    private void transferMovedMarkers(List<Block> movedBlocks, org.bukkit.block.BlockFace movement) {
        List<Block> marked = movedBlocks.stream().filter(placedBlocks::isMarked).toList();
        if (marked.isEmpty()) {
            return;
        }
        marked.forEach(placedBlocks::remove);
        plugin.getServer().getScheduler().runTask(plugin,
                () -> marked.forEach(block -> placedBlocks.mark(block.getRelative(movement))));
    }
}
