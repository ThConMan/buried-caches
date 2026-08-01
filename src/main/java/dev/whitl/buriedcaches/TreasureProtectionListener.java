package dev.whitl.buriedcaches;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.block.Barrel;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public final class TreasureProtectionListener implements Listener {

    private final BuriedCachesPlugin plugin;
    private final TreasureSpawner spawner;

    public TreasureProtectionListener(BuriedCachesPlugin plugin, TreasureSpawner spawner) {
        this.plugin = plugin;
        this.spawner = spawner;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void protectOpening(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        if (block == null || !(block.getState() instanceof Barrel barrel)) {
            return;
        }
        if (spawner.isLockedFor(barrel, event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(Component.text(
                    "That buried cache belongs to its finder for another "
                            + spawner.lockRemainingSeconds(barrel) + "s.", NamedTextColor.RED));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void protectBreaking(BlockBreakEvent event) {
        if (!(event.getBlock().getState() instanceof Barrel barrel)) {
            return;
        }
        if (spawner.isLockedFor(barrel, event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(Component.text(
                    "That buried cache belongs to its finder for another "
                            + spawner.lockRemainingSeconds(barrel) + "s.", NamedTextColor.RED));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void protectHoppers(InventoryMoveItemEvent event) {
        if (event.getSource().getHolder() instanceof Barrel barrel && spawner.isLocked(barrel)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void protectExplosion(BlockExplodeEvent event) {
        event.blockList().removeIf(this::isLockedTreasure);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void protectExplosion(EntityExplodeEvent event) {
        event.blockList().removeIf(this::isLockedTreasure);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void removeEmptyTreasure(InventoryCloseEvent event) {
        if (!plugin.getConfig().getBoolean("ownership.remove-empty-barrels", true)
                || !(event.getInventory().getHolder() instanceof Barrel barrel)
                || !spawner.isTreasure(barrel)) {
            return;
        }
        Block block = barrel.getBlock();
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (block.getType() == Material.BARREL && block.getState() instanceof Barrel current
                    && spawner.isTreasure(current) && current.getInventory().isEmpty()) {
                block.setType(Material.AIR, false);
            }
        });
    }

    private boolean isLockedTreasure(Block block) {
        return block.getState() instanceof Barrel barrel && spawner.isLocked(barrel);
    }
}
