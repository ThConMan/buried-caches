package dev.whitl.buriedcaches;

import org.bukkit.Chunk;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Arrays;

/**
 * Records which eligible blocks a player placed so breaking them again never
 * counts toward a cache.
 *
 * <p>Positions live in the chunk's persistent data as a sorted int array and are
 * searched in place. Every eligible break consults this, so the hot path must not
 * box values or build collections: a chunk someone has built in can hold thousands
 * of entries, and Veinminer multiplies each swing by the size of the vein.
 */
public final class PlacedBlockTracker {

    private static final int[] EMPTY = new int[0];

    private final NamespacedKey placedKey;

    public PlacedBlockTracker(BuriedCachesPlugin plugin) {
        this.placedKey = new NamespacedKey(plugin, "placed_eligible_blocks");
    }

    public void mark(Block block) {
        Chunk chunk = block.getChunk();
        int[] stored = read(chunk);
        int[] updated = insert(stored, pack(block));
        if (updated != stored) {
            write(chunk, updated);
        }
    }

    public boolean isMarked(Block block) {
        return contains(read(block.getChunk()), pack(block));
    }

    public boolean consume(Block block) {
        Chunk chunk = block.getChunk();
        int[] stored = read(chunk);
        int[] updated = remove(stored, pack(block));
        if (updated == stored) {
            return false;
        }
        write(chunk, updated);
        return true;
    }

    public void remove(Block block) {
        consume(block);
    }

    /** Returns {@code sorted} unchanged when the value is already present. */
    static int[] insert(int[] sorted, int value) {
        int index = Arrays.binarySearch(sorted, value);
        if (index >= 0) {
            return sorted;
        }
        int insertion = -index - 1;
        int[] updated = new int[sorted.length + 1];
        System.arraycopy(sorted, 0, updated, 0, insertion);
        updated[insertion] = value;
        System.arraycopy(sorted, insertion, updated, insertion + 1, sorted.length - insertion);
        return updated;
    }

    /** Returns {@code sorted} unchanged when the value is absent. */
    static int[] remove(int[] sorted, int value) {
        int index = Arrays.binarySearch(sorted, value);
        if (index < 0) {
            return sorted;
        }
        int[] updated = new int[sorted.length - 1];
        System.arraycopy(sorted, 0, updated, 0, index);
        System.arraycopy(sorted, index + 1, updated, index, sorted.length - index - 1);
        return updated;
    }

    static boolean contains(int[] sorted, int value) {
        return Arrays.binarySearch(sorted, value) >= 0;
    }

    private int[] read(Chunk chunk) {
        int[] stored = chunk.getPersistentDataContainer()
                .get(placedKey, PersistentDataType.INTEGER_ARRAY);
        return stored == null ? EMPTY : stored;
    }

    private void write(Chunk chunk, int[] positions) {
        PersistentDataContainer data = chunk.getPersistentDataContainer();
        if (positions.length == 0) {
            data.remove(placedKey);
            return;
        }
        data.set(placedKey, PersistentDataType.INTEGER_ARRAY, positions);
    }

    private int pack(Block block) {
        return PackedBlockPosition.pack(block.getX() & 15, block.getY(), block.getZ() & 15);
    }
}
