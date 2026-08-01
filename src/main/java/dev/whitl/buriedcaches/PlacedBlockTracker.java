package dev.whitl.buriedcaches;

import org.bukkit.Chunk;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public final class PlacedBlockTracker {

    private final NamespacedKey placedKey;

    public PlacedBlockTracker(BuriedCachesPlugin plugin) {
        this.placedKey = new NamespacedKey(plugin, "placed_eligible_blocks");
    }

    public void mark(Block block) {
        Chunk chunk = block.getChunk();
        Set<Integer> positions = read(chunk);
        if (positions.add(pack(block))) {
            write(chunk, positions);
        }
    }

    public boolean isMarked(Block block) {
        return read(block.getChunk()).contains(pack(block));
    }

    public boolean consume(Block block) {
        Chunk chunk = block.getChunk();
        Set<Integer> positions = read(chunk);
        boolean removed = positions.remove(pack(block));
        if (removed) {
            write(chunk, positions);
        }
        return removed;
    }

    public void remove(Block block) {
        consume(block);
    }

    private Set<Integer> read(Chunk chunk) {
        int[] stored = chunk.getPersistentDataContainer()
                .get(placedKey, PersistentDataType.INTEGER_ARRAY);
        Set<Integer> positions = new HashSet<>();
        if (stored != null) {
            Arrays.stream(stored).forEach(positions::add);
        }
        return positions;
    }

    private void write(Chunk chunk, Set<Integer> positions) {
        PersistentDataContainer data = chunk.getPersistentDataContainer();
        if (positions.isEmpty()) {
            data.remove(placedKey);
            return;
        }
        int[] stored = positions.stream().mapToInt(Integer::intValue).sorted().toArray();
        data.set(placedKey, PersistentDataType.INTEGER_ARRAY, stored);
    }

    private int pack(Block block) {
        return PackedBlockPosition.pack(block.getX() & 15, block.getY(), block.getZ() & 15);
    }
}
