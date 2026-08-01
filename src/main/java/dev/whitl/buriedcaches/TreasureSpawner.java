package dev.whitl.buriedcaches;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Barrel;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class TreasureSpawner {

    private final BuriedCachesPlugin plugin;
    private final TreasureLootService lootService;
    private final PlayerProgressStore progress;
    private final NamespacedKey markerKey;
    private final NamespacedKey ownerKey;
    private final NamespacedKey lockUntilKey;
    private final NamespacedKey tierKey;
    private final NamespacedKey expiresAtKey;

    public TreasureSpawner(BuriedCachesPlugin plugin, TreasureLootService lootService,
            PlayerProgressStore progress) {
        this.plugin = plugin;
        this.lootService = lootService;
        this.progress = progress;
        this.markerKey = new NamespacedKey(plugin, "treasure_barrel");
        this.ownerKey = new NamespacedKey(plugin, "owner");
        this.lockUntilKey = new NamespacedKey(plugin, "lock_until");
        this.tierKey = new NamespacedKey(plugin, "tier");
        this.expiresAtKey = new NamespacedKey(plugin, "expires_at");
    }

    public void spawn(Player player, Location minedLocation) {
        GeneratedTreasure treasure = lootService.generate(player, minedLocation);
        progress.recordFind(player, treasure.tier());
        Block block = minedLocation.getBlock();
        if (!block.getType().isAir()) {
            deliverFallback(player, treasure);
            return;
        }

        block.setType(Material.BARREL, false);
        if (!(block.getState() instanceof Barrel barrel)) {
            block.setType(Material.AIR, false);
            deliverFallback(player, treasure);
            return;
        }

        long now = System.currentTimeMillis();
        long lockUntil = now + plugin.lockMillis();
        CacheExpiryPolicy expiry = plugin.expiryPolicy();
        barrel.customName(Component.text(treasure.tier().displayName() + " Buried Cache",
                        treasure.tier().color())
                .decoration(TextDecoration.ITALIC, false));
        barrel.getPersistentDataContainer().set(markerKey, PersistentDataType.BYTE, (byte) 1);
        barrel.getPersistentDataContainer().set(ownerKey, PersistentDataType.STRING,
                player.getUniqueId().toString());
        barrel.getPersistentDataContainer().set(lockUntilKey, PersistentDataType.LONG, lockUntil);
        barrel.getPersistentDataContainer().set(tierKey, PersistentDataType.STRING, treasure.tier().id());
        if (expiry.enabled()) {
            barrel.getPersistentDataContainer().set(expiresAtKey, PersistentDataType.LONG,
                    expiry.expiresAt(now));
        }
        // Fill the snapshot inventory, not the live one: update() writes the
        // snapshot back to the block, so items placed in the live inventory
        // before update() would be wiped by the empty snapshot.
        List<ItemStack> overflow = fill(barrel.getSnapshotInventory(), treasure.items());
        barrel.update(true, false);

        for (ItemStack stack : overflow) {
            player.getInventory().addItem(stack).values()
                    .forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
        }

        if (expiry.enabled()) {
            scheduleExpiry(block, expiry.expiryMillis());
        }
        announce(player, minedLocation, treasure.tier(), false);
    }

    /**
     * Clears the expiry stamp once a player has opened the cache. From then on the
     * barrel is ordinary storage and only the empty-barrel cleanup can remove it.
     */
    public void markOpened(Barrel barrel) {
        if (!isTreasure(barrel)
                || !barrel.getPersistentDataContainer().has(expiresAtKey, PersistentDataType.LONG)) {
            return;
        }
        barrel.getPersistentDataContainer().remove(expiresAtKey);
        barrel.update(true, false);
    }

    public boolean isExpired(Barrel barrel, long nowMillis) {
        if (!isTreasure(barrel)) {
            return false;
        }
        long expiresAt = barrel.getPersistentDataContainer()
                .getOrDefault(expiresAtKey, PersistentDataType.LONG, 0L);
        return plugin.expiryPolicy().isExpired(expiresAt, nowMillis);
    }

    /** Removes the barrel only if it is still an unopened cache that is past due. */
    public boolean removeIfExpired(Block block) {
        if (block.getType() != Material.BARREL || !(block.getState() instanceof Barrel barrel)
                || !isExpired(barrel, System.currentTimeMillis())) {
            return false;
        }
        block.setType(Material.AIR, false);
        return true;
    }

    private void scheduleExpiry(Block block, long expiryMillis) {
        long ticks = Math.max(1L, expiryMillis / 50L);
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> removeIfExpired(block), ticks);
    }

    public boolean isTreasure(Barrel barrel) {
        return barrel.getPersistentDataContainer().has(markerKey, PersistentDataType.BYTE);
    }

    public boolean isLockedFor(Barrel barrel, UUID playerId) {
        if (!isTreasure(barrel)) {
            return false;
        }
        String owner = barrel.getPersistentDataContainer().get(ownerKey, PersistentDataType.STRING);
        long lockUntil = barrel.getPersistentDataContainer()
                .getOrDefault(lockUntilKey, PersistentDataType.LONG, 0L);
        return owner != null && !owner.equals(playerId.toString())
                && System.currentTimeMillis() < lockUntil;
    }

    public boolean isLocked(Barrel barrel) {
        return isTreasure(barrel) && System.currentTimeMillis() < barrel.getPersistentDataContainer()
                .getOrDefault(lockUntilKey, PersistentDataType.LONG, 0L);
    }

    public long lockRemainingSeconds(Barrel barrel) {
        long lockUntil = barrel.getPersistentDataContainer()
                .getOrDefault(lockUntilKey, PersistentDataType.LONG, 0L);
        return Math.max(0L, (lockUntil - System.currentTimeMillis() + 999L) / 1_000L);
    }

    private List<ItemStack> fill(Inventory inventory, List<ItemStack> loot) {
        List<Integer> slots = new ArrayList<>();
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            slots.add(slot);
        }
        Collections.shuffle(slots, ThreadLocalRandom.current());
        int slotIndex = 0;
        List<ItemStack> overflow = new ArrayList<>();
        for (ItemStack stack : loot) {
            if (slotIndex >= slots.size()) {
                overflow.add(stack);
                continue;
            }
            inventory.setItem(slots.get(slotIndex++), stack);
        }
        return overflow;
    }

    private void deliverFallback(Player player, GeneratedTreasure treasure) {
        for (ItemStack stack : treasure.items()) {
            player.getInventory().addItem(stack).values()
                    .forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
        }
        announce(player, player.getLocation(), treasure.tier(), true);
    }

    private void announce(Player player, Location location, TreasureTier tier, boolean inventoryFallback) {
        String suffix = inventoryFallback
                ? " The space closed, so its loot was delivered to you."
                : " It is yours for a short time.";
        player.sendMessage(Component.text("You uncovered a ", NamedTextColor.GOLD)
                .append(Component.text(tier.displayName() + " Buried Cache", tier.color()))
                .append(Component.text("!" + suffix, NamedTextColor.GOLD)));
        tier.celebration().play(player, location);
        if (tier.broadcast()) {
            plugin.getServer().broadcast(Component.text(player.getName() + " uncovered a ",
                            NamedTextColor.GOLD)
                    .append(Component.text(tier.displayName() + " Buried Cache", tier.color()))
                    .append(Component.text(" while mining!", NamedTextColor.GOLD)));
        }
    }
}
