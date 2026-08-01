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

    public TreasureSpawner(BuriedCachesPlugin plugin, TreasureLootService lootService,
            PlayerProgressStore progress) {
        this.plugin = plugin;
        this.lootService = lootService;
        this.progress = progress;
        this.markerKey = new NamespacedKey(plugin, "treasure_barrel");
        this.ownerKey = new NamespacedKey(plugin, "owner");
        this.lockUntilKey = new NamespacedKey(plugin, "lock_until");
        this.tierKey = new NamespacedKey(plugin, "tier");
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

        long lockUntil = System.currentTimeMillis()
                + plugin.getConfig().getLong("ownership.lock-seconds", 30L) * 1_000L;
        barrel.customName(Component.text(treasure.tier().displayName() + " Buried Cache",
                        treasure.tier().color())
                .decoration(TextDecoration.ITALIC, false));
        barrel.getPersistentDataContainer().set(markerKey, PersistentDataType.BYTE, (byte) 1);
        barrel.getPersistentDataContainer().set(ownerKey, PersistentDataType.STRING,
                player.getUniqueId().toString());
        barrel.getPersistentDataContainer().set(lockUntilKey, PersistentDataType.LONG, lockUntil);
        barrel.getPersistentDataContainer().set(tierKey, PersistentDataType.STRING, treasure.tier().id());
        List<ItemStack> overflow = fill(barrel.getInventory(), treasure.items());
        barrel.update(true, false);

        for (ItemStack stack : overflow) {
            player.getInventory().addItem(stack).values()
                    .forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
        }

        announce(player, minedLocation, treasure.tier(), false);
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
