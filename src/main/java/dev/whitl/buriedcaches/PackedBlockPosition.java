package dev.whitl.buriedcaches;

public final class PackedBlockPosition {

    private static final int Y_BIAS = 2_048;

    private PackedBlockPosition() {
    }

    public static int pack(int localX, int y, int localZ) {
        if (localX < 0 || localX > 15 || localZ < 0 || localZ > 15) {
            throw new IllegalArgumentException("local coordinates must be from 0 to 15");
        }
        int storedY = y + Y_BIAS;
        if (storedY < 0 || storedY > 4_095) {
            throw new IllegalArgumentException("y must be from -2048 to 2047");
        }
        return (storedY << 8) | (localZ << 4) | localX;
    }

    public static int localX(int packed) {
        return packed & 0xF;
    }

    public static int localZ(int packed) {
        return (packed >>> 4) & 0xF;
    }

    public static int y(int packed) {
        return (packed >>> 8) - Y_BIAS;
    }
}
